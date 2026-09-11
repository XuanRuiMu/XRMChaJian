package mljy.基础设施层.数据库;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mljy.基础设施层.调试日志器;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Singleton
public class 表初始化器 {
    private static final String 模块名 = "表初始化器";
    private static final String 建表SQL = "CREATE TABLE IF NOT EXISTS 生命条缩放设置 ("
            + "玩家UUID VARCHAR(36) PRIMARY KEY, "
            + "模式 VARCHAR(20) NOT NULL, "
            + "数值 INT NOT NULL, "
            + "更新时间 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private final 数据库连接池 连接池;

    @Inject
    public 表初始化器(数据库连接池 连接池) {
        this.连接池 = 连接池;
    }

    public boolean 初始化生命条缩放设置表() {
        if (!连接池.已启用()) {
            调试日志器.调试(模块名, "初始化跳过：数据库连接池未启用");
            return false;
        }
        try (Connection 连接 = 连接池.获取连接();
             PreparedStatement 语句 = 连接.prepareStatement(建表SQL)) {
            语句.executeUpdate();
            调试日志器.调试(模块名, "初始化成功：生命条缩放设置表已就绪");
            return true;
        } catch (SQLException 异常) {
            调试日志器.调试(模块名, "初始化失败：建表异常", 异常);
            return false;
        }
    }
}
