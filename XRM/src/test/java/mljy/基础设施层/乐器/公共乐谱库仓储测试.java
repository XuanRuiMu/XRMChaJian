package mljy.基础设施层.乐器;

import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.公共乐谱;
import mljy.领域层.乐器.公共乐谱.审核状态;
import mljy.领域层.乐器.音符;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * FP-18 公共乐谱库仓储测试。
 * 验证 YAML 文件 CRUD 操作。
 */
@DisplayName("FP-18: 公共乐谱库仓储")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 公共乐谱库仓储测试 {

    @Mock
    private JavaPlugin 插件mock;

    @TempDir
    private File 临时目录;

    private 公共乐谱库仓储 仓储;

    @BeforeEach
    void setUp() {
        when(插件mock.getDataFolder()).thenReturn(临时目录);
        仓储 = new 公共乐谱库仓储(插件mock);
    }

    private 乐谱 创建测试乐谱(String 名称) {
        return new 乐谱(名称, UUID.randomUUID(), "作者",
                System.currentTimeMillis(), 120,
                List.of(音符.of(0, 4, 1.0), 音符.of(7, 4, 0.8)));
    }

    @Nested
    @DisplayName("保存与加载")
    class 保存与加载测试 {

        @Test
        @DisplayName("保存后应能加载到完整数据")
        void 保存后能加载() {
            公共乐谱 乐谱 = 公共乐谱.创建("abc12345", 创建测试乐谱("测试"), UUID.randomUUID(), "上传者");
            boolean 已保存 = 仓储.保存(乐谱);
            assertTrue(已保存);

            Optional<公共乐谱> 加载结果 = 仓储.加载("abc12345");
            assertTrue(加载结果.isPresent());
            公共乐谱 加载的 = 加载结果.get();
            assertEquals("abc12345", 加载的.获取乐谱ID());
            assertEquals("上传者", 加载的.获取上传者名());
            assertEquals(审核状态.待审核, 加载的.获取状态());
            assertEquals(0L, 加载的.获取下载次数());
            assertNotNull(加载的.获取原乐谱());
            assertEquals("测试", 加载的.获取原乐谱().获取名称());
            assertEquals(2, 加载的.获取原乐谱().获取音符数量());
        }

        @Test
        @DisplayName("保存修改后的状态应能正确加载")
        void 保存状态变更后能加载() {
            公共乐谱 乐谱 = 公共乐谱.创建("state01", 创建测试乐谱("测试"), UUID.randomUUID(), "上传者");
            仓储.保存(乐谱);

            乐谱.设置状态(审核状态.已通过);
            乐谱.增加评分(4);
            乐谱.增加下载();
            仓储.保存(乐谱);

            Optional<公共乐谱> 加载结果 = 仓储.加载("state01");
            assertTrue(加载结果.isPresent());
            assertEquals(审核状态.已通过, 加载结果.get().获取状态());
            assertEquals(4L, 加载结果.get().获取评分总和());
            assertEquals(1L, 加载结果.get().获取评分人数());
            assertEquals(1L, 加载结果.get().获取下载次数());
        }

        @Test
        @DisplayName("保存 null 应返回 false")
        void 保存null返回false() {
            assertFalse(仓储.保存(null));
        }

        @Test
        @DisplayName("加载不存在的ID 应返回 empty")
        void 加载不存在返回empty() {
            Optional<公共乐谱> 结果 = 仓储.加载("nonexist");
            assertTrue(结果.isEmpty());
        }

        @Test
        @DisplayName("加载空ID 应返回 empty")
        void 加载空ID返回empty() {
            Optional<公共乐谱> 结果 = 仓储.加载("");
            assertTrue(结果.isEmpty());
        }

        @Test
        @DisplayName("加载null ID 应返回 empty")
        void 加载null返回empty() {
            Optional<公共乐谱> 结果 = 仓储.加载(null);
            assertTrue(结果.isEmpty());
        }
    }

    @Nested
    @DisplayName("删除")
    class 删除测试 {

        @Test
        @DisplayName("删除已存在的乐谱应返回 true")
        void 删除已存在返回true() {
            公共乐谱 乐谱 = 公共乐谱.创建("delete01", 创建测试乐谱("测试"), UUID.randomUUID(), "上传者");
            仓储.保存(乐谱);
            assertTrue(仓储.删除("delete01"));
            assertTrue(仓储.加载("delete01").isEmpty());
        }

        @Test
        @DisplayName("删除不存在的乐谱应返回 false")
        void 删除不存在返回false() {
            assertFalse(仓储.删除("nonexist"));
        }

        @Test
        @DisplayName("删除空ID 应返回 false")
        void 删除空ID返回false() {
            assertFalse(仓储.删除(""));
        }
    }

    @Nested
    @DisplayName("列出")
    class 列出测试 {

        @Test
        @DisplayName("列出所有 应返回字典序排序的ID列表")
        void 列出所有字典序() {
            仓储.保存(公共乐谱.创建("zzz", 创建测试乐谱("A"), UUID.randomUUID(), "上传者1"));
            仓储.保存(公共乐谱.创建("aaa", 创建测试乐谱("B"), UUID.randomUUID(), "上传者2"));
            仓储.保存(公共乐谱.创建("mmm", 创建测试乐谱("C"), UUID.randomUUID(), "上传者3"));

            List<String> 列表 = 仓储.列出所有();
            assertEquals(3, 列表.size());
            assertEquals("aaa", 列表.get(0));
            assertEquals("mmm", 列表.get(1));
            assertEquals("zzz", 列表.get(2));
        }

        @Test
        @DisplayName("空库列出所有 应返回空列表")
        void 空库列出所有返回空() {
            List<String> 列表 = 仓储.列出所有();
            assertTrue(列表.isEmpty());
        }

        @Test
        @DisplayName("列出按状态 应只返回指定状态的乐谱")
        void 列出按状态() {
            公共乐谱 乐谱1 = 公共乐谱.创建("pass01", 创建测试乐谱("A"), UUID.randomUUID(), "上传者1");
            公共乐谱 乐谱2 = 公共乐谱.创建("wait01", 创建测试乐谱("B"), UUID.randomUUID(), "上传者2");
            公共乐谱 乐谱3 = 公共乐谱.创建("pass02", 创建测试乐谱("C"), UUID.randomUUID(), "上传者3");
            乐谱1.设置状态(审核状态.已通过);
            乐谱3.设置状态(审核状态.已通过);
            仓储.保存(乐谱1);
            仓储.保存(乐谱2);
            仓储.保存(乐谱3);

            List<String> 已通过 = 仓储.列出按状态(审核状态.已通过);
            assertEquals(2, 已通过.size());
            assertTrue(已通过.contains("pass01"));
            assertTrue(已通过.contains("pass02"));

            List<String> 待审核 = 仓储.列出按状态(审核状态.待审核);
            assertEquals(1, 待审核.size());
            assertEquals("wait01", 待审核.get(0));
        }
    }
}
