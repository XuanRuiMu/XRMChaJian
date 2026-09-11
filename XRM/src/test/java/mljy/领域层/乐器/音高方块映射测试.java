package mljy.领域层.乐器;

import org.bukkit.Color;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FP-02 25色方块音高显示体系测试。
 * 权威对照表见需求文档「功能点 2：25 色方块音高显示体系」。
 */
@DisplayName("FP-02: 25色方块音高显示体系")
class 音高方块映射测试 {

    @Nested
    @DisplayName("音高转方块: 25个音高完整对照表")
    class 音高转方块对照表 {

        @Test
        @DisplayName("音高0 F#=青色混凝土粉末")
        void 音高0() {
            assertEquals(Material.CYAN_CONCRETE_POWDER, 音高方块映射.音高转方块(0));
        }

        @Test
        @DisplayName("音高1 G=淡蓝色混凝土粉末")
        void 音高1() {
            assertEquals(Material.LIGHT_BLUE_CONCRETE_POWDER, 音高方块映射.音高转方块(1));
        }

        @Test
        @DisplayName("音高2 G#=蓝色混凝土粉末")
        void 音高2() {
            assertEquals(Material.BLUE_CONCRETE_POWDER, 音高方块映射.音高转方块(2));
        }

        @Test
        @DisplayName("音高3 A=紫色混凝土粉末")
        void 音高3() {
            assertEquals(Material.PURPLE_CONCRETE_POWDER, 音高方块映射.音高转方块(3));
        }

        @Test
        @DisplayName("音高4 A#=品红色混凝土粉末")
        void 音高4() {
            assertEquals(Material.MAGENTA_CONCRETE_POWDER, 音高方块映射.音高转方块(4));
        }

        @Test
        @DisplayName("音高5 B=粉红色混凝土粉末")
        void 音高5() {
            assertEquals(Material.PINK_CONCRETE_POWDER, 音高方块映射.音高转方块(5));
        }

        @Test
        @DisplayName("音高6 C=棕色混凝土")
        void 音高6() {
            assertEquals(Material.BROWN_CONCRETE, 音高方块映射.音高转方块(6));
        }

        @Test
        @DisplayName("音高7 C#=红色混凝土")
        void 音高7() {
            assertEquals(Material.RED_CONCRETE, 音高方块映射.音高转方块(7));
        }

        @Test
        @DisplayName("音高8 D=橙色混凝土")
        void 音高8() {
            assertEquals(Material.ORANGE_CONCRETE, 音高方块映射.音高转方块(8));
        }

        @Test
        @DisplayName("音高9 D#=黄色混凝土")
        void 音高9() {
            assertEquals(Material.YELLOW_CONCRETE, 音高方块映射.音高转方块(9));
        }

        @Test
        @DisplayName("音高10 E=黄绿色混凝土")
        void 音高10() {
            assertEquals(Material.LIME_CONCRETE, 音高方块映射.音高转方块(10));
        }

        @Test
        @DisplayName("音高11 F=绿色混凝土")
        void 音高11() {
            assertEquals(Material.GREEN_CONCRETE, 音高方块映射.音高转方块(11));
        }

        @Test
        @DisplayName("音高12 F#=青色混凝土")
        void 音高12() {
            assertEquals(Material.CYAN_CONCRETE, 音高方块映射.音高转方块(12));
        }

        @Test
        @DisplayName("音高13 G=淡蓝色混凝土")
        void 音高13() {
            assertEquals(Material.LIGHT_BLUE_CONCRETE, 音高方块映射.音高转方块(13));
        }

        @Test
        @DisplayName("音高14 G#=蓝色混凝土")
        void 音高14() {
            assertEquals(Material.BLUE_CONCRETE, 音高方块映射.音高转方块(14));
        }

        @Test
        @DisplayName("音高15 A=紫色混凝土")
        void 音高15() {
            assertEquals(Material.PURPLE_CONCRETE, 音高方块映射.音高转方块(15));
        }

        @Test
        @DisplayName("音高16 A#=品红色混凝土")
        void 音高16() {
            assertEquals(Material.MAGENTA_CONCRETE, 音高方块映射.音高转方块(16));
        }

        @Test
        @DisplayName("音高17 B=粉红色混凝土")
        void 音高17() {
            assertEquals(Material.PINK_CONCRETE, 音高方块映射.音高转方块(17));
        }

        @Test
        @DisplayName("音高18 C=棕色陶瓦")
        void 音高18() {
            assertEquals(Material.BROWN_TERRACOTTA, 音高方块映射.音高转方块(18));
        }

        @Test
        @DisplayName("音高19 C#=红色陶瓦")
        void 音高19() {
            assertEquals(Material.RED_TERRACOTTA, 音高方块映射.音高转方块(19));
        }

        @Test
        @DisplayName("音高20 D=橙色陶瓦")
        void 音高20() {
            assertEquals(Material.ORANGE_TERRACOTTA, 音高方块映射.音高转方块(20));
        }

        @Test
        @DisplayName("音高21 D#=黄色陶瓦")
        void 音高21() {
            assertEquals(Material.YELLOW_TERRACOTTA, 音高方块映射.音高转方块(21));
        }

        @Test
        @DisplayName("音高22 E=黄绿色陶瓦")
        void 音高22() {
            assertEquals(Material.LIME_TERRACOTTA, 音高方块映射.音高转方块(22));
        }

        @Test
        @DisplayName("音高23 F=绿色陶瓦")
        void 音高23() {
            assertEquals(Material.GREEN_TERRACOTTA, 音高方块映射.音高转方块(23));
        }

        @Test
        @DisplayName("音高24 F#=青色陶瓦")
        void 音高24() {
            assertEquals(Material.CYAN_TERRACOTTA, 音高方块映射.音高转方块(24));
        }
    }

    @Nested
    @DisplayName("音高转方块: 越界边界")
    class 越界边界 {

        @Test
        @DisplayName("音高-1 抛 IllegalArgumentException（与音符.java风格一致）")
        void 低于0抛异常() {
            assertThrows(IllegalArgumentException.class, () -> 音高方块映射.音高转方块(-1));
        }

        @Test
        @DisplayName("音高25 抛 IllegalArgumentException")
        void 高于24抛异常() {
            assertThrows(IllegalArgumentException.class, () -> 音高方块映射.音高转方块(25));
        }

        @Test
        @DisplayName("音高-100 抛 IllegalArgumentException")
        void 远低于0抛异常() {
            assertThrows(IllegalArgumentException.class, () -> 音高方块映射.音高转方块(-100));
        }

        @Test
        @DisplayName("音高100 抛 IllegalArgumentException")
        void 远高于24抛异常() {
            assertThrows(IllegalArgumentException.class, () -> 音高方块映射.音高转方块(100));
        }
    }

    @Nested
    @DisplayName("音高转音名")
    class 音名测试 {

        @Test
        @DisplayName("音高0 F#")
        void 音高0音名() {
            assertEquals("F#", 音高方块映射.音高转音名(0));
        }

        @Test
        @DisplayName("音高1 G")
        void 音高1音名() {
            assertEquals("G", 音高方块映射.音高转音名(1));
        }

        @Test
        @DisplayName("音高5 B")
        void 音高5音名() {
            assertEquals("B", 音高方块映射.音高转音名(5));
        }

        @Test
        @DisplayName("音高6 C")
        void 音高6音名() {
            assertEquals("C", 音高方块映射.音高转音名(6));
        }

        @Test
        @DisplayName("音高11 F")
        void 音高11音名() {
            assertEquals("F", 音高方块映射.音高转音名(11));
        }

        @Test
        @DisplayName("音高12 F#（八度循环颜色不变）")
        void 音高12音名() {
            assertEquals("F#", 音高方块映射.音高转音名(12));
        }

        @Test
        @DisplayName("音高7 C#")
        void 音高7音名() {
            assertEquals("C#", 音高方块映射.音高转音名(7));
        }

        @Test
        @DisplayName("音高-1 抛 IllegalArgumentException")
        void 音名越界() {
            assertThrows(IllegalArgumentException.class, () -> 音高方块映射.音高转音名(-1));
        }
    }

    @Nested
    @DisplayName("音高转唱名")
    class 唱名测试 {

        @Test
        @DisplayName("音高0 Fi")
        void 音高0唱名() {
            assertEquals("Fi", 音高方块映射.音高转唱名(0));
        }

        @Test
        @DisplayName("音高1 Sol")
        void 音高1唱名() {
            assertEquals("Sol", 音高方块映射.音高转唱名(1));
        }

        @Test
        @DisplayName("音高2 Si")
        void 音高2唱名() {
            assertEquals("Si", 音高方块映射.音高转唱名(2));
        }

        @Test
        @DisplayName("音高3 La")
        void 音高3唱名() {
            assertEquals("La", 音高方块映射.音高转唱名(3));
        }

        @Test
        @DisplayName("音高4 Li")
        void 音高4唱名() {
            assertEquals("Li", 音高方块映射.音高转唱名(4));
        }

        @Test
        @DisplayName("音高5 Ti")
        void 音高5唱名() {
            assertEquals("Ti", 音高方块映射.音高转唱名(5));
        }

        @Test
        @DisplayName("音高6 Do")
        void 音高6唱名() {
            assertEquals("Do", 音高方块映射.音高转唱名(6));
        }

        @Test
        @DisplayName("音高7 Di")
        void 音高7唱名() {
            assertEquals("Di", 音高方块映射.音高转唱名(7));
        }

        @Test
        @DisplayName("音高8 Re")
        void 音高8唱名() {
            assertEquals("Re", 音高方块映射.音高转唱名(8));
        }

        @Test
        @DisplayName("音高9 Ri")
        void 音高9唱名() {
            assertEquals("Ri", 音高方块映射.音高转唱名(9));
        }

        @Test
        @DisplayName("音高10 Mi")
        void 音高10唱名() {
            assertEquals("Mi", 音高方块映射.音高转唱名(10));
        }

        @Test
        @DisplayName("音高11 Fa")
        void 音高11唱名() {
            assertEquals("Fa", 音高方块映射.音高转唱名(11));
        }

        @Test
        @DisplayName("音高12 Fi（八度循环唱名不变）")
        void 音高12唱名() {
            assertEquals("Fi", 音高方块映射.音高转唱名(12));
        }

        @Test
        @DisplayName("音高-1 抛 IllegalArgumentException")
        void 唱名越界() {
            assertThrows(IllegalArgumentException.class, () -> 音高方块映射.音高转唱名(-1));
        }
    }

    @Nested
    @DisplayName("方块转音高: 反向查找")
    class 方块转音高测试 {

        @Test
        @DisplayName("青色混凝土粉末->0")
        void 反向0() {
            assertEquals(0, 音高方块映射.方块转音高(Material.CYAN_CONCRETE_POWDER));
        }

        @Test
        @DisplayName("棕色混凝土->6")
        void 反向6() {
            assertEquals(6, 音高方块映射.方块转音高(Material.BROWN_CONCRETE));
        }

        @Test
        @DisplayName("绿色混凝土->11")
        void 反向11() {
            assertEquals(11, 音高方块映射.方块转音高(Material.GREEN_CONCRETE));
        }

        @Test
        @DisplayName("青色混凝土->12")
        void 反向12() {
            assertEquals(12, 音高方块映射.方块转音高(Material.CYAN_CONCRETE));
        }

        @Test
        @DisplayName("青色陶瓦->24")
        void 反向24() {
            assertEquals(24, 音高方块映射.方块转音高(Material.CYAN_TERRACOTTA));
        }

        @Test
        @DisplayName("非音高方块返回-1")
        void 非映射方块返回负一() {
            assertEquals(-1, 音高方块映射.方块转音高(Material.DIRT));
        }

        @Test
        @DisplayName("null返回-1")
        void null返回负一() {
            assertEquals(-1, 音高方块映射.方块转音高(null));
        }
    }

    @Nested
    @DisplayName("正向反向一致性")
    class 正反向一致性 {

        @Test
        @DisplayName("全部25音高 正转方块再反转为音高 一致")
        void 全25音高往返一致() {
            for (int 音高 = 0; 音高 <= 24; 音高++) {
                Material 方块 = 音高方块映射.音高转方块(音高);
                assertEquals(音高, 音高方块映射.方块转音高(方块),
                        "音高" + 音高 + "往返不一致");
            }
        }
    }

    @Nested
    @DisplayName("音高转颜色: 12 音名固定映射（用于 Particle.DUST 演奏可视化反馈，修复 GAP-06）")
    class 音高转颜色测试 {

        @Test
        @DisplayName("音高0 F#=青色（与青色混凝土粉末方块同色）")
        void 音高0颜色() {
            assertEquals(Color.fromRGB(76, 127, 153), 音高方块映射.音高转颜色(0));
        }

        @Test
        @DisplayName("音高1 G=淡蓝色")
        void 音高1颜色() {
            assertEquals(Color.fromRGB(102, 153, 216), 音高方块映射.音高转颜色(1));
        }

        @Test
        @DisplayName("音高2 G#=蓝色")
        void 音高2颜色() {
            assertEquals(Color.fromRGB(51, 76, 178), 音高方块映射.音高转颜色(2));
        }

        @Test
        @DisplayName("音高3 A=紫色")
        void 音高3颜色() {
            assertEquals(Color.fromRGB(127, 63, 178), 音高方块映射.音高转颜色(3));
        }

        @Test
        @DisplayName("音高4 A#=品红色")
        void 音高4颜色() {
            assertEquals(Color.fromRGB(178, 76, 216), 音高方块映射.音高转颜色(4));
        }

        @Test
        @DisplayName("音高5 B=粉红色")
        void 音高5颜色() {
            assertEquals(Color.fromRGB(242, 127, 165), 音高方块映射.音高转颜色(5));
        }

        @Test
        @DisplayName("音高6 C=棕色")
        void 音高6颜色() {
            assertEquals(Color.fromRGB(102, 76, 51), 音高方块映射.音高转颜色(6));
        }

        @Test
        @DisplayName("音高7 C#=红色")
        void 音高7颜色() {
            assertEquals(Color.fromRGB(153, 51, 51), 音高方块映射.音高转颜色(7));
        }

        @Test
        @DisplayName("音高8 D=橙色")
        void 音高8颜色() {
            assertEquals(Color.fromRGB(216, 127, 51), 音高方块映射.音高转颜色(8));
        }

        @Test
        @DisplayName("音高9 D#=黄色")
        void 音高9颜色() {
            assertEquals(Color.fromRGB(229, 229, 51), 音高方块映射.音高转颜色(9));
        }

        @Test
        @DisplayName("音高10 E=黄绿色")
        void 音高10颜色() {
            assertEquals(Color.fromRGB(127, 204, 25), 音高方块映射.音高转颜色(10));
        }

        @Test
        @DisplayName("音高11 F=绿色")
        void 音高11颜色() {
            assertEquals(Color.fromRGB(102, 127, 51), 音高方块映射.音高转颜色(11));
        }

        @Test
        @DisplayName("音高12 F#=青色（八度循环颜色不变，与音高0同色）")
        void 音高12颜色() {
            assertEquals(音高方块映射.音高转颜色(0), 音高方块映射.音高转颜色(12));
        }

        @Test
        @DisplayName("全部25音高 颜色按音名循环（音高n与n+12同色）")
        void 全25音高颜色循环一致() {
            for (int 音高 = 0; 音高 <= 12; 音高++) {
                assertEquals(音高方块映射.音高转颜色(音高),
                        音高方块映射.音高转颜色(音高 + 12),
                        "音高" + 音高 + "与" + (音高 + 12) + "颜色应一致");
            }
        }

        @Test
        @DisplayName("音高-1 抛 IllegalArgumentException")
        void 颜色越界低() {
            assertThrows(IllegalArgumentException.class, () -> 音高方块映射.音高转颜色(-1));
        }

        @Test
        @DisplayName("音高25 抛 IllegalArgumentException")
        void 颜色越界高() {
            assertThrows(IllegalArgumentException.class, () -> 音高方块映射.音高转颜色(25));
        }
    }
}
