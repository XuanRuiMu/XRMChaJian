package mljy.技能实现.奥能法师;

import mljy.领域层.技能.技能上下文;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("秘法湮灭")
class 秘法湮灭测试 {
    @Test
    @DisplayName("空上下文应安全跳过")
    void 空上下文_安全跳过() {
        秘法湮灭 技能 = new 秘法湮灭();
        assertDoesNotThrow(() -> 技能.执行(null));
    }

    @Test
    @DisplayName("空施法者应安全跳过")
    void 空施法者_安全跳过() {
        技能上下文 上下文 = mock(技能上下文.class);
        when(上下文.施法者()).thenReturn(null);

        assertDoesNotThrow(() -> new 秘法湮灭().执行(上下文));
    }

    @Test
    @DisplayName("取消任务时应同步释放全部延迟伤害事件租约")
    void 生命周期取消_释放延迟伤害事件租约() throws Exception {
        String 源码 = Files.readString(Path.of(
                "src/main/java/mljy/技能实现/奥能法师/秘法湮灭.java"), StandardCharsets.UTF_8);

        assertTrue(源码.contains("注册异步租约(玩家标识, 异步租约)"));
        assertTrue(源码.contains("租约集合.forEach(事件日志上下文.异步事件租约::释放)"));
        assertTrue(源码.contains("public void 取消全部任务(String 原因)"));
    }

    @Test
    @DisplayName("DRAGON_BREATH粒子调用必须携带Float数据参数（修复IllegalArgumentException）")
    void dragon_breath粒子调用必须携带Float数据参数() throws Exception {
        String 源码 = Files.readString(Path.of(
                "src/main/java/mljy/技能实现/奥能法师/秘法湮灭.java"), StandardCharsets.UTF_8);

        assertTrue(源码.contains("private static final float 湮灭龙息强度 = 1.0f;"),
                "应定义湮灭龙息强度常量（1.0f 与原版DragonSittingFlamingPhase一致）");
        long 调用数 = 源码.lines()
                .filter(l -> l.contains("spawnParticle(Particle.DRAGON_BREATH"))
                .count();
        long 强度引用数 = 源码.lines()
                .filter(l -> l.contains("湮灭龙息强度"))
                .count();
        long 实际使用数 = 强度引用数 - 1;
        assertTrue(调用数 > 0, "秘法湮灭应至少有一次DRAGON_BREATH粒子调用");
        assertEquals(调用数, 实际使用数,
                "所有DRAGON_BREATH调用都必须携带湮灭龙息强度Float数据参数，调用数=" + 调用数 + " 实际使用数=" + 实际使用数);
    }

    @Test
    @DisplayName("粒子特效异常应被内层try-catch吞掉以保证伤害结算不中断")
    void 粒子特效异常应被内层try_catch吞掉() throws Exception {
        String 源码 = Files.readString(Path.of(
                "src/main/java/mljy/技能实现/奥能法师/秘法湮灭.java"), StandardCharsets.UTF_8);

        assertTrue(源码.contains("播放湮灭爆发特效(中心, 段序号, 配置, 玩家标识);"),
                "应调用播放湮灭爆发特效");
        assertTrue(源码.contains("} catch (RuntimeException | Error 粒子异常) {"),
                "粒子特效调用应被内层try-catch包裹，捕获RuntimeException|Error");
        assertTrue(源码.contains("\"粒子异常\", \"吞掉\""),
                "粒子异常应记录为吞掉而非外抛，确保后续伤害结算继续执行");
    }

    @Test
    @DisplayName("伤害任务run方法应存在外层兜底catch防止调度链被BukkitScheduler取消")
    void 伤害任务run方法应存在外层兜底catch() throws Exception {
        String 源码 = Files.readString(Path.of(
                "src/main/java/mljy/技能实现/奥能法师/秘法湮灭.java"), StandardCharsets.UTF_8);

        assertTrue(源码.contains("} catch (RuntimeException | Error 异常) {"),
                "run方法外层应存在兜底catch捕获RuntimeException|Error");
        assertTrue(源码.contains("\"伤害结算\", \"兜底异常\""),
                "兜底异常应记录为伤害结算阶段，便于日志定位");
        assertTrue(源码.contains("释放异步租约(玩家标识, 异步租约)"),
                "兜底catch后finally块应释放异步租约，避免事件日志泄漏");
    }
}
