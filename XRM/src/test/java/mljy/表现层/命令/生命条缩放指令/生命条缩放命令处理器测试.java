package mljy.表现层.命令.生命条缩放指令;

import 暮澜纪元.通用.文本.关键词解析器;
import mljy.翻译服务;
import mljy.基础设施层.数据库.生命条缩放设置存储;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FP-05 生命条缩放命令处理器 - 持久化与默认缩放逻辑")
class 生命条缩放命令处理器测试 {

    @Mock
    private 翻译服务 翻译服务mock;
    @Mock
    private JavaPlugin 插件mock;
    @Mock
    private 生命条缩放设置存储 持久化存储mock;
    @Mock
    private Player 玩家mock;
    @Mock
    private FileConfiguration 配置mock;
    @Mock
    private AttributeInstance 最大生命属性mock;

    private 生命条缩放命令处理器 处理器;
    private final UUID 玩家标识 = UUID.randomUUID();

    @BeforeEach
    void 准备() {
        关键词解析器 解析器 = 关键词解析器.创建纯文本();
        处理器 = new 生命条缩放命令处理器(翻译服务mock, 解析器, 插件mock, 持久化存储mock);
        when(持久化存储mock.连接池已启用()).thenReturn(true);
        when(持久化存储mock.获取连接池状态()).thenReturn("主机=localhost, 已启用=true");
    }

    @Test
    @DisplayName("应用持久化设置：玩家为空时返回false")
    void 应用持久化设置_玩家为空_返回false() {
        assertFalse(处理器.应用持久化设置(null));
        verify(持久化存储mock, never()).加载(any());
    }

    @Test
    @DisplayName("应用持久化设置：连接池未启用时返回false")
    void 应用持久化设置_连接池未启用_返回false() {
        when(玩家mock.getUniqueId()).thenReturn(玩家标识);
        when(持久化存储mock.连接池已启用()).thenReturn(false);
        when(持久化存储mock.获取连接池状态()).thenReturn("主机=localhost, 已启用=false");
        when(持久化存储mock.加载(玩家标识)).thenReturn(Optional.empty());

        assertFalse(处理器.应用持久化设置(玩家mock));

        verify(持久化存储mock).加载(玩家标识);
        verify(玩家mock, never()).setHealthScale(anyDouble());
    }

    @Test
    @DisplayName("应用持久化设置：无持久化记录时返回false")
    void 应用持久化设置_无记录_返回false() {
        when(玩家mock.getUniqueId()).thenReturn(玩家标识);
        when(持久化存储mock.加载(玩家标识)).thenReturn(Optional.empty());

        assertFalse(处理器.应用持久化设置(玩家mock));

        verify(持久化存储mock).加载(玩家标识);
        verify(玩家mock, never()).setHealthScale(anyDouble());
    }

    @Test
    @DisplayName("应用持久化设置：总体个数模式应用成功")
    void 应用持久化设置_总体个数_应用成功() {
        when(玩家mock.getUniqueId()).thenReturn(玩家标识);
        when(持久化存储mock.加载(玩家标识))
                .thenReturn(Optional.of(new 生命条缩放设置存储.缩放设置("总体个数", 30)));
        when(玩家mock.getHealthScale()).thenReturn(20.0);
        when(玩家mock.isHealthScaled()).thenReturn(false);

        boolean 结果 = 处理器.应用持久化设置(玩家mock);

        assertTrue(结果);
        verify(玩家mock).setHealthScale(60.0);
        verify(玩家mock).setHealthScaled(true);
    }

    @Test
    @DisplayName("应用持久化设置：单体个数模式应用成功")
    void 应用持久化设置_单体个数_应用成功() {
        when(玩家mock.getUniqueId()).thenReturn(玩家标识);
        when(持久化存储mock.加载(玩家标识))
                .thenReturn(Optional.of(new 生命条缩放设置存储.缩放设置("单体个数", 10)));
        when(玩家mock.getAttribute(Attribute.MAX_HEALTH)).thenReturn(最大生命属性mock);
        when(最大生命属性mock.getValue()).thenReturn(20.0);
        when(玩家mock.getHealthScale()).thenReturn(20.0);
        when(玩家mock.isHealthScaled()).thenReturn(false);

        boolean 结果 = 处理器.应用持久化设置(玩家mock);

        assertTrue(结果);
        verify(玩家mock).setHealthScale(4.0);
        verify(玩家mock).setHealthScaled(true);
    }

    @Test
    @DisplayName("应用持久化设置：未知模式时返回false")
    void 应用持久化设置_未知模式_返回false() {
        when(玩家mock.getUniqueId()).thenReturn(玩家标识);
        when(持久化存储mock.加载(玩家标识))
                .thenReturn(Optional.of(new 生命条缩放设置存储.缩放设置("未知模式", 10)));

        boolean 结果 = 处理器.应用持久化设置(玩家mock);

        assertFalse(结果);
        verify(玩家mock, never()).setHealthScale(anyDouble());
    }

    @Test
    @DisplayName("应用默认缩放：玩家为空时返回false")
    void 应用默认缩放_玩家为空_返回false() {
        assertFalse(处理器.应用默认缩放(null));
        verify(玩家mock, never()).setHealthScale(anyDouble());
    }

    @Test
    @DisplayName("应用默认缩放：使用配置默认值20，应用40.0缩放成功")
    void 应用默认缩放_正常_应用成功() {
        when(插件mock.getConfig()).thenReturn(配置mock);
        when(配置mock.getInt(eq("显示.生命条.默认总体红心数"), anyInt())).thenReturn(20);
        when(玩家mock.getUniqueId()).thenReturn(玩家标识);
        when(玩家mock.getHealthScale()).thenReturn(20.0);
        when(玩家mock.isHealthScaled()).thenReturn(false);

        boolean 结果 = 处理器.应用默认缩放(玩家mock);

        assertTrue(结果);
        verify(玩家mock).setHealthScale(40.0);
        verify(玩家mock).setHealthScaled(true);
    }

    @Test
    @DisplayName("应用默认缩放：配置值越界时回退到硬编码默认值20")
    void 应用默认缩放_配置越界_回退默认值() {
        when(插件mock.getConfig()).thenReturn(配置mock);
        when(配置mock.getInt(eq("显示.生命条.默认总体红心数"), anyInt())).thenReturn(999);
        when(玩家mock.getUniqueId()).thenReturn(玩家标识);
        when(玩家mock.getHealthScale()).thenReturn(20.0);
        when(玩家mock.isHealthScaled()).thenReturn(false);

        boolean 结果 = 处理器.应用默认缩放(玩家mock);

        assertTrue(结果);
        verify(玩家mock).setHealthScale(40.0);
    }

    @Test
    @DisplayName("应用默认缩放：玩家setHealthScale抛异常时返回false")
    void 应用默认缩放_setHealthScale抛异常_返回false() {
        when(插件mock.getConfig()).thenReturn(配置mock);
        when(配置mock.getInt(eq("显示.生命条.默认总体红心数"), anyInt())).thenReturn(20);
        when(玩家mock.getUniqueId()).thenReturn(玩家标识);
        when(玩家mock.getHealthScale()).thenReturn(20.0);
        when(玩家mock.isHealthScaled()).thenReturn(false);
        org.mockito.Mockito.doThrow(new IllegalArgumentException("缩放值非法"))
                .when(玩家mock).setHealthScale(anyDouble());

        boolean 结果 = 处理器.应用默认缩放(玩家mock);

        assertFalse(结果);
    }
}
