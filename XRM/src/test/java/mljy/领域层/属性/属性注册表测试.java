package mljy.领域层.属性;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("属性注册表")
class 属性注册表测试 {

    private 属性注册表 注册表;

    @BeforeEach
    void setUp() {
        注册表 = new 属性注册表();
    }

    @Nested
    @DisplayName("标准属性注册")
    class 标准属性注册 {

        @Test
        @DisplayName("构造时应自动注册14项标准属性")
        void 构造_应注册14项标准属性() {
            List<String> 命令名 = 注册表.获取所有命令名();

            assertEquals(14, 命令名.size());
            assertTrue(命令名.contains("生命值"));
            assertTrue(命令名.contains("公CD修正"));
            assertTrue(命令名.contains("急速"));
            assertTrue(命令名.contains("力量"));
            assertTrue(命令名.contains("敏捷"));
            assertTrue(命令名.contains("智力"));
            assertTrue(命令名.contains("法术暴击几率"));
            assertTrue(命令名.contains("法术暴击伤害"));
            assertTrue(命令名.contains("精通"));
            assertTrue(命令名.contains("全能"));
            assertTrue(命令名.contains("吸血"));
            assertTrue(命令名.contains("躲闪"));
            assertTrue(命令名.contains("恢复"));
            assertTrue(命令名.contains("移速"));
        }

        @Test
        @DisplayName("查询存在的命令名应返回对应元数据")
        void 查询_存在命令名_应返回元数据() {
            Optional<属性元数据> 元数据 = 注册表.查询("生命值");

            assertTrue(元数据.isPresent());
            assertEquals("生命值", 元数据.get().命令名());
            assertEquals("生命值上限", 元数据.get().修饰器类别());
            assertEquals("管理员指令.属性.查看.生命值", 元数据.get().显示翻译键());
        }

        @Test
        @DisplayName("查询公CD修正应返回公共冷却时间修饰器类别")
        void 查询公CD修正_应返回公共冷却时间() {
            Optional<属性元数据> 元数据 = 注册表.查询("公CD修正");

            assertTrue(元数据.isPresent());
            assertEquals("公CD修正", 元数据.get().命令名());
            assertEquals("公共冷却时间", 元数据.get().修饰器类别());
        }

        @Test
        @DisplayName("查询恢复应返回生命恢复修饰器类别")
        void 查询恢复_应返回生命恢复() {
            Optional<属性元数据> 元数据 = 注册表.查询("恢复");

            assertTrue(元数据.isPresent());
            assertEquals("恢复", 元数据.get().命令名());
            assertEquals("生命恢复", 元数据.get().修饰器类别());
        }

        @Test
        @DisplayName("查询法术暴击几率应返回正确元数据")
        void 查询法术暴击几率_应返回正确元数据() {
            Optional<属性元数据> 元数据 = 注册表.查询("法术暴击几率");

            assertTrue(元数据.isPresent());
            assertEquals("法术暴击几率", 元数据.get().命令名());
            assertEquals("法术暴击几率", 元数据.get().修饰器类别());
            assertEquals("管理员指令.属性.查看.法术暴击几率", 元数据.get().显示翻译键());
        }

        @Test
        @DisplayName("查询法术暴击伤害应返回正确元数据")
        void 查询法术暴击伤害_应返回正确元数据() {
            Optional<属性元数据> 元数据 = 注册表.查询("法术暴击伤害");

            assertTrue(元数据.isPresent());
            assertEquals("法术暴击伤害", 元数据.get().命令名());
            assertEquals("法术暴击伤害", 元数据.get().修饰器类别());
            assertEquals("管理员指令.属性.查看.法术暴击伤害", 元数据.get().显示翻译键());
        }

        @Test
        @DisplayName("查询不存在的命令名应返回empty")
        void 查询_不存在命令名_应返回empty() {
            Optional<属性元数据> 元数据 = 注册表.查询("不存在的属性");

            assertTrue(元数据.isEmpty());
        }

        @Test
        @DisplayName("包含方法应正确判断命令名是否存在")
        void 包含_应正确判断() {
            assertTrue(注册表.包含("全能"));
            assertTrue(注册表.包含("躲闪"));
            assertFalse(注册表.包含("反伤"));
            assertFalse(注册表.包含(""));
        }
    }

    @Nested
    @DisplayName("动态注册(可插入性)")
    class 动态注册 {

        @Test
        @DisplayName("注册新属性后应能查询到")
        void 注册新属性_应能查询到() {
            注册表.注册(new 属性元数据("反伤", "反伤", "管理员指令.属性.查看.反伤"));

            Optional<属性元数据> 元数据 = 注册表.查询("反伤");

            assertTrue(元数据.isPresent());
            assertEquals("反伤", 元数据.get().命令名());
            assertEquals("管理员指令.属性.查看.反伤", 元数据.get().显示翻译键());
        }

        @Test
        @DisplayName("注册新属性后获取所有命令名应包含新属性")
        void 注册新属性_获取所有命令名_应包含() {
            int 初始数量 = 注册表.获取所有命令名().size();

            注册表.注册(new 属性元数据("反伤", "反伤", "管理员指令.属性.查看.反伤"));

            List<String> 命令名 = 注册表.获取所有命令名();
            assertEquals(初始数量 + 1, 命令名.size());
            assertTrue(命令名.contains("反伤"));
        }

        @Test
        @DisplayName("重复注册相同命令名应被忽略(幂等)")
        void 重复注册相同命令名_应被忽略() {
            int 初始数量 = 注册表.获取所有命令名().size();

            注册表.注册(new 属性元数据("生命值", "新类别", "新翻译键"));
            注册表.注册(new 属性元数据("生命值", "又一样", "又一键"));

            assertEquals(初始数量, 注册表.获取所有命令名().size());
            Optional<属性元数据> 元数据 = 注册表.查询("生命值");
            assertTrue(元数据.isPresent());
            assertEquals("生命值上限", 元数据.get().修饰器类别());
            assertEquals("管理员指令.属性.查看.生命值", 元数据.get().显示翻译键());
        }

        @Test
        @DisplayName("注册null元数据应被忽略")
        void 注册null_应被忽略() {
            int 初始数量 = 注册表.获取所有命令名().size();

            注册表.注册(null);

            assertEquals(初始数量, 注册表.获取所有命令名().size());
        }

        @Test
        @DisplayName("注册null命令名应被忽略")
        void 注册null命令名_应被忽略() {
            int 初始数量 = 注册表.获取所有命令名().size();

            注册表.注册(new 属性元数据(null, "类别", "翻译键"));

            assertEquals(初始数量, 注册表.获取所有命令名().size());
        }
    }

    @Nested
    @DisplayName("注册顺序(松散性验证)")
    class 注册顺序 {

        @Test
        @DisplayName("获取所有元数据应按注册顺序返回")
        void 获取所有元数据_应按注册顺序() {
            List<属性元数据> 元数据列表 = 注册表.获取所有元数据();

            assertEquals(14, 元数据列表.size());
            assertEquals("生命值", 元数据列表.get(0).命令名());
            assertEquals("公CD修正", 元数据列表.get(1).命令名());
            assertEquals("急速", 元数据列表.get(2).命令名());
            assertEquals("力量", 元数据列表.get(3).命令名());
            assertEquals("移速", 元数据列表.get(13).命令名());
        }

        @Test
        @DisplayName("动态注册的新属性应追加到列表末尾")
        void 动态注册_应追加到末尾() {
            注册表.注册(new 属性元数据("反伤", "反伤", "管理员指令.属性.查看.反伤"));

            List<属性元数据> 元数据列表 = 注册表.获取所有元数据();
            assertEquals(15, 元数据列表.size());
            assertEquals("反伤", 元数据列表.get(14).命令名());
        }

        @Test
        @DisplayName("获取所有元数据返回不可变列表")
        void 获取所有元数据_应返回不可变列表() {
            List<属性元数据> 元数据列表 = 注册表.获取所有元数据();

            assertThrows(UnsupportedOperationException.class, () -> 元数据列表.add(
                    new 属性元数据("x", "y", "z")));
        }

        @Test
        @DisplayName("获取所有命令名返回不可变列表")
        void 获取所有命令名_应返回不可变列表() {
            List<String> 命令名 = 注册表.获取所有命令名();

            assertThrows(UnsupportedOperationException.class, () -> 命令名.add("x"));
        }
    }

    @Nested
    @DisplayName("线程安全")
    class 线程安全 {

        @Test
        @DisplayName("并发注册不应丢失或重复")
        void 并发注册_不应丢失或重复() throws InterruptedException {
            int 线程数 = 20;
            int 每线程注册数 = 10;
            ExecutorService 线程池 = Executors.newFixedThreadPool(线程数);
            CountDownLatch 启动门 = new CountDownLatch(1);
            CountDownLatch 完成门 = new CountDownLatch(线程数);

            for (int i = 0; i < 线程数; i++) {
                final int 线程序号 = i;
                线程池.submit(() -> {
                    try {
                        启动门.await();
                        for (int j = 0; j < 每线程注册数; j++) {
                            String 命令名 = "并发属性_" + 线程序号 + "_" + j;
                            注册表.注册(new 属性元数据(命令名, 命令名, "翻译键_" + 命令名));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        完成门.countDown();
                    }
                });
            }

            启动门.countDown();
            assertTrue(完成门.await(10, TimeUnit.SECONDS));
            线程池.shutdown();

            List<String> 命令名 = 注册表.获取所有命令名();
            assertEquals(14 + 线程数 * 每线程注册数, 命令名.size());
        }

        @Test
        @DisplayName("并发查询与注册应不抛异常")
        void 并发查询与注册_不应抛异常() throws InterruptedException {
            int 线程数 = 10;
            ExecutorService 线程池 = Executors.newFixedThreadPool(线程数);
            CountDownLatch 完成门 = new CountDownLatch(线程数);
            AtomicInteger 异常计数 = new AtomicInteger();

            for (int i = 0; i < 线程数; i++) {
                final int 序号 = i;
                线程池.submit(() -> {
                    try {
                        for (int j = 0; j < 100; j++) {
                            if (序号 % 2 == 0) {
                                注册表.注册(new 属性元数据("查询属性_" + 序号 + "_" + j,
                                        "类别", "翻译键"));
                            } else {
                                注册表.查询("生命值");
                                注册表.包含("全能");
                                注册表.获取所有命令名();
                            }
                        }
                    } catch (Exception e) {
                        异常计数.incrementAndGet();
                    } finally {
                        完成门.countDown();
                    }
                });
            }

            assertTrue(完成门.await(10, TimeUnit.SECONDS));
            线程池.shutdown();
            assertEquals(0, 异常计数.get());
        }
    }
}
