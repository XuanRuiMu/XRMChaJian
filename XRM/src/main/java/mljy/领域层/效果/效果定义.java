package mljy.领域层.效果;

public record 效果定义(
        String 效果标识,
        String 名称,
        String 名称翻译键,
        String 描述翻译键,
        String 处理器类名,
        long 持续时间,
        int 最大层数,
        boolean 可驱散,
        boolean 可叠加
) {
    public static final long 永久持续 = -1L;
    public static final int 无层数上限 = -1;

    public boolean 是否永久() {
        return 持续时间 == 永久持续;
    }

    public boolean 是否有层数上限() {
        return 最大层数 != 无层数上限;
    }

    public boolean 是否有处理器() {
        return 处理器类名 != null && !处理器类名.isBlank();
    }
}
