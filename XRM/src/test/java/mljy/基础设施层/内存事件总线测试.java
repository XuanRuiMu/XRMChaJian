package mljy.基础设施层;

import mljy.领域事件;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("内存事件总线")
class 内存事件总线测试 {

    private static class 测试事件 implements 领域事件 {
    }

    @Nested
    @DisplayName("基本功能")
    class 基本功能 {

        @Test
        @DisplayName("订阅后发布应触发处理器")
        void 订阅后发布_应触发处理器() {
            内存事件总线 总线 = new 内存事件总线();
            AtomicInteger 计数器 = new AtomicInteger(0);
            总线.订阅(测试事件.class, 事件 -> 计数器.incrementAndGet());

            总线.发布(new 测试事件());

            assertEquals(1, 计数器.get());
        }

        @Test
        @DisplayName("无订阅者时发布应正常返回")
        void 无订阅者发布_应正常返回() {
            内存事件总线 总线 = new 内存事件总线();
            assertDoesNotThrow(() -> 总线.发布(new 测试事件()));
        }

        @Test
        @DisplayName("多个处理器应按订阅顺序执行")
        void 多个处理器_应全部执行() {
            内存事件总线 总线 = new 内存事件总线();
            AtomicInteger 计数器 = new AtomicInteger(0);
            总线.订阅(测试事件.class, 事件 -> 计数器.addAndGet(1));
            总线.订阅(测试事件.class, 事件 -> 计数器.addAndGet(10));

            总线.发布(new 测试事件());

            assertEquals(11, 计数器.get());
        }
    }

    @Nested
    @DisplayName("ERR-23: 线程安全")
    class 线程安全 {

        @Test
        @DisplayName("并发订阅和发布应不抛ConcurrentModificationException")
        void 并发订阅和发布_应不抛异常() throws Exception {
            int 线程数 = 20;
            int 迭代数 = 200;
            内存事件总线 总线 = new 内存事件总线();
            AtomicInteger 处理计数 = new AtomicInteger(0);

            总线.订阅(测试事件.class, 事件 -> 处理计数.incrementAndGet());

            CountDownLatch 开始闸 = new CountDownLatch(1);
            CountDownLatch 完成闸 = new CountDownLatch(线程数);
            AtomicReference<Throwable> 异常引用 = new AtomicReference<>();

            for (int i = 0; i < 线程数; i++) {
                Thread 线程 = new Thread(() -> {
                    try {
                        开始闸.await();
                        for (int j = 0; j < 迭代数; j++) {
                            总线.订阅(测试事件.class, 事件 -> {});
                            总线.发布(new 测试事件());
                        }
                    } catch (Throwable 异常) {
                        异常引用.compareAndSet(null, 异常);
                    } finally {
                        完成闸.countDown();
                    }
                });
                线程.start();
            }

            开始闸.countDown();
            assertTrue(完成闸.await(30, TimeUnit.SECONDS), "所有线程应在超时前完成");
            assertNull(异常引用.get(), "并发订阅和发布不应抛异常: " + 异常引用.get());
            assertTrue(处理计数.get() >= 线程数 * 迭代数, "处理器应被调用足够次数");
        }
    }
}
