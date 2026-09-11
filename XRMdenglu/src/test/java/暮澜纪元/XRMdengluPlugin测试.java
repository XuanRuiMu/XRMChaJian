package 暮澜纪元;

import mljy.XRMdengluPlugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("XRMdengluPlugin")
@SuppressWarnings("null")
class XRMdengluPlugin测试 {

    private XRMdengluPlugin 插件;

    @BeforeEach
    void 准备() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        插件 = (XRMdengluPlugin) unsafe.allocateInstance(XRMdengluPlugin.class);
    }

    @AfterEach
    void 清理() throws Exception {
        Field 外部插件实例Field = 登录服插件.class.getDeclaredField("外部插件实例");
        外部插件实例Field.setAccessible(true);
        外部插件实例Field.set(null, null);
        Field 实例Field = 登录服插件.class.getDeclaredField("实例");
        实例Field.setAccessible(true);
        实例Field.set(null, null);
    }

    @Nested
    @DisplayName("类加载")
    class 类加载 {

        @Test
        @DisplayName("类应可加载")
        void 类应可加载() {
            assertNotNull(XRMdengluPlugin.class);
        }
    }

    @Nested
    @DisplayName("onLoad")
    class onLoad测试 {

        @Test
        @DisplayName("应设置外部实例")
        void 应设置外部实例() throws Exception {
            插件.onLoad();

            Field 外部插件实例Field = 登录服插件.class.getDeclaredField("外部插件实例");
            外部插件实例Field.setAccessible(true);
            JavaPlugin 外部实例 = (JavaPlugin) 外部插件实例Field.get(null);
            assertSame(插件, 外部实例);
        }
    }

    @Nested
    @DisplayName("onEnable")
    class onEnable测试 {

        @Test
        @DisplayName("应创建并启动登录服插件")
        void 应创建并启动登录服插件() {
            try (MockedConstruction<登录服插件> mockedConstruction = mockConstruction(登录服插件.class)) {
                插件.onEnable();

                assertEquals(1, mockedConstruction.constructed().size());
                登录服插件 mock实例 = mockedConstruction.constructed().get(0);
                verify(mock实例).启动(插件);
            }
        }

        @Test
        @DisplayName("应将登录服插件实例保存到字段")
        void 应将登录服插件实例保存到字段() throws Exception {
            try (MockedConstruction<登录服插件> mockedConstruction = mockConstruction(登录服插件.class)) {
                插件.onEnable();

                Field 中文插件实例Field = XRMdengluPlugin.class.getDeclaredField("中文插件实例");
                中文插件实例Field.setAccessible(true);
                登录服插件 字段值 = (登录服插件) 中文插件实例Field.get(插件);
                assertNotNull(字段值);
                assertEquals(1, mockedConstruction.constructed().size());
                assertSame(mockedConstruction.constructed().get(0), 字段值);
            }
        }
    }

    @Nested
    @DisplayName("onDisable")
    class onDisable测试 {

        @Test
        @DisplayName("实例存在时应调用关闭")
        void 实例存在时应调用关闭() throws Exception {
            登录服插件 mock中文插件 = mock(登录服插件.class);
            Field 中文插件实例Field = XRMdengluPlugin.class.getDeclaredField("中文插件实例");
            中文插件实例Field.setAccessible(true);
            中文插件实例Field.set(插件, mock中文插件);

            插件.onDisable();

            verify(mock中文插件).关闭();
        }

        @Test
        @DisplayName("实例为null时不应崩溃")
        void 实例为null时不应崩溃() throws Exception {
            Field 中文插件实例Field = XRMdengluPlugin.class.getDeclaredField("中文插件实例");
            中文插件实例Field.setAccessible(true);
            中文插件实例Field.set(插件, null);

            assertDoesNotThrow(() -> 插件.onDisable());
        }
    }
}
