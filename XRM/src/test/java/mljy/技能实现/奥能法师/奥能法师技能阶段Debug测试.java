package mljy.技能实现.奥能法师;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("奥能法师技能阶段 debug 调用")
class 奥能法师技能阶段Debug测试 {

    private static Stream<Arguments> 提供技能和阶段() {
        return Stream.of(
                Arguments.of("奥术冲击", "1_1", new String[]{
                        "释放请求", "前置状态", "蓄力结束", "租约获取", "任务启动", "任务停止", "伤害结算", "释放后状态", "最终状态"}),
                Arguments.of("秘法崩裂", "1_2", new String[]{
                        "释放请求", "前置状态", "资源变化", "伤害结算", "任务启动", "任务停止", "取消原因", "释放后状态", "最终状态"}),
                Arguments.of("奥术护盾", "1_3", new String[]{
                        "释放请求", "前置状态", "资源变化", "效果变化", "任务启动", "任务停止", "释放后状态"}),
                Arguments.of("秘法陨落", "1_4", new String[]{
                        "释放请求", "前置状态", "任务启动", "任务停止", "伤害结算", "释放后状态", "最终状态"}),
                Arguments.of("混沌束缚", "1_5", new String[]{
                        "释放请求", "前置状态", "弹道阶段", "任务启动", "任务停止", "伤害结算", "效果变化", "范围伤害", "最终状态"}),
                Arguments.of("秘法回流", "1_6", new String[]{
                        "释放请求", "前置状态", "资源变化", "任务启动", "任务停止", "二段释放", "治疗结算", "最终状态"}),
                Arguments.of("奥能冥想", "1_7", new String[]{
                        "释放请求", "前置状态", "资源变化", "治疗结算", "任务启动", "任务停止", "蓄力开始", "蓄力结束", "效果变化", "释放后状态", "最终状态"}),
                Arguments.of("秘法湮灭", "1_8", new String[]{
                        "释放请求", "前置状态", "资源变化", "任务启动", "蓄力开始", "蓄力结束", "伤害结算", "取消原因", "最终状态"}),
                Arguments.of("精通魔法预兆", "1_9", new String[]{
                        "释放请求", "前置状态", "任务启动", "任务停止", "脱战资源变化", "释放后状态", "最终状态"})
        );
    }

    @ParameterizedTest(name = "{0}（{1}）应调用记录技能阶段并覆盖关键阶段")
    @MethodSource("提供技能和阶段")
    void 技能实现_应调用结构化阶段记录(String 技能名称, String 技能标识, String[] 阶段) throws Exception {
        Path 源码路径 = Path.of("src/main/java/mljy/技能实现/奥能法师", 技能名称 + ".java");
        String 源码 = Files.readString(源码路径, StandardCharsets.UTF_8);

        assertTrue(源码.contains("记录技能阶段("), 技能名称 + " 必须实际调用记录技能阶段");
        assertTrue(源码.contains("\"" + 技能标识 + "\""), 技能名称 + " 必须使用技能标识");
        assertTrue(源码.contains("获取调试根事件标识()"), 技能名称 + " 必须建立 root_event_id 来源");
        assertTrue(源码.contains("创建施法标识()"), 技能名称 + " 必须建立 cast_id");
        for (String 阶段名称 : 阶段) {
            assertTrue(源码.contains("\"" + 阶段名称 + "\""),
                    技能名称 + " 缺少阶段：" + 阶段名称);
        }
    }

    static Stream<Arguments> 提供FP8补充的debug参数() {
        return Stream.of(
                Arguments.of("奥术冲击", "speed=%.1f_blocks_per_second"),
                Arguments.of("秘法崩裂", "radius=%.1f base_damage=%.2f secret_damage=%.2f"),
                Arguments.of("秘法陨落", "horizontal_distance=%.1f vertical_height=%.1f"),
                Arguments.of("混沌束缚", "speed=%.1f_blocks_per_second"),
                Arguments.of("奥能冥想", "intelligence_granted=%d"),
                Arguments.of("秘法湮灭", "radius=%.1f decay_threshold=%d"),
                Arguments.of("精通魔法预兆", "secret_energy_cap=%.1f")
        );
    }

    @ParameterizedTest(name = "{0} 应包含 FP-8 补充的 debug 参数：{1}")
    @MethodSource("提供FP8补充的debug参数")
    void 技能实现_应包含FP8补充的debug参数(String 技能名称, String 期望参数片段) throws Exception {
        Path 源码路径 = Path.of("src/main/java/mljy/技能实现/奥能法师", 技能名称 + ".java");
        String 源码 = Files.readString(源码路径, StandardCharsets.UTF_8);
        assertTrue(源码.contains(期望参数片段),
                技能名称 + " 缺少 FP-8 补充的 debug 参数片段：" + 期望参数片段);
    }
}
