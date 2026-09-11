package mljy.领域层.任务;

/**
 * 任务目标领域对象。
 * 描述任务需要达成的具体目标：类型 + 目标ID + 数量。
 * 不同类型的目标ID含义不同：
 *   - 击杀实体：目标ID=实体类型名称（如 ZOMBIE）
 *   - 收集物品：目标ID=物品材质名称（如 IRON_INGOT）
 *   - 到达位置：目标ID=位置区域标识（关联位置配置）
 *   - 对话NPC：目标ID=NPC名称
 *   - 使用技能：目标ID=技能标识
 *   - 条件检查：目标ID=条件标识（如 hunger_level、command_used、class_selected）
 *   - 护送：目标ID=NPC名称
 */
public record 任务目标(
        任务目标类型 类型,
        String 目标ID,
        int 数量
) {
    /** 默认数量，用于对话/到达等单次目标 */
    public static final int 默认数量 = 1;

    /**
     * 工厂方法：创建单次目标（数量=1）。
     */
    public static 任务目标 单次(任务目标类型 类型, String 目标ID) {
        return new 任务目标(类型, 目标ID, 默认数量);
    }

    /**
     * 工厂方法：创建计数目标。
     */
    public static 任务目标 计数(任务目标类型 类型, String 目标ID, int 数量) {
        return new 任务目标(类型, 目标ID, 数量);
    }
}
