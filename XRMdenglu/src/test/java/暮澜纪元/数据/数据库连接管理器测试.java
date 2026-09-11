package 暮澜纪元.数据;

import com.zaxxer.hikari.HikariDataSource;
import 暮澜纪元.登录服插件;
import 暮澜纪元.职业.职业;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("数据库连接管理器")
@SuppressWarnings("null")
class 数据库连接管理器测试 {

    private 登录服插件 mock插件;
    private 数据库连接管理器 管理器;

    @BeforeEach
    void 准备() {
        mock插件 = mock(登录服插件.class);
        when(mock插件.getLogger()).thenReturn(Logger.getLogger("test"));
        管理器 = new 数据库连接管理器(mock插件);
    }

    @Nested
    @DisplayName("初始化")
    class 初始化组 {

        @Test
        @DisplayName("初始化_配置无效_应返回false")
        void 初始化_配置无效_应返回false() {
            when(mock插件.获取配置管理器()).thenReturn(null);
            assertFalse(管理器.初始化());
        }
    }

    @Nested
    @DisplayName("获取连接")
    class 获取连接组 {

        @Test
        @DisplayName("获取连接_数据源未初始化_应抛出SQLException")
        void 获取连接_数据源未初始化_应抛出SQLException() {
            SQLException 异常 = assertThrows(SQLException.class, () -> 管理器.获取连接());
            assertTrue(异常.getMessage().contains("数据源未初始化或已关闭"));
        }
    }

    @Nested
    @DisplayName("关闭")
    class 关闭组 {

        @Test
        @DisplayName("关闭_数据源为null_不应抛异常")
        void 关闭_数据源为null_不应抛异常() {
            assertDoesNotThrow(() -> 管理器.关闭());
        }
    }

    @Nested
    @DisplayName("加载玩家数据")
    class 加载玩家数据组 {

        private Connection mock连接;
        private PreparedStatement mock语句;
        private ResultSet mock结果集;

        @BeforeEach
        void 准备数据源() throws Exception {
            HikariDataSource mock数据源 = mock(HikariDataSource.class);
            mock连接 = mock(Connection.class);
            mock语句 = mock(PreparedStatement.class);
            mock结果集 = mock(ResultSet.class);
            when(mock数据源.getConnection()).thenReturn(mock连接);
            when(mock连接.prepareStatement(anyString())).thenReturn(mock语句);
            when(mock语句.executeQuery()).thenReturn(mock结果集);
            设置数据源字段(mock数据源);
        }

        @Test
        @DisplayName("加载玩家数据_玩家不存在_应返回空Optional")
        void 加载玩家数据_玩家不存在_应返回空Optional() throws Exception {
            when(mock结果集.next()).thenReturn(false);
            Optional<玩家数据> 结果 = 管理器.加载玩家数据(UUID.randomUUID());
            assertTrue(结果.isEmpty());
        }

        @Test
        @DisplayName("加载玩家数据_玩家存在_应返回数据")
        void 加载玩家数据_玩家存在_应返回数据() throws Exception {
            UUID 测试UUID = UUID.randomUUID();
            when(mock结果集.next()).thenReturn(true);
            when(mock结果集.getString("玩家名")).thenReturn("测试玩家");
            when(mock结果集.getString("职业")).thenReturn("奥能法师");
            when(mock结果集.getInt("等级")).thenReturn(60);
            Optional<玩家数据> 结果 = 管理器.加载玩家数据(测试UUID);
            assertTrue(结果.isPresent());
            玩家数据 数据 = 结果.get();
            assertEquals(测试UUID, 数据.获取UUID());
            assertEquals("测试玩家", 数据.获取名字());
            assertEquals(职业.奥能法师, 数据.获取职业());
            assertEquals(60, 数据.获取等级());
        }
    }

    @Nested
    @DisplayName("保存玩家数据")
    class 保存玩家数据组 {

        private Connection mock连接;
        private PreparedStatement mock语句;

        @BeforeEach
        void 准备数据源() throws Exception {
            HikariDataSource mock数据源 = mock(HikariDataSource.class);
            mock连接 = mock(Connection.class);
            mock语句 = mock(PreparedStatement.class);
            when(mock数据源.getConnection()).thenReturn(mock连接);
            when(mock连接.prepareStatement(anyString())).thenReturn(mock语句);
            设置数据源字段(mock数据源);
        }

        @Test
        @DisplayName("保存玩家数据_正常_应执行UPSERT并返回true")
        void 保存玩家数据_正常_应执行UPSERT并返回true() throws Exception {
            UUID 测试UUID = UUID.randomUUID();
            玩家数据 数据 = new 玩家数据(测试UUID, "测试玩家");
            数据.设置职业(职业.奥能法师);
            数据.设置等级(60);
            boolean 结果 = 管理器.保存玩家数据(数据);
            assertTrue(结果);
            verify(mock语句).setString(1, 测试UUID.toString());
            verify(mock语句).setString(2, "测试玩家");
            verify(mock语句).setString(3, "奥能法师");
            verify(mock语句).setInt(4, 60);
            verify(mock语句).executeUpdate();
        }

        @Test
        @DisplayName("保存玩家数据_数据库异常_应返回false")
        void 保存玩家数据_数据库异常_应返回false() throws Exception {
            UUID 测试UUID = UUID.randomUUID();
            玩家数据 数据 = new 玩家数据(测试UUID, "测试玩家");
            when(mock语句.executeUpdate()).thenThrow(new SQLException("模拟数据库错误"));
            boolean 结果 = 管理器.保存玩家数据(数据);
            assertFalse(结果);
        }
    }

    @Nested
    @DisplayName("获取或创建玩家数据")
    class 获取或创建玩家数据组 {

        @Test
        @DisplayName("获取或创建_玩家已存在_应返回已有数据")
        void 获取或创建_玩家已存在_应返回已有数据() {
            数据库连接管理器 spy管理器 = spy(new 数据库连接管理器(mock插件));
            UUID 测试UUID = UUID.randomUUID();
            玩家数据 已有数据 = new 玩家数据(测试UUID, "已有玩家");
            doReturn(Optional.of(已有数据)).when(spy管理器).加载玩家数据(any());
            玩家数据 结果 = spy管理器.获取或创建玩家数据(测试UUID, "新名字");
            assertSame(已有数据, 结果);
            verify(spy管理器, never()).保存玩家数据(any());
        }

        @Test
        @DisplayName("获取或创建_玩家不存在_应创建并保存")
        void 获取或创建_玩家不存在_应创建并保存() {
            数据库连接管理器 spy管理器 = spy(new 数据库连接管理器(mock插件));
            UUID 测试UUID = UUID.randomUUID();
            doReturn(Optional.empty()).when(spy管理器).加载玩家数据(any());
            doReturn(true).when(spy管理器).保存玩家数据(any());
            玩家数据 结果 = spy管理器.获取或创建玩家数据(测试UUID, "新玩家");
            assertEquals(测试UUID, 结果.获取UUID());
            assertEquals("新玩家", 结果.获取名字());
            verify(spy管理器).保存玩家数据(any());
        }
    }

    private void 设置数据源字段(HikariDataSource 数据源) throws Exception {
        Field 字段 = 数据库连接管理器.class.getDeclaredField("数据源");
        字段.setAccessible(true);
        字段.set(管理器, 数据源);
    }
}
