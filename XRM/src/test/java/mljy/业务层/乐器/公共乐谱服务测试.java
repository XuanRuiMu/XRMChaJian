package mljy.业务层.乐器;

import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.公共乐谱;
import mljy.领域层.乐器.公共乐谱.审核状态;
import mljy.领域层.乐器.音符;
import mljy.基础设施层.乐器.公共乐谱库仓储;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * FP-18 公共乐谱服务测试。
 * 验证上传/下载/评分/审核/删除/列出业务逻辑。
 * 仓储使用真实实现（YAML 文件 + @TempDir），不 mock，以验证端到端行为。
 */
@DisplayName("FP-18: 公共乐谱服务")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 公共乐谱服务测试 {

    @Mock
    private JavaPlugin 插件mock;

    @TempDir
    private File 临时目录;

    private 公共乐谱库仓储 仓储;
    private 公共乐谱服务 服务;

    @BeforeEach
    void setUp() {
        when(插件mock.getDataFolder()).thenReturn(临时目录);
        仓储 = new 公共乐谱库仓储(插件mock);
        服务 = new 公共乐谱服务(仓储);
    }

    private 乐谱 创建测试乐谱(String 名称) {
        return new 乐谱(名称, UUID.randomUUID(), "作者",
                System.currentTimeMillis(), 120,
                List.of(音符.of(0, 4, 1.0), 音符.of(7, 4, 0.8)));
    }

    private String 上传已通过乐谱(String 名称) {
        AtomicReference<String> 乐谱ID引用 = new AtomicReference<>();
        服务.上传(创建测试乐谱(名称), UUID.randomUUID(), "上传者", null, 乐谱ID引用::set);
        String 乐谱ID = 乐谱ID引用.get();
        assertNotNull(乐谱ID);
        服务.审核(乐谱ID, 审核状态.已通过);
        return 乐谱ID;
    }

    @Nested
    @DisplayName("上传")
    class 上传测试 {

        @Test
        @DisplayName("正常上传应返回成功并生成乐谱ID")
        void 正常上传返回成功() {
            乐谱 原乐谱 = 创建测试乐谱("我的乐谱");
            UUID 上传者 = UUID.randomUUID();
            AtomicReference<String> 乐谱ID引用 = new AtomicReference<>();

            公共乐谱服务.上传结果 结果 = 服务.上传(原乐谱, 上传者, "上传者A", null, 乐谱ID引用::set);

            assertEquals(公共乐谱服务.上传结果.成功, 结果);
            assertNotNull(乐谱ID引用.get());
            assertEquals(8, 乐谱ID引用.get().length());

            Optional<公共乐谱> 加载的 = 服务.加载(乐谱ID引用.get());
            assertTrue(加载的.isPresent());
            assertEquals("我的乐谱", 加载的.get().获取原乐谱().获取名称());
            assertEquals(上传者, 加载的.get().获取上传者标识());
            assertEquals("上传者A", 加载的.get().获取上传者名());
            assertEquals(审核状态.待审核, 加载的.get().获取状态());
        }

        @Test
        @DisplayName("公开名为空时应使用原乐谱名")
        void 公开名为空时使用原乐谱名() {
            乐谱 原乐谱 = 创建测试乐谱("原乐谱名");
            AtomicReference<String> 乐谱ID引用 = new AtomicReference<>();

            服务.上传(原乐谱, UUID.randomUUID(), "上传者", null, 乐谱ID引用::set);

            Optional<公共乐谱> 加载的 = 服务.加载(乐谱ID引用.get());
            assertTrue(加载的.isPresent());
            assertEquals("原乐谱名", 加载的.get().获取原乐谱().获取名称());
        }

        @Test
        @DisplayName("公开名非空时应使用公开名")
        void 公开名非空时使用公开名() {
            乐谱 原乐谱 = 创建测试乐谱("原乐谱名");
            AtomicReference<String> 乐谱ID引用 = new AtomicReference<>();

            服务.上传(原乐谱, UUID.randomUUID(), "上传者", "公开乐谱名", 乐谱ID引用::set);

            Optional<公共乐谱> 加载的 = 服务.加载(乐谱ID引用.get());
            assertTrue(加载的.isPresent());
            assertEquals("公开乐谱名", 加载的.get().获取原乐谱().获取名称());
        }

        @Test
        @DisplayName("原乐谱为 null 应返回参数无效")
        void 原乐谱null返回参数无效() {
            AtomicReference<String> 乐谱ID引用 = new AtomicReference<>();
            公共乐谱服务.上传结果 结果 = 服务.上传(null, UUID.randomUUID(), "上传者", null, 乐谱ID引用::set);
            assertEquals(公共乐谱服务.上传结果.参数无效, 结果);
            assertNull(乐谱ID引用.get());
        }

        @Test
        @DisplayName("上传者标识为 null 应返回参数无效")
        void 上传者标识null返回参数无效() {
            AtomicReference<String> 乐谱ID引用 = new AtomicReference<>();
            公共乐谱服务.上传结果 结果 = 服务.上传(创建测试乐谱("测试"), null, "上传者", null, 乐谱ID引用::set);
            assertEquals(公共乐谱服务.上传结果.参数无效, 结果);
        }

        @Test
        @DisplayName("公开名为空白时应使用原乐谱名")
        void 公开名空白时使用原乐谱名() {
            乐谱 原乐谱 = 创建测试乐谱("原名");
            AtomicReference<String> 乐谱ID引用 = new AtomicReference<>();

            服务.上传(原乐谱, UUID.randomUUID(), "上传者", "   ", 乐谱ID引用::set);

            Optional<公共乐谱> 加载的 = 服务.加载(乐谱ID引用.get());
            assertTrue(加载的.isPresent());
            assertEquals("原名", 加载的.get().获取原乐谱().获取名称());
        }
    }

    @Nested
    @DisplayName("下载")
    class 下载测试 {

        @Test
        @DisplayName("下载已通过乐谱应返回成功并增加下载计数")
        void 下载已通过乐谱成功() {
            String 乐谱ID = 上传已通过乐谱("可下载");
            AtomicReference<乐谱> 乐谱引用 = new AtomicReference<>();

            公共乐谱服务.下载结果 结果 = 服务.下载(乐谱ID, UUID.randomUUID(), 乐谱引用::set);

            assertEquals(公共乐谱服务.下载结果.成功, 结果);
            assertNotNull(乐谱引用.get());
            assertEquals("可下载", 乐谱引用.get().获取名称());

            Optional<公共乐谱> 重新加载 = 服务.加载(乐谱ID);
            assertTrue(重新加载.isPresent());
            assertEquals(1L, 重新加载.get().获取下载次数());
        }

        @Test
        @DisplayName("下载待审核乐谱应返回未通过审核")
        void 下载待审核返回未通过审核() {
            AtomicReference<String> 乐谱ID引用 = new AtomicReference<>();
            服务.上传(创建测试乐谱("待审核"), UUID.randomUUID(), "上传者", null, 乐谱ID引用::set);

            AtomicReference<乐谱> 乐谱引用 = new AtomicReference<>();
            公共乐谱服务.下载结果 结果 = 服务.下载(乐谱ID引用.get(), UUID.randomUUID(), 乐谱引用::set);

            assertEquals(公共乐谱服务.下载结果.未通过审核, 结果);
            assertNull(乐谱引用.get());
        }

        @Test
        @DisplayName("下载不存在的乐谱应返回不存在")
        void 下载不存在返回不存在() {
            AtomicReference<乐谱> 乐谱引用 = new AtomicReference<>();
            公共乐谱服务.下载结果 结果 = 服务.下载("nonexist", UUID.randomUUID(), 乐谱引用::set);
            assertEquals(公共乐谱服务.下载结果.不存在, 结果);
            assertNull(乐谱引用.get());
        }

        @Test
        @DisplayName("乐谱ID为 null 应返回参数无效")
        void 乐谱IDnull返回参数无效() {
            AtomicReference<乐谱> 乐谱引用 = new AtomicReference<>();
            公共乐谱服务.下载结果 结果 = 服务.下载(null, UUID.randomUUID(), 乐谱引用::set);
            assertEquals(公共乐谱服务.下载结果.参数无效, 结果);
        }

        @Test
        @DisplayName("乐谱ID为空字符串应返回参数无效")
        void 乐谱ID空字符串返回参数无效() {
            AtomicReference<乐谱> 乐谱引用 = new AtomicReference<>();
            公共乐谱服务.下载结果 结果 = 服务.下载("", UUID.randomUUID(), 乐谱引用::set);
            assertEquals(公共乐谱服务.下载结果.参数无效, 结果);
        }

        @Test
        @DisplayName("下载者标识为 null 应返回参数无效")
        void 下载者标识null返回参数无效() {
            String 乐谱ID = 上传已通过乐谱("可下载");
            AtomicReference<乐谱> 乐谱引用 = new AtomicReference<>();
            公共乐谱服务.下载结果 结果 = 服务.下载(乐谱ID, null, 乐谱引用::set);
            assertEquals(公共乐谱服务.下载结果.参数无效, 结果);
        }
    }

    @Nested
    @DisplayName("评分")
    class 评分测试 {

        @Test
        @DisplayName("评分已通过乐谱应返回成功并更新聚合")
        void 评分已通过乐谱成功() {
            String 乐谱ID = 上传已通过乐谱("可评分");
            UUID 评分者 = UUID.randomUUID();

            公共乐谱服务.评分结果 结果 = 服务.评分(乐谱ID, 评分者, 4);

            assertEquals(公共乐谱服务.评分结果.成功, 结果);
            Optional<公共乐谱> 重新加载 = 服务.加载(乐谱ID);
            assertTrue(重新加载.isPresent());
            assertEquals(4L, 重新加载.get().获取评分总和());
            assertEquals(1L, 重新加载.get().获取评分人数());
            assertEquals(4.0, 重新加载.get().获取平均评分(), 0.001);
        }

        @Test
        @DisplayName("多玩家评分应累计聚合")
        void 多玩家评分累计() {
            String 乐谱ID = 上传已通过乐谱("可评分");
            服务.评分(乐谱ID, UUID.randomUUID(), 3);
            服务.评分(乐谱ID, UUID.randomUUID(), 5);

            Optional<公共乐谱> 重新加载 = 服务.加载(乐谱ID);
            assertTrue(重新加载.isPresent());
            assertEquals(8L, 重新加载.get().获取评分总和());
            assertEquals(2L, 重新加载.get().获取评分人数());
            assertEquals(4.0, 重新加载.get().获取平均评分(), 0.001);
        }

        @Test
        @DisplayName("同一玩家重复评分应返回已评分且不更新聚合")
        void 同一玩家重复评分返回已评分() {
            String 乐谱ID = 上传已通过乐谱("可评分");
            UUID 评分者 = UUID.randomUUID();
            服务.评分(乐谱ID, 评分者, 4);

            公共乐谱服务.评分结果 第二次 = 服务.评分(乐谱ID, 评分者, 5);

            assertEquals(公共乐谱服务.评分结果.已评分, 第二次);
            Optional<公共乐谱> 重新加载 = 服务.加载(乐谱ID);
            assertTrue(重新加载.isPresent());
            assertEquals(4L, 重新加载.get().获取评分总和());
            assertEquals(1L, 重新加载.get().获取评分人数());
        }

        @Test
        @DisplayName("评分低于下限应返回评分越界")
        void 评分低于下限返回越界() {
            String 乐谱ID = 上传已通过乐谱("可评分");
            公共乐谱服务.评分结果 结果 = 服务.评分(乐谱ID, UUID.randomUUID(), 0);
            assertEquals(公共乐谱服务.评分结果.评分越界, 结果);
        }

        @Test
        @DisplayName("评分高于上限应返回评分越界")
        void 评分高于上限返回越界() {
            String 乐谱ID = 上传已通过乐谱("可评分");
            公共乐谱服务.评分结果 结果 = 服务.评分(乐谱ID, UUID.randomUUID(), 6);
            assertEquals(公共乐谱服务.评分结果.评分越界, 结果);
        }

        @Test
        @DisplayName("评分不存在的乐谱应返回不存在")
        void 评分不存在返回不存在() {
            公共乐谱服务.评分结果 结果 = 服务.评分("nonexist", UUID.randomUUID(), 3);
            assertEquals(公共乐谱服务.评分结果.不存在, 结果);
        }

        @Test
        @DisplayName("评分待审核乐谱应返回未通过审核")
        void 评分待审核返回未通过审核() {
            AtomicReference<String> 乐谱ID引用 = new AtomicReference<>();
            服务.上传(创建测试乐谱("待审核"), UUID.randomUUID(), "上传者", null, 乐谱ID引用::set);

            公共乐谱服务.评分结果 结果 = 服务.评分(乐谱ID引用.get(), UUID.randomUUID(), 3);
            assertEquals(公共乐谱服务.评分结果.未通过审核, 结果);
        }

        @Test
        @DisplayName("乐谱ID为 null 应返回参数无效")
        void 乐谱IDnull返回参数无效() {
            公共乐谱服务.评分结果 结果 = 服务.评分(null, UUID.randomUUID(), 3);
            assertEquals(公共乐谱服务.评分结果.参数无效, 结果);
        }

        @Test
        @DisplayName("评分者标识为 null 应返回参数无效")
        void 评分者标识null返回参数无效() {
            String 乐谱ID = 上传已通过乐谱("可评分");
            公共乐谱服务.评分结果 结果 = 服务.评分(乐谱ID, null, 3);
            assertEquals(公共乐谱服务.评分结果.参数无效, 结果);
        }
    }

    @Nested
    @DisplayName("审核")
    class 审核测试 {

        @Test
        @DisplayName("待审核乐谱通过审核应返回成功")
        void 通过审核成功() {
            AtomicReference<String> 乐谱ID引用 = new AtomicReference<>();
            服务.上传(创建测试乐谱("待审核"), UUID.randomUUID(), "上传者", null, 乐谱ID引用::set);

            公共乐谱服务.审核结果 结果 = 服务.审核(乐谱ID引用.get(), 审核状态.已通过);

            assertEquals(公共乐谱服务.审核结果.成功, 结果);
            Optional<公共乐谱> 加载的 = 服务.加载(乐谱ID引用.get());
            assertTrue(加载的.isPresent());
            assertEquals(审核状态.已通过, 加载的.get().获取状态());
        }

        @Test
        @DisplayName("待审核乐谱拒绝审核应返回成功")
        void 拒绝审核成功() {
            AtomicReference<String> 乐谱ID引用 = new AtomicReference<>();
            服务.上传(创建测试乐谱("待审核"), UUID.randomUUID(), "上传者", null, 乐谱ID引用::set);

            公共乐谱服务.审核结果 结果 = 服务.审核(乐谱ID引用.get(), 审核状态.已拒绝);

            assertEquals(公共乐谱服务.审核结果.成功, 结果);
            assertEquals(审核状态.已拒绝, 服务.加载(乐谱ID引用.get()).get().获取状态());
        }

        @Test
        @DisplayName("已通过乐谱再次审核应返回状态无效")
        void 已通过再次审核返回状态无效() {
            String 乐谱ID = 上传已通过乐谱("已通过");

            公共乐谱服务.审核结果 结果 = 服务.审核(乐谱ID, 审核状态.已拒绝);

            assertEquals(公共乐谱服务.审核结果.状态无效, 结果);
            assertEquals(审核状态.已通过, 服务.加载(乐谱ID).get().获取状态());
        }

        @Test
        @DisplayName("目标状态为待审核应返回状态无效")
        void 目标状态待审核返回状态无效() {
            AtomicReference<String> 乐谱ID引用 = new AtomicReference<>();
            服务.上传(创建测试乐谱("待审核"), UUID.randomUUID(), "上传者", null, 乐谱ID引用::set);

            公共乐谱服务.审核结果 结果 = 服务.审核(乐谱ID引用.get(), 审核状态.待审核);

            assertEquals(公共乐谱服务.审核结果.状态无效, 结果);
        }

        @Test
        @DisplayName("审核不存在的乐谱应返回不存在")
        void 审核不存在返回不存在() {
            公共乐谱服务.审核结果 结果 = 服务.审核("nonexist", 审核状态.已通过);
            assertEquals(公共乐谱服务.审核结果.不存在, 结果);
        }

        @Test
        @DisplayName("乐谱ID为 null 应返回参数无效")
        void 乐谱IDnull返回参数无效() {
            公共乐谱服务.审核结果 结果 = 服务.审核(null, 审核状态.已通过);
            assertEquals(公共乐谱服务.审核结果.参数无效, 结果);
        }

        @Test
        @DisplayName("新状态为 null 应返回参数无效")
        void 新状态null返回参数无效() {
            公共乐谱服务.审核结果 结果 = 服务.审核("anyscore", null);
            assertEquals(公共乐谱服务.审核结果.参数无效, 结果);
        }
    }

    @Nested
    @DisplayName("删除")
    class 删除测试 {

        @Test
        @DisplayName("删除已存在乐谱应返回 true 且文件被删除")
        void 删除已存在返回true() {
            String 乐谱ID = 上传已通过乐谱("待删除");

            assertTrue(服务.删除(乐谱ID));
            assertTrue(服务.加载(乐谱ID).isEmpty());
        }

        @Test
        @DisplayName("删除不存在的乐谱应返回 false")
        void 删除不存在返回false() {
            assertFalse(服务.删除("nonexist"));
        }

        @Test
        @DisplayName("删除空ID 应返回 false")
        void 删除空ID返回false() {
            assertFalse(服务.删除(""));
        }

        @Test
        @DisplayName("删除 null ID 应返回 false")
        void 删除null返回false() {
            assertFalse(服务.删除(null));
        }
    }

    @Nested
    @DisplayName("列出")
    class 列出测试 {

        @Test
        @DisplayName("列出已通过 应只返回已通过乐谱")
        void 列出已通过只返回已通过的() {
            String 通过1 = 上传已通过乐谱("通过1");
            String 通过2 = 上传已通过乐谱("通过2");
            AtomicReference<String> 待审核ID = new AtomicReference<>();
            服务.上传(创建测试乐谱("待审核"), UUID.randomUUID(), "上传者", null, 待审核ID::set);

            List<公共乐谱> 结果 = 服务.列出已通过();

            assertEquals(2, 结果.size());
            assertTrue(结果.stream().anyMatch(p -> p.获取乐谱ID().equals(通过1)));
            assertTrue(结果.stream().anyMatch(p -> p.获取乐谱ID().equals(通过2)));
            assertTrue(结果.stream().noneMatch(p -> p.获取乐谱ID().equals(待审核ID.get())));
        }

        @Test
        @DisplayName("列出待审核 应只返回待审核乐谱")
        void 列出待审核只返回待审核的() {
            AtomicReference<String> 待审核1 = new AtomicReference<>();
            AtomicReference<String> 待审核2 = new AtomicReference<>();
            服务.上传(创建测试乐谱("待审核1"), UUID.randomUUID(), "上传者", null, 待审核1::set);
            服务.上传(创建测试乐谱("待审核2"), UUID.randomUUID(), "上传者", null, 待审核2::set);
            String 通过 = 上传已通过乐谱("已通过");

            List<公共乐谱> 结果 = 服务.列出待审核();

            assertEquals(2, 结果.size());
            assertTrue(结果.stream().anyMatch(p -> p.获取乐谱ID().equals(待审核1.get())));
            assertTrue(结果.stream().anyMatch(p -> p.获取乐谱ID().equals(待审核2.get())));
            assertTrue(结果.stream().noneMatch(p -> p.获取乐谱ID().equals(通过)));
        }

        @Test
        @DisplayName("空库列出已通过 应返回空列表")
        void 空库列出已通过返回空() {
            List<公共乐谱> 结果 = 服务.列出已通过();
            assertTrue(结果.isEmpty());
        }

        @Test
        @DisplayName("空库列出待审核 应返回空列表")
        void 空库列出待审核返回空() {
            List<公共乐谱> 结果 = 服务.列出待审核();
            assertTrue(结果.isEmpty());
        }

        @Test
        @DisplayName("列出返回的列表应为不可修改")
        void 列表不可修改() {
            上传已通过乐谱("已通过");
            List<公共乐谱> 结果 = 服务.列出已通过();
            assertThrows(UnsupportedOperationException.class, () -> 结果.add(公共乐谱.创建("xx", 创建测试乐谱("x"), UUID.randomUUID(), "u")));
        }
    }

    @Nested
    @DisplayName("加载")
    class 加载测试 {

        @Test
        @DisplayName("加载已存在乐谱应返回 present")
        void 加载已存在返回present() {
            String 乐谱ID = 上传已通过乐谱("已存在");
            Optional<公共乐谱> 结果 = 服务.加载(乐谱ID);
            assertTrue(结果.isPresent());
            assertEquals("已存在", 结果.get().获取原乐谱().获取名称());
        }

        @Test
        @DisplayName("加载不存在的乐谱应返回 empty")
        void 加载不存在返回empty() {
            Optional<公共乐谱> 结果 = 服务.加载("nonexist");
            assertTrue(结果.isEmpty());
        }

        @Test
        @DisplayName("加载空ID 应返回 empty")
        void 加载空ID返回empty() {
            Optional<公共乐谱> 结果 = 服务.加载("");
            assertTrue(结果.isEmpty());
        }

        @Test
        @DisplayName("加载 null 应返回 empty")
        void 加载null返回empty() {
            Optional<公共乐谱> 结果 = 服务.加载(null);
            assertTrue(结果.isEmpty());
        }

        @Test
        @DisplayName("加载不应改变下载计数")
        void 加载不改变下载计数() {
            String 乐谱ID = 上传已通过乐谱("已存在");
            服务.下载(乐谱ID, UUID.randomUUID(), null);
            assertEquals(1L, 服务.加载(乐谱ID).get().获取下载次数());

            服务.加载(乐谱ID);
            assertEquals(1L, 服务.加载(乐谱ID).get().获取下载次数());
        }
    }

    @Nested
    @DisplayName("从配置加载")
    class 从配置加载测试 {

        @Test
        @DisplayName("加载自定义评分范围应覆盖默认")
        void 加载自定义评分范围() {
            org.bukkit.configuration.file.YamlConfiguration 配置 = new org.bukkit.configuration.file.YamlConfiguration();
            配置.set("公共乐谱库.评分下限", 2);
            配置.set("公共乐谱库.评分上限", 10);
            服务.从配置加载(配置);

            String 乐谱ID = 上传已通过乐谱("可评分");
            // 评分 1 应越界（下限 2）
            assertEquals(公共乐谱服务.评分结果.评分越界, 服务.评分(乐谱ID, UUID.randomUUID(), 1));
            // 评分 8 应成功（上限 10）
            assertEquals(公共乐谱服务.评分结果.成功, 服务.评分(乐谱ID, UUID.randomUUID(), 8));
        }

        @Test
        @DisplayName("加载自定义乐谱ID长度应覆盖默认")
        void 加载自定义乐谱ID长度() {
            org.bukkit.configuration.file.YamlConfiguration 配置 = new org.bukkit.configuration.file.YamlConfiguration();
            配置.set("公共乐谱库.乐谱ID长度", 12);
            服务.从配置加载(配置);

            AtomicReference<String> 乐谱ID引用 = new AtomicReference<>();
            服务.上传(创建测试乐谱("测试"), UUID.randomUUID(), "上传者", null, 乐谱ID引用::set);

            assertEquals(12, 乐谱ID引用.get().length());
        }

        @Test
        @DisplayName("ID长度过短应忽略使用默认")
        void ID长度过短忽略() {
            org.bukkit.configuration.file.YamlConfiguration 配置 = new org.bukkit.configuration.file.YamlConfiguration();
            配置.set("公共乐谱库.乐谱ID长度", 2);
            服务.从配置加载(配置);

            AtomicReference<String> 乐谱ID引用 = new AtomicReference<>();
            服务.上传(创建测试乐谱("测试"), UUID.randomUUID(), "上传者", null, 乐谱ID引用::set);

            assertEquals(8, 乐谱ID引用.get().length());
        }

        @Test
        @DisplayName("评分下限大于上限应忽略使用默认")
        void 评分下限大于上限忽略() {
            org.bukkit.configuration.file.YamlConfiguration 配置 = new org.bukkit.configuration.file.YamlConfiguration();
            配置.set("公共乐谱库.评分下限", 8);
            配置.set("公共乐谱库.评分上限", 3);
            服务.从配置加载(配置);

            String 乐谱ID = 上传已通过乐谱("可评分");
            // 默认范围 1-5 应保持
            assertEquals(公共乐谱服务.评分结果.成功, 服务.评分(乐谱ID, UUID.randomUUID(), 3));
            assertEquals(公共乐谱服务.评分结果.评分越界, 服务.评分(乐谱ID, UUID.randomUUID(), 6));
        }

        @Test
        @DisplayName("配置为 null 应保持默认值")
        void 配置null保持默认() {
            服务.从配置加载(null);

            String 乐谱ID = 上传已通过乐谱("可评分");
            // 默认范围 1-5 应保持
            assertEquals(公共乐谱服务.评分结果.评分越界, 服务.评分(乐谱ID, UUID.randomUUID(), 0));
            assertEquals(公共乐谱服务.评分结果.评分越界, 服务.评分(乐谱ID, UUID.randomUUID(), 6));
            assertEquals(公共乐谱服务.评分结果.成功, 服务.评分(乐谱ID, UUID.randomUUID(), 3));
        }

        @Test
        @DisplayName("配置无公共乐谱库段应保持默认值")
        void 配置无段保持默认() {
            org.bukkit.configuration.file.YamlConfiguration 配置 = new org.bukkit.configuration.file.YamlConfiguration();
            配置.set("其他段.键", 100);
            服务.从配置加载(配置);

            AtomicReference<String> 乐谱ID引用 = new AtomicReference<>();
            服务.上传(创建测试乐谱("测试"), UUID.randomUUID(), "上传者", null, 乐谱ID引用::set);
            assertEquals(8, 乐谱ID引用.get().length());
        }
    }
}
