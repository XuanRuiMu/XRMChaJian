package mljy.业务层;

import java.util.UUID;

public interface 公共冷却显示服务 {

    void 启动恒定显示(UUID 玩家标识);

    void 显示(UUID 玩家标识, double 公共冷却秒数);

    void 清理(UUID 玩家标识);
}
