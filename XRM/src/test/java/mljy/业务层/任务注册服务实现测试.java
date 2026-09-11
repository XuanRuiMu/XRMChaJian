package mljy.业务层;

import mljy.基础设施层.Yaml配置加载器;
import mljy.领域层.任务.任务定义;
import mljy.领域层.任务.任务分类;
import mljy.领域层.任务.任务目标;
import mljy.领域层.任务.任务目标类型;
import mljy.领域层.任务.任务奖励;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * ERR-20 复现与回归测试：任务注册服务实现NPC索引内层ArrayList竞态。
 *
 * 根因：NPC接取索引/NPC交付索引 外层为 ConcurrentHashMap，但内层为 ArrayList。
 *   computeIfAbsent(k -> new ArrayList<>()).add(定义) 写入 与
 *   Collections.unmodifiableList(getOrDefault(...)) 读取 并发时
 *   可能触发 ArrayList 的 ConcurrentModificationException。
 *
 * 修复：内层改为 CopyOnWriteArrayList。
 */
@DisplayName("ERR-20: 任务注册服务实现NPC索引线程安全")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 任务注册服务实现测试 {

    @Mock
    private Yaml配置加载器 配置加载器mock;
    @Mock
    private JavaPlugin 插件mock;

    private 任务注册服务实现 服务;

    @BeforeEach
    void setUp() {
        when(配置加载器mock.加载(anyString())).thenReturn(new YamlConfiguration());
        when(插件mock.getLogger()).thenReturn(Logger.getLogger("任务注册服务测试"));
        服务 = new 任务注册服务实现(配置加载器mock, 插件mock);
    }

    private 任务定义 创建任务定义(String 任务标识, String 接取NPC, String 交付NPC) {
        return 任务定义.主线(
                任务标识,
                "quest." + 任务标识 + ".name",
                "quest." + 任务标识 + ".description",
                任务目标.单次(任务目标类型.对话NPC, ""),
                任务奖励.空,
                接取NPC,
                交付NPC
        );
    }

    @Nested
    @DisplayName("ERR-20复现：并发注册和获取NPC任务应不抛异常")
    class NPC索引线程安全 {

        @Test
        @DisplayName("并发注册任务和获取NPC接取/交付任务应不抛ConcurrentModificationException")
        void 并发注册和获取_应不抛异常() throws Exception {
            int 线程数 = 20;
            int 迭代数 = 500;
            CountDownLatch 开始闸 = new CountDownLatch(1);
            CountDownLatch 完成闸 = new CountDownLatch(线程数);
            AtomicReference<Throwable> 异常引用 = new AtomicReference<>();

            for (int i = 0; i < 线程数; i++) {
                final int 索引 = i;
                Thread 线程 = new Thread(() -> {
                    try {
                        开始闸.await();
                        for (int j = 0; j < 迭代数; j++) {
                            String npc名 = "NPC" + (索引 % 5);
                            if (索引 % 2 == 0) {
                                任务定义 定义 = 创建任务定义("任务" + 索引 + "_" + j, npc名, npc名);
                                服务.注册(定义);
                            } else {
                                服务.获取NPC接取任务(npc名);
                                服务.获取NPC交付任务(npc名);
                            }
                        }
                    } catch (Throwable e) {
                        异常引用.set(e);
                    } finally {
                        完成闸.countDown();
                    }
                });
                线程.start();
            }

            开始闸.countDown();
            assertTrue(完成闸.await(30, TimeUnit.SECONDS), "所有线程应在超时前完成");
            assertNull(异常引用.get(), "并发注册和获取不应抛异常: " + 异常引用.get());
        }
    }

    @Nested
    @DisplayName("NPC索引基础功能（回归保护）")
    class NPC索引基础功能 {

        @Test
        @DisplayName("注册任务后应能按NPC查询")
        void 注册任务_应能按NPC查询() {
            任务定义 定义 = 创建任务定义("测试任务1", "村民A", "村民B");
            服务.注册(定义);

            List<任务定义> 接取列表 = 服务.获取NPC接取任务("村民A");
            List<任务定义> 交付列表 = 服务.获取NPC交付任务("村民B");

            assertEquals(1, 接取列表.size(), "村民A应有1个接取任务");
            assertEquals(1, 交付列表.size(), "村民B应有1个交付任务");
            assertEquals("测试任务1", 接取列表.get(0).任务标识());
        }

        @Test
        @DisplayName("空NPC名应返回空列表")
        void 空NPC名_应返回空列表() {
            assertTrue(服务.获取NPC接取任务("").isEmpty(), "空NPC名应返回空列表");
            assertTrue(服务.获取NPC交付任务(null).isEmpty(), "null NPC名应返回空列表");
        }

        @Test
        @DisplayName("未注册NPC应返回空列表")
        void 未注册NPC_应返回空列表() {
            assertTrue(服务.获取NPC接取任务("不存在的NPC").isEmpty(), "未注册NPC应返回空列表");
        }
    }
}
