package mljy.技能实现.奥能法师;

import mljy.业务层.效果调度服务;
import mljy.业务层.消息服务;
import mljy.业务层.目标选择服务;
import mljy.业务层.资源变更服务;
import mljy.业务层.资源变更服务实现;
import mljy.业务层.资源注册服务;
import mljy.业务层.属性.修饰器管理器;
import mljy.战斗服务;
import mljy.翻译服务;
import mljy.玩家服务;
import mljy.领域层.实体;
import mljy.领域层.效果.效果实例;
import mljy.领域层.技能.参数注册表;
import mljy.领域层.属性.属性快照;
import mljy.领域层.战斗.伤害结果;
import mljy.领域层.玩家.玩家快照;
import mljy.资源服务;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("奥能法师技能基础 - FP-08 秘兆数据驱动集成(带秘兆/仅资源日志键)")
@ExtendWith(MockitoExtension.class)
class 奥能法师秘兆数据驱动测试 {

    @Mock
    private 资源服务 资源服务mock;
    @Mock
    private 资源注册服务 资源注册服务mock;
    @Mock
    private 效果调度服务 效果调度服务;
    @Mock
    private 消息服务 消息服务;
    @Mock
    private 战斗服务 战斗服务;
    @Mock
    private 翻译服务 翻译服务;
    @Mock
    private 玩家服务 玩家服务;
    @Mock
    private 秘兆效果处理器 秘兆效果处理器;
    @Mock
    private 目标选择服务 目标选择服务;

    private 真实秘能测试技能 技能;
    private 玩家快照 施法者;
    private 实体 目标;
    private static final String 秘能 = "秘能";

    @BeforeEach
    void setUp() throws Exception {
        修饰器管理器 修饰器管理器 = new 修饰器管理器();
        参数注册表 参数注册表 = new 参数注册表();
        资源变更服务 真实服务 = new 资源变更服务实现(
                资源服务mock, 资源注册服务mock, 修饰器管理器, 效果调度服务, 参数注册表);

        java.util.UUID 秘能资源标识 = UUID.randomUUID();
        mljy.领域层.资源.资源 资源 = new mljy.领域层.资源.资源(秘能, 2, 4);
        when(资源服务mock.获取资源(any(UUID.class), eq(秘能))).thenReturn(Optional.of(资源));

        技能 = new 真实秘能测试技能();
        注入字段("资源变更服务", 真实服务);
        注入字段("效果调度服务", 效果调度服务);
        注入字段("秘兆效果处理器", 秘兆效果处理器);
        注入字段("消息服务", 消息服务);
        注入字段("战斗服务", 战斗服务);
        注入字段("翻译服务", 翻译服务);
        注入字段("玩家服务", 玩家服务);
        注入字段("目标选择服务", 目标选择服务);
        when(翻译服务.获取(any())).thenReturn("测试");
        when(战斗服务.应用伤害(any())).thenReturn(new 伤害结果(50.0, false, false));

        属性快照 属性 = 属性快照.创建(
                100.0, 1.5, 0.0,
                0.0, 0.0, 100.0,
                0.0, 0.0, 100.0,
                0.0, 1.5, 100.0,
                0.0, 0.0, 0.0, 0.0, 0.0);
        施法者 = new 玩家快照(UUID.randomUUID(), "测试法师", 1, null, 属性, Map.of(), null);
        目标 = mock(实体.class);
        when(目标.获取名称()).thenReturn("目标");
    }

    private void 注入字段(String 名称, Object 值) throws Exception {
        Field 字段 = 奥能法师技能基础.class.getDeclaredField(名称);
        字段.setAccessible(true);
        字段.set(技能, 值);
    }

    @Test
    @DisplayName("FP-08: 秘能增加触发秘兆时, 单体命中应使用命中仅资源带秘兆日志键并与获得秘能同行")
    void 秘能增加触发秘兆_应使用命中仅资源带秘兆日志键() {
        // 通过反射将概率为1的参数注入到资源变更服务使用的参数注册表中
        强制秘兆概率(1.0);

        技能.施放单体伤害带秘能(施法者, 目标, 1);

        verify(效果调度服务).添加(eq(施法者.唯一标识()), any(效果实例.class));
        verify(消息服务).发送技能日志(
                eq(施法者), any(UUID.class),
                eq("技能日志.命中仅资源带秘兆"), anyString());
        verify(消息服务, never()).发送技能日志(
                eq(施法者), any(UUID.class),
                eq("技能日志.命中仅资源"), anyString());
    }

    @Test
    @DisplayName("FP-08: 秘能增加未触发秘兆时, 单体命中应使用命中仅资源日志键")
    void 秘能增加未触发秘兆_应使用命中仅资源日志键() {
        强制秘兆概率(0.0);

        技能.施放单体伤害带秘能(施法者, 目标, 1);

        verify(效果调度服务, never()).添加(any(), any());
        verify(消息服务).发送技能日志(
                eq(施法者), any(UUID.class),
                eq("技能日志.命中仅资源"), anyString());
        verify(消息服务, never()).发送技能日志(
                eq(施法者), any(UUID.class),
                eq("技能日志.命中仅资源带秘兆"), anyString());
    }

    private void 强制秘兆概率(double 概率) {
        try {
            资源变更服务 服务 = (资源变更服务) 取字段("资源变更服务");
            if (服务 instanceof 资源变更服务实现 实现) {
                Field 注册表字段 = 资源变更服务实现.class.getDeclaredField("参数注册表");
                注册表字段.setAccessible(true);
                参数注册表 注册表 = (参数注册表) 注册表字段.get(实现);
                注册表.注册("1_9_1", new 参数注册表.参数条目("秘兆触发概率", String.valueOf(概率), "FP-08测试"));
                注册表.注册("1_9_1", new 参数注册表.参数条目("秘兆持续时间", "5000", "FP-08测试"));
            }
        } catch (Exception 忽略) {
            throw new IllegalStateException("无法注入秘兆参数", 忽略);
        }
    }

    private Object 取字段(String 名称) throws Exception {
        Field 字段 = 奥能法师技能基础.class.getDeclaredField(名称);
        字段.setAccessible(true);
        return 字段.get(技能);
    }

    private static class 真实秘能测试技能 extends 奥能法师技能基础 {
        void 施放单体伤害带秘能(玩家快照 施法者, 实体 目标, int 资源获取量) {
            应用单体伤害并记录命中(
                    施法者, 目标, 50.0, false,
                    "skill.奥能法师.测试.name", 资源获取量);
        }
    }
}
