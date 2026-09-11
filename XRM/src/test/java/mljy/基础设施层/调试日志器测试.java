package mljy.基础设施层;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("FP-01 调试日志器测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 调试日志器测试 {

    @Mock
    private Logger 日志器;

    @BeforeEach
    void 重置静态状态() {
        调试日志器.初始化(null, false);
    }

    @AfterEach
    void 清理静态状态() {
        调试日志器.初始化(null, false);
    }

    @Nested
    @DisplayName("初始化阶段：模拟插件onEnable→配置加载→调试日志器初始化链路")
    class 初始化阶段 {

        @Test
        @DisplayName("初始化启用=true时应通知文件输出尚未配置")
        void 开关为true_初始化日志应输出() {
            调试日志器.初始化(日志器, true);

            ArgumentCaptor<String> 捕获器 = ArgumentCaptor.forClass(String.class);
            verify(日志器).info(捕获器.capture());

            assertTrue(捕获器.getValue().startsWith("[XRM-DEBUG]"),
                    "初始化消息应以[XRM-DEBUG]开头，实际：" + 捕获器.getValue());
        }

        @Test
        @DisplayName("初始化启用=false时应仍输出状态提示")
        void 开关为false_初始化日志仍应输出() {
            调试日志器.初始化(日志器, false);

            ArgumentCaptor<String> 捕获器 = ArgumentCaptor.forClass(String.class);
            verify(日志器).info(捕获器.capture());

            assertTrue(捕获器.getValue().startsWith("[XRM-DEBUG]"),
                    "初始化消息应以[XRM-DEBUG]开头，实际：" + 捕获器.getValue());
        }

        @Test
        @DisplayName("初始化后启用状态应可查询，便于业务模块自行判断")
        void 初始化后_启用状态应可查询() {
            调试日志器.初始化(日志器, true);
            assertTrue(调试日志器.启用(), "启用状态应为true");

            调试日志器.初始化(日志器, false);
            assertFalse(调试日志器.启用(), "启用状态应为false");
        }

        @Test
        @DisplayName("模拟onEnable链路：从config.yml读取全局开关→初始化调试日志器→验证开关状态生效")
        void 模拟onEnable链路_从配置读取开关_初始化调试日志器() {
            YamlConfiguration 配置 = new YamlConfiguration();
            配置.set("调试.全局开关", true);
            boolean 开关值 = 配置.getBoolean("调试.全局开关", true);

            调试日志器.初始化(日志器, 开关值);

            assertTrue(调试日志器.启用());
            ArgumentCaptor<String> 捕获器 = ArgumentCaptor.forClass(String.class);
            verify(日志器).info(捕获器.capture());
            assertTrue(捕获器.getValue().startsWith("[XRM-DEBUG]"));
        }
    }

    @Nested
    @DisplayName("全局开关开启：调试方法应安全执行不抛异常")
    class 全局开关开启 {

        @Test
        @DisplayName("多模块调用调试方法应安全执行（文件日志器未初始化时静默跳过）")
        void 多模块调用_应安全执行() {
            调试日志器.初始化(日志器, true);

            assertDoesNotThrow(() -> 调试日志器.调试("属性计算服务", "计算修饰后属性"));
            assertDoesNotThrow(() -> 调试日志器.调试("技能释放服务", "开始释放技能"));
            assertDoesNotThrow(() -> 调试日志器.调试("奥术冲击", "弹道命中目标"));
        }

        @Test
        @DisplayName("无参调试日志应安全执行")
        void 无参调试日志_应安全执行() {
            调试日志器.初始化(日志器, true);

            assertDoesNotThrow(() -> 调试日志器.调试("属性计算服务", "测试消息"));
        }

        @Test
        @DisplayName("带参数调试日志应安全执行并格式化参数")
        void 带参数调试日志_应安全执行() {
            调试日志器.初始化(日志器, true);

            assertDoesNotThrow(() -> 调试日志器.调试("技能释放服务", "技能=%s 冷却=%d", "奥术冲击", 60));
        }

        @Test
        @DisplayName("带异常调试日志应安全执行")
        void 带异常调试日志_应安全执行() {
            调试日志器.初始化(日志器, true);

            RuntimeException 异常 = new RuntimeException("测试异常");
            assertDoesNotThrow(() -> 调试日志器.调试("属性计算服务", "处理失败", 异常));
        }

        @Test
        @DisplayName("中文模块名调试调用应安全执行无乱码异常")
        void 中文模块名_应安全执行() {
            调试日志器.初始化(日志器, true);

            assertDoesNotThrow(() -> 调试日志器.调试("天赋管理服务", "加载天赋定义"));
        }
    }

    @Nested
    @DisplayName("全局开关关闭：调试日志不应输出，避免性能开销")
    class 全局开关关闭 {

        @Test
        @DisplayName("开关false时无参调试日志不应产生副作用")
        void 开关false_无参调试日志_应安全返回() {
            调试日志器.初始化(日志器, false);
            clearInvocations(日志器);

            调试日志器.调试("属性计算服务", "不应输出");

            verify(日志器, never()).info(anyString());
        }

        @Test
        @DisplayName("开关false时带参调试日志不应产生副作用")
        void 开关false_带参调试日志_应安全返回() {
            调试日志器.初始化(日志器, false);
            clearInvocations(日志器);

            调试日志器.调试("技能释放服务", "技能=%s 冷却=%d", "奥术冲击", 60);

            verify(日志器, never()).info(anyString());
        }

        @Test
        @DisplayName("开关false时带异常调试日志不应产生副作用")
        void 开关false_带异常调试日志_应安全返回() {
            调试日志器.初始化(日志器, false);
            clearInvocations(日志器);

            调试日志器.调试("属性计算服务", "处理失败", new RuntimeException("测试异常"));

            verify(日志器, never()).info(anyString());
        }
    }

    @Nested
    @DisplayName("未初始化场景：调试日志器未初始化时不应抛出异常")
    class 未初始化场景 {

        @Test
        @DisplayName("未初始化时调用调试方法应安全返回不抛异常")
        void 未初始化_调用调试方法_应安全返回() {
            调试日志器.初始化(null, true);

            assertDoesNotThrow(() -> 调试日志器.调试("属性计算服务", "不应抛异常"));
            assertDoesNotThrow(() -> 调试日志器.调试("属性计算服务", "参数=%s", "测试"));
            assertDoesNotThrow(() -> 调试日志器.调试("属性计算服务", "异常测试", new RuntimeException("测试")));
        }
    }

    @Nested
    @DisplayName("模块级开关：通过已禁用模块黑名单按模块独立控制调试输出")
    class 模块级开关 {

        @Test
        @DisplayName("已禁用模块静默跳过无参调试日志")
        void 已禁用模块_无参调试日志_应安全返回() {
            调试日志器.初始化(日志器, true, Set.of("总菜单"));

            assertDoesNotThrow(() -> 调试日志器.调试("总菜单", "不应输出"));
        }

        @Test
        @DisplayName("已禁用模块静默跳过带参调试日志")
        void 已禁用模块_带参调试日志_应安全返回() {
            调试日志器.初始化(日志器, true, Set.of("总菜单"));

            assertDoesNotThrow(() -> 调试日志器.调试("总菜单", "玩家=%s", "测试"));
        }

        @Test
        @DisplayName("已禁用模块静默跳过带异常调试日志")
        void 已禁用模块_带异常调试日志_应安全返回() {
            调试日志器.初始化(日志器, true, Set.of("总菜单"));

            assertDoesNotThrow(() -> 调试日志器.调试("总菜单", "处理失败", new RuntimeException("测试")));
        }

        @Test
        @DisplayName("未禁用模块应安全执行（文件日志器未初始化时静默跳过）")
        void 未禁用模块_调试日志_应安全执行() {
            调试日志器.初始化(日志器, true, Set.of("总菜单"));

            assertDoesNotThrow(() -> 调试日志器.调试("奥术冲击", "弹道命中"));
        }

        @Test
        @DisplayName("多模块场景：所有调用应安全执行")
        void 多模块场景_应安全执行() {
            调试日志器.初始化(日志器, true, Set.of("总菜单", "总菜单命令"));

            assertDoesNotThrow(() -> 调试日志器.调试("总菜单", "应被过滤"));
            assertDoesNotThrow(() -> 调试日志器.调试("总菜单命令", "应被过滤"));
            assertDoesNotThrow(() -> 调试日志器.调试("反伤监听器", "应输出"));
            assertDoesNotThrow(() -> 调试日志器.调试("躲闪监听器", "应输出"));
        }

        @Test
        @DisplayName("空禁用集合时所有模块正常执行")
        void 空禁用集合_所有模块应安全执行() {
            调试日志器.初始化(日志器, true, Set.of());

            assertDoesNotThrow(() -> 调试日志器.调试("总菜单", "应输出"));
            assertDoesNotThrow(() -> 调试日志器.调试("奥术冲击", "应输出"));
        }

        @Test
        @DisplayName("2参数重载等价于传入空禁用集合")
        void 两参数重载_等价于空禁用集合() {
            调试日志器.初始化(日志器, true);

            assertDoesNotThrow(() -> 调试日志器.调试("总菜单", "应输出"));
        }

        @Test
        @DisplayName("null禁用集合应安全降级为空集不抛异常")
        void null禁用集合_应安全降级为空集() {
            调试日志器.初始化(日志器, true, null);

            assertDoesNotThrow(() -> 调试日志器.调试("总菜单", "应输出"));
        }

        @Test
        @DisplayName("全局开关关闭时禁用模块配置不生效（全局开关优先级最高）")
        void 全局开关关闭_禁用模块配置_不应产生副作用() {
            调试日志器.初始化(日志器, false, Set.of("总菜单"));
            clearInvocations(日志器);

            调试日志器.调试("总菜单", "不应输出");
            调试日志器.调试("奥术冲击", "不应输出");

            verify(日志器, never()).info(anyString());
        }
    }
}
