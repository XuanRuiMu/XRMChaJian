package mljy.领域层.乐器;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FP-19 分层键盘 split 配置测试。
 * 验证不可变值对象的：选择乐器逻辑 / 切换分层 / 设置分界点 / 设置左右乐器 / clamp 行为。
 */
@DisplayName("FP-19: 分层键盘 split 配置")
class 分层配置测试 {

    @Nested
    @DisplayName("默认配置")
    class 默认配置测试 {

        @Test
        @DisplayName("默认配置应为：未启用 / 分界点 12 / 左右乐器为空")
        void 默认配置应为未启用分界点12() {
            分层配置 配置 = 分层配置.默认配置();
            assertFalse(配置.是否启用分层());
            assertEquals(分层配置.默认分界点, 配置.获取分界点());
            assertTrue(配置.获取左乐器().isEmpty());
            assertTrue(配置.获取右乐器().isEmpty());
        }

        @Test
        @DisplayName("默认分界点应为 12")
        void 默认分界点应为12() {
            assertEquals(12, 分层配置.默认分界点);
        }

        @Test
        @DisplayName("分界点上下限应为 0 和 24")
        void 分界点上下限应为0和24() {
            assertEquals(0, 分层配置.分界点下限);
            assertEquals(24, 分层配置.分界点上限);
        }
    }

    @Nested
    @DisplayName("选择乐器")
    class 选择乐器测试 {

        @Test
        @DisplayName("未启用分层时应始终返回默认乐器")
        void 未启用分层应返回默认乐器() {
            分层配置 配置 = 分层配置.默认配置();
            乐器定义 默认 = 创建测试乐器("默认");
            乐器定义 选择 = 配置.选择乐器(5, 默认);
            assertSame(默认, 选择);
        }

        @Test
        @DisplayName("未启用分层时高音高也返回默认乐器")
        void 未启用分层高音高也返回默认乐器() {
            分层配置 配置 = 分层配置.默认配置();
            乐器定义 默认 = 创建测试乐器("默认");
            乐器定义 选择 = 配置.选择乐器(20, 默认);
            assertSame(默认, 选择);
        }

        @Test
        @DisplayName("启用分层但左右乐器为空时应回退到默认乐器")
        void 启用分层无配置时回退默认乐器() {
            分层配置 配置 = 分层配置.默认配置().切换分层();
            乐器定义 默认 = 创建测试乐器("默认");
            assertTrue(配置.是否启用分层());
            乐器定义 选择低 = 配置.选择乐器(5, 默认);
            assertSame(默认, 选择低);
            乐器定义 选择高 = 配置.选择乐器(20, 默认);
            assertSame(默认, 选择高);
        }

        @Test
        @DisplayName("启用分层时音高 < 分界点应返回左乐器")
        void 启用分层低音高返回左乐器() {
            乐器定义 默认 = 创建测试乐器("默认");
            乐器定义 左 = 创建测试乐器("左");
            乐器定义 右 = 创建测试乐器("右");
            分层配置 配置 = new 分层配置(true, 12, 左, 右);
            乐器定义 选择 = 配置.选择乐器(11, 默认);
            assertSame(左, 选择);
        }

        @Test
        @DisplayName("启用分层时音高 >= 分界点应返回右乐器")
        void 启用分层高音高返回右乐器() {
            乐器定义 默认 = 创建测试乐器("默认");
            乐器定义 左 = 创建测试乐器("左");
            乐器定义 右 = 创建测试乐器("右");
            分层配置 配置 = new 分层配置(true, 12, 左, 右);
            乐器定义 选择 = 配置.选择乐器(12, 默认);
            assertSame(右, 选择);
        }

        @Test
        @DisplayName("启用分层时音高 = 分界点-1 应返回左乐器")
        void 启用分层分界点减一返回左乐器() {
            乐器定义 默认 = 创建测试乐器("默认");
            乐器定义 左 = 创建测试乐器("左");
            乐器定义 右 = 创建测试乐器("右");
            分层配置 配置 = new 分层配置(true, 12, 左, 右);
            乐器定义 选择 = 配置.选择乐器(11, 默认);
            assertSame(左, 选择);
        }

        @Test
        @DisplayName("启用分层但左乐器为空时应回退到默认乐器")
        void 启用分层无左乐器回退默认() {
            乐器定义 默认 = 创建测试乐器("默认");
            乐器定义 右 = 创建测试乐器("右");
            分层配置 配置 = new 分层配置(true, 12, null, 右);
            乐器定义 选择 = 配置.选择乐器(5, 默认);
            assertSame(默认, 选择);
        }

        @Test
        @DisplayName("启用分层但右乐器为空时应回退到默认乐器")
        void 启用分层无右乐器回退默认() {
            乐器定义 默认 = 创建测试乐器("默认");
            乐器定义 左 = 创建测试乐器("左");
            分层配置 配置 = new 分层配置(true, 12, 左, null);
            乐器定义 选择 = 配置.选择乐器(15, 默认);
            assertSame(默认, 选择);
        }
    }

    @Nested
    @DisplayName("不可变性：切换/设置返回新实例")
    class 不可变性测试 {

        @Test
        @DisplayName("切换分层应返回新实例且原实例不变")
        void 切换分层应返回新实例() {
            分层配置 原配置 = 分层配置.默认配置();
            assertFalse(原配置.是否启用分层());
            分层配置 新配置 = 原配置.切换分层();
            assertNotSame(原配置, 新配置);
            assertTrue(新配置.是否启用分层());
            assertFalse(原配置.是否启用分层(), "原实例不应被修改");
        }

        @Test
        @DisplayName("设置分界点应返回新实例且原分界点不变")
        void 设置分界点应返回新实例() {
            分层配置 原配置 = new 分层配置(true, 12, null, null);
            分层配置 新配置 = 原配置.设置分界点(8);
            assertNotSame(原配置, 新配置);
            assertEquals(8, 新配置.获取分界点());
            assertEquals(12, 原配置.获取分界点(), "原实例分界点不应被修改");
        }

        @Test
        @DisplayName("设置左乐器应返回新实例且原左乐器不变")
        void 设置左乐器应返回新实例() {
            乐器定义 原左 = 创建测试乐器("原左");
            分层配置 原配置 = new 分层配置(true, 12, 原左, null);
            乐器定义 新左 = 创建测试乐器("新左");
            分层配置 新配置 = 原配置.设置左乐器(新左);
            assertNotSame(原配置, 新配置);
            assertSame(新左, 新配置.获取左乐器().orElse(null));
            assertSame(原左, 原配置.获取左乐器().orElse(null), "原实例左乐器不应被修改");
        }

        @Test
        @DisplayName("设置右乐器应返回新实例且原右乐器不变")
        void 设置右乐器应返回新实例() {
            乐器定义 原右 = 创建测试乐器("原右");
            分层配置 原配置 = new 分层配置(true, 12, null, 原右);
            乐器定义 新右 = 创建测试乐器("新右");
            分层配置 新配置 = 原配置.设置右乐器(新右);
            assertNotSame(原配置, 新配置);
            assertSame(新右, 新配置.获取右乐器().orElse(null));
            assertSame(原右, 原配置.获取右乐器().orElse(null), "原实例右乐器不应被修改");
        }

        @Test
        @DisplayName("连续切换两次应回到原状态")
        void 连续切换两次回到原状态() {
            分层配置 原配置 = 分层配置.默认配置();
            分层配置 一次 = 原配置.切换分层();
            分层配置 二次 = 一次.切换分层();
            assertNotSame(原配置, 二次);
            assertFalse(二次.是否启用分层());
            assertEquals(原配置.获取分界点(), 二次.获取分界点());
        }
    }

    @Nested
    @DisplayName("分界点 clamp")
    class 分界点Clamp测试 {

        @Test
        @DisplayName("分界点负数应 clamp 到 0")
        void 分界点负数应clamp到0() {
            分层配置 配置 = new 分层配置(true, -5, null, null);
            assertEquals(0, 配置.获取分界点());
        }

        @Test
        @DisplayName("分界点超过上限应 clamp 到 24")
        void 分界点超过上限应clamp到24() {
            分层配置 配置 = new 分层配置(true, 30, null, null);
            assertEquals(24, 配置.获取分界点());
        }

        @Test
        @DisplayName("分界点 0 应被接受")
        void 分界点0应被接受() {
            分层配置 配置 = new 分层配置(true, 0, null, null);
            assertEquals(0, 配置.获取分界点());
        }

        @Test
        @DisplayName("分界点 24 应被接受")
        void 分界点24应被接受() {
            分层配置 配置 = new 分层配置(true, 24, null, null);
            assertEquals(24, 配置.获取分界点());
        }

        @Test
        @DisplayName("设置分界点方法也应 clamp")
        void 设置分界点方法也应clamp() {
            分层配置 原配置 = 分层配置.默认配置();
            分层配置 新配置 = 原配置.设置分界点(100);
            assertEquals(24, 新配置.获取分界点());
        }
    }

    @Nested
    @DisplayName("获取左右乐器")
    class 获取左右乐器测试 {

        @Test
        @DisplayName("获取左乐器应返回 Optional")
        void 获取左乐器应返回Optional() {
            乐器定义 左 = 创建测试乐器("左");
            分层配置 配置 = new 分层配置(true, 12, 左, null);
            Optional<乐器定义> 结果 = 配置.获取左乐器();
            assertTrue(结果.isPresent());
            assertSame(左, 结果.get());
        }

        @Test
        @DisplayName("获取右乐器应返回 Optional")
        void 获取右乐器应返回Optional() {
            乐器定义 右 = 创建测试乐器("右");
            分层配置 配置 = new 分层配置(true, 12, null, 右);
            Optional<乐器定义> 结果 = 配置.获取右乐器();
            assertTrue(结果.isPresent());
            assertSame(右, 结果.get());
        }

        @Test
        @DisplayName("未配置时获取左乐器应返回空 Optional")
        void 未配置时获取左乐器应返回空() {
            分层配置 配置 = 分层配置.默认配置();
            assertTrue(配置.获取左乐器().isEmpty());
        }

        @Test
        @DisplayName("未配置时获取右乐器应返回空 Optional")
        void 未配置时获取右乐器应返回空() {
            分层配置 配置 = 分层配置.默认配置();
            assertTrue(配置.获取右乐器().isEmpty());
        }
    }

    /**
     * 创建测试用乐器定义。
     * 使用默认参数避免对真实 乐器注册表 的依赖。
     */
    private 乐器定义 创建测试乐器(String 名称) {
        return new 乐器定义(
                名称, 名称, Sound.BLOCK_NOTE_BLOCK_HARP, "xrm.test." + 名称,
                Material.STICK, 9000, 2, 0, 24, 0,
                true, "测试乐器 " + 名称, null, 乐器定义.物理机制_瞬时触发);
    }
}
