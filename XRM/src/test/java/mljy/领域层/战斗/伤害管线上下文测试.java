package mljy.领域层.战斗;

import mljy.领域事件;
import mljy.领域层.属性.修饰器;
import mljy.领域层.属性.修饰方式;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("伤害管线上下文")
class 伤害管线上下文测试 {

    private 伤害上下文 伤害上下文;

    @BeforeEach
    void setUp() {
        伤害上下文 = new 伤害上下文(
                null, null, 100, false, false, 2.0,
                List.of(new 修饰器("伤害_全伤害百分比", "g1", 修饰方式.相加, 30, 0))
        );
    }

    @Test
    @DisplayName("应实现领域事件接口")
    void 应实现领域事件接口() {
        伤害管线上下文 上下文 = new 伤害管线上下文(伤害上下文, 伤害管线阶段.全局伤害修饰, 100);

        assertTrue(上下文 instanceof 领域事件);
    }

    @Test
    @DisplayName("构造后应保留传入的伤害上下文")
    void 构造_应保留伤害上下文() {
        伤害管线上下文 上下文 = new 伤害管线上下文(伤害上下文, 伤害管线阶段.全局伤害修饰, 100);

        assertSame(伤害上下文, 上下文.获取伤害上下文());
    }

    @Test
    @DisplayName("构造后应保留传入的当前阶段")
    void 构造_应保留当前阶段() {
        伤害管线上下文 上下文 = new 伤害管线上下文(伤害上下文, 伤害管线阶段.目标减伤, 100);

        assertEquals(伤害管线阶段.目标减伤, 上下文.获取当前阶段());
    }

    @Test
    @DisplayName("构造后应保留传入的数值")
    void 构造_应保留数值() {
        伤害管线上下文 上下文 = new 伤害管线上下文(伤害上下文, 伤害管线阶段.基础伤害, 150.5);

        assertEquals(150.5, 上下文.获取数值());
    }

    @Test
    @DisplayName("设置数值应更新数值")
    void 设置数值_应更新数值() {
        伤害管线上下文 上下文 = new 伤害管线上下文(伤害上下文, 伤害管线阶段.基础伤害, 100);

        上下文.设置数值(250.5);

        assertEquals(250.5, 上下文.获取数值());
    }

    @Test
    @DisplayName("设置数值为零应正常处理")
    void 设置数值_为零应正常处理() {
        伤害管线上下文 上下文 = new 伤害管线上下文(伤害上下文, 伤害管线阶段.最终修正, 100);

        上下文.设置数值(0);

        assertEquals(0, 上下文.获取数值());
    }

    @Test
    @DisplayName("设置数值为负应正常处理")
    void 设置数值_为负应正常处理() {
        伤害管线上下文 上下文 = new 伤害管线上下文(伤害上下文, 伤害管线阶段.最终修正, 100);

        上下文.设置数值(-50);

        assertEquals(-50, 上下文.获取数值());
    }

    @Test
    @DisplayName("不同阶段应可独立构造")
    void 不同阶段_应可独立构造() {
        伤害管线上下文 阶段5上下文 = new 伤害管线上下文(伤害上下文, 伤害管线阶段.全局伤害修饰, 100);
        伤害管线上下文 阶段6上下文 = new 伤害管线上下文(伤害上下文, 伤害管线阶段.目标减伤, 80);

        assertEquals(伤害管线阶段.全局伤害修饰, 阶段5上下文.获取当前阶段());
        assertEquals(伤害管线阶段.目标减伤, 阶段6上下文.获取当前阶段());
        assertEquals(100, 阶段5上下文.获取数值());
        assertEquals(80, 阶段6上下文.获取数值());
    }
}
