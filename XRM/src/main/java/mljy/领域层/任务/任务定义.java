package mljy.领域层.任务;

import java.util.List;

/**
 * 任务定义领域对象（不可变）。
 * 由任务注册服务从 YAML 配置加载，描述任务的静态属性。
 * 对应需求文档《任务.md》第一章数据模型。
 */
public record 任务定义(
        String 任务标识,
        任务分类 分类,
        String 名称翻译键,
        String 描述翻译键,
        任务目标 目标,
        任务奖励 奖励,
        List<String> 前置任务,
        String 接取NPC,
        String 交付NPC,
        int 所需等级,
        boolean 可重复,
        String 接取对话键,
        String 进行中对话键,
        String 可交付对话键,
        String 完成后对话键
) {
    /** 默认所需等级 */
    public static final int 默认所需等级 = 0;
    /** 默认不可重复 */
    public static final boolean 默认可重复 = false;

    /**
     * 工厂方法：创建主线任务定义（不可重复，无前置）。
     */
    public static 任务定义 主线(String 任务标识, String 名称翻译键, String 描述翻译键,
                              任务目标 目标, 任务奖励 奖励,
                              String 接取NPC, String 交付NPC) {
        return new 任务定义(任务标识, 任务分类.主线, 名称翻译键, 描述翻译键,
                目标, 奖励, List.of(), 接取NPC, 交付NPC,
                默认所需等级, 默认可重复, "", "", "", "");
    }

    /**
     * 工厂方法：创建带前置的主线任务定义。
     */
    public static 任务定义 主线带前置(String 任务标识, String 名称翻译键, String 描述翻译键,
                                    任务目标 目标, 任务奖励 奖励,
                                    List<String> 前置任务,
                                    String 接取NPC, String 交付NPC) {
        return new 任务定义(任务标识, 任务分类.主线, 名称翻译键, 描述翻译键,
                目标, 奖励, 前置任务, 接取NPC, 交付NPC,
                默认所需等级, 默认可重复, "", "", "", "");
    }

    /**
     * 工厂方法：创建日常任务定义（可重复）。
     */
    public static 任务定义 日常(String 任务标识, String 名称翻译键, String 描述翻译键,
                              任务目标 目标, 任务奖励 奖励,
                              String 接取NPC, String 交付NPC) {
        return new 任务定义(任务标识, 任务分类.日常, 名称翻译键, 描述翻译键,
                目标, 奖励, List.of(), 接取NPC, 交付NPC,
                默认所需等级, true, "", "", "", "");
    }

    /**
     * 判断是否有前置任务。
     */
    public boolean 是否有前置() {
        return 前置任务 != null && !前置任务.isEmpty();
    }

    /**
     * 判断是否有接取对话。
     */
    public boolean 是否有接取对话() {
        return 接取对话键 != null && !接取对话键.isBlank();
    }

    /**
     * 判断是否有进行中对话。
     */
    public boolean 是否有进行中对话() {
        return 进行中对话键 != null && !进行中对话键.isBlank();
    }

    /**
     * 判断是否有可交付对话。
     */
    public boolean 是否有可交付对话() {
        return 可交付对话键 != null && !可交付对话键.isBlank();
    }

    /**
     * 判断是否有完成后对话。
     */
    public boolean 是否有完成后对话() {
        return 完成后对话键 != null && !完成后对话键.isBlank();
    }
}
