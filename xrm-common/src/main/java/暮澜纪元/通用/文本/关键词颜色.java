package 暮澜纪元.通用.文本;

import java.util.List;

public enum 关键词颜色 {
    蓄力(List.of("<aqua>", "<bold>"), List.of("</bold>", "</aqua>"), "§b§l"),
    引导(List.of("<blue>", "<bold>"), List.of("</bold>", "</blue>"), "§9§l"),
    延迟(List.of("<yellow>", "<bold>"), List.of("</bold>", "</yellow>"), "§e§l"),
    瞬发(List.of("<green>"), List.of("</green>"), "§a"),
    冷却(List.of("<dark_gray>"), List.of("</dark_gray>"), "§8"),
    公共冷却(List.of("<dark_gray>"), List.of("</dark_gray>"), "§8"),
    打断(List.of("<red>"), List.of("</red>"), "§c"),

    物理(List.of("<gray>"), List.of("</gray>"), "§7"),
    魔法(List.of("<blue>"), List.of("</blue>"), "§9"),
    火焰(List.of("<red>"), List.of("</red>"), "§c"),
    冰霜(List.of("<aqua>"), List.of("</aqua>"), "§b"),
    自然(List.of("<green>"), List.of("</green>"), "§a"),
    暗影(List.of("<dark_purple>"), List.of("</dark_purple>"), "§5"),
    神圣(List.of("<yellow>"), List.of("</yellow>"), "§e"),
    奥术(List.of("<dark_aqua>"), List.of("</dark_aqua>"), "§3"),
    混乱(List.of("<dark_red>", "<bold>"), List.of("</bold>", "</dark_red>"), "§4§l"),
    星界(List.of("<light_purple>"), List.of("</light_purple>"), "§d"),
    多彩(List.of("<gold>", "<bold>"), List.of("</bold>", "</gold>"), "§6§l"),
    生命(List.of("<dark_green>", "<bold>"), List.of("</bold>", "</dark_green>"), "§2§l"),
    死亡(List.of("<dark_gray>"), List.of("</dark_gray>"), "§8"),
    真实(List.of("<white>", "<bold>"), List.of("</bold>", "</white>"), "§f§l"),

    第一技能(List.of("<red>"), List.of("</red>"), "§c"),
    第二技能(List.of("<gold>"), List.of("</gold>"), "§6"),
    第三技能(List.of("<yellow>"), List.of("</yellow>"), "§e"),
    第四技能(List.of("<green>"), List.of("</green>"), "§a"),
    第五技能(List.of("<aqua>"), List.of("</aqua>"), "§b"),
    第六技能(List.of("<blue>"), List.of("</blue>"), "§9"),
    第七技能(List.of("<dark_purple>"), List.of("</dark_purple>"), "§5"),
    第八技能(List.of("<gray>"), List.of("</gray>"), "§7"),
    第九技能(List.of("<white>"), List.of("</white>"), "§f"),

    资源(List.of("<aqua>", "<bold>"), List.of("</bold>", "</aqua>"), "§b§l"),
    秘能(List.of("<dark_purple>", "<bold>"), List.of("</bold>", "</dark_purple>"), "§5§l"),
    秘兆(List.of("<light_purple>", "<bold>"), List.of("</bold>", "</light_purple>"), "§d§l"),

    暴击(List.of("<gold>", "<bold>"), List.of("</bold>", "</gold>"), "§6§l"),
    暴击几率(List.of("<yellow>"), List.of("</yellow>"), "§e"),
    暴击伤害(List.of("<gold>"), List.of("</gold>"), "§6"),
    法术暴击几率(List.of("<yellow>"), List.of("</yellow>"), "§e"),
    法术暴击伤害(List.of("<gold>"), List.of("</gold>"), "§6"),
    护盾(List.of("<yellow>"), List.of("</yellow>"), "§e"),
    奥术护盾(List.of("<yellow>"), List.of("</yellow>"), "§e"),
    吸血(List.of("<dark_red>"), List.of("</dark_red>"), "§4"),
    躲闪(List.of("<light_purple>"), List.of("</light_purple>"), "§d"),
    免疫(List.of("<white>", "<bold>"), List.of("</bold>", "</white>"), "§f§l"),
    吸收(List.of("<gold>"), List.of("</gold>"), "§6"),
    反弹(List.of("<dark_red>", "<bold>"), List.of("</bold>", "</dark_red>"), "§4§l"),
    格挡(List.of("<gray>"), List.of("</gray>"), "§7"),

    增益(List.of("<green>"), List.of("</green>"), "§a"),
    减益(List.of("<red>"), List.of("</red>"), "§c"),
    缓慢(List.of("<gray>"), List.of("</gray>"), "§7"),
    沉默(List.of("<dark_gray>"), List.of("</dark_gray>"), "§8"),
    眩晕(List.of("<dark_red>"), List.of("</dark_red>"), "§4"),
    定身(List.of("<dark_aqua>"), List.of("</dark_aqua>"), "§3"),

    输出(List.of("<red>"), List.of("</red>"), "§c"),
    治疗(List.of("<green>"), List.of("</green>"), "§a"),
    坦克(List.of("<blue>"), List.of("</blue>"), "§9"),

    魔法世界(List.of("<dark_purple>"), List.of("</dark_purple>"), "§5"),
    自然世界(List.of("<green>"), List.of("</green>"), "§a"),
    机械世界(List.of("<gray>"), List.of("</gray>"), "§7"),
    神灵世界(List.of("<aqua>"), List.of("</aqua>"), "§b"),
    圣洁世界(List.of("<yellow>"), List.of("</yellow>"), "§e"),
    暗影世界(List.of("<dark_gray>"), List.of("</dark_gray>"), "§8"),
    元素世界(List.of("<gold>"), List.of("</gold>"), "§6"),
    混沌世界(List.of("<dark_red>"), List.of("</dark_red>"), "§4"),

    伤害数值(List.of("<dark_red>"), List.of("</dark_red>"), "§4"),
    治疗数值(List.of("<dark_green>"), List.of("</dark_green>"), "§2"),
    秒数(List.of("<aqua>"), List.of("</aqua>"), "§b"),
    百分比(List.of("<white>"), List.of("</white>"), "§f"),
    点数(List.of("<white>"), List.of("</white>"), "§f"),
    目标数量(List.of("<white>"), List.of("</white>"), "§f"),

    技能日志前缀("[技能日志]", List.of("<light_purple>"), List.of("</light_purple>"), "§d"),
    战斗日志前缀("[战斗日志]", List.of("<red>"), List.of("</red>"), "§c"),
    错误("[错误]", List.of("<red>"), List.of("</red>"), "§c"),
    成功("[成功]", List.of("<green>"), List.of("</green>"), "§a"),
    警告("[警告]", List.of("<yellow>"), List.of("</yellow>"), "§e"),
    信息("[信息]", List.of("<aqua>"), List.of("</aqua>"), "§b"),

    进入战斗(List.of("<red>"), List.of("</red>"), "§c"),
    脱战(List.of("<green>"), List.of("</green>"), "§a"),
    脱战回血(List.of("<dark_green>"), List.of("</dark_green>"), "§2"),
    仇恨(List.of("<dark_red>"), List.of("</dark_red>"), "§4"),

    活力充足(List.of("<green>"), List.of("</green>"), "§a"),
    活力中等(List.of("<yellow>"), List.of("</yellow>"), "§e"),
    活力不足(List.of("<red>"), List.of("</red>"), "§c"),
    就绪(List.of("<green>"), List.of("</green>"), "§a"),
    冷却中(List.of("<red>"), List.of("</red>"), "§c"),

    空槽位(List.of("<dark_gray>"), List.of("</dark_gray>"), "§8"),
    计分板标题(List.of("<gold>", "<bold>"), List.of("</bold>", "</gold>"), "§6§l"),

    等级(List.of("<dark_purple>"), List.of("</dark_purple>"), "§5"),
    玩家名(List.of("<white>"), List.of("</white>"), "§f"),
    实体名(List.of("<white>"), List.of("</white>"), "§f"),
    攻击者(List.of("<gray>"), List.of("</gray>"), "§7"),

    公共冷却时间(List.of("<dark_gray>"), List.of("</dark_gray>"), "§8"),

    力量(List.of("<red>"), List.of("</red>"), "§c"),
    敏捷(List.of("<green>"), List.of("</green>"), "§a"),
    智力(List.of("<blue>"), List.of("</blue>"), "§9"),
    生命恢复(List.of("<dark_green>"), List.of("</dark_green>"), "§2"),
    急速(List.of("<aqua>"), List.of("</aqua>"), "§b"),
    精通(List.of("<dark_purple>"), List.of("</dark_purple>"), "§5"),
    全能(List.of("<dark_aqua>"), List.of("</dark_aqua>"), "§3"),
    移速(List.of("<gray>"), List.of("</gray>"), "§7"),

    生命条(List.of("<dark_green>"), List.of("</dark_green>"), "§2"),
    生命值(List.of("<dark_green>"), List.of("</dark_green>"), "§2"),
    其他数值(List.of("<aqua>"), List.of("</aqua>"), "§b"),

    接触伤害(List.of("<gray>"), List.of("</gray>"), "§7"),
    实体攻击(List.of("<dark_gray>"), List.of("</dark_gray>"), "§8"),
    横扫攻击(List.of("<dark_gray>"), List.of("</dark_gray>"), "§8"),
    投射物(List.of("<gold>"), List.of("</gold>"), "§6"),
    窒息(List.of("<dark_gray>"), List.of("</dark_gray>"), "§8"),
    摔落(List.of("<gray>"), List.of("</gray>"), "§7"),
    燃烧(List.of("<red>"), List.of("</red>"), "§c"),
    岩浆(List.of("<red>"), List.of("</red>"), "§c"),
    溺水(List.of("<dark_aqua>"), List.of("</dark_aqua>"), "§3"),
    方块爆炸(List.of("<dark_red>"), List.of("</dark_red>"), "§4"),
    爆炸(List.of("<dark_red>"), List.of("</dark_red>"), "§4"),
    闪电(List.of("<yellow>"), List.of("</yellow>"), "§e"),
    饥饿(List.of("<gold>"), List.of("</gold>"), "§6"),
    中毒(List.of("<dark_purple>"), List.of("</dark_purple>"), "§5"),
    凋零(List.of("<dark_purple>"), List.of("</dark_purple>"), "§5"),
    下落方块(List.of("<gray>"), List.of("</gray>"), "§7"),
    荆棘(List.of("<dark_purple>"), List.of("</dark_purple>"), "§5"),
    龙息(List.of("<dark_purple>"), List.of("</dark_purple>"), "§5"),
    特殊伤害(List.of("<white>"), List.of("</white>"), "§f"),
    撞墙(List.of("<gray>"), List.of("</gray>"), "§7"),
    灼热地面(List.of("<red>"), List.of("</red>"), "§c"),
    挤压(List.of("<gray>"), List.of("</gray>"), "§7"),
    脱水(List.of("<dark_gray>"), List.of("</dark_gray>"), "§8"),
    冻结(List.of("<aqua>"), List.of("</aqua>"), "§b"),
    音爆(List.of("<dark_purple>"), List.of("</dark_purple>"), "§5"),

    火球(List.of("<gold>"), List.of("</gold>"), "§6"),
    小火球(List.of("<gold>"), List.of("</gold>"), "§6"),
    龙息火球(List.of("<gold>"), List.of("</gold>"), "§6"),
    凋灵头颅(List.of("<gold>"), List.of("</gold>"), "§6");

    private final String 显示文本;
    private final List<String> 开标签列表;
    private final List<String> 关标签列表;
    private final String 传统代码;

    关键词颜色(List<String> 开标签列表, List<String> 关标签列表, String 传统代码) {
        this(null, 开标签列表, 关标签列表, 传统代码);
    }

    关键词颜色(String 显示文本, List<String> 开标签列表, List<String> 关标签列表, String 传统代码) {
        this.显示文本 = 显示文本 != null ? 显示文本 : name();
        this.开标签列表 = 开标签列表;
        this.关标签列表 = 关标签列表;
        this.传统代码 = 传统代码;
    }

    public String 获取显示文本() {
        return 显示文本;
    }

    public String 获取MiniMessage开标签() {
        return String.join("", 开标签列表);
    }

    public String 获取MiniMessage关标签() {
        return String.join("", 关标签列表);
    }

    public String 格式化MiniMessage(String 文本) {
        return 获取MiniMessage开标签() + 文本 + 获取MiniMessage关标签();
    }

    public String 获取传统颜色代码() {
        return 传统代码;
    }

    public String 格式化传统代码(String 文本) {
        return 传统代码 + 文本;
    }
}
