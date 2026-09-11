package mljy.领域层.乐器;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 乐谱领域对象。
 * 包含音符列表、速度（BPM）、作者、创建时间、乐谱名称。
 * 乐谱可保存到YAML文件并加载回放。
 * <p>
 * FP-09 非破坏性量化：应用量化时保存原始音符列表到 {@link #原始音符列表}，
 * 当前 {@link #音符列表} 替换为量化视图；撤销量化时从原始恢复。
 */
public class 乐谱 {
    private final String 名称;
    private final UUID 作者标识;
    private final String 作者名;
    private final long 创建时间戳;
    private final int 速度BPM;
    private final List<音符> 音符列表;
    // FP-09 非破坏性量化：量化前为 null，量化后保存原版，撤销量化时恢复并置 null
    private List<音符> 原始音符列表;

    public 乐谱(String 名称, UUID 作者标识, String 作者名, long 创建时间戳, int 速度BPM, List<音符> 音符列表) {
        this.名称 = 名称;
        this.作者标识 = 作者标识;
        this.作者名 = 作者名;
        this.创建时间戳 = 创建时间戳;
        this.速度BPM = 速度BPM;
        this.音符列表 = new ArrayList<>(音符列表);
        this.原始音符列表 = null;
    }

    public static 乐谱 创建(String 名称, UUID 作者标识, String 作者名, int 速度BPM, List<音符> 音符列表) {
        return new 乐谱(名称, 作者标识, 作者名, System.currentTimeMillis(), 速度BPM, 音符列表);
    }

    public String 获取名称() {
        return 名称;
    }

    public UUID 获取作者标识() {
        return 作者标识;
    }

    public String 获取作者名() {
        return 作者名;
    }

    public long 获取创建时间戳() {
        return 创建时间戳;
    }

    public int 获取速度BPM() {
        return 速度BPM;
    }

    public List<音符> 获取音符列表() {
        return Collections.unmodifiableList(音符列表);
    }

    public int 获取音符数量() {
        return 音符列表.size();
    }

    public long 获取总时长Tick() {
        long 总时长 = 0;
        for (音符 音符 : 音符列表) {
            总时长 += 音符.获取时值Tick();
        }
        return 总时长;
    }

    public double 获取总时长秒() {
        return 获取总时长Tick() / 20.0;
    }

    public long 获取播放间隔Tick() {
        if (速度BPM <= 0) {
            return 4;
        }
        return Math.max(1, Math.round(1200.0 / 速度BPM));
    }

    /**
     * FP-09 应用量化（非破坏性）：保存当前音符列表为原始版本，替换为量化后音符列表。
     * 多次调用：第一次保存原始，后续调用不再覆盖原始版本（保留最原始状态）。
     * 调用方应在调用前基于 {@link #获取原始音符列表()} 或 {@link #获取音符列表()} 计算量化结果，
     * 避免基于已量化的音符列表二次量化导致偏差累积。
     *
     * @param 量化后音符列表 量化后的音符列表
     */
    public void 应用量化(List<音符> 量化后音符列表) {
        if (量化后音符列表 == null) {
            return;
        }
        if (this.原始音符列表 == null) {
            this.原始音符列表 = new ArrayList<>(this.音符列表);
        }
        this.音符列表.clear();
        this.音符列表.addAll(量化后音符列表);
    }

    /**
     * FP-09 撤销量化：恢复到原始音符列表。未量化时无操作。
     *
     * @return true=已撤销；false=未量化无操作
     */
    public boolean 撤销量化() {
        if (this.原始音符列表 == null) {
            return false;
        }
        this.音符列表.clear();
        this.音符列表.addAll(this.原始音符列表);
        this.原始音符列表 = null;
        return true;
    }

    /**
     * FP-09 是否已量化（应用量化后未撤销）。
     *
     * @return true=已量化（原始音符列表非 null）；false=未量化
     */
    public boolean 是否已量化() {
        return this.原始音符列表 != null;
    }

    /**
     * FP-09 获取原始音符列表（未量化时返回空列表）。
     *
     * @return 不可修改的原始音符列表视图；未量化时返回空列表
     */
    public List<音符> 获取原始音符列表() {
        return this.原始音符列表 == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(this.原始音符列表);
    }
}
