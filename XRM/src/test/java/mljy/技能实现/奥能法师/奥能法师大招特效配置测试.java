package mljy.技能实现.奥能法师;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("奥能法师大招特效配置")
class 奥能法师大招特效配置测试 {
    @Test
    @DisplayName("默认配置应保持现有视觉周期并放大强度")
    void 默认配置_保持周期并放大强度() {
        奥能法师大招特效配置 配置 = 奥能法师大招特效配置.默认配置();

        assertEquals(20, 配置.冥想开始持续刻());
        assertEquals(2, 配置.冥想开始间隔刻());
        assertEquals(26, 配置.冥想完成持续刻());
        assertEquals(60, 配置.湮灭充能持续刻());
        assertEquals(1.35, 配置.强度(), 0.001);
        assertTrue(配置.粒子数量(110) > 110);
        assertTrue(配置.粒子数量(1000) <= 250);
    }

    @Test
    @DisplayName("无效插件配置加载失败时应安全回退默认值")
    void 配置加载失败_回退默认值() {
        奥能法师大招特效配置 配置 = 奥能法师大招特效配置.加载(null);

        assertEquals(48.0, 配置.最大距离(), 0.001);
        assertEquals(250, 配置.粒子数量(1000));
        assertTrue(配置.冥想持续间隔刻() >= 1);
    }
}
