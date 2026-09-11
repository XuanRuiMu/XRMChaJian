package mljy.基础设施层;

import com.google.inject.Inject;
import mljy.翻译服务;
import mljy.玩家服务;
import mljy.领域层.玩家.玩家会话;
import 暮澜纪元.通用.文本.技能名解析器;

import java.util.Optional;
import java.util.UUID;

public class XRM技能名解析器 implements 技能名解析器 {
    private static final String 技能键前缀 = "skill.";
    private static final String 技能键槽位中段 = ".slot.";
    private static final String 技能键后缀 = ".name";

    private final 翻译服务 翻译服务;
    private final 玩家服务 玩家服务;

    @Inject
    public XRM技能名解析器(翻译服务 翻译服务, 玩家服务 玩家服务) {
        this.翻译服务 = 翻译服务;
        this.玩家服务 = 玩家服务;
    }

    @Override
    public Optional<String> 解析技能名(String 槽位中文数字, UUID 玩家标识) {
        if (玩家标识 == null) {
            return Optional.empty();
        }
        Optional<玩家会话> 会话可选 = 玩家服务.获取会话(玩家标识);
        if (会话可选.isEmpty()) {
            return Optional.empty();
        }
        String 专精 = 会话可选.get().获取专精();
        String 翻译键 = 技能键前缀 + 专精 + 技能键槽位中段 + 槽位中文数字 + 技能键后缀;
        String 技能名 = 翻译服务.获取(翻译键);
        if (技能名.equals(翻译键)) {
            return Optional.empty();
        }
        return Optional.of(技能名);
    }
}
