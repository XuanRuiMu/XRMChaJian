package 暮澜纪元.通用.文本;

import java.util.Optional;
import java.util.UUID;

public interface 技能名解析器 {

    Optional<String> 解析技能名(String 槽位中文数字, UUID 玩家标识);
}
