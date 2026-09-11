package mljy.基础设施层;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class 事件日志上下文测试 {

    @AfterEach
    void 清理上下文() {
        事件日志上下文.清理全部玩家目标因果凭证();
        事件日志上下文.清除();
    }

    @Test
    void 同一事件标识不受时间影响() throws InterruptedException {
        UUID 事件标识 = 事件日志上下文.开始新事件();

        Thread.sleep(250L);

        assertEquals(事件标识, 事件日志上下文.获取当前事件标识());
    }

    @Test
    void 嵌套事件执行后恢复外层事件() {
        UUID 外层事件 = 事件日志上下文.开始新事件();
        UUID 内层事件 = UUID.randomUUID();

        事件日志上下文.在事件中执行(内层事件, () -> assertEquals(
                内层事件, 事件日志上下文.获取当前事件标识()));

        assertEquals(外层事件, 事件日志上下文.获取当前事件标识());
        assertNotEquals(外层事件, 内层事件);
    }

    @Test
    void 清除后无当前事件() {
        事件日志上下文.开始新事件();

        事件日志上下文.清除();

        assertNull(事件日志上下文.获取当前事件标识或空());
    }

    @Test
    void 同一事件对象复用根事件标识_结束后不再复用() {
        Object 事件对象 = new Object();

        UUID 第一次标识 = 事件日志上下文.获取或创建事件标识(事件对象);
        UUID 第二次标识 = 事件日志上下文.获取或创建事件标识(事件对象);

        assertEquals(第一次标识, 第二次标识);

        事件日志上下文.结束事件对象(事件对象);
        UUID 新事件标识 = 事件日志上下文.获取或创建事件标识(事件对象);

        assertNotEquals(第一次标识, 新事件标识);
        事件日志上下文.结束事件对象(事件对象);
    }

    @Test
    void 当前活跃根事件下未显式绑定的事件创建独立根事件() {
        Object 根事件对象 = new Object();
        Object 子事件对象 = new Object();
        UUID 根事件 = 事件日志上下文.获取或创建事件标识(根事件对象);
        UUID 子事件 = 事件日志上下文.获取或创建事件标识(子事件对象);

        assertNotEquals(根事件, 子事件);

        事件日志上下文.结束事件对象(子事件对象);
        事件日志上下文.结束事件对象(根事件对象);
    }

    @Test
    void 独立自然DOTtick无当前事件时创建新根事件() {
        事件日志上下文.清除();
        Object 第一次DOT事件 = new Object();
        UUID 第一次根事件 = 事件日志上下文.获取或创建事件标识(第一次DOT事件);
        事件日志上下文.结束事件对象(第一次DOT事件);

        事件日志上下文.清除();
        Object 第二次DOT事件 = new Object();
        UUID 第二次根事件 = 事件日志上下文.获取或创建事件标识(第二次DOT事件);

        assertNotEquals(第一次根事件, 第二次根事件);

        事件日志上下文.结束事件对象(第二次DOT事件);
    }

    @Test
    void 同次施法延迟子事件使用独立根事件_异步租约已废弃() {
        UUID 根事件 = 事件日志上下文.开始新事件();
        AtomicInteger 完成次数 = new AtomicInteger();
        // 异步事件租约已废弃：返回空操作租约，不再延迟根事件完成
        事件日志上下文.异步事件租约 租约 = 事件日志上下文.获取异步事件租约(根事件);
        Object 延迟伤害事件 = new Object();

        assertTrue(事件日志上下文.注册完成回调(根事件, 完成次数::incrementAndGet));
        // 结束事件立即执行完成回调，不再等待异步子事件租约释放
        事件日志上下文.结束事件(根事件);
        assertEquals(1, 完成次数.get(), "根事件结束后应立即执行完成回调，不再等待租约释放");

        UUID 施法标识 = UUID.randomUUID();
        UUID 弹道标识 = UUID.randomUUID();
        事件日志上下文.在显式子事件中执行(根事件, 施法标识, 弹道标识, () -> {
            // 显式传播凭据已废弃：未绑定的事件对象创建独立新根事件，不再继承父根事件
            UUID 延迟伤害根事件 = 事件日志上下文.获取或创建事件标识(延迟伤害事件);
            assertNotEquals(根事件, 延迟伤害根事件, "延迟伤害应使用独立根事件，不再回绑到父根事件");
        });
        事件日志上下文.结束事件对象(延迟伤害事件);

        // 租约释放为空操作，不应再次触发回调
        租约.释放();
        assertEquals(1, 完成次数.get(), "租约释放为空操作，回调次数不应变化");
    }

    @Test
    void 显式子事件中每个子事件对象创建独立根事件() {
        UUID 根事件 = 事件日志上下文.开始新事件();
        Object 命中事件 = new Object();
        Object 无关事件 = new Object();

        事件日志上下文.在显式子事件中执行(根事件, UUID.randomUUID(), UUID.randomUUID(), () -> {
            // 显式传播凭据已废弃：所有未绑定的事件对象都创建独立新根事件
            UUID 命中根事件 = 事件日志上下文.获取或创建事件标识(命中事件);
            UUID 无关根事件 = 事件日志上下文.获取或创建事件标识(无关事件);
            assertNotEquals(根事件, 命中根事件, "命中事件应创建独立根事件，不再继承父根事件");
            assertNotEquals(根事件, 无关根事件, "无关事件应创建独立根事件");
            assertNotEquals(命中根事件, 无关根事件, "不同子事件对象应各自创建独立根事件");
        });

        事件日志上下文.结束事件对象(命中事件);
        事件日志上下文.结束事件对象(无关事件);
        事件日志上下文.结束事件(根事件);
    }

    @Test
    void 因果事件对象转移后_新对象复用原根事件() {
        Object 输入事件 = new Object();
        Object 伤害事件 = new Object();
        UUID 根事件 = 事件日志上下文.获取或创建事件标识(输入事件);

        assertTrue(事件日志上下文.转移事件对象根事件(输入事件, 伤害事件, 根事件));
        assertEquals(根事件, 事件日志上下文.获取或创建事件标识(伤害事件));
        事件日志上下文.清除();
        assertNotEquals(根事件, 事件日志上下文.获取或创建事件标识(输入事件));

        事件日志上下文.结束事件对象(输入事件);
        事件日志上下文.结束事件对象(伤害事件);
    }

    @Test
    void 玩家目标因果凭证_按玩家绑定且命中后只能消费一次() {
        UUID 玩家标识 = UUID.randomUUID();
        Object 输入事件 = new Object();
        Object 目标对象 = new Object();
        Object 伤害事件 = new Object();
        UUID 根事件 = 事件日志上下文.获取或创建事件标识(输入事件);

        assertTrue(事件日志上下文.创建玩家目标因果凭证(玩家标识, 根事件, 输入事件));
        事件日志上下文.结束事件对象(输入事件);
        assertTrue(事件日志上下文.是否已托管(根事件), "输入结束后应保留根事件等待命中");

        assertEquals(根事件,
                事件日志上下文.消费玩家目标因果凭证(玩家标识, 目标对象, 伤害事件));
        assertEquals(根事件, 事件日志上下文.获取或创建事件标识(伤害事件));
        assertNull(事件日志上下文.消费玩家目标因果凭证(
                玩家标识, new Object(), new Object()), "已消费凭证不得再次绑定其他目标");

        事件日志上下文.结束事件对象(伤害事件);
        assertFalse(事件日志上下文.是否已托管(根事件));
    }

    @Test
    void 玩家目标因果凭证_不同玩家不得互相消费() {
        UUID 输入玩家 = UUID.randomUUID();
        UUID 其他玩家 = UUID.randomUUID();
        Object 输入事件 = new Object();
        Object 目标对象 = new Object();
        Object 伤害事件 = new Object();
        UUID 根事件 = 事件日志上下文.获取或创建事件标识(输入事件);

        assertTrue(事件日志上下文.创建玩家目标因果凭证(输入玩家, 根事件, 输入事件));
        事件日志上下文.结束事件对象(输入事件);
        assertNull(事件日志上下文.消费玩家目标因果凭证(其他玩家, 目标对象, 伤害事件));
        assertEquals(根事件,
                事件日志上下文.消费玩家目标因果凭证(输入玩家, 目标对象, 伤害事件));
        事件日志上下文.结束事件对象(伤害事件);
    }

    @Test
    void 重复玩家输入_拒绝创建第二张凭证并关闭旧根事件() {
        UUID 玩家标识 = UUID.randomUUID();
        Object 第一个输入 = new Object();
        Object 第二个输入 = new Object();
        UUID 第一个根事件 = 事件日志上下文.获取或创建事件标识(第一个输入);
        UUID 第二个根事件 = 事件日志上下文.获取或创建事件标识(第二个输入);

        assertTrue(事件日志上下文.创建玩家目标因果凭证(玩家标识, 第一个根事件, 第一个输入));
        事件日志上下文.结束事件对象(第一个输入);
        assertFalse(事件日志上下文.创建玩家目标因果凭证(玩家标识, 第二个根事件, 第二个输入));
        事件日志上下文.结束事件对象(第二个输入);

        assertFalse(事件日志上下文.是否已托管(第一个根事件));
        assertFalse(事件日志上下文.是否已托管(第二个根事件));
    }

    @Test
    void 玩家目标因果凭证_超时只清理泄漏不参与命中判断() {
        UUID 玩家标识 = UUID.randomUUID();
        Object 输入事件 = new Object();
        UUID 根事件 = 事件日志上下文.获取或创建事件标识(输入事件);

        assertTrue(事件日志上下文.创建玩家目标因果凭证(玩家标识, 根事件, 输入事件));
        事件日志上下文.结束事件对象(输入事件);
        事件日志上下文.清理过期玩家目标因果凭证(System.currentTimeMillis() + 6000L);

        assertFalse(事件日志上下文.是否已托管(根事件));
        assertNull(事件日志上下文.消费玩家目标因果凭证(
                玩家标识, new Object(), new Object()));
    }

    @Test
    void 根事件结束后立即执行完成回调() {
        UUID 事件标识 = 事件日志上下文.开始新事件();
        AtomicInteger 执行次数 = new AtomicInteger();

        assertTrue(事件日志上下文.注册完成回调(事件标识, 执行次数::incrementAndGet));
        assertEquals(0, 执行次数.get());

        事件日志上下文.结束事件(事件标识);

        assertEquals(1, 执行次数.get());
    }

    @Test
    void 完成回调不再等待异步子事件租约释放() {
        UUID 事件标识 = 事件日志上下文.开始新事件();
        AtomicInteger 执行次数 = new AtomicInteger();

        // 异步子事件租约已废弃：持有/释放均为空操作，不再延迟完成回调
        assertTrue(事件日志上下文.持有异步子事件(事件标识));
        assertTrue(事件日志上下文.注册完成回调(事件标识, 执行次数::incrementAndGet));

        事件日志上下文.结束事件(事件标识);
        assertEquals(1, 执行次数.get(), "根事件结束后应立即执行回调，不再等待租约释放");

        事件日志上下文.释放异步子事件(事件标识);
        assertEquals(1, 执行次数.get(), "释放异步子事件为空操作，回调次数不应变化");
    }

    @Test
    void 异步事件租约重复释放不影响回调次数() {
        UUID 事件标识 = 事件日志上下文.开始新事件();
        AtomicInteger 执行次数 = new AtomicInteger();

        事件日志上下文.异步事件租约 租约 = 事件日志上下文.获取异步事件租约(事件标识);
        assertTrue(事件日志上下文.注册完成回调(事件标识, 执行次数::incrementAndGet));

        事件日志上下文.结束事件(事件标识);
        assertEquals(1, 执行次数.get(), "根事件结束立即执行回调");

        // 租约重复释放为空操作，不影响回调次数
        租约.释放();
        租约.close();
        assertEquals(1, 执行次数.get(), "租约释放为空操作，回调次数不应变化");
    }

    @Test
    void 异步事件租约_持有与释放均为空操作() {
        UUID 事件标识 = 事件日志上下文.开始新事件();
        AtomicInteger 执行次数 = new AtomicInteger();

        // 持有异步子事件为空操作，不影响完成回调时机
        assertTrue(事件日志上下文.持有异步子事件(事件标识));
        assertTrue(事件日志上下文.注册完成回调(事件标识, 执行次数::incrementAndGet));

        事件日志上下文.结束事件(事件标识);
        assertEquals(1, 执行次数.get(), "即使持有异步子事件，根事件结束仍立即执行回调");

        // 释放异步子事件为空操作
        事件日志上下文.释放异步子事件(事件标识);
        assertEquals(1, 执行次数.get(), "释放为空操作，回调次数不变");
    }
}
