package mljy.业务层.消息;

import org.bukkit.entity.Entity;

public interface 伤害日志格式化服务 {
    String 获取实体来源名(Entity 来源实体);

    String 获取实体名称(Entity 实体);

    String 获取来源翻译(String 翻译键);

    String 获取未知来源名();

    String 获取安全翻译(String 翻译键, String 回退文本);
}
