package mljy.领域层.乐器;

/**
 * FP-03 6×9 GUI 功能位定义。
 * 9 个功能位默认分配到 GUI 第 0 行（槽 0-8）。
 * <p>
 * 前 3 个（保存/清除/关闭）功能已实现；后 6 个（NBS导入/NBS导出/和弦预设/公共乐谱库/钢琴卷帘编辑/分层切换）
 * 本轮仅放置按钮 + 提示"功能未实现"翻译键，功能本身由后续 FP-H/FP-I/FP-J 实现。
 * <p>
 * 翻译键固定为「演奏GUI.功能位.{标识}」；槽位/材质/模型数据由 乐器配置.yml「功能位」段配置覆盖。
 */
public enum 功能位定义 {
    保存("保存"),
    清除("清除"),
    关闭("关闭"),
    NBS导入("NBS导入"),
    NBS导出("NBS导出"),
    和弦预设("和弦预设"),
    公共乐谱库("公共乐谱库"),
    钢琴卷帘编辑("钢琴卷帘编辑"),
    分层切换("分层切换");

    private static final String 翻译根键 = "演奏GUI.功能位";

    private final String 标识;

    功能位定义(String 标识) {
        this.标识 = 标识;
    }

    public String 获取标识() {
        return 标识;
    }

    /**
     * 获取翻译键「演奏GUI.功能位.{标识}」。
     */
    public String 获取名称键() {
        return 翻译根键 + "." + 标识;
    }

    /**
     * 根据标识字符串反查功能位定义。
     *
     * @param 标识 功能位标识（如"保存"）
     * @return 功能位定义；未找到返回 null
     */
    public static 功能位定义 从标识查找(String 标识) {
        if (标识 == null) {
            return null;
        }
        for (功能位定义 位 : values()) {
            if (位.标识.equals(标识)) {
                return 位;
            }
        }
        return null;
    }
}
