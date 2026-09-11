package 暮澜纪元.数据;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import 暮澜纪元.登录服插件;
import 暮澜纪元.职业.职业;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库连接管理器。
 * 使用HikariCP连接池管理MySQL连接。
 * 与XRM主插件共用相同的玩家数据表结构。
 */
public class 数据库连接管理器 {

    private final 登录服插件 插件;
    private HikariDataSource 数据源;
    private final Map<UUID, 玩家数据> 玩家数据缓存 = new ConcurrentHashMap<>();

    public 数据库连接管理器(登录服插件 插件) {
        this.插件 = 插件;
    }

    /**
     * 初始化数据库连接池。
     *
     * @return 是否成功连接
     */
    public boolean 初始化() {
        try {
            HikariConfig 配置 = new HikariConfig();
            配置.setJdbcUrl(插件.获取配置管理器().数据库连接字符串());
            配置.setUsername(插件.获取配置管理器().数据库用户名());
            配置.setPassword(插件.获取配置管理器().数据库密码());
            配置.setDriverClassName("com.mysql.cj.jdbc.Driver");
            配置.setMaximumPoolSize(5);
            配置.setMinimumIdle(1);
            配置.setConnectionTimeout(30000);
            配置.setIdleTimeout(600000);
            配置.setMaxLifetime(1800000);
            配置.setPoolName("XRMdenglu连接池");

            数据源 = new HikariDataSource(配置);
            插件.获取消息管理器().系统日志("数据库连接池初始化成功");

            创建表结构();

            return true;
        } catch (Exception e) {
            插件.getLogger().severe("初始化数据库连接池失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 创建必要的表结构。
     * 与XRM主插件使用相同的表结构，确保数据互通。
     */
    private void 创建表结构() {
        String 创建玩家数据表 = """
            CREATE TABLE IF NOT EXISTS 玩家数据 (
                UUID VARCHAR(36) PRIMARY KEY COMMENT '玩家唯一标识',
                玩家名 VARCHAR(32) NOT NULL COMMENT '玩家名称',
                职业 VARCHAR(32) COMMENT '所选职业',
                等级 INT DEFAULT 1 COMMENT '玩家等级',
                技能日志开启 BOOLEAN DEFAULT TRUE COMMENT '技能日志开关',
                战斗日志开启 BOOLEAN DEFAULT TRUE COMMENT '战斗日志开关',
                最后更新时间 TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

        try (Connection 连接 = 获取连接();
             Statement 语句 = 连接.createStatement()) {
            语句.execute(创建玩家数据表);
            插件.获取消息管理器().系统日志("数据库表结构初始化完成");
        } catch (SQLException e) {
            插件.getLogger().severe("创建数据库表失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据库连接。
     *
     * @return 数据库连接
     * @throws SQLException 连接异常
     */
    public Connection 获取连接() throws SQLException {
        if (数据源 == null || 数据源.isClosed()) {
            throw new SQLException("数据源未初始化或已关闭");
        }
        return 数据源.getConnection();
    }

    /**
     * 关闭连接池。
     */
    public void 关闭() {
        if (数据源 != null && !数据源.isClosed()) {
            数据源.close();
            插件.获取消息管理器().系统日志("数据库连接池已关闭");
        }
        数据源 = null;
    }

    /**
     * 加载玩家数据。
     *
     * @param 玩家UUID 玩家UUID
     * @return 玩家数据Optional
     */
    public Optional<玩家数据> 加载玩家数据(UUID 玩家UUID) {
        String 查询语句 = "SELECT 玩家名, 职业, 等级 FROM 玩家数据 WHERE UUID = ?";
        try (Connection 连接 = 获取连接();
             PreparedStatement 预编译语句 = 连接.prepareStatement(查询语句)) {
            预编译语句.setString(1, 玩家UUID.toString());
            try (ResultSet 结果集 = 预编译语句.executeQuery()) {
                if (!结果集.next()) {
                    return Optional.empty();
                }
                玩家数据 数据 = new 玩家数据(玩家UUID, 结果集.getString("玩家名"));

                int 等级 = 结果集.getInt("等级");
                if (等级 > 0) {
                    数据.设置等级(等级);
                }

                String 职业名 = 结果集.getString("职业");
                if (职业名 != null) {
                    try {
                        数据.设置职业(职业.valueOf(职业名));
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                return Optional.of(数据);
            }
        } catch (SQLException e) {
            插件.getLogger().severe("加载玩家数据时发生错误: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * 保存玩家数据。
     *
     * @param 数据 玩家数据
     * @return 是否保存成功
     */
    public boolean 保存玩家数据(玩家数据 数据) {
        String 更新语句 = """
            INSERT INTO 玩家数据 (UUID, 玩家名, 职业, 等级)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                玩家名 = VALUES(玩家名),
                职业 = VALUES(职业),
                等级 = VALUES(等级)
            """;
        try (Connection 连接 = 获取连接();
             PreparedStatement 预编译语句 = 连接.prepareStatement(更新语句)) {
            预编译语句.setString(1, 数据.获取UUID().toString());
            预编译语句.setString(2, 数据.获取名字());
            预编译语句.setString(3, 数据.获取职业() != null ? 数据.获取职业().name() : null);
            预编译语句.setInt(4, 数据.获取等级());
            预编译语句.executeUpdate();
            return true;
        } catch (SQLException e) {
            插件.getLogger().severe("保存玩家数据时发生错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取或创建玩家数据。
     *
     * @param 玩家UUID 玩家UUID
     * @param 名字 玩家名字
     * @return 玩家数据
     */
    public 玩家数据 获取或创建玩家数据(UUID 玩家UUID, String 名字) {
        return 玩家数据缓存.computeIfAbsent(玩家UUID, uuid -> {
            Optional<玩家数据> 数据可选 = 加载玩家数据(uuid);
            if (数据可选.isPresent()) {
                return 数据可选.get();
            }
            玩家数据 数据 = new 玩家数据(uuid, 名字);
            保存玩家数据(数据);
            return 数据;
        });
    }

    public void 移除缓存(UUID 玩家UUID) {
        玩家数据缓存.remove(玩家UUID);
    }
}
