package mljy.领域层.乐器;

import org.bukkit.Color;
import org.bukkit.Material;

import java.util.Map;
import java.util.Optional;

/**
 * FP-02 25色方块音高显示体系。
 * 颜色 = 音名（%12），材质 = 八度区域（0-5粉末/6-17混凝土/18-24陶瓦）。
 * 纯静态工具类，无状态无依赖。权威对照表见需求文档「功能点 2」。
 */
public final class 音高方块映射 {

    private 音高方块映射() {
    }

    /**
     * 音高转对应方块材质。音高必须在 0-24 范围内。
     *
     * @param 音高 音高值 0-24
     * @return 对应 Material
     * @throws IllegalArgumentException 音高越界
     */
    public static Material 音高转方块(int 音高) {
        if (音高 < 0 || 音高 > 24) {
            throw new IllegalArgumentException("音高必须在0-24范围内: " + 音高);
        }
        return switch (音高) {
            case 0 -> Material.CYAN_CONCRETE_POWDER;
            case 1 -> Material.LIGHT_BLUE_CONCRETE_POWDER;
            case 2 -> Material.BLUE_CONCRETE_POWDER;
            case 3 -> Material.PURPLE_CONCRETE_POWDER;
            case 4 -> Material.MAGENTA_CONCRETE_POWDER;
            case 5 -> Material.PINK_CONCRETE_POWDER;
            case 6 -> Material.BROWN_CONCRETE;
            case 7 -> Material.RED_CONCRETE;
            case 8 -> Material.ORANGE_CONCRETE;
            case 9 -> Material.YELLOW_CONCRETE;
            case 10 -> Material.LIME_CONCRETE;
            case 11 -> Material.GREEN_CONCRETE;
            case 12 -> Material.CYAN_CONCRETE;
            case 13 -> Material.LIGHT_BLUE_CONCRETE;
            case 14 -> Material.BLUE_CONCRETE;
            case 15 -> Material.PURPLE_CONCRETE;
            case 16 -> Material.MAGENTA_CONCRETE;
            case 17 -> Material.PINK_CONCRETE;
            case 18 -> Material.BROWN_TERRACOTTA;
            case 19 -> Material.RED_TERRACOTTA;
            case 20 -> Material.ORANGE_TERRACOTTA;
            case 21 -> Material.YELLOW_TERRACOTTA;
            case 22 -> Material.LIME_TERRACOTTA;
            case 23 -> Material.GREEN_TERRACOTTA;
            case 24 -> Material.CYAN_TERRACOTTA;
            default -> throw new IllegalStateException("不可达分支: " + 音高);
        };
    }

    /**
     * 方块材质反查音高。用于GUI点击方块识别音高。
     *
     * @param 方块 方块材质
     * @return 对应音高 0-24；非映射方块或 null 返回 -1
     */
    public static int 方块转音高(Material 方块) {
        if (方块 == null) {
            return -1;
        }
        return Optional.ofNullable(反向表.get(方块)).orElse(-1);
    }

    /**
     * 音高转音名（带升号）。如 F#、G、C#。
     *
     * @param 音高 音高值 0-24
     * @return 音名
     * @throws IllegalArgumentException 音高越界
     */
    public static String 音高转音名(int 音高) {
        if (音高 < 0 || 音高 > 24) {
            throw new IllegalArgumentException("音高必须在0-24范围内: " + 音高);
        }
        return switch (音高 % 12) {
            case 0 -> "F#";
            case 1 -> "G";
            case 2 -> "G#";
            case 3 -> "A";
            case 4 -> "A#";
            case 5 -> "B";
            case 6 -> "C";
            case 7 -> "C#";
            case 8 -> "D";
            case 9 -> "D#";
            case 10 -> "E";
            case 11 -> "F";
            default -> throw new IllegalStateException("不可达分支: " + 音高);
        };
    }

    /**
     * 音高转唱名。如 Fi、Sol、Do。
     *
     * @param 音高 音高值 0-24
     * @return 唱名
     * @throws IllegalArgumentException 音高越界
     */
    public static String 音高转唱名(int 音高) {
        if (音高 < 0 || 音高 > 24) {
            throw new IllegalArgumentException("音高必须在0-24范围内: " + 音高);
        }
        return switch (音高 % 12) {
            case 0 -> "Fi";
            case 1 -> "Sol";
            case 2 -> "Si";
            case 3 -> "La";
            case 4 -> "Li";
            case 5 -> "Ti";
            case 6 -> "Do";
            case 7 -> "Di";
            case 8 -> "Re";
            case 9 -> "Ri";
            case 10 -> "Mi";
            case 11 -> "Fa";
            default -> throw new IllegalStateException("不可达分支: " + 音高);
        };
    }

    /**
     * 音高转粒子颜色。颜色 = 音名（%12），同八度同色，与方块颜色一致。
     * RGB 取 Minecraft 标准 DyeColor 值，对应需求文档「功能点 2」颜色→音名映射表。
     * 用于演奏可视化反馈（Particle.DUST）。
     *
     * @param 音高 音高值 0-24
     * @return 颜色
     * @throws IllegalArgumentException 音高越界
     */
    public static Color 音高转颜色(int 音高) {
        if (音高 < 0 || 音高 > 24) {
            throw new IllegalArgumentException("音高必须在0-24范围内: " + 音高);
        }
        return switch (音高 % 12) {
            case 0 -> Color.fromRGB(76, 127, 153);
            case 1 -> Color.fromRGB(102, 153, 216);
            case 2 -> Color.fromRGB(51, 76, 178);
            case 3 -> Color.fromRGB(127, 63, 178);
            case 4 -> Color.fromRGB(178, 76, 216);
            case 5 -> Color.fromRGB(242, 127, 165);
            case 6 -> Color.fromRGB(102, 76, 51);
            case 7 -> Color.fromRGB(153, 51, 51);
            case 8 -> Color.fromRGB(216, 127, 51);
            case 9 -> Color.fromRGB(229, 229, 51);
            case 10 -> Color.fromRGB(127, 204, 25);
            case 11 -> Color.fromRGB(102, 127, 51);
            default -> throw new IllegalStateException("不可达分支: " + 音高);
        };
    }

    private static final Map<Material, Integer> 反向表 = Map.ofEntries(
            Map.entry(Material.CYAN_CONCRETE_POWDER, 0),
            Map.entry(Material.LIGHT_BLUE_CONCRETE_POWDER, 1),
            Map.entry(Material.BLUE_CONCRETE_POWDER, 2),
            Map.entry(Material.PURPLE_CONCRETE_POWDER, 3),
            Map.entry(Material.MAGENTA_CONCRETE_POWDER, 4),
            Map.entry(Material.PINK_CONCRETE_POWDER, 5),
            Map.entry(Material.BROWN_CONCRETE, 6),
            Map.entry(Material.RED_CONCRETE, 7),
            Map.entry(Material.ORANGE_CONCRETE, 8),
            Map.entry(Material.YELLOW_CONCRETE, 9),
            Map.entry(Material.LIME_CONCRETE, 10),
            Map.entry(Material.GREEN_CONCRETE, 11),
            Map.entry(Material.CYAN_CONCRETE, 12),
            Map.entry(Material.LIGHT_BLUE_CONCRETE, 13),
            Map.entry(Material.BLUE_CONCRETE, 14),
            Map.entry(Material.PURPLE_CONCRETE, 15),
            Map.entry(Material.MAGENTA_CONCRETE, 16),
            Map.entry(Material.PINK_CONCRETE, 17),
            Map.entry(Material.BROWN_TERRACOTTA, 18),
            Map.entry(Material.RED_TERRACOTTA, 19),
            Map.entry(Material.ORANGE_TERRACOTTA, 20),
            Map.entry(Material.YELLOW_TERRACOTTA, 21),
            Map.entry(Material.LIME_TERRACOTTA, 22),
            Map.entry(Material.GREEN_TERRACOTTA, 23),
            Map.entry(Material.CYAN_TERRACOTTA, 24)
    );
}
