package mljy.领域层.战斗;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("伤害管线阶段枚举")
class 伤害管线阶段测试 {

    @Test
    @DisplayName("应包含7个阶段")
    void 应包含7个阶段() {
        伤害管线阶段[] 阶段 = 伤害管线阶段.values();

        assertEquals(7, 阶段.length);
    }

    @Test
    @DisplayName("基础伤害应为第一个阶段")
    void 基础伤害_应为第一阶段() {
        assertEquals(0, 伤害管线阶段.基础伤害.ordinal());
    }

    @Test
    @DisplayName("最终修正应为最后一个阶段")
    void 最终修正_应为最后阶段() {
        伤害管线阶段[] 阶段 = 伤害管线阶段.values();

        assertEquals(伤害管线阶段.最终修正, 阶段[阶段.length - 1]);
    }

    @Nested
    @DisplayName("阶段顺序")
    class 阶段顺序 {

        @Test
        @DisplayName("精通修饰应在暴击判定之前")
        void 精通修饰_在暴击判定之前() {
            assertTrue(伤害管线阶段.精通修饰.ordinal() < 伤害管线阶段.暴击判定.ordinal());
        }

        @Test
        @DisplayName("暴击判定应在全局伤害修饰之前")
        void 暴击判定_在全局伤害修饰之前() {
            assertTrue(伤害管线阶段.暴击判定.ordinal() < 伤害管线阶段.全局伤害修饰.ordinal());
        }

        @Test
        @DisplayName("全局伤害修饰应在目标减伤之前")
        void 全局伤害修饰_在目标减伤之前() {
            assertTrue(伤害管线阶段.全局伤害修饰.ordinal() < 伤害管线阶段.目标减伤.ordinal());
        }

        @Test
        @DisplayName("目标减伤应在护盾吸收之前")
        void 目标减伤_在护盾吸收之前() {
            assertTrue(伤害管线阶段.目标减伤.ordinal() < 伤害管线阶段.护盾吸收.ordinal());
        }

        @Test
        @DisplayName("护盾吸收应在最终修正之前")
        void 护盾吸收_在最终修正之前() {
            assertTrue(伤害管线阶段.护盾吸收.ordinal() < 伤害管线阶段.最终修正.ordinal());
        }
    }

    @Test
    @DisplayName("valueOf应按名称解析")
    void valueOf_应按名称解析() {
        assertEquals(伤害管线阶段.全局伤害修饰, 伤害管线阶段.valueOf("全局伤害修饰"));
        assertEquals(伤害管线阶段.目标减伤, 伤害管线阶段.valueOf("目标减伤"));
    }
}
