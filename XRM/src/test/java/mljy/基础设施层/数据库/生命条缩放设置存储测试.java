package mljy.基础设施层.数据库;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("FP-9 生命条缩放设置存储 - MySQL 持久化 Repository 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class 生命条缩放设置存储测试 {

    private static final String 保存SQL =
            "REPLACE INTO 生命条缩放设置 (玩家UUID, 模式, 数值) VALUES (?, ?, ?)";
    private static final String 加载SQL =
            "SELECT 模式, 数值 FROM 生命条缩放设置 WHERE 玩家UUID = ?";
    private static final String 删除SQL =
            "DELETE FROM 生命条缩放设置 WHERE 玩家UUID = ?";

    @Mock
    private 数据库连接池 连接池;
    @Mock
    private Connection 连接;
    @Mock
    private PreparedStatement 语句;
    @Mock
    private ResultSet 结果集;

    private 生命条缩放设置存储 存储;
    private UUID 玩家标识;

    @BeforeEach
    void setUp() throws SQLException {
        存储 = new 生命条缩放设置存储(连接池);
        玩家标识 = UUID.randomUUID();
        when(连接池.已启用()).thenReturn(true);
        when(连接池.获取连接()).thenReturn(连接);
        when(连接.prepareStatement(anyString())).thenReturn(语句);
    }

    @Nested
    @DisplayName("保存 - REPLACE INTO 持久化写入")
    class 保存测试 {

        @Test
        @DisplayName("成功 - 应使用参数化 SQL 并设置参数后执行更新")
        void 成功_应使用参数化SQL并设置参数后执行更新() throws SQLException {
            boolean 结果 = 存储.保存(玩家标识, "总体个数", 30);

            assertTrue(结果);
            verify(连接).prepareStatement(保存SQL);
            verify(语句).setString(1, 玩家标识.toString());
            verify(语句).setString(2, "总体个数");
            verify(语句).setInt(3, 30);
            verify(语句).executeUpdate();
        }

        @Test
        @DisplayName("单体模式成功 - 应传递单体个数与数值")
        void 单体模式_应传递单体个数与数值() throws SQLException {
            boolean 结果 = 存储.保存(玩家标识, "单体个数", 50);

            assertTrue(结果);
            verify(语句).setString(2, "单体个数");
            verify(语句).setInt(3, 50);
        }

        @Test
        @DisplayName("连接池未启用 - 应跳过并返回false")
        void 连接池未启用_应跳过() throws SQLException {
            when(连接池.已启用()).thenReturn(false);

            boolean 结果 = 存储.保存(玩家标识, "总体个数", 30);

            assertFalse(结果);
            verify(连接池, never()).获取连接();
            verify(语句, never()).executeUpdate();
        }

        @Test
        @DisplayName("UUID为null - 应跳过并返回false")
        void uuid为null_应跳过() throws SQLException {
            boolean 结果 = 存储.保存(null, "总体个数", 30);

            assertFalse(结果);
            verify(连接池, never()).获取连接();
        }

        @Test
        @DisplayName("模式为null - 应跳过并返回false")
        void 模式为null_应跳过() throws SQLException {
            boolean 结果 = 存储.保存(玩家标识, null, 30);

            assertFalse(结果);
            verify(连接池, never()).获取连接();
        }

        @Test
        @DisplayName("模式为空白 - 应跳过并返回false")
        void 模式为空白_应跳过() throws SQLException {
            boolean 结果 = 存储.保存(玩家标识, "   ", 30);

            assertFalse(结果);
            verify(连接池, never()).获取连接();
        }

        @Test
        @DisplayName("SQLException - 应捕获并返回false不抛出")
        void sql异常_应返回false不抛出() throws SQLException {
            when(语句.executeUpdate()).thenThrow(new SQLException("模拟SQL错误"));

            boolean 结果 = 存储.保存(玩家标识, "总体个数", 30);

            assertFalse(结果);
        }

        @Test
        @DisplayName("获取连接抛SQLException - 应捕获并返回false")
        void 获取连接异常_应返回false() throws SQLException {
            when(连接池.获取连接()).thenThrow(new SQLException("连接失败"));

            boolean 结果 = 存储.保存(玩家标识, "总体个数", 30);

            assertFalse(结果);
        }

        @Test
        @DisplayName("prepareStatement抛SQLException - 应捕获并返回false")
        void prepareStatement异常_应返回false() throws SQLException {
            when(连接.prepareStatement(anyString())).thenThrow(new SQLException("预编译失败"));

            boolean 结果 = 存储.保存(玩家标识, "总体个数", 30);

            assertFalse(结果);
        }
    }

    @Nested
    @DisplayName("加载 - SELECT 持久化记录")
    class 加载测试 {

        @Test
        @DisplayName("有记录 - 应解析模式与数值并返回Optional")
        void 有记录_应解析模式与数值() throws SQLException {
            when(语句.executeQuery()).thenReturn(结果集);
            when(结果集.next()).thenReturn(true);
            when(结果集.getString("模式")).thenReturn("总体个数");
            when(结果集.getInt("数值")).thenReturn(30);

            Optional<生命条缩放设置存储.缩放设置> 结果 = 存储.加载(玩家标识);

            assertTrue(结果.isPresent());
            assertEquals("总体个数", 结果.get().模式());
            assertEquals(30, 结果.get().数值());
            verify(连接).prepareStatement(加载SQL);
            verify(语句).setString(1, 玩家标识.toString());
        }

        @Test
        @DisplayName("无记录 - ResultSet.next()为false应返回empty")
        void 无记录_应返回empty() throws SQLException {
            when(语句.executeQuery()).thenReturn(结果集);
            when(结果集.next()).thenReturn(false);

            Optional<生命条缩放设置存储.缩放设置> 结果 = 存储.加载(玩家标识);

            assertTrue(结果.isEmpty());
            verify(结果集, never()).getString(anyString());
        }

        @Test
        @DisplayName("连接池未启用 - 应返回empty且不访问数据库")
        void 连接池未启用_应返回empty() throws SQLException {
            when(连接池.已启用()).thenReturn(false);

            Optional<生命条缩放设置存储.缩放设置> 结果 = 存储.加载(玩家标识);

            assertTrue(结果.isEmpty());
            verify(连接池, never()).获取连接();
        }

        @Test
        @DisplayName("UUID为null - 应返回empty且不访问数据库")
        void uuid为null_应返回empty() throws SQLException {
            Optional<生命条缩放设置存储.缩放设置> 结果 = 存储.加载(null);

            assertTrue(结果.isEmpty());
            verify(连接池, never()).获取连接();
        }

        @Test
        @DisplayName("executeQuery抛SQLException - 应返回empty")
        void executeQuery异常_应返回empty() throws SQLException {
            when(语句.executeQuery()).thenThrow(new SQLException("查询失败"));

            Optional<生命条缩放设置存储.缩放设置> 结果 = 存储.加载(玩家标识);

            assertTrue(结果.isEmpty());
        }

        @Test
        @DisplayName("获取连接抛SQLException - 应返回empty")
        void 获取连接异常_应返回empty() throws SQLException {
            when(连接池.获取连接()).thenThrow(new SQLException("连接失败"));

            Optional<生命条缩放设置存储.缩放设置> 结果 = 存储.加载(玩家标识);

            assertTrue(结果.isEmpty());
        }

        @Test
        @DisplayName("结果集解析抛SQLException - 应返回empty")
        void 结果集解析异常_应返回empty() throws SQLException {
            when(语句.executeQuery()).thenReturn(结果集);
            when(结果集.next()).thenReturn(true);
            when(结果集.getString("模式")).thenThrow(new SQLException("解析失败"));

            Optional<生命条缩放设置存储.缩放设置> 结果 = 存储.加载(玩家标识);

            assertTrue(结果.isEmpty());
        }
    }

    @Nested
    @DisplayName("删除 - DELETE 持久化记录")
    class 删除测试 {

        @Test
        @DisplayName("成功 - 应使用参数化 SQL 并设置UUID后执行删除")
        void 成功_应使用参数化SQL并设置UUID() throws SQLException {
            boolean 结果 = 存储.删除(玩家标识);

            assertTrue(结果);
            verify(连接).prepareStatement(删除SQL);
            verify(语句).setString(1, 玩家标识.toString());
            verify(语句).executeUpdate();
        }

        @Test
        @DisplayName("连接池未启用 - 应跳过并返回false")
        void 连接池未启用_应跳过() throws SQLException {
            when(连接池.已启用()).thenReturn(false);

            boolean 结果 = 存储.删除(玩家标识);

            assertFalse(结果);
            verify(连接池, never()).获取连接();
        }

        @Test
        @DisplayName("UUID为null - 应跳过并返回false")
        void uuid为null_应跳过() throws SQLException {
            boolean 结果 = 存储.删除(null);

            assertFalse(结果);
            verify(连接池, never()).获取连接();
        }

        @Test
        @DisplayName("SQLException - 应捕获并返回false不抛出")
        void sql异常_应返回false不抛出() throws SQLException {
            when(语句.executeUpdate()).thenThrow(new SQLException("模拟SQL错误"));

            boolean 结果 = 存储.删除(玩家标识);

            assertFalse(结果);
        }

        @Test
        @DisplayName("获取连接抛SQLException - 应捕获并返回false")
        void 获取连接异常_应返回false() throws SQLException {
            when(连接池.获取连接()).thenThrow(new SQLException("连接失败"));

            boolean 结果 = 存储.删除(玩家标识);

            assertFalse(结果);
        }
    }

    @Nested
    @DisplayName("SQL 安全 - 参数化查询验证")
    class SQL安全验证 {

        @Test
        @DisplayName("保存SQL - 应使用占位符而非字符串拼接")
        void 保存sql_应使用占位符() throws SQLException {
            存储.保存(玩家标识, "总体个数", 30);

            verify(连接).prepareStatement(eq(保存SQL));
            verify(语句).setString(1, 玩家标识.toString());
            verify(语句).setString(2, "总体个数");
            verify(语句).setInt(3, 30);
        }

        @Test
        @DisplayName("加载SQL - 应使用占位符而非字符串拼接")
        void 加载sql_应使用占位符() throws SQLException {
            when(语句.executeQuery()).thenReturn(结果集);
            when(结果集.next()).thenReturn(false);

            存储.加载(玩家标识);

            verify(连接).prepareStatement(eq(加载SQL));
            verify(语句).setString(1, 玩家标识.toString());
        }

        @Test
        @DisplayName("删除SQL - 应使用占位符而非字符串拼接")
        void 删除sql_应使用占位符() throws SQLException {
            存储.删除(玩家标识);

            verify(连接).prepareStatement(eq(删除SQL));
            verify(语句).setString(1, 玩家标识.toString());
        }

        @Test
        @DisplayName("UUID转换 - 应通过toString()序列化为标准UUID字符串")
        void uuid转换_应调用toString() throws SQLException {
            存储.保存(玩家标识, "总体个数", 30);

            verify(语句).setString(1, 玩家标识.toString());
        }
    }

    @Nested
    @DisplayName("record 缩放设置 - 不可变数据载体")
    class 缩放设置record测试 {

        @Test
        @DisplayName("构造与访问 - 应正确存储模式与数值")
        void 构造与访问_应正确存储() {
            生命条缩放设置存储.缩放设置 设置 = new 生命条缩放设置存储.缩放设置("总体个数", 30);

            assertEquals("总体个数", 设置.模式());
            assertEquals(30, 设置.数值());
        }

        @Test
        @DisplayName("相等性 - 相同模式与数值应相等")
        void 相等性_相同模式与数值应相等() {
            生命条缩放设置存储.缩放设置 设置1 = new 生命条缩放设置存储.缩放设置("总体个数", 30);
            生命条缩放设置存储.缩放设置 设置2 = new 生命条缩放设置存储.缩放设置("总体个数", 30);

            assertEquals(设置1, 设置2);
            assertEquals(设置1.hashCode(), 设置2.hashCode());
        }

        @Test
        @DisplayName("不相等 - 不同模式或数值应不等")
        void 不相等_不同模式或数值应不等() {
            生命条缩放设置存储.缩放设置 设置1 = new 生命条缩放设置存储.缩放设置("总体个数", 30);
            生命条缩放设置存储.缩放设置 设置2 = new 生命条缩放设置存储.缩放设置("单体个数", 30);
            生命条缩放设置存储.缩放设置 设置3 = new 生命条缩放设置存储.缩放设置("总体个数", 50);

            assertNotEquals(设置1, 设置2);
            assertNotEquals(设置1, 设置3);
        }
    }

    @Nested
    @DisplayName("FP-7 持久化完整流程 - 保存/加载/删除闭环验证")
    class 持久化完整流程 {

        @Test
        @DisplayName("保存后加载 - 应返回与保存时相同的模式与数值")
        void 保存后加载_应返回相同值() throws SQLException {
            when(语句.executeQuery()).thenReturn(结果集);
            when(结果集.next()).thenReturn(true);
            when(结果集.getString("模式")).thenReturn("总体个数");
            when(结果集.getInt("数值")).thenReturn(30);

            boolean 保存结果 = 存储.保存(玩家标识, "总体个数", 30);
            Optional<生命条缩放设置存储.缩放设置> 加载结果 = 存储.加载(玩家标识);

            assertTrue(保存结果);
            assertTrue(加载结果.isPresent());
            assertEquals("总体个数", 加载结果.get().模式());
            assertEquals(30, 加载结果.get().数值());
            verify(连接).prepareStatement(保存SQL);
            verify(连接).prepareStatement(加载SQL);
            verify(语句, times(2)).setString(1, 玩家标识.toString());
            verify(语句).setString(2, "总体个数");
            verify(语句).setInt(3, 30);
        }

        @Test
        @DisplayName("单体模式保存后加载 - 应返回单体个数与数值")
        void 单体模式保存后加载_应返回相同值() throws SQLException {
            when(语句.executeQuery()).thenReturn(结果集);
            when(结果集.next()).thenReturn(true);
            when(结果集.getString("模式")).thenReturn("单体个数");
            when(结果集.getInt("数值")).thenReturn(50);

            boolean 保存结果 = 存储.保存(玩家标识, "单体个数", 50);
            Optional<生命条缩放设置存储.缩放设置> 加载结果 = 存储.加载(玩家标识);

            assertTrue(保存结果);
            assertTrue(加载结果.isPresent());
            assertEquals("单体个数", 加载结果.get().模式());
            assertEquals(50, 加载结果.get().数值());
        }

        @Test
        @DisplayName("保存后删除再加载 - 加载应返回empty")
        void 保存后删除再加载_应返回empty() throws SQLException {
            when(语句.executeQuery()).thenReturn(结果集);
            when(结果集.next()).thenReturn(false);

            boolean 保存结果 = 存储.保存(玩家标识, "总体个数", 30);
            boolean 删除结果 = 存储.删除(玩家标识);
            Optional<生命条缩放设置存储.缩放设置> 加载结果 = 存储.加载(玩家标识);

            assertTrue(保存结果);
            assertTrue(删除结果);
            assertTrue(加载结果.isEmpty());
            verify(连接).prepareStatement(保存SQL);
            verify(连接).prepareStatement(删除SQL);
            verify(连接).prepareStatement(加载SQL);
        }

        @Test
        @DisplayName("REPLACE INTO 语义 - 同一玩家重复保存应覆盖旧记录")
        void 重复保存_应使用REPLACE覆盖() throws SQLException {
            boolean 第一次保存 = 存储.保存(玩家标识, "总体个数", 30);
            boolean 第二次保存 = 存储.保存(玩家标识, "单体个数", 50);

            assertTrue(第一次保存);
            assertTrue(第二次保存);
            verify(连接, times(2)).prepareStatement(保存SQL);
            verify(语句).setString(2, "总体个数");
            verify(语句).setInt(3, 30);
            verify(语句).setString(2, "单体个数");
            verify(语句).setInt(3, 50);
        }

        @Test
        @DisplayName("参数化查询 - 所有SQL应使用占位符而非字符串拼接")
        void 参数化查询_所有SQL应使用占位符() throws SQLException {
            存储.保存(玩家标识, "总体个数", 30);

            when(语句.executeQuery()).thenReturn(结果集);
            when(结果集.next()).thenReturn(false);
            存储.加载(玩家标识);

            存储.删除(玩家标识);

            verify(连接).prepareStatement(eq(保存SQL));
            verify(连接).prepareStatement(eq(加载SQL));
            verify(连接).prepareStatement(eq(删除SQL));
            assertTrue(保存SQL.contains("?") && !保存SQL.contains(玩家标识.toString()));
            assertTrue(加载SQL.contains("?") && !加载SQL.contains(玩家标识.toString()));
            assertTrue(删除SQL.contains("?") && !删除SQL.contains(玩家标识.toString()));
        }
    }
}
