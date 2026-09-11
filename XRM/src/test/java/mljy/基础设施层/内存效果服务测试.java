package mljy.基础设施层;

import mljy.领域层.效果.效果实例;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("内存效果服务")
class 内存效果服务测试 {

    private 内存效果服务 服务;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() {
        服务 = new 内存效果服务();
        玩家标识 = UUID.randomUUID();
    }

    private 效果实例 创建效果(String 标识) {
        return new 效果实例(标识, "技能" + 标识, 1, System.currentTimeMillis() + 10000);
    }

    @Nested
    @DisplayName("基本功能")
    class 基本功能 {

        @Test
        @DisplayName("添加效果后应可查找")
        void 添加效果_应可查找() {
            效果实例 效果 = 创建效果("燃烧");

            服务.添加效果(玩家标识, 效果);

            Optional<效果实例> 查找结果 = 服务.查找效果(玩家标识, "燃烧");
            assertTrue(查找结果.isPresent());
            assertEquals("燃烧", 查找结果.get().获取效果标识());
        }

        @Test
        @DisplayName("移除效果后应不可查找")
        void 移除效果_应不可查找() {
            服务.添加效果(玩家标识, 创建效果("燃烧"));

            服务.移除效果(玩家标识, "燃烧");

            assertFalse(服务.拥有效果(玩家标识, "燃烧"));
        }

        @Test
        @DisplayName("获取效果列表应返回所有效果")
        void 获取效果列表_应返回全部() {
            服务.添加效果(玩家标识, 创建效果("燃烧"));
            服务.添加效果(玩家标识, 创建效果("中毒"));

            List<效果实例> 列表 = 服务.获取效果列表(玩家标识);

            assertEquals(2, 列表.size());
        }

        @Test
        @DisplayName("更新效果应替换同标识效果")
        void 更新效果_应替换() {
            效果实例 原效果 = 创建效果("燃烧");
            服务.添加效果(玩家标识, 原效果);

            效果实例 新效果 = new 效果实例("燃烧", "技能燃烧", 3, System.currentTimeMillis() + 20000);
            服务.更新效果(玩家标识, 新效果);

            Optional<效果实例> 查找结果 = 服务.查找效果(玩家标识, "燃烧");
            assertTrue(查找结果.isPresent());
            assertEquals(3, 查找结果.get().获取层数());
        }
    }

    @Nested
    @DisplayName("ERR-25: 线程安全")
    class 线程安全 {

        @Test
        @DisplayName("并发添加和移除效果应不抛ConcurrentModificationException")
        void 并发添加和移除_应不抛异常() throws Exception {
            int 线程数 = 20;
            int 迭代数 = 100;
            CountDownLatch 开始闸 = new CountDownLatch(1);
            CountDownLatch 完成闸 = new CountDownLatch(线程数);
            AtomicReference<Throwable> 异常引用 = new AtomicReference<>();

            for (int i = 0; i < 线程数; i++) {
                final int 索引 = i;
                Thread 线程 = new Thread(() -> {
                    try {
                        开始闸.await();
                        for (int j = 0; j < 迭代数; j++) {
                            String 效果标识 = "效果_" + 索引 + "_" + j;
                            服务.添加效果(玩家标识, 创建效果(效果标识));
                            服务.查找效果(玩家标识, 效果标识);
                            服务.获取效果列表(玩家标识);
                            服务.移除效果(玩家标识, 效果标识);
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
            assertNull(异常引用.get(), "并发添加和移除不应抛异常: " + 异常引用.get());
        }

        @Test
        @DisplayName("并发更新和查找效果应不抛ConcurrentModificationException")
        void 并发更新和查找_应不抛异常() throws Exception {
            int 线程数 = 10;
            int 迭代数 = 200;
            服务.添加效果(玩家标识, 创建效果("共享效果"));

            CountDownLatch 开始闸 = new CountDownLatch(1);
            CountDownLatch 完成闸 = new CountDownLatch(线程数);
            AtomicReference<Throwable> 异常引用 = new AtomicReference<>();

            for (int i = 0; i < 线程数; i++) {
                final int 索引 = i;
                Thread 线程 = new Thread(() -> {
                    try {
                        开始闸.await();
                        for (int j = 0; j < 迭代数; j++) {
                            效果实例 更新效果 = new 效果实例("共享效果", "技能", 索引, System.currentTimeMillis() + 10000);
                            服务.更新效果(玩家标识, 更新效果);
                            服务.查找效果(玩家标识, "共享效果");
                            服务.获取效果列表(玩家标识);
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
            assertNull(异常引用.get(), "并发更新和查找不应抛异常: " + 异常引用.get());
        }
    }
}
