package mljy.业务层;

import mljy.业务层.乐器.公共乐谱服务.上传结果;
import mljy.业务层.乐器.公共乐谱服务.下载结果;
import mljy.业务层.乐器.公共乐谱服务.评分结果;
import mljy.业务层.乐器.公共乐谱服务.审核结果;
import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.公共乐谱;
import mljy.领域层.乐器.公共乐谱.审核状态;
import mljy.领域层.乐器.分层配置;
import mljy.领域层.乐器.力度档;
import mljy.领域层.乐器.乐器定义;
import mljy.领域层.乐器.键位绑定表;
import mljy.领域层.乐器.合奏状态;
import mljy.领域层.乐器.量化档位;
import mljy.领域层.乐器.延音模式;
import mljy.领域层.乐器.和弦类型;
import mljy.领域层.乐器.弯音调制状态;
import mljy.领域层.乐器.表情控制状态;
import mljy.领域层.乐器.钢琴卷帘编辑器;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 乐器服务接口。
 * 提供演奏、录制、乐谱管理、合奏等功能。
 * FP-13 配置驱动改造：所有 乐器类型 引用替换为 乐器定义。
 */
public interface 乐器服务 {

    /**
     * 乐谱保存结果状态（GAP-01 修复）。
     * 用于让调用方区分重名/超限/无录制等场景，决定后续 UI 交互。
     */
    enum 乐谱保存结果 {
        成功,
        重名,
        超限,
        无录制,
        参数无效
    }

    void 演奏音符(Player 玩家, 乐器定义 乐器, int 音高);

    void 演奏音符(Player 玩家, 乐器定义 乐器, int 音高, float 力度);

    void 打开演奏界面(Player 玩家, 乐器定义 乐器);

    void 切换乐器(Player 玩家, 乐器定义 新乐器);

    void 开始录制(UUID 玩家标识, 乐器定义 乐器);

    void 停止录制(UUID 玩家标识);

    void 添加录制音符(UUID 玩家标识, int 音高, int 时值Tick, double 力度);

    boolean 是否录制中(UUID 玩家标识);

    /**
     * GAP-01: 保存乐谱并返回结果状态。
     * 同名乐谱已存在时返回 {@link 乐谱保存结果#重名}，由调用方决定是否覆盖。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 乐谱名 乐谱名称
     * @return 保存结果状态
     */
    乐谱保存结果 保存乐谱带结果(UUID 玩家标识, String 乐谱名);

    /**
     * GAP-01: 强制覆盖保存乐谱（同名直接覆盖）。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 乐谱名 乐谱名称
     * @return 是否保存成功（无录制或参数无效时返回 false）
     */
    boolean 保存乐谱并覆盖(UUID 玩家标识, String 乐谱名);

    /**
     * 获取乐谱存储上限（每个玩家私有库的最大乐谱数量，从配置加载）。
     * 命令层在"超限"错误消息中使用此值，避免硬编码。
     *
     * @return 最大乐谱存储数
     */
    int 获取最大乐谱存储数();

    /**
     * GAP-03: 记录按键按下时刻（用于计算真实按键时长）。
     * MC GUI 点击是瞬时事件，调用方应在按下后立即调用 {@link #记录按键松开}；
     * mod 玩家未来通过松开事件记录真实时长（FP-12）。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 音高 音高
     * @param 力度 力度
     */
    void 记录按键按下(UUID 玩家标识, int 音高, double 力度);

    /**
     * GAP-03: 记录按键松开，计算按下→松开的时长作为真实时值并写入录制音符。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 音高 音高
     */
    void 记录按键松开(UUID 玩家标识, int 音高);

    Optional<乐谱> 加载乐谱(UUID 玩家标识, String 乐谱名);

    boolean 删除乐谱(UUID 玩家标识, String 乐谱名);

    List<String> 获取乐谱列表(UUID 玩家标识);

    void 播放乐谱(Player 玩家, 乐谱 乐谱);

    void 停止播放(UUID 玩家标识);

    boolean 是否播放中(UUID 玩家标识);

    boolean 开始合奏(UUID 队长标识, String 乐谱名);

    boolean 确认合奏(UUID 玩家标识);

    boolean 拒绝合奏(UUID 玩家标识);

    boolean 停止合奏(UUID 队长标识);

    boolean 是否合奏中(UUID 玩家标识);

    Optional<合奏状态> 获取合奏状态(UUID 玩家标识);

    String 获取默认乐谱名();

    ItemStack 创建乐器物品(乐器定义 乐器);

    void 给予乐器(Player 玩家, 乐器定义 乐器);

    Optional<乐器定义> 获取玩家当前乐器(UUID 玩家标识);

    void 设置玩家当前乐器(UUID 玩家标识, 乐器定义 乐器);

    void 玩家退出清理(UUID 玩家标识);

    void 重载配置();

    // ===== FP-C 节拍器接入 =====

    /**
     * FP-08: 设置玩家节拍器开关。
     * 开启后玩家演奏/录制时启动节拍器时钟；关闭时停止节拍器。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 开启 是否开启
     */
    void 设置节拍器开关(UUID 玩家标识, boolean 开启);

    /**
     * FP-08: 查询玩家节拍器是否开启。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 是否开启
     */
    boolean 是否节拍器开启(UUID 玩家标识);

    /**
     * FP-08.3: 设置 count-in 小节数（运行时覆盖配置，热重载会重置）。
     *
     * @param 小节数 0/1/2
     */
    void 设置CountIn小节数(int 小节数);

    /**
     * FP-08.3: 获取当前 count-in 小节数。
     *
     * @return 小节数
     */
    int 获取CountIn小节数();

    // ===== FP-E 三轨输入归一 + 逐音域绑定 + 5 档力度 + mod velocity-by-duration =====

    /**
     * FP-01 三轨输入归一：统一演奏指令。
     * 三轨输入（mod/GUI/聊天命令）归一为同一后端演奏入口。
     * 服务端权威计算 pitch（含力度微调）+ volume（含力度档映射），调用方只需提供乐器/音高/力度档/时值。
     *
     * @param 玩家   演奏玩家
     * @param 乐器   乐器定义
     * @param 音高   音高（0-59，5 音域 × 12 半音；服务端 clamp 到乐器音域上下限）
     * @param 力度档 力度档（PP/P/MF/F/FF，影响 volume 与 pitch 微调）
     * @param 时值Tick 时值（tick，用于录音写入；演奏即时发声不依赖此时值）
     */
    void 演奏指令(Player 玩家, 乐器定义 乐器, int 音高, 力度档 力度档, int 时值Tick);

    /**
     * FP-01 mod 输入预留接口（本轮不实现 mod 端，仅服务端预留入口）。
     * mod 端通过 PluginMessage 通道 "xrm:music_input" 发送按键事件包到服务端，
     * 服务端解析后调用本方法触发演奏。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 乐器     乐器定义
     * @param 音高     音高（0-59）
     * @param velocity mod 玩家连续力度（0-127）；-1 表示使用玩家当前力度档
     * @param 按下时间戳 按键按下时刻（毫秒）
     * @param 松开时间戳 按键松开时刻（毫秒）；瞬时事件为 0
     */
    void 处理mod输入(UUID 玩家标识, 乐器定义 乐器, int 音高, int velocity, long 按下时间戳, long 松开时间戳);

    /**
     * FP-06 获取玩家当前音域（默认 0，玩家通过 Z/X 切换）。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 当前音域编号（0 到 键位绑定表.音域数量-1）
     */
    int 获取玩家音域(UUID 玩家标识);

    /**
     * FP-06 切换玩家音域（Z/X 切换音域）。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 增加     true=升一个八度（X 键），false=降一个八度（Z 键）
     * @return 切换后的音域编号
     */
    int 切换玩家音域(UUID 玩家标识, boolean 增加);

    /**
     * FP-06 设置玩家当前音域（直接指定）。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 音域     音域编号（自动 clamp 到有效范围）
     */
    void 设置玩家音域(UUID 玩家标识, int 音域);

    /**
     * FP-05 获取玩家当前力度档（默认 MF，玩家通过 C/V 切换）。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 当前力度档
     */
    力度档 获取玩家力度档(UUID 玩家标识);

    /**
     * FP-05 切换玩家力度档（C/V 切换力度）。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 增加     true=升一档（V 键），false=降一档（C 键）
     * @return 切换后的力度档
     */
    力度档 切换玩家力度档(UUID 玩家标识, boolean 增加);

    /**
     * FP-05 设置玩家当前力度档（直接指定）。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 力度档   力度档
     */
    void 设置玩家力度档(UUID 玩家标识, 力度档 力度档);

    /**
     * FP-05.2 设置玩家是否启用 mod 真实力度（velocity-by-duration）。
     * 仅 mod 玩家可启用；启用后按键时长反推 velocity 0-127。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 启用     是否启用
     */
    void 设置玩家真实力度开关(UUID 玩家标识, boolean 启用);

    /**
     * FP-05.2 查询玩家是否启用 mod 真实力度。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 是否启用
     */
    boolean 是否玩家真实力度开启(UUID 玩家标识);

    /**
     * FP-05.2 velocity-by-duration 算法：
     * velocity = clamp(round((松开-按下)/最大有效时长 × 127), 0, 127)。
     *
     * @param 按下时间戳 按键按下时刻（毫秒）
     * @param 松开时间戳 按键松开时刻（毫秒）
     * @return velocity 0-127
     */
    int 计算velocityByDuration(long 按下时间戳, long 松开时间戳);

    /**
     * FP-06 获取键位绑定表（5 音域 × 12 键映射，从 乐器配置.yml 加载）。
     *
     * @return 键位绑定表
     */
    键位绑定表 获取键位绑定表();

    /**
     * FP-06 处理聊天命令模式按键（Apple Musical Typing 流派）。
     * 由监听器在聊天事件中调用，将字母键映射为演奏指令。
     * a/s/d/f/g/h/j/w/e/t/y/u → 当前音域对应音高
     * z/x → 切换音域
     * c/v → 切换力度档
     *
     * @param 玩家 玩家
     * @param 键   键名（单字符，大小写不敏感）
     * @return true=已消费此按键；false=非音乐键，调用方应放行
     */
    boolean 处理聊天按键(Player 玩家, String 键);

    // ===== FP-F 延音系统（FP-04）=====

    /**
     * FP-04 设置玩家延音模式（按住/切换）。
     * 默认按住模式（真实钢琴踏板）；切换模式为可选。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 模式     延音模式
     */
    void 设置延音模式(UUID 玩家标识, 延音模式 模式);

    /**
     * FP-04 获取玩家当前延音模式。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 延音模式（默认 按住）
     */
    延音模式 获取延音模式(UUID 玩家标识);

    /**
     * FP-04 切换玩家延音模式（按住 ↔ 切换）。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 切换后的延音模式
     */
    延音模式 切换延音模式(UUID 玩家标识);

    /**
     * FP-04 开始延音：对指定音高以略低音量重复触发若干次模拟自然衰减尾音。
     * 受 MC 原版 playSound「一击即逝」限制，延音通过 BukkitRunnable 周期性重触发实现。
     * 已有延音时先停止旧延音再启动新延音。
     *
     * @param 玩家 玩家
     * @param 乐器 乐器定义
     * @param 音高 音高（0-59）
     * @param 档   力度档（影响 volume 与 pitch 微调）
     */
    void 开始延音(Player 玩家, 乐器定义 乐器, int 音高, 力度档 档);

    /**
     * FP-04 停止玩家延音（取消延音任务）。
     *
     * @param 玩家标识 玩家唯一标识
     */
    void 停止延音(UUID 玩家标识);

    /**
     * FP-04 查询玩家是否处于延音中。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 是否延音中
     */
    boolean 是否延音中(UUID 玩家标识);

    /**
     * FP-04 获取玩家当前延音的音高。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 延音中的音高；未延音返回 -1
     */
    int 获取延音音高(UUID 玩家标识);

    // ===== FP-G 量化 + swing + 可视化反馈 =====

    /**
     * FP-09 量化玩家最近乐谱（非破坏性）。
     * 量化前保存原始音符列表，量化后替换为量化视图；多次量化从同一原始基准计算。
     * 量化同时应用当前 swing 比例（如已设置且 > 0.50）。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 档位     量化档位（1/8/1/16/1/32 + 三连音变体）
     */
    void 量化乐谱(UUID 玩家标识, 量化档位 档位);

    /**
     * FP-09 撤销量化，恢复原始音符列表。
     *
     * @param 玩家标识 玩家唯一标识
     * @return true=已撤销；false=未量化或无乐谱
     */
    boolean 撤销量化(UUID 玩家标识);

    /**
     * FP-09 查询玩家是否已量化当前乐谱。
     *
     * @param 玩家标识 玩家唯一标识
     * @return true=已量化；false=未量化或无乐谱
     */
    boolean 是否已量化(UUID 玩家标识);

    /**
     * O2 设置玩家 swing 比例（50%-65%，50% = 直，>50% 三连音感）。
     * 比例会被 clamp 到 [0.50, 0.65] 范围内。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 比例     swing 比例（0.50-0.65）
     */
    void 设置Swing(UUID 玩家标识, double 比例);

    /**
     * O2 获取玩家 swing 比例。
     *
     * @param 玩家标识 玩家唯一标识
     * @return swing 比例（默认 0.50 = 关闭）
     */
    double 获取Swing(UUID 玩家标识);

    // ===== FP-I 和弦预设 + 和弦记忆（O8）+ 公共乐谱库（O4）=====

    /**
     * FP-11 播放和弦：按根音 + 和弦类型同时触发各音（非琶音）。
     * 各音 clamp 到乐器音域；超出上限的音通过八度下移尝试落入音域。
     *
     * @param 玩家 演奏玩家
     * @param 乐器 乐器定义
     * @param 根音 根音（0-59）
     * @param 类型 和弦类型
     * @param 档   力度档
     */
    void 播放和弦(Player 玩家, 乐器定义 乐器, int 根音, 和弦类型 类型, 力度档 档);

    /**
     * O8 启动和弦记忆：以根音 + 和弦类型循环播放和弦，每 N tick 重触发一次。
     * 已在和弦记忆中时先停止旧记忆再启动新记忆。
     *
     * @param 玩家     演奏玩家
     * @param 乐器     乐器定义
     * @param 根音     根音（0-59）
     * @param 类型     和弦类型
     * @param 档       力度档
     * @param 重复间隔 重触发间隔（tick）
     */
    void 启动和弦记忆(Player 玩家, 乐器定义 乐器, int 根音, 和弦类型 类型, 力度档 档, int 重复间隔);

    /**
     * O8 停止玩家和弦记忆。
     *
     * @param 玩家标识 玩家唯一标识
     */
    void 停止和弦记忆(UUID 玩家标识);

    /**
     * O8 查询玩家是否处于和弦记忆中。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 是否和弦记忆中
     */
    boolean 是否和弦记忆中(UUID 玩家标识);

    /**
     * FP-18 上传公共乐谱：将玩家私有乐谱上传到公共库。
     * 上传后进入"待审核"状态，需管理员审核通过后才能被下载/评分。
     *
     * @param 玩家       上传玩家
     * @param 本地乐谱名 玩家私有库中的乐谱名
     * @param 公开名     公共库中的展示名（null 或空则使用本地乐谱名）
     * @return 上传结果
     */
    上传结果 上传公共乐谱(Player 玩家, String 本地乐谱名, String 公开名);

    /**
     * FP-18 下载公共乐谱到玩家私有库。
     * 仅"已通过"审核的乐谱可下载；下载会复制乐谱到玩家私有目录并增加下载计数。
     *
     * @param 玩家   下载玩家
     * @param 乐谱ID 公共乐谱ID
 * @return 下载结果
     */
    下载结果 下载公共乐谱(Player 玩家, String 乐谱ID);

    /**
     * FP-18 评分公共乐谱。同一玩家对同一乐谱仅可评一次。
     *
     * @param 玩家   评分玩家
     * @param 乐谱ID 公共乐谱ID
     * @param 评分   评分值（1-5）
     * @return 评分结果
     */
    评分结果 评分公共乐谱(Player 玩家, String 乐谱ID, int 评分);

    /**
     * FP-18 审核公共乐谱（管理员操作）。
     * 仅允许 待审核 -> 已通过 / 已拒绝。
     *
     * @param 玩家   操作玩家（管理员）
     * @param 乐谱ID 公共乐谱ID
     * @param 状态   目标状态（已通过 / 已拒绝）
     * @return 审核结果
     */
    审核结果 审核公共乐谱(Player 玩家, String 乐谱ID, 审核状态 状态);

    /**
     * FP-18 列出已通过审核的公共乐谱（仅元数据，不复制乐谱对象）。
     *
     * @return 公共乐谱列表
     */
    List<公共乐谱> 列出公共乐谱();

    // ===== FP-H NBS/MIDI 导入导出 + NoteBlockAPI 集成 =====

    /**
     * FP-07 从 .nbs 文件导入乐谱到玩家私有库。
     * 使用 OpenNBS v4 标准解析，导入后乐谱保存到玩家私有目录。
     *
     * @param 玩家     导入玩家
     * @param nbs文件 .nbs 文件
     * @param 乐谱名   保存的乐谱名（null 或空则使用 NBS 标题）
     * @return 导入后的乐谱；失败返回 Optional.empty()
     */
    Optional<乐谱> 导入NBS(Player 玩家, File nbs文件, String 乐谱名);

    /**
     * FP-07 将玩家私有乐谱导出为 .nbs 文件（OpenNBS v4 格式）。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 乐谱名   乐谱名
     * @param 目标文件 目标 .nbs 文件
     * @return true=导出成功；false=乐谱不存在或写入失败
     */
    boolean 导出NBS(UUID 玩家标识, String 乐谱名, File 目标文件);

    /**
     * FP-17 从 .mid 文件导入乐谱到玩家私有库。
     * 使用 javax.sound.midi 解析，导入后乐谱保存到玩家私有目录。
     *
     * @param 玩家     导入玩家
     * @param midi文件 .mid 文件
     * @param 乐谱名   保存的乐谱名（null 或空则使用默认名）
     * @return 导入后的乐谱；失败返回 Optional.empty()
     */
    Optional<乐谱> 导入MIDI(Player 玩家, File midi文件, String 乐谱名);

    /**
     * FP-17 将玩家私有乐谱导出为 .mid 文件（MIDI type 0 格式）。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 乐谱名   乐谱名
     * @param 目标文件 目标 .mid 文件
     * @return true=导出成功；false=乐谱不存在或写入失败
     */
    boolean 导出MIDI(UUID 玩家标识, String 乐谱名, File 目标文件);

    /**
     * FP-23 使用 NoteBlockAPI 播放玩家私有乐谱。
     * NoteBlockAPI 未安装时降级为原生 playSound 播放（返回 false 表示已降级）。
     *
     * @param 玩家   播放玩家
     * @param 乐谱名 乐谱名
     * @return true=NoteBlockAPI 播放成功；false=已降级为原生播放或乐谱不存在
     */
    boolean 使用NoteBlockAPI播放(Player 玩家, String 乐谱名);

    // ===== FP-J 分层键盘 split（O7）+ piano roll（O9）+ 弯音轮调制轮（O6）+ 表情控制（O10）+ 资源包 + mod 边界 =====

    /**
     * FP-19 设置玩家分层键盘模式开关。
     * 开启后演奏指令根据音高分界点选择不同乐器（左/右区）。
     * GUI 玩家与 mod 玩家共用此状态。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 启用     是否启用分层
     */
    void 设置分层模式(UUID 玩家标识, boolean 启用);

    /**
     * FP-19 查询玩家是否启用分层键盘模式。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 是否启用
     */
    boolean 是否分层模式(UUID 玩家标识);

    /**
     * FP-19 设置分层分界点（音高 < 分界点 → 左乐器，音高 >= 分界点 → 右乐器）。
     * 分界点会被 clamp 到 [分层配置.分界点下限, 分层配置.分界点上限]。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 分界点   分界点音高
     */
    void 设置分界点(UUID 玩家标识, int 分界点);

    /**
     * FP-19 获取玩家分层分界点。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 分界点音高（默认 {@link 分层配置#默认分界点}）
     */
    int 获取分界点(UUID 玩家标识);

    /**
     * FP-19 设置分层左区乐器（音高 < 分界点时使用）。
     * null 表示回退到玩家当前乐器。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 乐器     左区乐器定义；null 回退默认
     */
    void 设置分层左乐器(UUID 玩家标识, 乐器定义 乐器);

    /**
     * FP-19 设置分层右区乐器（音高 >= 分界点时使用）。
     * null 表示回退到玩家当前乐器。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 乐器     右区乐器定义；null 回退默认
     */
    void 设置分层右乐器(UUID 玩家标识, 乐器定义 乐器);

    /**
     * FP-19 获取玩家分层配置（不可变快照）。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 分层配置；未设置返回 {@link 分层配置#默认配置()}
     */
    分层配置 获取分层配置(UUID 玩家标识);

    /**
     * FP-19 切换玩家分层模式开关（GUI「分层切换」按钮调用）。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 切换后是否启用分层
     */
    boolean 切换分层模式(UUID 玩家标识);

    // ===== FP-20 piano roll 简化编辑器（O9）=====

    /**
     * FP-20 打开 piano roll 编辑器加载指定乐谱。
     * 编辑前备份原始乐谱（非破坏性，可恢复）。
     *
     * @param 玩家   编辑玩家
     * @param 乐谱名 待编辑乐谱名（玩家私有库）
     * @return true=已打开；false=乐谱不存在
     */
    boolean 打开PianoRoll编辑器(Player 玩家, String 乐谱名);

    /**
     * FP-20 关闭 piano roll 编辑器（不保存，丢弃编辑）。
     *
     * @param 玩家标识 玩家唯一标识
     */
    void 关闭PianoRoll编辑器(UUID 玩家标识);

    /**
     * FP-20 获取玩家当前 piano roll 编辑器实例。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 编辑器实例；未打开返回 Optional.empty()
     */
    Optional<钢琴卷帘编辑器> 获取PianoRoll编辑器(UUID 玩家标识);

    /**
     * FP-20 编辑 piano roll 中指定索引的音符。
     * 操作类型与参数：
     * <ul>
     *   <li>删除：仅用 音符索引</li>
     *   <li>移动/复制：用 音符索引 + 新音高 + 新时值Tick</li>
     *   <li>改力度：用 音符索引 + 新力度（0.0-1.0）</li>
     *   <li>新增：用 音高 + 时值Tick + 力度（音符索引忽略）</li>
     * </ul>
     *
     * @param 玩家标识 玩家唯一标识
     * @param 操作     编辑操作类型
     * @param 音符索引 目标音符索引（新增操作时忽略）
     * @param 新音高   目标音高（移动/复制/新增时使用）
     * @param 新时值Tick 目标时值（移动/复制/新增时使用）
     * @param 新力度   目标力度（改力度/新增时使用，0.0-1.0）
     * @return 编辑结果状态
     */
    钢琴卷帘编辑器.编辑结果 编辑PianoRoll音符(UUID 玩家标识, 钢琴卷帘编辑器.编辑操作 操作,
                                            int 音符索引, int 新音高, int 新时值Tick, double 新力度);

    /**
     * FP-20 保存 piano roll 编辑结果到玩家私有库。
     * 同名乐谱直接覆盖（已是非破坏性编辑，原始备份在编辑器中保留）。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 乐谱名   保存的乐谱名（null 或空则使用编辑中乐谱原名）
     * @return true=保存成功；false=编辑器未打开或乐谱名为空
     */
    boolean 保存PianoRoll编辑(UUID 玩家标识, String 乐谱名);

    /**
     * FP-20 撤销 piano roll 所有编辑，恢复到原始乐谱。
     *
     * @param 玩家标识 玩家唯一标识
     * @return true=已恢复；false=编辑器未打开
     */
    boolean 撤销PianoRoll编辑(UUID 玩家标识);

    // ===== FP-21 弯音轮 + 调制轮（mod 独享，O6）=====

    /**
     * FP-21 处理 mod 玩家弯音轮事件。
     * 弯音值 0-127（64=无弯音，0=-2 半音，127=+2 半音）。
     * 服务端权威计算 pitch 偏移，影响后续演奏指令的 pitch。
     * 反作弊：值范围自动 clamp 到 [0, 127]。
     *
     * @param 玩家   演奏玩家
     * @param 弯音值 弯音值（0-127）
     */
    void 处理弯音轮(Player 玩家, int 弯音值);

    /**
     * FP-21 处理 mod 玩家调制轮事件。
     * 调制值 0-127（0=无颤音，127=最大颤音深度）。
     * 服务端权威计算颤音 pitch 偏移（基于时间相位 + sin 函数）。
     * 反作弊：值范围自动 clamp 到 [0, 127]。
     *
     * @param 玩家   演奏玩家
     * @param 调制值 调制值（0-127）
     */
    void 处理调制轮(Player 玩家, int 调制值);

    /**
     * FP-21 获取玩家弯音/调制状态（不可变快照）。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 弯音调制状态；未设置返回 {@link 弯音调制状态#默认状态()}
     */
    弯音调制状态 获取弯音调制状态(UUID 玩家标识);

    /**
     * FP-21 设置玩家弯音范围（±N 半音，默认 ±2）。
     *
     * @param 玩家标识 玩家唯一标识
     * @param 半音数   弯音范围半音数（1-12）
     */
    void 设置弯音范围(UUID 玩家标识, int 半音数);

    /**
     * FP-21 获取玩家弯音范围。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 弯音范围半音数（默认 {@link 弯音调制状态#默认弯音范围半音}）
     */
    int 获取弯音范围(UUID 玩家标识);

    // ===== FP-22 表情控制（mod 独享，O10）=====

    /**
     * FP-22 处理 mod 玩家表情控制事件。
     * 支持 CC1（modulation）/CC2（breath）/CC4（foot）/CC11（expression）。
     * 服务端权威计算表情参数：CC11 影响 volume（乘法叠加）；CC1/CC2/CC4 影响音色变化。
     * 反作弊：CC 编号与值范围自动校验。
     *
     * @param 玩家  演奏玩家
     * @param cc编号 CC 编号（仅支持 1/2/4/11，其他忽略）
     * @param 值    CC 值（0-127，自动 clamp）
     */
    void 处理表情控制(Player 玩家, int cc编号, int 值);

    /**
     * FP-22 获取玩家表情控制状态（不可变快照）。
     *
     * @param 玩家标识 玩家唯一标识
     * @return 表情控制状态；未设置返回 {@link 表情控制状态#默认状态()}
     */
    表情控制状态 获取表情控制状态(UUID 玩家标识);

    // ===== FP-14 资源包（保底方案）=====

    /**
     * FP-14 应用资源包到玩家。
     * 通过 Player.setResourcePack 推送资源包；资源包未生成时先自动生成。
     * 玩家拒绝下载时降级为原版保底音色，功能完整可用。
     *
     * @param 玩家 目标玩家
     * @return true=已发送应用请求；false=资源包未配置或玩家不在线
     */
    boolean 应用资源包(Player 玩家);

    /**
     * FP-14 生成默认资源包到 plugins/XRM/资源包/ 目录。
     * 包含 assets/minecraft/sounds/xrm/ 子目录与 pack.mcmeta。
     *
     * @return true=生成成功；false=已存在且未变更或生成失败
     */
    boolean 生成资源包();

    /**
     * FP-14 查询资源包是否已配置（资源包文件存在且可读）。
     *
     * @return true=已配置；false=未配置
     */
    boolean 资源包已配置();

    // ===== FP-12 mod 边界 =====

    /**
     * FP-12 注册 mod 输入通道 "xrm:music_input"。
     * 由 玄锐暮插件.onEnable 调用，注册 PluginMessage 通道接收 mod 端按键事件。
     * mod 端发送 JSON 消息：{type: "note_on"/"note_off"/"pitch_bend"/"modulation"/"expression", ...}
     *
     * @return true=注册成功；false=已注册或注册失败
     */
    boolean 注册mod输入通道();

    /**
     * FP-12 查询 mod 输入通道是否已注册。
     *
     * @return true=已注册；false=未注册
     */
    boolean mod输入通道已注册();
}
