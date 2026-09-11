package mljy.表现层.菜单;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FP-09 总菜单配置 - bug2 修复验证（硬编码提取到配置）")
class 总菜单配置测试 {

    @Nested
    @DisplayName("默认值 - 配置缺失时使用默认值")
    class 默认值 {

        @Test
        @DisplayName("空配置 - 应使用全部默认值")
        void 空配置_应使用全部默认值() {
            YamlConfiguration 配置 = new YamlConfiguration();

            总菜单配置 菜单配置 = new 总菜单配置(配置);

            assertEquals(54, 菜单配置.菜单大小(), "默认菜单大小应为54");
            assertEquals(20, 菜单配置.任务系统槽位(), "默认任务系统槽位应为20");
            assertEquals(28, 菜单配置.属性系统槽位(), "默认属性系统槽位应为28");
            assertEquals(11, 菜单配置.技能日志槽位(), "默认技能日志槽位应为11");
            assertEquals(13, 菜单配置.战斗日志槽位(), "默认战斗日志槽位应为13");
            assertEquals("BOOK", 菜单配置.任务系统物品(), "默认任务系统物品应为BOOK");
            assertEquals("DIAMOND_SWORD", 菜单配置.属性系统物品(), "默认属性系统物品应为DIAMOND_SWORD");
            assertEquals("FEATHER", 菜单配置.技能日志物品(), "默认技能日志物品应为FEATHER");
            assertEquals("IRON_SWORD", 菜单配置.战斗日志物品(), "默认战斗日志物品应为IRON_SWORD");
            assertEquals("sx", 菜单配置.属性命令(), "默认属性命令应为sx");
        }
    }

    @Nested
    @DisplayName("自定义值 - 配置存在时读取自定义值")
    class 自定义值 {

        @Test
        @DisplayName("自定义菜单大小 - 应读取配置值")
        void 自定义菜单大小_应读取配置值() {
            YamlConfiguration 配置 = new YamlConfiguration();
            配置.set("总菜单.菜单大小", 27);

            总菜单配置 菜单配置 = new 总菜单配置(配置);

            assertEquals(27, 菜单配置.菜单大小(), "应读取自定义菜单大小27");
        }

        @Test
        @DisplayName("自定义任务系统槽位和物品 - 应读取配置值")
        void 自定义任务系统_应读取配置值() {
            YamlConfiguration 配置 = new YamlConfiguration();
            配置.set("总菜单.任务系统.槽位", 10);
            配置.set("总菜单.任务系统.物品材质", "WRITABLE_BOOK");

            总菜单配置 菜单配置 = new 总菜单配置(配置);

            assertEquals(10, 菜单配置.任务系统槽位(), "应读取自定义任务系统槽位10");
            assertEquals("WRITABLE_BOOK", 菜单配置.任务系统物品(), "应读取自定义任务系统物品WRITABLE_BOOK");
        }

        @Test
        @DisplayName("自定义属性系统配置 - 应读取槽位/物品/命令")
        void 自定义属性系统_应读取完整配置() {
            YamlConfiguration 配置 = new YamlConfiguration();
            配置.set("总菜单.属性系统.槽位", 16);
            配置.set("总菜单.属性系统.物品材质", "NETHERITE_SWORD");
            配置.set("总菜单.属性系统.命令", "shuxing");

            总菜单配置 菜单配置 = new 总菜单配置(配置);

            assertEquals(16, 菜单配置.属性系统槽位(), "应读取自定义属性系统槽位16");
            assertEquals("NETHERITE_SWORD", 菜单配置.属性系统物品(), "应读取自定义属性系统物品NETHERITE_SWORD");
            assertEquals("shuxing", 菜单配置.属性命令(), "应读取自定义属性命令shuxing");
        }

        @Test
        @DisplayName("自定义技能日志配置 - 应读取槽位和物品")
        void 自定义技能日志_应读取配置值() {
            YamlConfiguration 配置 = new YamlConfiguration();
            配置.set("总菜单.技能日志设置.槽位", 22);
            配置.set("总菜单.技能日志设置.物品材质", "PAPER");

            总菜单配置 菜单配置 = new 总菜单配置(配置);

            assertEquals(22, 菜单配置.技能日志槽位(), "应读取自定义技能日志槽位22");
            assertEquals("PAPER", 菜单配置.技能日志物品(), "应读取自定义技能日志物品PAPER");
        }

        @Test
        @DisplayName("自定义战斗日志配置 - 应读取槽位和物品")
        void 自定义战斗日志_应读取配置值() {
            YamlConfiguration 配置 = new YamlConfiguration();
            配置.set("总菜单.战斗日志设置.槽位", 24);
            配置.set("总菜单.战斗日志设置.物品材质", "STONE_SWORD");

            总菜单配置 菜单配置 = new 总菜单配置(配置);

            assertEquals(24, 菜单配置.战斗日志槽位(), "应读取自定义战斗日志槽位24");
            assertEquals("STONE_SWORD", 菜单配置.战斗日志物品(), "应读取自定义战斗日志物品STONE_SWORD");
        }
    }

    @Nested
    @DisplayName("config.yml 一致性 - 默认值应与 config.yml 中的值一致")
    class 配置文件一致性 {

        @Test
        @DisplayName("默认值应与项目 config.yml 中的配置一致")
        void 默认值应与配置文件一致() {
            YamlConfiguration 配置 = new YamlConfiguration();
            配置.set("总菜单.菜单大小", 54);
            配置.set("总菜单.任务系统.槽位", 20);
            配置.set("总菜单.任务系统.物品材质", "BOOK");
            配置.set("总菜单.属性系统.槽位", 28);
            配置.set("总菜单.属性系统.物品材质", "DIAMOND_SWORD");
            配置.set("总菜单.属性系统.命令", "sx");
            配置.set("总菜单.技能日志设置.槽位", 11);
            配置.set("总菜单.技能日志设置.物品材质", "FEATHER");
            配置.set("总菜单.战斗日志设置.槽位", 13);
            配置.set("总菜单.战斗日志设置.物品材质", "IRON_SWORD");

            总菜单配置 菜单配置 = new 总菜单配置(配置);

            assertEquals(54, 菜单配置.菜单大小());
            assertEquals(20, 菜单配置.任务系统槽位());
            assertEquals(28, 菜单配置.属性系统槽位());
            assertEquals(11, 菜单配置.技能日志槽位());
            assertEquals(13, 菜单配置.战斗日志槽位());
            assertEquals("BOOK", 菜单配置.任务系统物品());
            assertEquals("DIAMOND_SWORD", 菜单配置.属性系统物品());
            assertEquals("FEATHER", 菜单配置.技能日志物品());
            assertEquals("IRON_SWORD", 菜单配置.战斗日志物品());
            assertEquals("sx", 菜单配置.属性命令());
        }
    }
}
