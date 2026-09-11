package mljy.回归测试;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FP-04：奥能法师属性与「职业，专精与属性.md」第18-33行一致性测试。
 *
 * 需求文档（开发需求文档/职业，专精与属性.md 第18-33行）规定奥能法师基础属性：
 *   生命值=等级×100（基础值100）、公共冷却时间=1.2秒、力量=0、敏捷=0、
 *   智力=等级×5（基础值5）、法术暴击几率=10%、法术暴击伤害=5%、
 *   精通=5、急速=5、全能=5、吸血=5、躲闪=5、生命恢复=10/s、移速=0.1。
 *
 * 本测试只校验配置中与文档不一致需更新的三项（公CD/急速/精通），
 * 并对全部属性做一次回归校验，防止后续误改。
 */
@DisplayName("回归测试 - FP-04：奥能法师属性与属性文档一致性")
class 奥能法师属性文档一致性测试 {

    private static final String 配置路径 = "专精.奥能法师.基础属性";
    private static final double 容差 = 0.000001;

    private YamlConfiguration 加载源码配置() throws Exception {
        File 源文件 = new File("src/main/resources/专精属性.yml");
        assertTrue(源文件.exists(), "源码 resources/专精属性.yml 应存在");
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(Files.newInputStream(源文件.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("公共冷却时间应为1.2（FP-04 重点更新：1.0→1.2）")
    void 公共冷却时间_应为1点2() throws Exception {
        YamlConfiguration 配置 = 加载源码配置();
        double 值 = 配置.getDouble(配置路径 + ".公共冷却时间", -1);
        assertEquals(1.2, 值, 容差, "奥能法师公共冷却时间应为1.2，实际：" + 值);
    }

    @Test
    @DisplayName("急速应为5（FP-04 重点更新：0→5）")
    void 急速_应为5() throws Exception {
        YamlConfiguration 配置 = 加载源码配置();
        double 值 = 配置.getDouble(配置路径 + ".急速", -1);
        assertEquals(5, 值, 容差, "奥能法师急速应为5，实际：" + 值);
    }

    @Test
    @DisplayName("精通应为5（FP-04 重点更新：0→5）")
    void 精通_应为5() throws Exception {
        YamlConfiguration 配置 = 加载源码配置();
        double 值 = 配置.getDouble(配置路径 + ".精通", -1);
        assertEquals(5, 值, 容差, "奥能法师精通应为5，实际：" + 值);
    }

    @Test
    @DisplayName("全属性回归校验：奥能法师基础属性应与属性文档第18-33行一致")
    void 全属性_应与文档一致() throws Exception {
        YamlConfiguration 配置 = 加载源码配置();
        // 生命值/智力为等级缩放公式的基础值（系数），由代码侧按等级计算
        assertEquals(100, 配置.getDouble(配置路径 + ".生命值上限", -1), 容差, "生命值上限基础值");
        assertEquals(0, 配置.getDouble(配置路径 + ".力量", -1), 容差, "力量");
        assertEquals(0, 配置.getDouble(配置路径 + ".敏捷", -1), 容差, "敏捷");
        assertEquals(5, 配置.getDouble(配置路径 + ".智力", -1), 容差, "智力基础值");
        assertEquals(10, 配置.getDouble(配置路径 + ".法术暴击几率", -1), 容差, "法术暴击几率");
        assertEquals(5, 配置.getDouble(配置路径 + ".法术暴击伤害", -1), 容差, "法术暴击伤害");
        assertEquals(5, 配置.getDouble(配置路径 + ".精通", -1), 容差, "精通");
        assertEquals(5, 配置.getDouble(配置路径 + ".急速", -1), 容差, "急速");
        assertEquals(5, 配置.getDouble(配置路径 + ".全能", -1), 容差, "全能");
        assertEquals(5, 配置.getDouble(配置路径 + ".吸血", -1), 容差, "吸血");
        assertEquals(5, 配置.getDouble(配置路径 + ".躲闪", -1), 容差, "躲闪");
        assertEquals(10, 配置.getDouble(配置路径 + ".生命恢复", -1), 容差, "生命恢复");
        assertEquals(0.1, 配置.getDouble(配置路径 + ".移速", -1), 容差, "移速");
        assertEquals(1.2, 配置.getDouble(配置路径 + ".公共冷却时间", -1), 容差, "公共冷却时间");
    }
}
