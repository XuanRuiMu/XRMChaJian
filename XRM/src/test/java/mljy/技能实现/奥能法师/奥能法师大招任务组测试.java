package mljy.技能实现.奥能法师;

import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("奥能法师大招任务组")
class 奥能法师大招任务组测试 {
    @Test
    @DisplayName("同一玩家重复施法应取消全部已登记任务")
    void 重复施法_取消全部任务() {
        奥能法师大招任务组 任务组 = new 奥能法师大招任务组();
        UUID 玩家标识 = UUID.randomUUID();
        BukkitTask 任务一 = mock(BukkitTask.class);
        BukkitTask 任务二 = mock(BukkitTask.class);

        任务组.注册(玩家标识, 任务一, "奥能冥想");
        任务组.注册(玩家标识, 任务二, "奥能冥想");
        任务组.取消(玩家标识, "奥能冥想", "重复施法");

        verify(任务一).cancel();
        verify(任务二).cancel();
    }
}
