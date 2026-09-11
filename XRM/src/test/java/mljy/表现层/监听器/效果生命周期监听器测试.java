package mljy.表现层.监听器;

import mljy.业务层.效果调度服务;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("效果生命周期监听器")
class 效果生命周期监听器测试 {
    private static final Path 源文件 = Path.of(
            "src/main/java/mljy/表现层/监听器/效果生命周期监听器.java");

    @Test
    @DisplayName("监听器应实现Bukkit生命周期事件清理")
    void 生命周期事件_清理效果() throws Exception {
        String 源码 = Files.readString(源文件, StandardCharsets.UTF_8);

        assertTrue(效果调度服务.class.isAssignableFrom(
                效果生命周期监听器.class.getDeclaredConstructor(效果调度服务.class).getParameterTypes()[0]));
        assertTrue(源码.contains("PlayerDeathEvent"));
        assertTrue(源码.contains("PlayerChangedWorldEvent"));
        assertTrue(源码.contains("效果调度服务.清空(玩家标识, 清理原因)"));
        assertTrue(源码.contains("\"玩家死亡\""));
        assertTrue(源码.contains("\"世界切换\""));
    }
}
