package mljy.基础设施层.数据库;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mljy.基础设施层.调试日志器;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class 生命条缩放设置存储 {
    private static final String 模块名 = "生命条缩放设置存储";
    private static final String 保存SQL = "REPLACE INTO 生命条缩放设置 (玩家UUID, 模式, 数值) VALUES (?, ?, ?)";
    private static final String 加载SQL = "SELECT 模式, 数值 FROM 生命条缩放设置 WHERE 玩家UUID = ?";
    private static final String 删除SQL = "DELETE FROM 生命条缩放设置 WHERE 玩家UUID = ?";

    public record 缩放设置(String 模式, int 数值) {}

    private final 数据库连接池 连接池;

    @Inject
    public 生命条缩放设置存储(数据库连接池 连接池) {
        this.连接池 = 连接池;
    }

    public boolean 保存(UUID 玩家标识, String 模式, int 数值) {
        if (玩家标识 == null || 模式 == null || 模式.isBlank()) {
            调试日志器.调试(模块名, "保存跳过：参数无效，玩家=%s, 模式=%s", 玩家标识, 模式);
            return false;
        }
        if (!连接池.已启用()) {
            调试日志器.调试(模块名, "保存跳过：数据库连接池未启用，玩家=%s, 模式=%s, 数值=%d", 玩家标识, 模式, 数值);
            return false;
        }
        调试日志器.调试(模块名, "保存开始：玩家=%s, 模式=%s, 数值=%d, SQL=%s", 玩家标识, 模式, 数值, 保存SQL);
        try (Connection 连接 = 连接池.获取连接();
             PreparedStatement 语句 = 连接.prepareStatement(保存SQL)) {
            语句.setString(1, 玩家标识.toString());
            语句.setString(2, 模式);
            语句.setInt(3, 数值);
            int 影响行数 = 语句.executeUpdate();
            调试日志器.调试(模块名, "保存成功：玩家=%s, 模式=%s, 数值=%d, 影响行数=%d", 玩家标识, 模式, 数值, 影响行数);
            return true;
        } catch (SQLException 异常) {
            调试日志器.调试(模块名, "保存失败：玩家=" + 玩家标识 + ", 模式=" + 模式 + ", 数值=" + 数值
                    + ", SQLState=" + 异常.getSQLState() + ", ErrorCode=" + 异常.getErrorCode(), 异常);
            return false;
        }
    }

    public String 获取连接池状态() {
        return 连接池.获取配置摘要();
    }

    public boolean 连接池已启用() {
        return 连接池.已启用();
    }

    public Optional<缩放设置> 加载(UUID 玩家标识) {
        if (玩家标识 == null) {
            调试日志器.调试(模块名, "加载跳过：玩家标识为空");
            return Optional.empty();
        }
        if (!连接池.已启用()) {
            调试日志器.调试(模块名, "加载跳过：数据库连接池未启用，玩家=%s", 玩家标识);
            return Optional.empty();
        }
        调试日志器.调试(模块名, "加载开始：玩家=%s, SQL=%s", 玩家标识, 加载SQL);
        try (Connection 连接 = 连接池.获取连接();
             PreparedStatement 语句 = 连接.prepareStatement(加载SQL)) {
            语句.setString(1, 玩家标识.toString());
            try (ResultSet 结果 = 语句.executeQuery()) {
                if (结果.next()) {
                    String 模式 = 结果.getString("模式");
                    int 数值 = 结果.getInt("数值");
                    调试日志器.调试(模块名, "加载命中：玩家=%s, 模式=%s, 数值=%d", 玩家标识, 模式, 数值);
                    return Optional.of(new 缩放设置(模式, 数值));
                }
                调试日志器.调试(模块名, "加载完成：无持久化记录，玩家=%s", 玩家标识);
                return Optional.empty();
            }
        } catch (SQLException 异常) {
            调试日志器.调试(模块名, "加载失败：玩家=" + 玩家标识
                    + ", SQLState=" + 异常.getSQLState() + ", ErrorCode=" + 异常.getErrorCode(), 异常);
            return Optional.empty();
        }
    }

    public boolean 删除(UUID 玩家标识) {
        if (玩家标识 == null) {
            调试日志器.调试(模块名, "删除跳过：玩家标识为空");
            return false;
        }
        if (!连接池.已启用()) {
            调试日志器.调试(模块名, "删除跳过：数据库连接池未启用，玩家=%s", 玩家标识);
            return false;
        }
        调试日志器.调试(模块名, "删除开始：玩家=%s, SQL=%s", 玩家标识, 删除SQL);
        try (Connection 连接 = 连接池.获取连接();
             PreparedStatement 语句 = 连接.prepareStatement(删除SQL)) {
            语句.setString(1, 玩家标识.toString());
            int 影响行数 = 语句.executeUpdate();
            调试日志器.调试(模块名, "删除成功：玩家=%s, 影响行数=%d", 玩家标识, 影响行数);
            return true;
        } catch (SQLException 异常) {
            调试日志器.调试(模块名, "删除失败：玩家=" + 玩家标识
                    + ", SQLState=" + 异常.getSQLState() + ", ErrorCode=" + 异常.getErrorCode(), 异常);
            return false;
        }
    }
}
