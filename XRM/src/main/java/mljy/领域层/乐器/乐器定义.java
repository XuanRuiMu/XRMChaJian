package mljy.领域层.乐器;

import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.Optional;

/**
 * FP-13 乐器定义（配置驱动）。
 * 不可变值对象，承载从 乐器配置.yml 加载的单个乐器全部参数。
 * 替代旧的硬编码 乐器类型 枚举，由 乐器注册表 在启动时从配置加载。
 *
 * 音高转 pitch 公式：pow(2.0, (有效音高 - 12) / 12.0)，
 * 有效音高先 clamp 到 [音域下限, 音域上限]。
 *
 * 物理机制取值（FP-13.4）：
 * - "瞬时触发"：钢琴/鼓类，瞬时触发自然衰减
 * - "持续激励"：弦乐/管乐，按键持续=持续激励
 * - "慢衰减"：吉他/贝斯/头颅，3-5 秒慢衰减
 */
public final class 乐器定义 {

    public static final String 物理机制_瞬时触发 = "瞬时触发";
    public static final String 物理机制_持续激励 = "持续激励";
    public static final String 物理机制_慢衰减 = "慢衰减";

    private final String 标识;
    private final String 显示名称;
    private final Sound 原版音色;
    private final String 资源包音色键;
    private final Material 物品材质;
    private final int 自定义模型数据;
    private final int 默认音域;
    private final int 音域下限;
    private final int 音域上限;
    private final int 八度偏移;
    private final boolean 力度敏感;
    private final String 描述;
    private final Integer NoteBlockAPI乐器ID;
    private final String 物理机制;

    public 乐器定义(String 标识, String 显示名称, Sound 原版音色, String 资源包音色键,
                   Material 物品材质, int 自定义模型数据, int 默认音域,
                   int 音域下限, int 音域上限, int 八度偏移,
                   boolean 力度敏感, String 描述,
                   Integer NoteBlockAPI乐器ID, String 物理机制) {
        this.标识 = 标识;
        this.显示名称 = 显示名称;
        this.原版音色 = 原版音色;
        this.资源包音色键 = 资源包音色键;
        this.物品材质 = 物品材质;
        this.自定义模型数据 = 自定义模型数据;
        this.默认音域 = 默认音域;
        this.音域下限 = 音域下限;
        this.音域上限 = 音域上限;
        this.八度偏移 = 八度偏移;
        this.力度敏感 = 力度敏感;
        this.描述 = 描述;
        this.NoteBlockAPI乐器ID = NoteBlockAPI乐器ID;
        this.物理机制 = 物理机制 == null ? 物理机制_瞬时触发 : 物理机制;
    }

    public String 获取标识() {
        return 标识;
    }

    public String 获取显示名称() {
        return 显示名称;
    }

    public Sound 获取原版音色() {
        return 原版音色;
    }

    /**
     * 资源包自定义音色键（如 xrm.instrument.piano），未配置时为空。
     * 用于资源包增强音色，未装资源包的玩家回退到 获取原版音色()。
     *
     * @return 资源包音色键；未配置返回 Optional.empty()
     */
    public Optional<String> 获取资源包音色键() {
        return Optional.ofNullable(资源包音色键);
    }

    public Material 获取物品材质() {
        return 物品材质;
    }

    public int 获取自定义模型数据() {
        return 自定义模型数据;
    }

    public int 获取默认音域() {
        return 默认音域;
    }

    public int 获取音域下限() {
        return 音域下限;
    }

    public int 获取音域上限() {
        return 音域上限;
    }

    public int 获取八度偏移() {
        return 八度偏移;
    }

    public boolean 是否力度敏感() {
        return 力度敏感;
    }

    public String 获取描述() {
        return 描述;
    }

    /**
     * NoteBlockAPI 乐器 ID（0-17，见 FP-23.4）。
     * 未配置时返回 Optional.empty()，表示该乐器不使用 NoteBlockAPI 播放。
     *
     * @return NoteBlockAPI 乐器 ID；未配置返回 Optional.empty()
     */
    public Optional<Integer> 获取NoteBlockAPI乐器ID() {
        return Optional.ofNullable(NoteBlockAPI乐器ID);
    }

    public String 获取物理机制() {
        return 物理机制;
    }

    /**
     * 音高转 pitch（用于 playSound 的 pitch 参数）。
     * 有效音高先 clamp 到 [音域下限, 音域上限]。
     *
     * @param 音高 0-24 原始音高
     * @return pitch 值 0.5-2.0
     */
    public float 音高转Pitch(int 音高) {
        int 有效音高 = Math.max(音域下限, Math.min(音域上限, 音高));
        return (float) Math.pow(2.0, (有效音高 - 12) / 12.0);
    }
}
