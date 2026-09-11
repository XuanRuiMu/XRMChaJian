package mljy.基础设施层.数据库;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import mljy.基础设施层.调试日志器;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;

@Singleton
public class 数据库连接池 {
    private static final String 模块名 = "数据库连接池";
    private static final String 配置前缀 = "数据库.";
    private static final int 默认端口 = 3306;
    private static final int 最大池大小 = 10;
    private static final int 连接超时毫秒 = 5000;
    private static final int 空闲超时毫秒 = 30000;
    private static final int 最大寿命毫秒 = 1800000;

    private final HikariDataSource 数据源;
    private final boolean 已启用;
    private final String 配置摘要;

    @Inject
    public 数据库连接池(JavaPlugin 插件) {
        FileConfiguration 配置 = 插件.getConfig();
        String 主机 = 配置.getString(配置前缀 + "主机", "localhost");
        int 端口 = 配置.getInt(配置前缀 + "端口", 默认端口);
        String 数据库名 = 配置.getString(配置前缀 + "数据库名", "暮澜纪元");
        String 用户名 = 配置.getString(配置前缀 + "用户名", "root");
        String 密码 = 配置.getString(配置前缀 + "密码", "");
        this.配置摘要 = "主机=" + 主机 + ", 端口=" + 端口 + ", 数据库=" + 数据库名 + ", 用户=" + 用户名;
        String 连接URL = "jdbc:mysql://" + 主机 + ":" + 端口 + "/" + 数据库名
                + "?useSSL=false&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";

        HikariConfig hikari配置 = new HikariConfig();
        hikari配置.setPoolName("XRM-数据库连接池");
        hikari配置.setJdbcUrl(连接URL);
        hikari配置.setUsername(用户名);
        hikari配置.setPassword(密码);
        hikari配置.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikari配置.setMaximumPoolSize(最大池大小);
        hikari配置.setConnectionTimeout(连接超时毫秒);
        hikari配置.setIdleTimeout(空闲超时毫秒);
        hikari配置.setMaxLifetime(最大寿命毫秒);
        hikari配置.setConnectionTestQuery("SELECT 1");
        hikari配置.addDataSourceProperty("cachePrepStmts", "true");
        hikari配置.addDataSourceProperty("prepStmtCacheSize", "64");
        hikari配置.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        HikariDataSource 临时数据源 = null;
        boolean 临时启用 = false;
        try {
            临时数据源 = new HikariDataSource(hikari配置);
            临时启用 = true;
            调试日志器.调试(模块名, "初始化成功：%s, 池大小=%d", 配置摘要, 最大池大小);
        } catch (RuntimeException 异常) {
            调试日志器.调试(模块名, "初始化失败：连接池创建异常，" + 配置摘要
                    + ", 异常类型=" + 异常.getClass().getSimpleName()
                    + ", 异常消息=" + 异常.getMessage(), 异常);
            临时启用 = false;
        }
        this.数据源 = 临时数据源;
        this.已启用 = 临时启用;
    }

    public Connection 获取连接() throws SQLException {
        if (!已启用 || 数据源 == null) {
            throw new SQLException("数据库连接池未启用");
        }
        return 数据源.getConnection();
    }

    public boolean 已启用() {
        return 已启用;
    }

    public String 获取配置摘要() {
        return 配置摘要 + ", 已启用=" + 已启用 + ", 数据源关闭=" + (数据源 == null ? "数据源为空" : 数据源.isClosed());
    }

    public void 关闭() {
        if (数据源 != null && !数据源.isClosed()) {
            try {
                数据源.close();
                调试日志器.调试(模块名, "连接池已关闭");
            } catch (RuntimeException 异常) {
                调试日志器.调试(模块名, "关闭连接池异常", 异常);
            }
        }
    }
}
