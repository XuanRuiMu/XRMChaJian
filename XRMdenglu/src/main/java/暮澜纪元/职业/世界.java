package 暮澜纪元.职业;

import java.util.Arrays;
import java.util.List;

/**
 * 世界枚举。
 * 定义所有可选的世界及其包含的职业。
 */
public enum 世界 {
    魔法世界("世界.魔法世界", "<dark_purple>", Arrays.asList(职业.奥能法师, 职业.战斗法师, 职业.符文法师, 职业.大魔导师)),
    自然世界("世界.自然世界", "<green>", Arrays.asList(职业.德鲁伊, 职业.精灵卫士, 职业.树人守卫, 职业.荆棘领主)),
    机械世界("世界.机械世界", "<gray>", Arrays.asList(职业.枪炮师, 职业.机械师, 职业.修补匠)),
    神灵世界("世界.神灵世界", "<aqua>", Arrays.asList(职业.狐灵法师, 职业.蛇影刺客, 职业.鹿灵祭司, 职业.虎威战士, 职业.灵魂行者)),
    圣洁世界("世界.圣洁世界", "<yellow>", Arrays.asList(职业.骑士, 职业.牧师, 职业.神谕者, 职业.战地统帅)),
    暗影世界("世界.暗影世界", "<dark_gray>", Arrays.asList(职业.影舞者, 职业.暗影术士, 职业.冰霜战士, 职业.暗影守卫, 职业.末日预言者, 职业.死灵收割者)),
    元素世界("世界.元素世界", "<gold>", Arrays.asList(职业.女巫, 职业.沸血战士, 职业.雷电督军, 职业.平衡术士)),
    混沌世界("世界.混沌世界", "<dark_red>", Arrays.asList(职业.深渊吞噬者, 职业.混沌使者, 职业.时空旅行家, 职业.幻影灵魂, 职业.梦魇咒术师));

    private final String 语言键;
    private final String 颜色标签;
    private final List<职业> 包含职业列表;

    世界(String 语言键, String 颜色标签, List<职业> 包含职业列表) {
        this.语言键 = 语言键;
        this.颜色标签 = 颜色标签;
        this.包含职业列表 = 包含职业列表;
    }

    public String 获取语言键() {
        return this.语言键;
    }

    public String 获取颜色标签() {
        return this.颜色标签;
    }

    public List<职业> 获取包含职业() {
        return this.包含职业列表;
    }
}
