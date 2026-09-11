package mljy.业务层;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import mljy.翻译服务;
import mljy.基础设施层.调试日志器;
import mljy.基础设施层.乐器.NBS解析器;
import mljy.基础设施层.乐器.NBS导出器;
import mljy.基础设施层.乐器.MIDI解析器;
import mljy.基础设施层.乐器.MIDI导出器;
import mljy.基础设施层.乐器.NoteBlockAPI适配器;
import mljy.基础设施层.乐器.资源包管理器;
import mljy.基础设施层.乐器.mod输入通道;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.业务层.乐器.节拍器;
import mljy.业务层.乐器.公共乐谱服务;
import mljy.业务层.乐器.公共乐谱服务.上传结果;
import mljy.业务层.乐器.公共乐谱服务.下载结果;
import mljy.业务层.乐器.公共乐谱服务.评分结果;
import mljy.业务层.乐器.公共乐谱服务.审核结果;
import mljy.领域层.乐器.乐谱;
import mljy.领域层.乐器.公共乐谱;
import mljy.领域层.乐器.公共乐谱.审核状态;
import mljy.领域层.乐器.力度档;
import mljy.领域层.乐器.乐器定义;
import mljy.领域层.乐器.乐器注册表;
import mljy.领域层.乐器.键位绑定表;
import mljy.领域层.乐器.和弦类型;
import mljy.领域层.乐器.和弦预设;
import mljy.领域层.乐器.音符;
import mljy.领域层.乐器.合奏状态;
import mljy.领域层.乐器.音高方块映射;
import mljy.领域层.乐器.功能位定义;
import mljy.领域层.乐器.量化档位;
import mljy.领域层.乐器.延音模式;
import mljy.领域层.乐器.分层配置;
import mljy.领域层.乐器.弯音调制状态;
import mljy.领域层.乐器.表情控制状态;
import mljy.领域层.乐器.钢琴卷帘编辑器;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 乐器服务实现。
 * 管理演奏、录制、乐谱持久化、乐谱播放、合奏等功能。
 * 乐谱保存到 plugins/XRM/乐谱/{UUID}/{乐谱名}.yml。
 * 声音播放使用 player.playSound(Location, Sound, volume, pitch)，pitch 对应音高（0.5-2.0）。
 *
 * FP-13 配置驱动改造：所有 乐器类型 引用替换为 乐器定义，通过注入的 乐器注册表 查询。
 */
@Singleton
public class 乐器服务实现 implements 乐器服务 {
    private static final String 乐器配置文件名 = "乐器配置.yml";

    private static final int 默认速度BPM = 120;
    private static final int 默认时值Tick = 4;
    private static final double 默认力度 = 1.0;
    private static final int 每Tick = 1;
    private static final int GUI大小 = 54;
    private static final int 合奏最小人数 = 2;
    private static final long 毫秒每秒 = 1000L;
    private static final long 毫秒每Tick = 50L;
    private static final float 默认音量 = 1.0f;
    private static final int 粒子数量 = 8;
    private static final double 粒子偏移 = 0.3;
    private static final float 粒子尺寸 = 1.0f;

    private static final String 翻译根键 = "乐器服务";
    private static final String 录制开始键 = 翻译根键 + ".录制开始";
    private static final String 录制停止键 = 翻译根键 + ".录制停止";
    private static final String 乐谱保存成功键 = 翻译根键 + ".乐谱保存成功";
    private static final String 乐谱删除成功键 = 翻译根键 + ".乐谱删除成功";
    private static final String 乐谱开始播放键 = 翻译根键 + ".乐谱开始播放";
    private static final String 乐谱播放结束键 = 翻译根键 + ".乐谱播放结束";
    private static final String 合奏开始键 = 翻译根键 + ".合奏开始";
    private static final String 合奏结束键 = 翻译根键 + ".合奏结束";
    private static final String 合奏拒绝键 = 翻译根键 + ".合奏拒绝";
    private static final String 合奏确认进度键 = 翻译根键 + ".合奏确认进度";
    private static final String 合奏倒计时键 = 翻译根键 + ".合奏倒计时";
    private static final String 合奏已停止键 = 翻译根键 + ".合奏已停止";
    private static final String 默认乐谱名键 = 翻译根键 + ".默认乐谱名";
    private static final String 切换乐器键 = 翻译根键 + ".切换乐器";
    private static final String 给予成功键 = 翻译根键 + ".给予成功";
    private static final String 乐谱已存在键 = 翻译根键 + ".乐谱已存在";
    private static final String 乐谱超限键 = 翻译根键 + ".乐谱超限";
    private static final String 无录制内容键 = 翻译根键 + ".无录制内容";
    private static final String 乐谱名无效键 = 翻译根键 + ".乐谱名无效";

    private static final String 演奏GUI根键 = "演奏GUI";
    private static final String 演奏标题键 = 演奏GUI根键 + ".演奏标题";
    private static final String 音符按钮键 = 演奏GUI根键 + ".音符按钮";
    private static final String 音符说明键 = 演奏GUI根键 + ".音符说明";
    private static final String 功能未实现键 = 演奏GUI根键 + ".功能未实现";
    private static final String 延音模式切换键 = 演奏GUI根键 + ".延音模式切换";
    private static final String 延音模式按住键 = 演奏GUI根键 + ".延音模式.按住";
    private static final String 延音模式切换模式键 = 演奏GUI根键 + ".延音模式.切换";
    private static final String 延音已切换键 = 演奏GUI根键 + ".延音已切换";
    private static final String 音域显示键 = 演奏GUI根键 + ".音域显示";
    private static final String 力度显示键 = 演奏GUI根键 + ".力度显示";

    // FP-F 控制按钮翻译键（中部显示区）
    private static final String 八度减键 = 演奏GUI根键 + ".八度减";
    private static final String 八度加键 = 演奏GUI根键 + ".八度加";
    private static final String 力度减键 = 演奏GUI根键 + ".力度减";
    private static final String 力度加键 = 演奏GUI根键 + ".力度加";
    private static final String 录制按钮键 = 演奏GUI根键 + ".录制";
    private static final String 停止按钮键 = 演奏GUI根键 + ".停止";
    private static final String 播放按钮键 = 演奏GUI根键 + ".播放";

    private static final String 物品前缀键 = "乐器命令.物品.名称前缀";
    private static final String 物品音色键 = "乐器命令.物品.音色";

    // FP-E 翻译键
    private static final String 音域切换键 = 翻译根键 + ".音域切换";
    private static final String 力度切换键 = 翻译根键 + ".力度切换";
    private static final String 真实力度已开启键 = 翻译根键 + ".真实力度已开启";
    private static final String 真实力度已关闭键 = 翻译根键 + ".真实力度已关闭";
    private static final String 音高越界键 = 翻译根键 + ".音高越界";

    // FP-G 翻译键（量化 + swing + 录制指示）
    private static final String 量化已应用键 = 翻译根键 + ".量化已应用";
    private static final String 量化已撤销键 = 翻译根键 + ".量化已撤销";
    private static final String 无可量化乐谱键 = 翻译根键 + ".无可量化乐谱";
    private static final String swing已设置键 = 翻译根键 + ".swing已设置";
    private static final String 录制中标题键 = 翻译根键 + ".录制中标题";

    // FP-I 翻译键（和弦 + 公共乐谱库）
    private static final String 和弦已播放键 = 翻译根键 + ".和弦已播放";
    private static final String 和弦记忆已启动键 = 翻译根键 + ".和弦记忆已启动";
    private static final String 和弦记忆已停止键 = 翻译根键 + ".和弦记忆已停止";
    private static final String 公共乐谱上传成功键 = 翻译根键 + ".公共乐谱上传成功";
    private static final String 公共乐谱上传重名键 = 翻译根键 + ".公共乐谱上传重名";
    private static final String 公共乐谱上传参数无效键 = 翻译根键 + ".公共乐谱上传参数无效";
    private static final String 公共乐谱下载成功键 = 翻译根键 + ".公共乐谱下载成功";
    private static final String 公共乐谱不存在键 = 翻译根键 + ".公共乐谱不存在";
    private static final String 公共乐谱未通过审核键 = 翻译根键 + ".公共乐谱未通过审核";
    private static final String 公共乐谱评分成功键 = 翻译根键 + ".公共乐谱评分成功";
    private static final String 公共乐谱已评分键 = 翻译根键 + ".公共乐谱已评分";
    private static final String 公共乐谱评分越界键 = 翻译根键 + ".公共乐谱评分越界";
    private static final String 公共乐谱审核成功键 = 翻译根键 + ".公共乐谱审核成功";
    private static final String 公共乐谱状态无效键 = 翻译根键 + ".公共乐谱状态无效";
    private static final String 公共乐谱列表为空键 = 翻译根键 + ".公共乐谱列表为空";
    private static final String 公共乐谱列表项键 = 翻译根键 + ".公共乐谱列表项";
    // FP-H NBS/MIDI 导入导出 + NoteBlockAPI
    private static final String NBS导入成功键 = 翻译根键 + ".NBS导入成功";
    private static final String NBS导入失败键 = 翻译根键 + ".NBS导入失败";
    private static final String NBS导出成功键 = 翻译根键 + ".NBS导出成功";
    private static final String NBS导出失败键 = 翻译根键 + ".NBS导出失败";
    private static final String MIDI导入成功键 = 翻译根键 + ".MIDI导入成功";
    private static final String MIDI导入失败键 = 翻译根键 + ".MIDI导入失败";
    private static final String MIDI导出成功键 = 翻译根键 + ".MIDI导出成功";
    private static final String MIDI导出失败键 = 翻译根键 + ".MIDI导出失败";
    private static final String 文件不可读键 = 翻译根键 + ".文件不可读";
    private static final String 乐谱不存在键 = 翻译根键 + ".乐谱不存在";

    // FP-J 翻译键（分层 + piano roll + 资源包）
    private static final String 分层已开启键 = 翻译根键 + ".分层已开启";
    private static final String 分层已关闭键 = 翻译根键 + ".分层已关闭";
    private static final String 分界点已设置键 = 翻译根键 + ".分界点已设置";
    private static final String 分层乐器已设置键 = 翻译根键 + ".分层乐器已设置";
    private static final String pianoRoll已打开键 = 翻译根键 + ".pianoRoll已打开";
    private static final String pianoRoll已关闭键 = 翻译根键 + ".pianoRoll已关闭";
    private static final String pianoRoll保存成功键 = 翻译根键 + ".pianoRoll保存成功";
    private static final String pianoRoll已撤销键 = 翻译根键 + ".pianoRoll已撤销";
    private static final String pianoRoll编辑失败键 = 翻译根键 + ".pianoRoll编辑失败";
    private static final String 资源包应用提示键 = 翻译根键 + ".资源包应用提示";

    // FP-E 配置常量
    private static final String 键位绑定段键 = "键位绑定";
    private static final String mod段键 = "mod";
    private static final String modVelocityByDuration键 = "velocity-by-duration";
    private static final String mod最大有效时长ms键 = "最大有效时长ms";
    private static final int mod最大有效时长ms默认 = 400;
    private static final int velocity上限 = 127;
    private static final int velocity下限 = 0;
    private static final int 音高下限 = 0;
    private static final int 音高上限 = 59;

    // FP-F 配置段键
    private static final String 功能位段键 = "功能位";
    private static final String 中部控制段键 = "中部控制";
    private static final String 钢琴布局段键 = "钢琴布局";
    private static final String 延音段键 = "延音";
    private static final String 槽位键 = "槽位";
    private static final String 材质键 = "材质";
    private static final String 模型数据键 = "模型数据";
    private static final String 默认模式键 = "默认模式";
    private static final String 重触发间隔键 = "重触发间隔";
    private static final String 重触发次数键 = "重触发次数";
    private static final String 衰减系数键 = "衰减系数";
    private static final String 黑键槽位键 = "黑键槽位";
    private static final String 白键槽位键 = "白键槽位";
    private static final String 黑键音高偏移键 = "黑键音高偏移";
    private static final String 白键音高偏移键 = "白键音高偏移";

    // FP-F 默认钢琴布局（与 乐器配置.yml 一致，配置缺失时回退）
    private static final int[] 默认黑键槽位 = {37, 39, 41, 42, 43};
    private static final int[] 默认白键槽位 = {45, 46, 47, 48, 49, 50, 51};
    private static final int[] 默认黑键音高偏移 = {1, 3, 6, 8, 10};
    private static final int[] 默认白键音高偏移 = {0, 2, 4, 5, 7, 9, 11};
    private static final int 默认延音重触发间隔 = 4;
    private static final int 默认延音重触发次数 = 4;
    private static final double 默认延音衰减系数 = 0.85;

    // FP-G 配置段键（量化 + swing + 可视化）
    private static final String 量化段键 = "量化";
    private static final String swing段键 = "swing";
    private static final String 可视化段键 = "可视化";
    private static final String 默认档位键 = "默认档位";
    private static final String 量化默认开关键 = "默认开启";
    private static final String swing默认比例键 = "默认比例";
    private static final String swing默认开关键 = "默认开启";
    private static final String GUI高亮持续tick键 = "GUI高亮持续tick";
    private static final String 粒子启用键 = "粒子启用";
    private static final String 拍号显示键 = "拍号显示";
    // FP-I 配置段键（和弦记忆默认重复间隔）
    private static final String 和弦段键 = "和弦";
    private static final String 默认记忆重复间隔键 = "默认记忆重复间隔";
    private static final int 默认记忆重复间隔 = 10;
    // FP-G 默认值
    private static final int 默认GUI高亮持续tick = 8;
    private static final double swing比例下限 = 0.50;
    private static final double swing比例上限 = 0.65;
    private static final double 默认Swing比例 = 0.50;
    // FP-10 录制中标题显示参数
    private static final int 录制中标题淡入tick = 2;
    private static final int 录制中标题停留tick = 10;
    private static final int 录制中标题淡出tick = 5;

    private static final String 乐谱目录名 = "乐谱";
    private static final String 音高键 = "音高";
    private static final String 时值键 = "时值";
    private static final String 力度键 = "力度";
    private static final String 名称键 = "名称";
    private static final String 作者键 = "作者";
    private static final String 作者名键 = "作者名";
    private static final String 创建时间键 = "创建时间";
    private static final String 速度键 = "速度BPM";
    private static final String 音符列表键 = "音符列表";

    private final 翻译服务 翻译服务;
    private final 关键词解析器 关键词解析器;
    private final 组队服务 组队服务;
    private final JavaPlugin 插件;
    private final 乐器注册表 乐器注册表;
    private final 节拍器 节拍器;
    private final MiniMessage 迷你消息;

    private volatile double 听觉范围;
    private volatile boolean 音符粒子效果;
    private volatile int 最大乐谱存储数;
    private volatile int 最大录制时长秒;

    // FP-E 配置参数
    private volatile 键位绑定表 键位绑定;
    private volatile boolean modVelocityByDuration默认;
    private volatile int mod最大有效时长ms;

    // FP-F 配置参数（功能位 + 中部控制 + 钢琴布局 + 延音）
    private volatile Map<Integer, 功能位定义> 功能位槽位表 = new HashMap<>();
    private volatile Map<功能位定义, Material> 功能位材质表 = new HashMap<>();
    private volatile Map<功能位定义, Integer> 功能位模型数据表 = new HashMap<>();
    private volatile int 槽位音域减 = 9;
    private volatile int 槽位音域显示 = 10;
    private volatile int 槽位音域加 = 11;
    private volatile int 槽位力度减 = 12;
    private volatile int 槽位力度显示 = 13;
    private volatile int 槽位力度加 = 14;
    private volatile int 槽位录制 = 15;
    private volatile int 槽位停止 = 16;
    private volatile int 槽位播放 = 17;
    private volatile int 槽位延音模式切换 = 18;
    private volatile int[] 黑键槽位数组 = 默认黑键槽位.clone();
    private volatile int[] 白键槽位数组 = 默认白键槽位.clone();
    private volatile int[] 黑键音高偏移数组 = 默认黑键音高偏移.clone();
    private volatile int[] 白键音高偏移数组 = 默认白键音高偏移.clone();
    private volatile 延音模式 默认延音模式值 = 延音模式.默认模式();
    private volatile int 延音重触发间隔 = 默认延音重触发间隔;
    private volatile int 延音重触发次数 = 默认延音重触发次数;
    private volatile double 延音衰减系数 = 默认延音衰减系数;

    // FP-G 配置参数（量化 + swing + 可视化）
    private volatile 量化档位 默认量化档位值 = 量化档位.默认档位();
    private volatile boolean 量化默认开启值 = true;
    private volatile double 默认swing比例值 = 默认Swing比例;
    private volatile boolean swing默认开启值 = false;
    private volatile int GUI高亮持续tick值 = 默认GUI高亮持续tick;
    private volatile boolean 粒子启用值 = true;
    private volatile boolean 拍号显示启用值 = false;
    // FP-I 配置参数（和弦记忆默认重复间隔）
    private volatile int 默认记忆重复间隔值 = 默认记忆重复间隔;

    private final Map<UUID, 乐器定义> 玩家当前乐器 = new ConcurrentHashMap<>();
    private final Map<UUID, 录制状态> 录制状态表 = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> 播放任务表 = new ConcurrentHashMap<>();
    private final Map<UUID, 合奏状态内部> 合奏状态表 = new ConcurrentHashMap<>();
    private final Map<UUID, Inventory> 演奏界面表 = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> 玩家节拍器开关表 = new ConcurrentHashMap<>();
    // FP-E 玩家演奏状态：音域 / 力度档 / 真实力度开关
    private final Map<UUID, Integer> 玩家音域表 = new ConcurrentHashMap<>();
    private final Map<UUID, 力度档> 玩家力度档表 = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> 玩家真实力度表 = new ConcurrentHashMap<>();
    // FP-F 玩家延音状态：延音模式 + 延音任务 + 延音音高
    private final Map<UUID, 延音模式> 玩家延音模式表 = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> 玩家延音任务表 = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> 玩家延音音高表 = new ConcurrentHashMap<>();
    // FP-G 玩家量化/swing 状态：量化乐谱（当前操作的乐谱）+ swing 比例 + 量化开关 + 量化档位
    private final Map<UUID, 乐谱> 玩家量化乐谱表 = new ConcurrentHashMap<>();
    private final Map<UUID, Double> 玩家Swing表 = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> 玩家量化开关表 = new ConcurrentHashMap<>();
    private final Map<UUID, 量化档位> 玩家量化档位表 = new ConcurrentHashMap<>();
    // FP-I 和弦记忆状态：玩家 -> 和弦记忆任务（O8 循环触发）
    private final Map<UUID, BukkitRunnable> 玩家和弦记忆任务表 = new ConcurrentHashMap<>();

    // FP-J 玩家分层/piano roll/弯音调制/表情控制状态表
    private final Map<UUID, 分层配置> 玩家分层配置表 = new ConcurrentHashMap<>();
    private final Map<UUID, 钢琴卷帘编辑器> 玩家PianoRoll编辑器表 = new ConcurrentHashMap<>();
    private final Map<UUID, 弯音调制状态> 玩家弯音调制状态表 = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> 玩家弯音范围表 = new ConcurrentHashMap<>();
    private final Map<UUID, 表情控制状态> 玩家表情控制状态表 = new ConcurrentHashMap<>();

    private final 公共乐谱服务 公共乐谱服务;
    private final 资源包管理器 资源包管理器;
    /**
     * FP-12 mod 输入通道：使用 Provider 延迟解析，打破 乐器服务实现 ↔ mod输入通道 的循环依赖。
     * mod输入通道 构造器注入 乐器服务，乐器服务实现 通过 Provider 字段注入 mod输入通道。
     */
    @Inject
    private com.google.inject.Provider<mod输入通道> mod输入通道Provider;

    @Inject
    public 乐器服务实现(翻译服务 翻译服务, 关键词解析器 关键词解析器,
                       组队服务 组队服务, JavaPlugin 插件, 乐器注册表 乐器注册表,
                       节拍器 节拍器, 公共乐谱服务 公共乐谱服务,
                       资源包管理器 资源包管理器) {
        this.翻译服务 = 翻译服务;
        this.关键词解析器 = 关键词解析器;
        this.组队服务 = 组队服务;
        this.插件 = 插件;
        this.乐器注册表 = 乐器注册表;
        this.节拍器 = 节拍器;
        this.公共乐谱服务 = 公共乐谱服务;
        this.资源包管理器 = 资源包管理器;
        this.迷你消息 = MiniMessage.miniMessage();
        this.键位绑定 = 键位绑定表.默认表();
        this.modVelocityByDuration默认 = false;
        this.mod最大有效时长ms = mod最大有效时长ms默认;
        刷新全局参数();
        加载乐器配置参数();
        加载FP_F配置();
        加载FP_G配置();
        加载FP_I配置();
        加载FP_J配置();
    }

    private void 刷新全局参数() {
        乐器注册表.全局参数 全局 = 乐器注册表.获取全局参数();
        this.听觉范围 = 全局.获取听觉范围();
        this.音符粒子效果 = 全局.是否音符粒子效果();
        this.最大乐谱存储数 = 全局.获取最大乐谱存储数();
        this.最大录制时长秒 = 全局.获取最大录制时长秒();
    }

    /**
     * FP-E 加载乐器配置参数（键位绑定 + mod 真实力度）。
     * 启动时与 /乐器 重载 时调用。
     */
    private void 加载乐器配置参数() {
        File 配置文件 = new File(插件.getDataFolder(), 乐器配置文件名);
        if (!配置文件.exists()) {
            this.键位绑定 = 键位绑定表.默认表();
            this.modVelocityByDuration默认 = false;
            this.mod最大有效时长ms = mod最大有效时长ms默认;
            return;
        }
        FileConfiguration 配置 = YamlConfiguration.loadConfiguration(配置文件);
        this.键位绑定 = 键位绑定表.从配置加载(配置);
        org.bukkit.configuration.ConfigurationSection mod段 = 配置.getConfigurationSection(mod段键);
        if (mod段 != null) {
            this.modVelocityByDuration默认 = mod段.getBoolean(modVelocityByDuration键, false);
            this.mod最大有效时长ms = Math.max(1, mod段.getInt(mod最大有效时长ms键, mod最大有效时长ms默认));
        } else {
            this.modVelocityByDuration默认 = false;
            this.mod最大有效时长ms = mod最大有效时长ms默认;
        }
    }

    /**
     * FP-F 加载功能位 + 中部控制 + 钢琴布局 + 延音配置。
     * 启动时与 /乐器 重载 时调用。配置缺失时回退到默认值。
     */
    private void 加载FP_F配置() {
        File 配置文件 = new File(插件.getDataFolder(), 乐器配置文件名);
        if (!配置文件.exists()) {
            加载默认FP_F配置();
            return;
        }
        FileConfiguration 配置 = YamlConfiguration.loadConfiguration(配置文件);
        加载功能位配置(配置);
        加载中部控制配置(配置);
        加载钢琴布局配置(配置);
        加载延音配置(配置);
    }

    private void 加载默认FP_F配置() {
        Map<Integer, 功能位定义> 槽位表 = new HashMap<>();
        Map<功能位定义, Material> 材质表 = new HashMap<>();
        Map<功能位定义, Integer> 模型表 = new HashMap<>();
        槽位表.put(0, 功能位定义.保存);
        槽位表.put(1, 功能位定义.清除);
        槽位表.put(2, 功能位定义.关闭);
        槽位表.put(3, 功能位定义.NBS导入);
        槽位表.put(4, 功能位定义.NBS导出);
        槽位表.put(5, 功能位定义.和弦预设);
        槽位表.put(6, 功能位定义.公共乐谱库);
        槽位表.put(7, 功能位定义.钢琴卷帘编辑);
        槽位表.put(8, 功能位定义.分层切换);
        材质表.put(功能位定义.保存, Material.WRITABLE_BOOK);
        材质表.put(功能位定义.清除, Material.BARRIER);
        材质表.put(功能位定义.关闭, Material.TNT);
        材质表.put(功能位定义.NBS导入, Material.MUSIC_DISC_CAT);
        材质表.put(功能位定义.NBS导出, Material.MUSIC_DISC_MALL);
        材质表.put(功能位定义.和弦预设, Material.SEA_LANTERN);
        材质表.put(功能位定义.公共乐谱库, Material.CHEST);
        材质表.put(功能位定义.钢琴卷帘编辑, Material.FILLED_MAP);
        材质表.put(功能位定义.分层切换, Material.COMPARATOR);
        模型表.put(功能位定义.保存, 9990);
        模型表.put(功能位定义.清除, 9991);
        模型表.put(功能位定义.关闭, 9992);
        模型表.put(功能位定义.NBS导入, 9993);
        模型表.put(功能位定义.NBS导出, 9994);
        模型表.put(功能位定义.和弦预设, 9995);
        模型表.put(功能位定义.公共乐谱库, 9996);
        模型表.put(功能位定义.钢琴卷帘编辑, 9997);
        模型表.put(功能位定义.分层切换, 9998);
        this.功能位槽位表 = 槽位表;
        this.功能位材质表 = 材质表;
        this.功能位模型数据表 = 模型表;
        this.黑键槽位数组 = 默认黑键槽位.clone();
        this.白键槽位数组 = 默认白键槽位.clone();
        this.黑键音高偏移数组 = 默认黑键音高偏移.clone();
        this.白键音高偏移数组 = 默认白键音高偏移.clone();
        this.默认延音模式值 = 延音模式.默认模式();
        this.延音重触发间隔 = 默认延音重触发间隔;
        this.延音重触发次数 = 默认延音重触发次数;
        this.延音衰减系数 = 默认延音衰减系数;
    }

    private void 加载功能位配置(FileConfiguration 配置) {
        ConfigurationSection 段 = 配置.getConfigurationSection(功能位段键);
        if (段 == null) {
            加载默认FP_F配置();
            return;
        }
        Map<Integer, 功能位定义> 槽位表 = new HashMap<>();
        Map<功能位定义, Material> 材质表 = new HashMap<>();
        Map<功能位定义, Integer> 模型表 = new HashMap<>();
        for (功能位定义 位 : 功能位定义.values()) {
            ConfigurationSection 位段 = 段.getConfigurationSection(位.获取标识());
            if (位段 == null) {
                continue;
            }
            int 槽位 = 位段.getInt(槽位键, -1);
            if (槽位 < 0 || 槽位 >= GUI大小) {
                continue;
            }
            String 材质名 = 位段.getString(材质键, "PAPER");
            Material 材质;
            try {
                材质 = Material.valueOf(材质名.toUpperCase());
            } catch (IllegalArgumentException e) {
                材质 = Material.PAPER;
            }
            int 模型数据 = 位段.getInt(模型数据键, 9990);
            槽位表.put(槽位, 位);
            材质表.put(位, 材质);
            模型表.put(位, 模型数据);
        }
        this.功能位槽位表 = 槽位表;
        this.功能位材质表 = 材质表;
        this.功能位模型数据表 = 模型表;
    }

    private void 加载中部控制配置(FileConfiguration 配置) {
        ConfigurationSection 段 = 配置.getConfigurationSection(中部控制段键);
        if (段 == null) {
            return;
        }
        this.槽位音域减 = 段.getInt("音域减", 9);
        this.槽位音域显示 = 段.getInt("音域显示", 10);
        this.槽位音域加 = 段.getInt("音域加", 11);
        this.槽位力度减 = 段.getInt("力度减", 12);
        this.槽位力度显示 = 段.getInt("力度显示", 13);
        this.槽位力度加 = 段.getInt("力度加", 14);
        this.槽位录制 = 段.getInt("录制", 15);
        this.槽位停止 = 段.getInt("停止", 16);
        this.槽位播放 = 段.getInt("播放", 17);
        this.槽位延音模式切换 = 段.getInt("延音模式切换", 18);
    }

    private void 加载钢琴布局配置(FileConfiguration 配置) {
        ConfigurationSection 段 = 配置.getConfigurationSection(钢琴布局段键);
        if (段 == null) {
            return;
        }
        this.黑键槽位数组 = 读取整型数组(段, 黑键槽位键, 默认黑键槽位);
        this.白键槽位数组 = 读取整型数组(段, 白键槽位键, 默认白键槽位);
        this.黑键音高偏移数组 = 读取整型数组(段, 黑键音高偏移键, 默认黑键音高偏移);
        this.白键音高偏移数组 = 读取整型数组(段, 白键音高偏移键, 默认白键音高偏移);
    }

    private void 加载延音配置(FileConfiguration 配置) {
        ConfigurationSection 段 = 配置.getConfigurationSection(延音段键);
        if (段 == null) {
            return;
        }
        String 模式名 = 段.getString(默认模式键, "按住");
        this.默认延音模式值 = "切换".equals(模式名) ? 延音模式.切换 : 延音模式.按住;
        this.延音重触发间隔 = Math.max(1, 段.getInt(重触发间隔键, 默认延音重触发间隔));
        this.延音重触发次数 = Math.max(1, 段.getInt(重触发次数键, 默认延音重触发次数));
        double 衰减 = 段.getDouble(衰减系数键, 默认延音衰减系数);
        this.延音衰减系数 = Math.max(0.0, Math.min(1.0, 衰减));
    }

    /**
     * FP-G 加载量化 + swing + 可视化配置。
     * 启动时与 /乐器 重载 时调用。配置缺失时回退到默认值。
     */
    private void 加载FP_G配置() {
        File 配置文件 = new File(插件.getDataFolder(), 乐器配置文件名);
        if (!配置文件.exists()) {
            return;
        }
        FileConfiguration 配置 = YamlConfiguration.loadConfiguration(配置文件);
        加载量化配置(配置);
        加载swing配置(配置);
        加载可视化配置(配置);
    }

    /**
     * FP-I 加载和弦 + 公共乐谱库配置。
     * 启动时与 /乐器 重载 时调用。配置缺失时回退到默认值。
     */
    private void 加载FP_I配置() {
        File 配置文件 = new File(插件.getDataFolder(), 乐器配置文件名);
        if (!配置文件.exists()) {
            return;
        }
        FileConfiguration 配置 = YamlConfiguration.loadConfiguration(配置文件);
        ConfigurationSection 和弦段 = 配置.getConfigurationSection(和弦段键);
        if (和弦段 != null) {
            this.默认记忆重复间隔值 = Math.max(1, 和弦段.getInt(默认记忆重复间隔键, 默认记忆重复间隔));
        }
        公共乐谱服务.从配置加载(配置);
    }

    // FP-J 配置段键
    private static final String 分层段键 = "分层";
    private static final String pianoRoll段键 = "piano_roll";
    private static final String 弯音调制段键 = "弯音调制";
    private static final String 表情控制段键 = "表情控制";
    private static final String 资源包段键 = "资源包";
    private static final String mod通道段键 = "mod通道";
    // FP-J 配置字段名
    private static final String 默认启用键 = "默认启用";
    private static final String 默认分界点键 = "默认分界点";
    private static final String 最小时间单位tick键 = "最小时间单位tick";
    private static final String 默认弯音范围键 = "默认弯音范围半音";
    private static final String 默认调制频率键 = "默认调制频率Hz";
    private static final String 资源包启用键 = "启用";
    // FP-J 配置参数（volatile 支持热重载）
    private volatile int 默认弯音范围半音值 = 弯音调制状态.默认弯音范围半音;
    private volatile int 默认调制频率Hz值 = 弯音调制状态.默认调制频率Hz;
    private volatile boolean 资源包启用值 = false;

    /**
     * FP-J 加载分层/piano roll/弯音调制/表情控制/资源包/mod通道配置。
     * 启动时与 /乐器 重载 时调用。配置缺失时回退到默认值。
     */
    private void 加载FP_J配置() {
        File 配置文件 = new File(插件.getDataFolder(), 乐器配置文件名);
        if (!配置文件.exists()) {
            资源包管理器.从配置加载(false);
            return;
        }
        FileConfiguration 配置 = YamlConfiguration.loadConfiguration(配置文件);
        加载分层配置(配置);
        加载弯音调制配置(配置);
        加载资源包配置(配置);
    }

    private void 加载分层配置(FileConfiguration 配置) {
        ConfigurationSection 段 = 配置.getConfigurationSection(分层段键);
        if (段 == null) {
            return;
        }
        // 分层默认配置仅影响新玩家的初始分层配置（默认未启用，分界点 12）
        // 现有玩家的分层配置保存在 玩家分层配置表 中，热重载不影响
    }

    private void 加载弯音调制配置(FileConfiguration 配置) {
        ConfigurationSection 段 = 配置.getConfigurationSection(弯音调制段键);
        if (段 == null) {
            return;
        }
        this.默认弯音范围半音值 = Math.max(1, Math.min(12,
                段.getInt(默认弯音范围键, 弯音调制状态.默认弯音范围半音)));
        this.默认调制频率Hz值 = Math.max(1, Math.min(10,
                段.getInt(默认调制频率键, 弯音调制状态.默认调制频率Hz)));
    }

    private void 加载资源包配置(FileConfiguration 配置) {
        ConfigurationSection 段 = 配置.getConfigurationSection(资源包段键);
        boolean 启用 = 段 != null && 段.getBoolean(资源包启用键, false);
        资源包管理器.从配置加载(启用);
    }

    private void 加载量化配置(FileConfiguration 配置) {
        ConfigurationSection 段 = 配置.getConfigurationSection(量化段键);
        if (段 == null) {
            return;
        }
        this.默认量化档位值 = 量化档位.按标识查找(段.getString(默认档位键, 量化档位.默认档位().获取标识()));
        this.量化默认开启值 = 段.getBoolean(量化默认开关键, true);
    }

    private void 加载swing配置(FileConfiguration 配置) {
        ConfigurationSection 段 = 配置.getConfigurationSection(swing段键);
        if (段 == null) {
            return;
        }
        double 比例 = 段.getDouble(swing默认比例键, 默认Swing比例);
        this.默认swing比例值 = Math.max(swing比例下限, Math.min(swing比例上限, 比例));
        this.swing默认开启值 = 段.getBoolean(swing默认开关键, false);
    }

    private void 加载可视化配置(FileConfiguration 配置) {
        ConfigurationSection 段 = 配置.getConfigurationSection(可视化段键);
        if (段 == null) {
            return;
        }
        this.GUI高亮持续tick值 = Math.max(1, 段.getInt(GUI高亮持续tick键, 默认GUI高亮持续tick));
        this.粒子启用值 = 段.getBoolean(粒子启用键, true);
        this.拍号显示启用值 = 段.getBoolean(拍号显示键, false);
        // 同步到节拍器（运行中会话不受影响，新会话生效）
        节拍器.设置拍号显示启用(this.拍号显示启用值);
    }

    private static int[] 读取整型数组(ConfigurationSection 段, String 键, int[] 默认值) {
        List<Integer> 列表 = 段.getIntegerList(键);
        if (列表.isEmpty()) {
            return 默认值.clone();
        }
        int[] 结果 = new int[列表.size()];
        for (int i = 0; i < 列表.size(); i++) {
            结果[i] = 列表.get(i);
        }
        return 结果;
    }

    @Override
    public void 演奏音符(Player 玩家, 乐器定义 乐器, int 音高) {
        演奏音符(玩家, 乐器, 音高, (float) 默认力度);
    }

    @Override
    public void 演奏音符(Player 玩家, 乐器定义 乐器, int 音高, float 力度) {
        if (玩家 == null || 乐器 == null) {
            return;
        }
        int 有效音高 = Math.max(0, Math.min(24, 音高));
        float pitch = 乐器.音高转Pitch(有效音高);
        float 音量 = Math.max(0.0f, Math.min(1.0f, 力度)) * 默认音量;
        玩家.playSound(玩家.getLocation(), 乐器.获取原版音色(), 音量, pitch);
        for (Player 附近玩家 : 玩家.getWorld().getPlayers()) {
            if (附近玩家.equals(玩家)) {
                continue;
            }
            if (附近玩家.getLocation().distanceSquared(玩家.getLocation()) <= 听觉范围 * 听觉范围) {
                附近玩家.playSound(玩家.getLocation(), 乐器.获取原版音色(), 音量, pitch);
            }
        }
        播放音高粒子(玩家, 有效音高);
        // 演奏音符 仅负责发声，录制由调用方通过 记录按键按下/松开 显式触发（GAP-03 修复）
    }

    private void 播放音高粒子(Player 玩家, int 有效音高) {
        // FP-10 粒子反馈：全局开关 + FP-G 可视化.粒子启用 双重控制
        if (!音符粒子效果 || !粒子启用值) {
            return;
        }
        Color 颜色 = 音高方块映射.音高转颜色(有效音高);
        玩家.getWorld().spawnParticle(
                Particle.DUST,
                玩家.getLocation().add(0, 1, 0),
                粒子数量,
                粒子偏移, 粒子偏移, 粒子偏移,
                0.0,
                new Particle.DustOptions(颜色, 粒子尺寸)
        );
    }

    @Override
    public void 打开演奏界面(Player 玩家, 乐器定义 乐器) {
        if (玩家 == null || 乐器 == null) {
            return;
        }
        设置玩家当前乐器(玩家.getUniqueId(), 乐器);
        演奏界面持有者 持有者 = new 演奏界面持有者(乐器);
        String 标题 = 翻译服务.获取(演奏标题键, 乐器.获取显示名称(), 乐器.获取默认音域());
        Inventory 界面 = Bukkit.createInventory(持有者, GUI大小, 迷你消息.deserialize(标题));
        填充演奏界面(界面, 乐器, 乐器.获取默认音域());
        演奏界面表.put(玩家.getUniqueId(), 界面);
        玩家.openInventory(界面);
    }

    /**
     * FP-03 6×9 钢琴布局填充演奏界面。
     * 第 0 行（槽 0-8）：9 功能位
     * 第 1-3 行（槽 9-35）：中部显示区（音域/力度切换 + 录制/停止/播放 + 延音模式切换 + 显示）
     * 第 4 行（槽 36-44）：5 黑键
     * 第 5 行（槽 45-53）：7 白键
     */
    private void 填充演奏界面(Inventory 界面, 乐器定义 乐器, int 八度) {
        填充功能位(界面);
        填充中部控制(界面, 乐器, 八度);
        填充钢琴键(界面, 八度);
    }

    private void 填充功能位(Inventory 界面) {
        for (Map.Entry<Integer, 功能位定义> 入口 : 功能位槽位表.entrySet()) {
            int 槽位 = 入口.getKey();
            if (槽位 < 0 || 槽位 >= GUI大小) {
                continue;
            }
            界面.setItem(槽位, 创建功能位按钮(入口.getValue()));
        }
    }

    private void 填充中部控制(Inventory 界面, 乐器定义 乐器, int 八度) {
        界面.setItem(槽位音域减, 创建控制按钮(Material.ARROW, 八度减键, 9901));
        界面.setItem(槽位音域显示, 创建音域显示按钮(乐器, 八度));
        界面.setItem(槽位音域加, 创建控制按钮(Material.ARROW, 八度加键, 9902));
        界面.setItem(槽位力度减, 创建控制按钮(Material.ARROW, 力度减键, 9903));
        界面.setItem(槽位力度显示, 创建力度显示按钮(乐器.是否力度敏感()));
        界面.setItem(槽位力度加, 创建控制按钮(Material.ARROW, 力度加键, 9904));
        界面.setItem(槽位录制, 创建控制按钮(Material.REDSTONE_TORCH, 录制按钮键, 9905));
        界面.setItem(槽位停止, 创建控制按钮(Material.STRUCTURE_VOID, 停止按钮键, 9906));
        界面.setItem(槽位播放, 创建控制按钮(Material.PAPER, 播放按钮键, 9907));
        界面.setItem(槽位延音模式切换, 创建延音模式切换按钮());
    }

    private void 填充钢琴键(Inventory 界面, int 八度) {
        int 黑键数 = Math.min(黑键槽位数组.length, 黑键音高偏移数组.length);
        for (int i = 0; i < 黑键数; i++) {
            int 槽位 = 黑键槽位数组[i];
            int 偏移 = 黑键音高偏移数组[i];
            int 音高 = 八度 * 12 + 偏移;
            if (音高 < 0 || 音高 > 24) {
                continue;
            }
            if (槽位 < 0 || 槽位 >= GUI大小) {
                continue;
            }
            界面.setItem(槽位, 创建音符按钮(音高));
        }
        int 白键数 = Math.min(白键槽位数组.length, 白键音高偏移数组.length);
        for (int i = 0; i < 白键数; i++) {
            int 槽位 = 白键槽位数组[i];
            int 偏移 = 白键音高偏移数组[i];
            int 音高 = 八度 * 12 + 偏移;
            if (音高 < 0 || 音高 > 24) {
                continue;
            }
            if (槽位 < 0 || 槽位 >= GUI大小) {
                continue;
            }
            界面.setItem(槽位, 创建音符按钮(音高));
        }
    }

    /**
     * FP-03 重新填充界面：玩家切换音域后刷新 GUI 内容。
     * 公开给监听器调用，避免监听器重复实现钢琴布局逻辑。
     */
    public void 重新填充界面(Inventory 界面, 乐器定义 乐器, int 八度) {
        填充演奏界面(界面, 乐器, 八度);
    }

    private ItemStack 创建音符按钮(int 音高) {
        Material 材质 = 音高方块映射.音高转方块(音高);
        ItemStack 按钮 = new ItemStack(材质);
        ItemMeta 元数据 = 按钮.getItemMeta();
        if (元数据 != null) {
            String 名称 = 翻译服务.获取(音符按钮键, 音高);
            元数据.customName(Component.text(名称));
            String 说明 = 翻译服务.获取(音符说明键, 音高, 音高 / 12, 音高 % 12);
            元数据.lore(List.of(Component.text(说明)));
            设置自定义模型数据(元数据, 音高);
            按钮.setItemMeta(元数据);
        }
        return 按钮;
    }

    private void 设置自定义模型数据(ItemMeta 元数据, int 数值) {
        CustomModelDataComponent 组件 = 元数据.getCustomModelDataComponent();
        组件.setFloats(List.of((float) 数值));
        元数据.setCustomModelDataComponent(组件);
    }

    private ItemStack 创建控制按钮(Material 材质, String 名称键, int 模型数据) {
        ItemStack 按钮 = new ItemStack(材质);
        ItemMeta 元数据 = 按钮.getItemMeta();
        if (元数据 != null) {
            String 名称 = 翻译服务.获取(名称键);
            元数据.customName(Component.text(名称));
            设置自定义模型数据(元数据, 模型数据);
            按钮.setItemMeta(元数据);
        }
        return 按钮;
    }

    /**
     * FP-03 创建功能位按钮（9 功能位：保存/清除/关闭/NBS导入/NBS导出/和弦预设/公共乐谱库/钢琴卷帘编辑/分层切换）。
     * 材质与模型数据从配置加载；名称走翻译键「演奏GUI.功能位.{标识}」。
     */
    private ItemStack 创建功能位按钮(功能位定义 位) {
        Material 材质 = 功能位材质表.getOrDefault(位, Material.PAPER);
        int 模型数据 = 功能位模型数据表.getOrDefault(位, 9990);
        ItemStack 按钮 = new ItemStack(材质);
        ItemMeta 元数据 = 按钮.getItemMeta();
        if (元数据 != null) {
            String 名称 = 翻译服务.获取(位.获取名称键());
            元数据.customName(Component.text(名称));
            设置自定义模型数据(元数据, 模型数据);
            按钮.setItemMeta(元数据);
        }
        return 按钮;
    }

    /**
     * FP-03 创建音域显示按钮（中部显示区，显示当前音域编号）。
     */
    private ItemStack 创建音域显示按钮(乐器定义 乐器, int 八度) {
        ItemStack 按钮 = new ItemStack(Material.PAPER);
        ItemMeta 元数据 = 按钮.getItemMeta();
        if (元数据 != null) {
            String 名称 = 翻译服务.获取(音域显示键, 八度);
            元数据.customName(Component.text(名称));
            设置自定义模型数据(元数据, 9910);
            按钮.setItemMeta(元数据);
        }
        return 按钮;
    }

    /**
     * FP-03 创建力度显示按钮（中部显示区，显示当前力度档标识）。
     * 非力度敏感乐器显示「—」占位。
     */
    private ItemStack 创建力度显示按钮(boolean 力度敏感) {
        Material 材质 = 力度敏感 ? Material.PAPER : Material.GRAY_STAINED_GLASS_PANE;
        ItemStack 按钮 = new ItemStack(材质);
        ItemMeta 元数据 = 按钮.getItemMeta();
        if (元数据 != null) {
            String 名称 = 翻译服务.获取(力度显示键, 力度敏感 ? "MF" : "—");
            元数据.customName(Component.text(名称));
            设置自定义模型数据(元数据, 9911);
            按钮.setItemMeta(元数据);
        }
        return 按钮;
    }

    /**
     * FP-04 创建延音模式切换按钮（中部显示区，显示当前延音模式）。
     */
    private ItemStack 创建延音模式切换按钮() {
        ItemStack 按钮 = new ItemStack(Material.COMPARATOR);
        ItemMeta 元数据 = 按钮.getItemMeta();
        if (元数据 != null) {
            String 模式键 = 默认延音模式值 == 延音模式.按住 ? 延音模式按住键 : 延音模式切换模式键;
            String 名称 = 翻译服务.获取(延音模式切换键, 翻译服务.获取(模式键));
            元数据.customName(Component.text(名称));
            设置自定义模型数据(元数据, 9912);
            按钮.setItemMeta(元数据);
        }
        return 按钮;
    }

    /**
     * FP-03 发送"功能未实现"提示（后 6 个功能位点击时调用）。
     */
    public void 发送功能未实现提示(Player 玩家) {
        发送消息(玩家, 功能未实现键);
    }

    @Override
    public void 切换乐器(Player 玩家, 乐器定义 新乐器) {
        if (玩家 == null || 新乐器 == null) {
            return;
        }
        设置玩家当前乐器(玩家.getUniqueId(), 新乐器);
        发送消息(玩家, 切换乐器键, 新乐器.获取显示名称());
        打开演奏界面(玩家, 新乐器);
    }

    @Override
    public void 开始录制(UUID 玩家标识, 乐器定义 乐器) {
        if (玩家标识 == null || 乐器 == null) {
            return;
        }
        停止录制(玩家标识);
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 == null) {
            return;
        }
        if (是否节拍器开启(玩家标识)) {
            节拍器.启动带CountIn(玩家, 节拍器.获取默认BPM(),
                    节拍器.获取默认拍号分子(), 节拍器.获取默认拍号分母(),
                    () -> 启动录制任务(玩家标识, 玩家, 乐器));
        } else {
            启动录制任务(玩家标识, 玩家, 乐器);
        }
    }

    private void 启动录制任务(UUID 玩家标识, Player 玩家, 乐器定义 乐器) {
        录制状态 状态 = new 录制状态(System.currentTimeMillis());
        录制状态表.put(玩家标识, 状态);
        发送消息(玩家, 录制开始键, 乐器.获取显示名称());
        // FP-10 录制指示器：录制开始时显示"录制中"标题
        显示录制中标题(玩家);
    }

    @Override
    public void 停止录制(UUID 玩家标识) {
        录制状态 状态 = 录制状态表.remove(玩家标识);
        if (状态 == null) {
            return;
        }
        节拍器.停止(玩家标识);
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            发送消息(玩家, 录制停止键, 状态.音符序列.size());
        }
    }

    @Override
    public void 添加录制音符(UUID 玩家标识, int 音高, int 时值Tick, double 力度) {
        录制状态 状态 = 录制状态表.get(玩家标识);
        if (状态 == null) {
            return;
        }
        long 已录制秒 = (System.currentTimeMillis() - 状态.开始时间戳) / 毫秒每秒;
        if (已录制秒 >= 最大录制时长秒) {
            停止录制(玩家标识);
            return;
        }
        状态.音符序列.add(音符.of(音高, 时值Tick, 力度));
    }

    @Override
    public void 记录按键按下(UUID 玩家标识, int 音高, double 力度) {
        if (玩家标识 == null) {
            return;
        }
        录制状态 状态 = 录制状态表.get(玩家标识);
        if (状态 == null) {
            return;
        }
        int 有效音高 = Math.max(0, Math.min(24, 音高));
        状态.按下时间戳.put(有效音高, System.currentTimeMillis());
        状态.按下力度.put(有效音高, 力度);
    }

    @Override
    public void 记录按键松开(UUID 玩家标识, int 音高) {
        if (玩家标识 == null) {
            return;
        }
        录制状态 状态 = 录制状态表.get(玩家标识);
        if (状态 == null) {
            return;
        }
        int 有效音高 = Math.max(0, Math.min(24, 音高));
        Long 按下时刻 = 状态.按下时间戳.remove(有效音高);
        if (按下时刻 == null) {
            return;
        }
        Double 力度 = 状态.按下力度.remove(有效音高);
        if (力度 == null) {
            力度 = 默认力度;
        }
        long 差值毫秒 = System.currentTimeMillis() - 按下时刻;
        // GAP-03: 按下→松开的真实时长转 tick；瞬时点击（GUI）差值≈0，回退到 默认时值Tick
        int 时值Tick = (int) Math.max(默认时值Tick, 差值毫秒 / 毫秒每Tick);
        添加录制音符(玩家标识, 有效音高, 时值Tick, 力度);
    }

    @Override
    public boolean 是否录制中(UUID 玩家标识) {
        return 录制状态表.containsKey(玩家标识);
    }

    @Override
    public 乐谱保存结果 保存乐谱带结果(UUID 玩家标识, String 乐谱名) {
        if (玩家标识 == null || 乐谱名 == null || 乐谱名.isBlank()) {
            if (玩家标识 != null) {
                Player 玩家 = Bukkit.getPlayer(玩家标识);
                if (玩家 != null) {
                    发送消息(玩家, 乐谱名无效键);
                }
            }
            return 乐谱保存结果.参数无效;
        }
        录制状态 状态 = 录制状态表.get(玩家标识);
        if (状态 == null || 状态.音符序列.isEmpty()) {
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 != null) {
                发送消息(玩家, 无录制内容键);
            }
            return 乐谱保存结果.无录制;
        }
        List<String> 现有乐谱 = 获取乐谱列表(玩家标识);
        if (现有乐谱.contains(乐谱名)) {
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 != null) {
                发送消息(玩家, 乐谱已存在键, 乐谱名);
            }
            return 乐谱保存结果.重名;
        }
        if (现有乐谱.size() >= 最大乐谱存储数) {
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 != null) {
                发送消息(玩家, 乐谱超限键, 最大乐谱存储数);
            }
            return 乐谱保存结果.超限;
        }
        执行保存(玩家标识, 乐谱名, 状态);
        return 乐谱保存结果.成功;
    }

    @Override
    public boolean 保存乐谱并覆盖(UUID 玩家标识, String 乐谱名) {
        if (玩家标识 == null || 乐谱名 == null || 乐谱名.isBlank()) {
            return false;
        }
        录制状态 状态 = 录制状态表.get(玩家标识);
        if (状态 == null || 状态.音符序列.isEmpty()) {
            return false;
        }
        List<String> 现有乐谱 = 获取乐谱列表(玩家标识);
        if (!现有乐谱.contains(乐谱名) && 现有乐谱.size() >= 最大乐谱存储数) {
            return false;
        }
        执行保存(玩家标识, 乐谱名, 状态);
        return true;
    }

    private void 执行保存(UUID 玩家标识, String 乐谱名, 录制状态 状态) {
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        String 作者名 = 玩家 != null ? 玩家.getName() : 玩家标识.toString();
        乐谱 新乐谱 = 乐谱.创建(乐谱名, 玩家标识, 作者名, 默认速度BPM, 状态.音符序列);
        // FP-09: 录音默认开启量化，保存时应用量化（非破坏性，原始音符列表保存到 乐谱.原始音符列表）
        if (是否玩家量化开启(玩家标识)) {
            量化档位 档位 = 玩家量化档位表.getOrDefault(玩家标识, 默认量化档位值);
            double swing = 获取Swing(玩家标识);
            List<音符> 量化后 = 量化音符列表(新乐谱.获取音符列表(), 档位, 默认速度BPM, swing);
            新乐谱.应用量化(量化后);
        }
        保存乐谱到文件(新乐谱);
        玩家量化乐谱表.put(玩家标识, 新乐谱);
        录制状态表.remove(玩家标识);
        if (玩家 != null) {
            发送消息(玩家, 乐谱保存成功键, 乐谱名);
        }
    }

    private void 保存乐谱到文件(乐谱 乐谱) {
        File 文件 = 获取乐谱文件(乐谱.获取作者标识(), 乐谱.获取名称());
        创建父目录(文件);
        YamlConfiguration 配置 = new YamlConfiguration();
        配置.set(名称键, 乐谱.获取名称());
        配置.set(作者键, 乐谱.获取作者标识().toString());
        配置.set(作者名键, 乐谱.获取作者名());
        配置.set(创建时间键, 乐谱.获取创建时间戳());
        配置.set(速度键, 乐谱.获取速度BPM());
        List<Map<String, Object>> 音符数据列表 = new ArrayList<>();
        for (音符 音符 : 乐谱.获取音符列表()) {
            Map<String, Object> 音符数据 = new HashMap<>();
            音符数据.put(音高键, 音符.获取音高());
            音符数据.put(时值键, 音符.获取时值Tick());
            音符数据.put(力度键, 音符.获取力度());
            音符数据列表.add(音符数据);
        }
        配置.set(音符列表键, 音符数据列表);
        try {
            配置.save(文件);
        } catch (IOException e) {
            插件.getLogger().warning("保存乐谱失败: " + e.getMessage());
        }
    }

    @Override
    public Optional<乐谱> 加载乐谱(UUID 玩家标识, String 乐谱名) {
        if (玩家标识 == null || 乐谱名 == null || 乐谱名.isBlank()) {
            return Optional.empty();
        }
        File 文件 = 获取乐谱文件(玩家标识, 乐谱名);
        if (!文件.exists()) {
            return Optional.empty();
        }
        YamlConfiguration 配置 = YamlConfiguration.loadConfiguration(文件);
        String 名称 = 配置.getString(名称键, 乐谱名);
        UUID 作者标识 = UUID.fromString(配置.getString(作者键, 玩家标识.toString()));
        String 作者名 = 配置.getString(作者名键, "");
        long 创建时间 = 配置.getLong(创建时间键, System.currentTimeMillis());
        int 速度 = 配置.getInt(速度键, 默认速度BPM);
        List<Map<?, ?>> 音符数据列表 = 配置.getMapList(音符列表键);
        List<音符> 音符列表 = new ArrayList<>();
        for (Map<?, ?> 音符数据 : 音符数据列表) {
            Object 音高值 = 音符数据.get(音高键);
            Object 时值值 = 音符数据.get(时值键);
            Object 力度值 = 音符数据.get(力度键);
            if (音高值 == null || 时值值 == null || 力度值 == null) {
                调试日志器.调试("乐器服务", "跳过损坏音符条目：音高=%s, 时值=%s, 力度=%s", 音高值, 时值值, 力度值);
                continue;
            }
            int 音高 = (int) 音高值;
            int 时值 = (int) 时值值;
            double 力度 = ((Number) 力度值).doubleValue();
            音符列表.add(音符.of(音高, 时值, 力度));
        }
        return Optional.of(new 乐谱(名称, 作者标识, 作者名, 创建时间, 速度, 音符列表));
    }

    @Override
    public boolean 删除乐谱(UUID 玩家标识, String 乐谱名) {
        if (玩家标识 == null || 乐谱名 == null || 乐谱名.isBlank()) {
            return false;
        }
        File 文件 = 获取乐谱文件(玩家标识, 乐谱名);
        boolean 已删除 = 文件.delete();
        if (已删除) {
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 != null) {
                发送消息(玩家, 乐谱删除成功键, 乐谱名);
            }
        }
        return 已删除;
    }

    @Override
    public List<String> 获取乐谱列表(UUID 玩家标识) {
        if (玩家标识 == null) {
            return Collections.emptyList();
        }
        File 目录 = 获取乐谱目录(玩家标识);
        if (!目录.exists()) {
            return Collections.emptyList();
        }
        File[] 文件列表 = 目录.listFiles((dir, name) -> name.endsWith(".yml"));
        if (文件列表 == null || 文件列表.length == 0) {
            return Collections.emptyList();
        }
        List<String> 乐谱名列表 = new ArrayList<>();
        for (File 文件 : 文件列表) {
            String 文件名 = 文件.getName();
            乐谱名列表.add(文件名.substring(0, 文件名.length() - 4));
        }
        Collections.sort(乐谱名列表);
        return 乐谱名列表;
    }

    @Override
    public void 播放乐谱(Player 玩家, 乐谱 乐谱) {
        if (玩家 == null || 乐谱 == null || 乐谱.获取音符数量() == 0) {
            return;
        }
        停止播放(玩家.getUniqueId());
        乐器定义 临时乐器 = 获取玩家当前乐器(玩家.getUniqueId()).orElse(null);
        if (临时乐器 == null) {
            临时乐器 = 乐器注册表.默认乐器().orElse(null);
        }
        if (临时乐器 == null) {
            return;
        }
        // FP-09: 缓存当前播放的乐谱，供后续 量化乐谱/撤销量化 操作
        玩家量化乐谱表.put(玩家.getUniqueId(), 乐谱);
        final 乐器定义 乐器 = 临时乐器;
        发送消息(玩家, 乐谱开始播放键, 乐谱.获取名称());
        if (是否节拍器开启(玩家.getUniqueId())) {
            节拍器.启动带CountIn(玩家, 乐谱.获取速度BPM(),
                    节拍器.获取默认拍号分子(), 节拍器.获取默认拍号分母(),
                    () -> 启动播放任务(玩家, 乐器, 乐谱));
        } else {
            启动播放任务(玩家, 乐器, 乐谱);
        }
    }

    private void 启动播放任务(Player 玩家, 乐器定义 乐器, 乐谱 乐谱) {
        List<音符> 音符列表 = 乐谱.获取音符列表();
        final int 总数 = 音符列表.size();
        if (总数 == 0) {
            return;
        }
        // GAP-02: 按每个音符自身 时值Tick 安排下一音符播放时机（长短音区分），
        // 不再用固定 BPM 间隔。每 tick 检查一次，等待累计达到当前音符时值后再播下一音符。
        BukkitRunnable 任务 = new BukkitRunnable() {
            private int 索引 = 0;
            private int 等待剩余 = 0;

            @Override
            public void run() {
                if (!玩家.isOnline()) {
                    播放任务表.remove(玩家.getUniqueId());
                    节拍器.停止(玩家.getUniqueId());
                    cancel();
                    return;
                }
                if (等待剩余 > 0) {
                    等待剩余--;
                    return;
                }
                if (索引 >= 总数) {
                    发送消息(玩家, 乐谱播放结束键, 乐谱.获取名称());
                    播放任务表.remove(玩家.getUniqueId());
                    节拍器.停止(玩家.getUniqueId());
                    cancel();
                    return;
                }
                音符 当前音符 = 音符列表.get(索引);
                演奏音符(玩家, 乐器, 当前音符.获取音高(), (float) 当前音符.获取力度());
                // 当前音符播放后等待 时值Tick-1 个 tick（本 tick 已用 1 个）
                等待剩余 = Math.max(0, 当前音符.获取时值Tick() - 1);
                索引++;
            }
        };
        播放任务表.put(玩家.getUniqueId(), 任务);
        任务.runTaskTimer(插件, 0L, 每Tick);
    }

    @Override
    public void 停止播放(UUID 玩家标识) {
        BukkitRunnable 任务 = 播放任务表.remove(玩家标识);
        if (任务 != null) {
            任务.cancel();
        }
        节拍器.停止(玩家标识);
    }

    @Override
    public boolean 是否播放中(UUID 玩家标识) {
        return 播放任务表.containsKey(玩家标识);
    }

    @Override
    public boolean 开始合奏(UUID 队长标识, String 乐谱名) {
        if (队长标识 == null || 乐谱名 == null || 乐谱名.isBlank()) {
            return false;
        }
        Optional<mljy.领域层.组队.队伍> 队伍可选 = 组队服务.获取玩家队伍(队长标识);
        if (队伍可选.isEmpty()) {
            return false;
        }
        mljy.领域层.组队.队伍 队伍 = 队伍可选.get();
        if (!队伍.获取队长标识().equals(队长标识)) {
            return false;
        }
        Optional<乐谱> 乐谱可选 = 加载乐谱(队长标识, 乐谱名);
        if (乐谱可选.isEmpty()) {
            return false;
        }
        Set<UUID> 成员集合 = 队伍.获取成员标识集合();
        int 在线人数 = 0;
        for (UUID 成员 : 成员集合) {
            Player 玩家 = Bukkit.getPlayer(成员);
            if (玩家 != null && 玩家.isOnline()) {
                在线人数++;
            }
        }
        if (在线人数 < 合奏最小人数) {
            return false;
        }
        合奏状态内部 状态 = new 合奏状态内部(队长标识, 乐谱可选.get(), 队伍.获取成员标识集合());
        合奏状态表.put(队长标识, 状态);
        for (UUID 成员 : 成员集合) {
            合奏状态表.put(成员, 状态);
        }
        Player 队长 = Bukkit.getPlayer(队长标识);
        if (队长 != null) {
            发送消息(队长, 合奏倒计时键, 乐谱名);
        }
        return true;
    }

    @Override
    public boolean 确认合奏(UUID 玩家标识) {
        合奏状态内部 状态 = 合奏状态表.get(玩家标识);
        if (状态 == null || 状态.已开始) {
            return false;
        }
        状态.已确认集合.add(玩家标识);
        int 已确认数 = 状态.已确认集合.size();
        int 总人数 = 状态.参与成员.size();
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            发送消息(玩家, 合奏确认进度键, 已确认数, 总人数);
        }
        if (已确认数 >= 总人数) {
            开始合奏播放(状态);
        }
        return true;
    }

    @Override
    public boolean 拒绝合奏(UUID 玩家标识) {
        合奏状态内部 状态 = 合奏状态表.get(玩家标识);
        if (状态 == null) {
            return false;
        }
        通知合奏成员(状态, 合奏拒绝键, 玩家标识);
        清理合奏状态(状态);
        return true;
    }

    @Override
    public boolean 停止合奏(UUID 队长标识) {
        合奏状态内部 状态 = 合奏状态表.get(队长标识);
        if (状态 == null || !状态.队长标识.equals(队长标识)) {
            return false;
        }
        通知合奏成员(状态, 合奏已停止键);
        for (UUID 成员 : 状态.参与成员) {
            停止播放(成员);
        }
        清理合奏状态(状态);
        return true;
    }

    @Override
    public boolean 是否合奏中(UUID 玩家标识) {
        合奏状态内部 状态 = 合奏状态表.get(玩家标识);
        return 状态 != null && 状态.已开始;
    }

    @Override
    public Optional<合奏状态> 获取合奏状态(UUID 玩家标识) {
        合奏状态内部 状态 = 合奏状态表.get(玩家标识);
        if (状态 == null) {
            return Optional.empty();
        }
        return Optional.of(new 合奏状态(
                状态.乐谱.获取名称(),
                状态.乐谱.获取速度BPM(),
                状态.已开始,
                状态.已确认集合.size(),
                状态.参与成员.size()
        ));
    }

    @Override
    public String 获取默认乐谱名() {
        return 翻译服务.获取(默认乐谱名键);
    }

    private void 开始合奏播放(合奏状态内部 状态) {
        状态.已开始 = true;
        通知合奏成员(状态, 合奏开始键, 状态.乐谱.获取名称());
        int bpm = 状态.乐谱.获取速度BPM();
        int 拍号分子 = 节拍器.获取默认拍号分子();
        int 拍号分母 = 节拍器.获取默认拍号分母();
        final 乐谱 合奏乐谱 = 状态.乐谱;
        final Set<UUID> 成员集合 = 状态.参与成员;
        节拍器.启动合奏CountIn(成员集合, bpm, 拍号分子, 拍号分母, () -> {
            for (UUID 成员 : 成员集合) {
                Player 玩家 = Bukkit.getPlayer(成员);
                if (玩家 == null || !玩家.isOnline()) {
                    continue;
                }
                乐器定义 乐器 = 获取玩家当前乐器(成员).orElse(null);
                if (乐器 == null) {
                    乐器 = 乐器注册表.默认乐器().orElse(null);
                }
                if (乐器 == null) {
                    continue;
                }
                启动播放任务(玩家, 乐器, 合奏乐谱);
            }
        });
    }

    private void 清理合奏状态(合奏状态内部 状态) {
        for (UUID 成员 : 状态.参与成员) {
            合奏状态表.remove(成员);
        }
    }

    private void 通知合奏成员(合奏状态内部 状态, String 消息键, Object... 参数) {
        for (UUID 成员 : 状态.参与成员) {
            Player 玩家 = Bukkit.getPlayer(成员);
            if (玩家 != null && 玩家.isOnline()) {
                发送消息(玩家, 消息键, 参数);
            }
        }
    }

    @Override
    public ItemStack 创建乐器物品(乐器定义 乐器) {
        if (乐器 == null) {
            return new ItemStack(Material.AIR);
        }
        ItemStack 物品 = new ItemStack(乐器.获取物品材质());
        ItemMeta 元数据 = 物品.getItemMeta();
        if (元数据 != null) {
            String 前缀 = 翻译服务.获取(物品前缀键);
            元数据.customName(Component.text(前缀 + 乐器.获取显示名称()));
            List<String> 说明 = new ArrayList<>();
            String 音色标签 = 翻译服务.获取(物品音色键);
            说明.add(音色标签 + 乐器.获取显示名称());
            说明.add(乐器.获取描述());
            元数据.lore(说明.stream().map(Component::text).toList());
            设置自定义模型数据(元数据, 乐器.获取自定义模型数据());
            物品.setItemMeta(元数据);
        }
        return 物品;
    }

    @Override
    public void 给予乐器(Player 玩家, 乐器定义 乐器) {
        if (玩家 == null || 乐器 == null) {
            return;
        }
        ItemStack 物品 = 创建乐器物品(乐器);
        玩家.getInventory().addItem(物品);
        发送消息(玩家, 给予成功键, 玩家.getName(), 乐器.获取显示名称());
    }

    @Override
    public Optional<乐器定义> 获取玩家当前乐器(UUID 玩家标识) {
        return Optional.ofNullable(玩家当前乐器.get(玩家标识));
    }

    @Override
    public void 设置玩家当前乐器(UUID 玩家标识, 乐器定义 乐器) {
        if (乐器 == null) {
            玩家当前乐器.remove(玩家标识);
        } else {
            玩家当前乐器.put(玩家标识, 乐器);
        }
    }

    @Override
    public void 玩家退出清理(UUID 玩家标识) {
        停止录制(玩家标识);
        停止播放(玩家标识);
        停止延音(玩家标识);
        停止和弦记忆(玩家标识);
        节拍器.停止(玩家标识);
        玩家节拍器开关表.remove(玩家标识);
        演奏界面表.remove(玩家标识);
        玩家音域表.remove(玩家标识);
        玩家力度档表.remove(玩家标识);
        玩家真实力度表.remove(玩家标识);
        玩家延音模式表.remove(玩家标识);
        // FP-G 玩家量化/swing 状态清理
        玩家量化乐谱表.remove(玩家标识);
        玩家Swing表.remove(玩家标识);
        玩家量化开关表.remove(玩家标识);
        玩家量化档位表.remove(玩家标识);
        // FP-J 玩家分层/piano roll/弯音调制/表情控制状态清理
        玩家分层配置表.remove(玩家标识);
        玩家PianoRoll编辑器表.remove(玩家标识);
        玩家弯音调制状态表.remove(玩家标识);
        玩家弯音范围表.remove(玩家标识);
        玩家表情控制状态表.remove(玩家标识);
        资源包管理器.玩家退出清理(玩家标识);
        合奏状态内部 状态 = 合奏状态表.get(玩家标识);
        if (状态 != null) {
            通知合奏成员(状态, 合奏结束键);
            清理合奏状态(状态);
        }
    }

    @Override
    public void 重载配置() {
        插件.reloadConfig();
        File 配置文件 = new File(插件.getDataFolder(), 乐器配置文件名);
        if (配置文件.exists()) {
            FileConfiguration 乐器配置 = YamlConfiguration.loadConfiguration(配置文件);
            乐器注册表.从配置加载(乐器配置);
            节拍器.从配置加载(乐器配置);
            公共乐谱服务.从配置加载(乐器配置);
        }
        刷新全局参数();
        加载乐器配置参数();
        加载FP_F配置();
        加载FP_G配置();
        加载FP_I配置();
        加载FP_J配置();
    }

    @Override
    public void 设置节拍器开关(UUID 玩家标识, boolean 开启) {
        if (玩家标识 == null) {
            return;
        }
        if (开启) {
            玩家节拍器开关表.put(玩家标识, Boolean.TRUE);
        } else {
            玩家节拍器开关表.remove(玩家标识);
            节拍器.停止(玩家标识);
        }
    }

    @Override
    public boolean 是否节拍器开启(UUID 玩家标识) {
        return 玩家标识 != null && 玩家节拍器开关表.containsKey(玩家标识);
    }

    @Override
    public void 设置CountIn小节数(int 小节数) {
        节拍器.设置CountIn小节数(小节数);
    }

    @Override
    public int 获取CountIn小节数() {
        return 节拍器.获取CountIn小节数();
    }

    // ===== FP-E 三轨输入归一 + 逐音域绑定 + 5 档力度 + mod velocity-by-duration =====

    @Override
    public void 演奏指令(Player 玩家, 乐器定义 乐器, int 音高, 力度档 力度档, int 时值Tick) {
        if (玩家 == null || 乐器 == null || 力度档 == null) {
            return;
        }
        int clamp音高 = Math.max(音高下限, Math.min(音高上限, 音高));
        // FP-19 分层键盘：根据分层配置选择乐器（分层启用时按音高分界点选左右乐器）
        UUID 玩家标识 = 玩家.getUniqueId();
        分层配置 分层 = 玩家分层配置表.getOrDefault(玩家标识, 分层配置.默认配置());
        乐器定义 实际乐器 = 分层.选择乐器(clamp音高, 乐器);
        // 服务端权威计算：pitch = 乐器.音高转Pitch + 力度档.获取pitch微调
        float pitch = 实际乐器.音高转Pitch(clamp音高) + 力度档.获取pitch微调();
        // FP-21 弯音轮 + 调制轮：应用 pitch 倍率（mod 独享，对所有玩家无副作用）
        弯音调制状态 弯音调制 = 玩家弯音调制状态表.get(玩家标识);
        if (弯音调制 != null) {
            int 弯音范围 = 玩家弯音范围表.getOrDefault(玩家标识, 默认弯音范围半音值);
            pitch *= 弯音调制.计算弯音Pitch倍率(弯音范围);
            pitch *= 弯音调制.计算调制Pitch倍率(System.currentTimeMillis(), 默认调制频率Hz值);
        }
        // 服务端权威计算：volume = 力度档.获取音量 × 默认音量
        float 音量 = 力度档.获取音量() * 默认音量;
        // FP-22 表情控制：应用 CC11 expression volume 倍率 + CC2 breath 附加（mod 独享）
        表情控制状态 表情 = 玩家表情控制状态表.get(玩家标识);
        if (表情 != null) {
            音量 *= 表情.计算表情Volume倍率();
            音量 += 表情.计算气声Volume附加();
            音量 = Math.max(0.0f, Math.min(1.0f, 音量));
        }
        玩家.playSound(玩家.getLocation(), 实际乐器.获取原版音色(), 音量, pitch);
        for (Player 附近玩家 : 玩家.getWorld().getPlayers()) {
            if (附近玩家.equals(玩家)) {
                continue;
            }
            if (附近玩家.getLocation().distanceSquared(玩家.getLocation()) <= 听觉范围 * 听觉范围) {
                附近玩家.playSound(玩家.getLocation(), 实际乐器.获取原版音色(), 音量, pitch);
            }
        }
        // 粒子颜色映射仅支持 0-24（音高方块映射限制），需进一步 clamp 到乐器音域范围
        int 粒子音高 = Math.max(实际乐器.获取音域下限(), Math.min(实际乐器.获取音域上限(), clamp音高));
        播放音高粒子(玩家, 粒子音高);
        // 录制：调用方通过 记录按键按下/松开 显式触发（GAP-03 修复保留）。
        // 演奏指令仅负责发声，录制由调用方在按下/松开时调用 记录按键按下/松开。
    }

    @Override
    public void 处理mod输入(UUID 玩家标识, 乐器定义 乐器, int 音高, int velocity,
                              long 按下时间戳, long 松开时间戳) {
        // FP-01 mod 输入预留接口：本轮不实现 mod 端代码，仅服务端预留入口。
        // mod 端通过 PluginMessage 通道 "xrm:music_input" 发送按键事件包到服务端，
        // 服务端解析后调用本方法。当前实现：转 5 档力度或 velocity-by-duration 后走统一演奏指令。
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 == null || 乐器 == null) {
            return;
        }
        int clamp音高 = Math.max(音高下限, Math.min(音高上限, 音高));
        int 时值Tick = 默认时值Tick;
        if (按下时间戳 > 0 && 松开时间戳 > 0 && 松开时间戳 > 按下时间戳) {
            时值Tick = (int) Math.max(默认时值Tick, (松开时间戳 - 按下时间戳) / 毫秒每Tick);
        }
        力度档 档 = 获取玩家力度档(玩家标识);
        if (是否玩家真实力度开启(玩家标识) && velocity >= 0) {
            // velocity-by-duration：mod 玩家启用真实力度，按 velocity 转 5 档力度档（参数近似方案）
            档 = velocity转力度档(velocity);
        }
        演奏指令(玩家, 乐器, clamp音高, 档, 时值Tick);
    }

    /**
     * FP-05.2 velocity 转 5 档力度档（参数近似方案的离散映射）。
     * velocity 范围 [0,25]→PP，[26,50]→P，[51,90]→MF，[91,110]→F，[111,127]→FF。
     */
    private 力度档 velocity转力度档(int velocity) {
        int v = Math.max(velocity下限, Math.min(velocity上限, velocity));
        if (v <= 25) return 力度档.PP;
        if (v <= 50) return 力度档.P;
        if (v <= 90) return 力度档.MF;
        if (v <= 110) return 力度档.F;
        return 力度档.FF;
    }

    @Override
    public int 获取玩家音域(UUID 玩家标识) {
        if (玩家标识 == null) {
            return 0;
        }
        return 玩家音域表.getOrDefault(玩家标识, 0);
    }

    @Override
    public int 切换玩家音域(UUID 玩家标识, boolean 增加) {
        if (玩家标识 == null) {
            return 0;
        }
        int 当前 = 获取玩家音域(玩家标识);
        int 新音域 = 键位绑定.切换音域(当前, 增加);
        玩家音域表.put(玩家标识, 新音域);
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            发送消息(玩家, 音域切换键, 新音域);
        }
        return 新音域;
    }

    @Override
    public void 设置玩家音域(UUID 玩家标识, int 音域) {
        if (玩家标识 == null) {
            return;
        }
        玩家音域表.put(玩家标识, 键位绑定.规范化音域(音域));
    }

    @Override
    public 力度档 获取玩家力度档(UUID 玩家标识) {
        if (玩家标识 == null) {
            return 力度档.默认档();
        }
        return 玩家力度档表.getOrDefault(玩家标识, 力度档.默认档());
    }

    @Override
    public 力度档 切换玩家力度档(UUID 玩家标识, boolean 增加) {
        if (玩家标识 == null) {
            return 力度档.默认档();
        }
        力度档 当前 = 获取玩家力度档(玩家标识);
        力度档 新档 = 增加 ? 当前.下一档() : 当前.上一档();
        玩家力度档表.put(玩家标识, 新档);
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            发送消息(玩家, 力度切换键, 新档.获取标识());
        }
        return 新档;
    }

    @Override
    public void 设置玩家力度档(UUID 玩家标识, 力度档 力度档) {
        if (玩家标识 == null || 力度档 == null) {
            return;
        }
        玩家力度档表.put(玩家标识, 力度档);
    }

    @Override
    public void 设置玩家真实力度开关(UUID 玩家标识, boolean 启用) {
        if (玩家标识 == null) {
            return;
        }
        if (启用) {
            玩家真实力度表.put(玩家标识, Boolean.TRUE);
        } else {
            玩家真实力度表.remove(玩家标识);
        }
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            发送消息(玩家, 启用 ? 真实力度已开启键 : 真实力度已关闭键);
        }
    }

    @Override
    public boolean 是否玩家真实力度开启(UUID 玩家标识) {
        return 玩家标识 != null && 玩家真实力度表.containsKey(玩家标识);
    }

    @Override
    public int 计算velocityByDuration(long 按下时间戳, long 松开时间戳) {
        if (松开时间戳 <= 按下时间戳) {
            return velocity下限;
        }
        long 差值 = 松开时间戳 - 按下时间戳;
        if (差值 >= mod最大有效时长ms) {
            return velocity上限;
        }
        // velocity = clamp(round(差值 / 最大有效时长 × 127), 0, 127)
        int velocity = (int) Math.round((double) 差值 / mod最大有效时长ms * velocity上限);
        return Math.max(velocity下限, Math.min(velocity上限, velocity));
    }

    @Override
    public 键位绑定表 获取键位绑定表() {
        return 键位绑定;
    }

    @Override
    public boolean 处理聊天按键(Player 玩家, String 键) {
        if (玩家 == null || 键 == null || 键.isBlank()) {
            return false;
        }
        String 小写键 = 键.trim().toLowerCase();
        if (小写键.length() != 1) {
            return false;
        }
        char 字符 = 小写键.charAt(0);
        // FP-06 Z/X 切换音域
        if (字符 == 'z') {
            切换玩家音域(玩家.getUniqueId(), false);
            return true;
        }
        if (字符 == 'x') {
            切换玩家音域(玩家.getUniqueId(), true);
            return true;
        }
        // FP-05 C/V 切换力度档
        if (字符 == 'c') {
            切换玩家力度档(玩家.getUniqueId(), false);
            return true;
        }
        if (字符 == 'v') {
            切换玩家力度档(玩家.getUniqueId(), true);
            return true;
        }
        // FP-06 Apple Musical Typing 白键/黑键 → 当前音域对应音高
        乐器定义 乐器 = 获取玩家当前乐器(玩家.getUniqueId()).orElse(null);
        if (乐器 == null) {
            乐器 = 乐器注册表.默认乐器().orElse(null);
        }
        if (乐器 == null) {
            return false;
        }
        int 当前音域 = 获取玩家音域(玩家.getUniqueId());
        java.util.OptionalInt 音高可选 = 键位绑定.查询音高(当前音域, 小写键);
        if (音高可选.isEmpty()) {
            return false;
        }
        int 音高 = 音高可选.getAsInt();
        if (音高 < 0 || 音高 > 音高上限) {
            发送消息(玩家, 音高越界键, 音高);
            return true;
        }
        力度档 档 = 获取玩家力度档(玩家.getUniqueId());
        演奏指令(玩家, 乐器, 音高, 档, 默认时值Tick);
        // 录制：聊天命令模式为瞬时事件，按下后立即松开
        if (是否录制中(玩家.getUniqueId())) {
            记录按键按下(玩家.getUniqueId(), 音高, 档.获取音量());
            记录按键松开(玩家.getUniqueId(), 音高);
        }
        return true;
    }

    public Optional<Inventory> 获取演奏界面(UUID 玩家标识) {
        return Optional.ofNullable(演奏界面表.get(玩家标识));
    }

    public void 移除演奏界面(UUID 玩家标识) {
        演奏界面表.remove(玩家标识);
    }

    public int 获取最大乐谱存储数() {
        return 最大乐谱存储数;
    }

    public int 获取最大录制时长秒() {
        return 最大录制时长秒;
    }

    public double 获取听觉范围() {
        return 听觉范围;
    }

    public boolean 是否音符粒子效果() {
        return 音符粒子效果;
    }

    // ===== FP-G 量化 + swing + 可视化反馈 公共访问器 =====

    public 量化档位 获取默认量化档位() {
        return 默认量化档位值;
    }

    public boolean 是否默认量化开启() {
        return 量化默认开启值;
    }

    public double 获取默认Swing比例() {
        return 默认swing比例值;
    }

    public boolean 是否默认Swing开启() {
        return swing默认开启值;
    }

    public int 获取GUI高亮持续tick() {
        return GUI高亮持续tick值;
    }

    public boolean 是否粒子启用() {
        return 粒子启用值;
    }

    public boolean 是否拍号显示启用() {
        return 拍号显示启用值;
    }

    /**
     * FP-09 查询玩家量化开关（未设置时回退到默认值）。
     */
    public boolean 是否玩家量化开启(UUID 玩家标识) {
        if (玩家标识 == null) {
            return 量化默认开启值;
        }
        return 玩家量化开关表.getOrDefault(玩家标识, 量化默认开启值);
    }

    /**
     * FP-09 设置玩家量化开关（运行时覆盖配置）。
     */
    public void 设置玩家量化开关(UUID 玩家标识, boolean 启用) {
        if (玩家标识 == null) {
            return;
        }
        玩家量化开关表.put(玩家标识, 启用);
    }

    /**
     * FP-09 设置玩家量化档位（运行时覆盖配置）。
     */
    public void 设置玩家量化档位(UUID 玩家标识, 量化档位 档位) {
        if (玩家标识 == null || 档位 == null) {
            return;
        }
        玩家量化档位表.put(玩家标识, 档位);
    }

    /**
     * FP-09 获取玩家当前量化档位（未设置时回退到默认值）。
     */
    public 量化档位 获取玩家量化档位(UUID 玩家标识) {
        if (玩家标识 == null) {
            return 默认量化档位值;
        }
        return 玩家量化档位表.getOrDefault(玩家标识, 默认量化档位值);
    }

    private File 获取乐谱目录(UUID 玩家标识) {
        return new File(插件.getDataFolder(), 乐谱目录名 + File.separator + 玩家标识.toString());
    }

    private File 获取乐谱文件(UUID 玩家标识, String 乐谱名) {
        return new File(获取乐谱目录(玩家标识), 乐谱名 + ".yml");
    }

    private void 创建父目录(File 文件) {
        File 父目录 = 文件.getParentFile();
        if (父目录 != null && !父目录.exists()) {
            父目录.mkdirs();
        }
    }

    private void 发送消息(Player 玩家, String 消息键, Object... 参数) {
        if (玩家 == null || !玩家.isOnline()) {
            return;
        }
        String 文本 = 翻译服务.获取(消息键);
        String 解析后 = 关键词解析器.解析(文本, 玩家.getUniqueId(), 参数);
        玩家.sendMessage(迷你消息.deserialize(解析后));
    }

    // ===== FP-F 布局查询接口（供监听器分发点击）=====

    public Map<Integer, 功能位定义> 获取功能位槽位表() {
        return Collections.unmodifiableMap(功能位槽位表);
    }

    public int 获取槽位音域减() {
        return 槽位音域减;
    }

    public int 获取槽位音域显示() {
        return 槽位音域显示;
    }

    public int 获取槽位音域加() {
        return 槽位音域加;
    }

    public int 获取槽位力度减() {
        return 槽位力度减;
    }

    public int 获取槽位力度显示() {
        return 槽位力度显示;
    }

    public int 获取槽位力度加() {
        return 槽位力度加;
    }

    public int 获取槽位录制() {
        return 槽位录制;
    }

    public int 获取槽位停止() {
        return 槽位停止;
    }

    public int 获取槽位播放() {
        return 槽位播放;
    }

    public int 获取槽位延音模式切换() {
        return 槽位延音模式切换;
    }

    public int[] 获取黑键槽位数组() {
        return 黑键槽位数组.clone();
    }

    public int[] 获取白键槽位数组() {
        return 白键槽位数组.clone();
    }

    public int[] 获取黑键音高偏移数组() {
        return 黑键音高偏移数组.clone();
    }

    public int[] 获取白键音高偏移数组() {
        return 白键音高偏移数组.clone();
    }

    /**
     * FP-03 解析槽位对应的音高。
     * 若槽位为黑键或白键，返回对应音高（基于传入八度计算）；否则返回 -1。
     *
     * @param 槽位 GUI 槽位
     * @param 八度 当前音域
     * @return 音高（0-24）；非琴键槽位返回 -1
     */
    public int 解析槽位音高(int 槽位, int 八度) {
        for (int i = 0; i < 黑键槽位数组.length && i < 黑键音高偏移数组.length; i++) {
            if (黑键槽位数组[i] == 槽位) {
                int 音高 = 八度 * 12 + 黑键音高偏移数组[i];
                if (音高 >= 0 && 音高 <= 24) {
                    return 音高;
                }
                return -1;
            }
        }
        for (int i = 0; i < 白键槽位数组.length && i < 白键音高偏移数组.length; i++) {
            if (白键槽位数组[i] == 槽位) {
                int 音高 = 八度 * 12 + 白键音高偏移数组[i];
                if (音高 >= 0 && 音高 <= 24) {
                    return 音高;
                }
                return -1;
            }
        }
        return -1;
    }

    // ===== FP-F 延音系统（FP-04）=====

    @Override
    public void 设置延音模式(UUID 玩家标识, 延音模式 模式) {
        if (玩家标识 == null || 模式 == null) {
            return;
        }
        玩家延音模式表.put(玩家标识, 模式);
    }

    @Override
    public 延音模式 获取延音模式(UUID 玩家标识) {
        if (玩家标识 == null) {
            return 默认延音模式值;
        }
        return 玩家延音模式表.getOrDefault(玩家标识, 默认延音模式值);
    }

    @Override
    public 延音模式 切换延音模式(UUID 玩家标识) {
        if (玩家标识 == null) {
            return 默认延音模式值;
        }
        延音模式 当前 = 获取延音模式(玩家标识);
        延音模式 新模式 = 当前.切换();
        玩家延音模式表.put(玩家标识, 新模式);
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            String 模式键 = 新模式 == 延音模式.按住 ? 延音模式按住键 : 延音模式切换模式键;
            发送消息(玩家, 延音已切换键, 翻译服务.获取(模式键));
        }
        return 新模式;
    }

    @Override
    public void 开始延音(Player 玩家, 乐器定义 乐器, int 音高, 力度档 档) {
        if (玩家 == null || 乐器 == null || 档 == null) {
            return;
        }
        UUID 玩家标识 = 玩家.getUniqueId();
        // 已有延音时先停止旧延音
        停止延音(玩家标识);
        int 有效音高 = Math.max(音高下限, Math.min(音高上限, 音高));
        // 粒子音高需 clamp 到 0-24（音高方块映射限制）
        int 粒子音高 = Math.max(乐器.获取音域下限(), Math.min(乐器.获取音域上限(), 有效音高));
        // 初始触发：使用演奏指令（含力度档 pitch 微调 + 录制 + 粒子）
        演奏指令(玩家, 乐器, 有效音高, 档, 默认时值Tick);
        // 启动延音任务（剩余次数 = 总次数 - 1，首次已触发）
        int 剩余次数 = Math.max(0, 延音重触发次数 - 1);
        玩家延音音高表.put(玩家标识, 粒子音高);
        if (剩余次数 <= 0) {
            // 仅首次触发，无重触发任务；按住模式下仍需记录延音中状态以便 GUI 关闭时停止
            return;
        }
        final int 最终剩余次数 = 剩余次数;
        final int 延音粒子音高 = 粒子音高;
        final float 初始音量 = 档.获取音量();
        BukkitRunnable 任务 = new BukkitRunnable() {
            private int 剩余 = 最终剩余次数;
            private float 当前音量 = 初始音量;

            @Override
            public void run() {
                if (!玩家.isOnline()) {
                    停止延音(玩家标识);
                    return;
                }
                if (剩余 <= 0) {
                    停止延音(玩家标识);
                    return;
                }
                当前音量 *= (float) 延音衰减系数;
                播放延音音符(玩家, 乐器, 延音粒子音高, 当前音量);
                剩余--;
            }
        };
        玩家延音任务表.put(玩家标识, 任务);
        任务.runTaskTimer(插件, 延音重触发间隔, 延音重触发间隔);
    }

    @Override
    public void 停止延音(UUID 玩家标识) {
        if (玩家标识 == null) {
            return;
        }
        BukkitRunnable 任务 = 玩家延音任务表.remove(玩家标识);
        if (任务 != null) {
            try {
                任务.cancel();
            } catch (IllegalStateException ignored) {
                // 任务已调度/已取消，忽略
            }
        }
        玩家延音音高表.remove(玩家标识);
    }

    @Override
    public boolean 是否延音中(UUID 玩家标识) {
        return 玩家标识 != null && 玩家延音任务表.containsKey(玩家标识);
    }

    @Override
    public int 获取延音音高(UUID 玩家标识) {
        if (玩家标识 == null) {
            return -1;
        }
        return 玩家延音音高表.getOrDefault(玩家标识, -1);
    }

    // ===== FP-G 接口实现（量化 + swing）=====

    @Override
    public void 量化乐谱(UUID 玩家标识, 量化档位 档位) {
        if (玩家标识 == null || 档位 == null) {
            return;
        }
        乐谱 当前乐谱 = 玩家量化乐谱表.get(玩家标识);
        if (当前乐谱 == null) {
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 != null) {
                发送消息(玩家, 无可量化乐谱键);
            }
            return;
        }
        // 量化基准：已量化时从原始音符列表计算，未量化时从当前音符列表计算
        List<音符> 基准列表 = 当前乐谱.是否已量化()
                ? 当前乐谱.获取原始音符列表()
                : 当前乐谱.获取音符列表();
        double swing = 获取Swing(玩家标识);
        List<音符> 量化后 = 量化音符列表(基准列表, 档位, 当前乐谱.获取速度BPM(), swing);
        当前乐谱.应用量化(量化后);
        保存乐谱到文件(当前乐谱);
        玩家量化档位表.put(玩家标识, 档位);
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            发送消息(玩家, 量化已应用键, 档位.获取标识());
        }
    }

    @Override
    public boolean 撤销量化(UUID 玩家标识) {
        if (玩家标识 == null) {
            return false;
        }
        乐谱 当前乐谱 = 玩家量化乐谱表.get(玩家标识);
        if (当前乐谱 == null) {
            return false;
        }
        boolean 已撤销 = 当前乐谱.撤销量化();
        if (已撤销) {
            保存乐谱到文件(当前乐谱);
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 != null) {
                发送消息(玩家, 量化已撤销键);
            }
        }
        return 已撤销;
    }

    @Override
    public boolean 是否已量化(UUID 玩家标识) {
        if (玩家标识 == null) {
            return false;
        }
        乐谱 当前乐谱 = 玩家量化乐谱表.get(玩家标识);
        return 当前乐谱 != null && 当前乐谱.是否已量化();
    }

    @Override
    public void 设置Swing(UUID 玩家标识, double 比例) {
        if (玩家标识 == null) {
            return;
        }
        double 限制比例 = Math.max(swing比例下限, Math.min(swing比例上限, 比例));
        玩家Swing表.put(玩家标识, 限制比例);
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            String 比例文本 = String.format("%.0f%%", 限制比例 * 100);
            发送消息(玩家, swing已设置键, 比例文本);
        }
    }

    @Override
    public double 获取Swing(UUID 玩家标识) {
        if (玩家标识 == null) {
            return 默认swing比例值;
        }
        return 玩家Swing表.getOrDefault(玩家标识, 默认swing比例值);
    }

    // ===== FP-I 接口实现（和弦 + 公共乐谱库）=====

    @Override
    public void 播放和弦(Player 玩家, 乐器定义 乐器, int 根音, 和弦类型 类型, 力度档 档) {
        if (玩家 == null || 乐器 == null || 类型 == null || 档 == null) {
            return;
        }
        List<Integer> 音高列表 = 和弦预设.生成和弦(根音, 类型,
                乐器.获取音域下限(), 乐器.获取音域上限());
        if (音高列表.isEmpty()) {
            发送消息(玩家, 音高越界键, 根音);
            return;
        }
        for (int 音高 : 音高列表) {
            演奏指令(玩家, 乐器, 音高, 档, 默认时值Tick);
        }
        发送消息(玩家, 和弦已播放键, 类型.获取标识());
    }

    @Override
    public void 启动和弦记忆(Player 玩家, 乐器定义 乐器, int 根音, 和弦类型 类型, 力度档 档, int 重复间隔) {
        if (玩家 == null || 乐器 == null || 类型 == null || 档 == null) {
            return;
        }
        UUID 玩家标识 = 玩家.getUniqueId();
        // 已在和弦记忆中时先停止旧记忆
        停止和弦记忆(玩家标识);
        // 重复间隔 <= 0 时使用配置默认值
        int 有效间隔 = 重复间隔 <= 0 ? 默认记忆重复间隔值 : Math.max(1, 重复间隔);
        // 首次触发一次和弦
        播放和弦(玩家, 乐器, 根音, 类型, 档);
        发送消息(玩家, 和弦记忆已启动键, 类型.获取标识());
        final 乐器定义 最终乐器 = 乐器;
        final int 最终根音 = 根音;
        final 和弦类型 最终类型 = 类型;
        final 力度档 最终档 = 档;
        BukkitRunnable 任务 = new BukkitRunnable() {
            @Override
            public void run() {
                if (!玩家.isOnline()) {
                    停止和弦记忆(玩家标识);
                    return;
                }
                播放和弦(玩家, 最终乐器, 最终根音, 最终类型, 最终档);
            }
        };
        玩家和弦记忆任务表.put(玩家标识, 任务);
        任务.runTaskTimer(插件, 有效间隔, 有效间隔);
    }

    @Override
    public void 停止和弦记忆(UUID 玩家标识) {
        if (玩家标识 == null) {
            return;
        }
        BukkitRunnable 任务 = 玩家和弦记忆任务表.remove(玩家标识);
        if (任务 != null) {
            try {
                任务.cancel();
            } catch (IllegalStateException ignored) {
                // 任务已调度/已取消，忽略
            }
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 != null) {
                发送消息(玩家, 和弦记忆已停止键);
            }
        }
    }

    @Override
    public boolean 是否和弦记忆中(UUID 玩家标识) {
        return 玩家标识 != null && 玩家和弦记忆任务表.containsKey(玩家标识);
    }

    @Override
    public 上传结果 上传公共乐谱(Player 玩家, String 本地乐谱名, String 公开名) {
        if (玩家 == null || 本地乐谱名 == null || 本地乐谱名.isBlank()) {
            if (玩家 != null) {
                发送消息(玩家, 公共乐谱上传参数无效键);
            }
            return 上传结果.参数无效;
        }
        UUID 玩家标识 = 玩家.getUniqueId();
        Optional<乐谱> 本地乐谱 = 加载乐谱(玩家标识, 本地乐谱名.trim());
        if (本地乐谱.isEmpty()) {
            发送消息(玩家, 公共乐谱不存在键, 本地乐谱名);
            return 上传结果.参数无效;
        }
        final String[] 生成的ID = new String[1];
        上传结果 结果 = 公共乐谱服务.上传(本地乐谱.get(), 玩家标识, 玩家.getName(), 公开名,
                id -> 生成的ID[0] = id);
        if (结果 == 上传结果.成功) {
            发送消息(玩家, 公共乐谱上传成功键, 生成的ID[0]);
        } else if (结果 == 上传结果.重名) {
            发送消息(玩家, 公共乐谱上传重名键);
        } else {
            发送消息(玩家, 公共乐谱上传参数无效键);
        }
        return 结果;
    }

    @Override
    public 下载结果 下载公共乐谱(Player 玩家, String 乐谱ID) {
        if (玩家 == null || 乐谱ID == null || 乐谱ID.isBlank()) {
            if (玩家 != null) {
                发送消息(玩家, 公共乐谱不存在键, String.valueOf(乐谱ID));
            }
            return 下载结果.参数无效;
        }
        final 乐谱[] 下载的乐谱 = new 乐谱[1];
        下载结果 结果 = 公共乐谱服务.下载(乐谱ID.trim(), 玩家.getUniqueId(),
                乐谱 -> 下载的乐谱[0] = 乐谱);
        if (结果 == 下载结果.成功 && 下载的乐谱[0] != null) {
            // 复制到玩家私有目录（使用公开名作为本地名）
            乐谱 公开 = 下载的乐谱[0];
            保存乐谱到文件(new 乐谱(公开.获取名称(), 玩家.getUniqueId(), 玩家.getName(),
                    System.currentTimeMillis(), 公开.获取速度BPM(), 公开.获取音符列表()));
            发送消息(玩家, 公共乐谱下载成功键, 公开.获取名称());
        } else if (结果 == 下载结果.不存在) {
            发送消息(玩家, 公共乐谱不存在键, 乐谱ID);
        } else if (结果 == 下载结果.未通过审核) {
            发送消息(玩家, 公共乐谱未通过审核键, 乐谱ID);
        }
        return 结果;
    }

    @Override
    public 评分结果 评分公共乐谱(Player 玩家, String 乐谱ID, int 评分) {
        if (玩家 == null || 乐谱ID == null || 乐谱ID.isBlank()) {
            if (玩家 != null) {
                发送消息(玩家, 公共乐谱不存在键, String.valueOf(乐谱ID));
            }
            return 评分结果.参数无效;
        }
        评分结果 结果 = 公共乐谱服务.评分(乐谱ID.trim(), 玩家.getUniqueId(), 评分);
        if (结果 == 评分结果.成功) {
            发送消息(玩家, 公共乐谱评分成功键, 乐谱ID);
        } else if (结果 == 评分结果.不存在) {
            发送消息(玩家, 公共乐谱不存在键, 乐谱ID);
        } else if (结果 == 评分结果.未通过审核) {
            发送消息(玩家, 公共乐谱未通过审核键, 乐谱ID);
        } else if (结果 == 评分结果.已评分) {
            发送消息(玩家, 公共乐谱已评分键, 乐谱ID);
        } else if (结果 == 评分结果.评分越界) {
            发送消息(玩家, 公共乐谱评分越界键);
        }
        return 结果;
    }

    @Override
    public 审核结果 审核公共乐谱(Player 玩家, String 乐谱ID, 审核状态 状态) {
        if (玩家 == null || 乐谱ID == null || 乐谱ID.isBlank() || 状态 == null) {
            if (玩家 != null) {
                发送消息(玩家, 公共乐谱状态无效键, String.valueOf(乐谱ID));
            }
            return 审核结果.参数无效;
        }
        审核结果 结果 = 公共乐谱服务.审核(乐谱ID.trim(), 状态);
        if (结果 == 审核结果.成功) {
            发送消息(玩家, 公共乐谱审核成功键, 乐谱ID, 状态.name());
        } else if (结果 == 审核结果.不存在) {
            发送消息(玩家, 公共乐谱不存在键, 乐谱ID);
        } else if (结果 == 审核结果.状态无效) {
            发送消息(玩家, 公共乐谱状态无效键, 乐谱ID);
        }
        return 结果;
    }

    @Override
    public List<公共乐谱> 列出公共乐谱() {
        return 公共乐谱服务.列出已通过();
    }

    // ===== FP-H NBS/MIDI 导入导出 + NoteBlockAPI 集成 =====

    @Override
    public Optional<乐谱> 导入NBS(Player 玩家, File nbs文件, String 乐谱名) {
        if (玩家 == null || nbs文件 == null) {
            return Optional.empty();
        }
        if (!nbs文件.exists() || !nbs文件.canRead()) {
            发送消息(玩家, 文件不可读键, nbs文件.getName());
            return Optional.empty();
        }
        try {
            乐谱 导入乐谱 = NBS解析器.解析(nbs文件, 玩家.getUniqueId(), 玩家.getName(), 乐谱名);
            List<String> 现有乐谱 = 获取乐谱列表(玩家.getUniqueId());
            if (!现有乐谱.contains(导入乐谱.获取名称()) && 现有乐谱.size() >= 最大乐谱存储数) {
                发送消息(玩家, 乐谱超限键, 最大乐谱存储数);
                return Optional.empty();
            }
            保存乐谱到文件(导入乐谱);
            发送消息(玩家, NBS导入成功键, 导入乐谱.获取名称());
            return Optional.of(导入乐谱);
        } catch (IOException e) {
            插件.getLogger().warning("NBS 导入失败: " + e.getMessage());
            发送消息(玩家, NBS导入失败键, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean 导出NBS(UUID 玩家标识, String 乐谱名, File 目标文件) {
        if (玩家标识 == null || 乐谱名 == null || 乐谱名.isBlank() || 目标文件 == null) {
            return false;
        }
        Optional<乐谱> 已加载 = 加载乐谱(玩家标识, 乐谱名);
        if (已加载.isEmpty()) {
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 != null) {
                发送消息(玩家, 乐谱不存在键, 乐谱名);
            }
            return false;
        }
        创建父目录(目标文件);
        try {
            // FP-03 修复：获取玩家当前乐器的 NoteBlockAPI 乐器 ID，导出时保留正确乐器
            乐器定义 当前乐器 = 获取玩家当前乐器(玩家标识).orElse(null);
            int 乐器ID = 当前乐器 == null ? 0 : 当前乐器.获取NoteBlockAPI乐器ID().orElse(0);
            NBS导出器.导出(已加载.get(), 目标文件, 乐器ID);
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 != null) {
                发送消息(玩家, NBS导出成功键, 乐谱名);
            }
            return true;
        } catch (IOException e) {
            插件.getLogger().warning("NBS 导出失败: " + e.getMessage());
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 != null) {
                发送消息(玩家, NBS导出失败键, e.getMessage());
            }
            return false;
        }
    }

    @Override
    public Optional<乐谱> 导入MIDI(Player 玩家, File midi文件, String 乐谱名) {
        if (玩家 == null || midi文件 == null) {
            return Optional.empty();
        }
        if (!midi文件.exists() || !midi文件.canRead()) {
            发送消息(玩家, 文件不可读键, midi文件.getName());
            return Optional.empty();
        }
        try {
            乐谱 导入乐谱 = MIDI解析器.解析(midi文件, 玩家.getUniqueId(), 玩家.getName(), 乐谱名);
            List<String> 现有乐谱 = 获取乐谱列表(玩家.getUniqueId());
            if (!现有乐谱.contains(导入乐谱.获取名称()) && 现有乐谱.size() >= 最大乐谱存储数) {
                发送消息(玩家, 乐谱超限键, 最大乐谱存储数);
                return Optional.empty();
            }
            保存乐谱到文件(导入乐谱);
            发送消息(玩家, MIDI导入成功键, 导入乐谱.获取名称());
            return Optional.of(导入乐谱);
        } catch (IOException e) {
            插件.getLogger().warning("MIDI 导入失败: " + e.getMessage());
            发送消息(玩家, MIDI导入失败键, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean 导出MIDI(UUID 玩家标识, String 乐谱名, File 目标文件) {
        if (玩家标识 == null || 乐谱名 == null || 乐谱名.isBlank() || 目标文件 == null) {
            return false;
        }
        Optional<乐谱> 已加载 = 加载乐谱(玩家标识, 乐谱名);
        if (已加载.isEmpty()) {
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 != null) {
                发送消息(玩家, 乐谱不存在键, 乐谱名);
            }
            return false;
        }
        创建父目录(目标文件);
        try {
            MIDI导出器.导出(已加载.get(), 目标文件);
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 != null) {
                发送消息(玩家, MIDI导出成功键, 乐谱名);
            }
            return true;
        } catch (IOException e) {
            插件.getLogger().warning("MIDI 导出失败: " + e.getMessage());
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 != null) {
                发送消息(玩家, MIDI导出失败键, e.getMessage());
            }
            return false;
        }
    }

    @Override
    public boolean 使用NoteBlockAPI播放(Player 玩家, String 乐谱名) {
        if (玩家 == null || 乐谱名 == null || 乐谱名.isBlank()) {
            return false;
        }
        Optional<乐谱> 已加载 = 加载乐谱(玩家.getUniqueId(), 乐谱名);
        if (已加载.isEmpty() || 已加载.get().获取音符数量() == 0) {
            return false;
        }
        乐谱 乐谱 = 已加载.get();
        // FP-03 修复：获取玩家当前乐器，用于决定 NoteBlockAPI 乐器 ID
        乐器定义 当前乐器 = 获取玩家当前乐器(玩家.getUniqueId()).orElse(null);
        if (当前乐器 == null) {
            当前乐器 = 乐器注册表.默认乐器().orElse(null);
        }
        if (当前乐器 == null) {
            return false;
        }
        // FP-03 修复：乐器未配置 NoteBlockAPI 乐器 ID 时，降级为原生 playSound 播放
        Optional<Integer> 乐器IDOpt = 当前乐器.获取NoteBlockAPI乐器ID();
        if (乐器IDOpt.isEmpty()) {
            停止播放(玩家.getUniqueId());
            播放乐谱(玩家, 乐谱);
            return false;
        }
        // 停止现有原生播放与 NoteBlockAPI 播放
        停止播放(玩家.getUniqueId());
        NoteBlockAPI适配器.停止播放(玩家.getUniqueId());
        if (!NoteBlockAPI适配器.是否可用()) {
            // 降级为原生 playSound 播放
            播放乐谱(玩家, 乐谱);
            return false;
        }
        // FP-03 修复：传入玩家当前乐器的 NoteBlockAPI 乐器 ID，替代适配器内硬编码的钢琴
        boolean 已播放 = NoteBlockAPI适配器.播放乐谱(玩家, 乐谱, 乐器IDOpt.get());
        if (已播放) {
            // 缓存当前播放的乐谱，供后续 量化乐谱/撤销量化 操作
            玩家量化乐谱表.put(玩家.getUniqueId(), 乐谱);
            发送消息(玩家, 乐谱开始播放键, 乐谱.获取名称());
        } else {
            // 适配器报告播放失败，降级为原生播放
            播放乐谱(玩家, 乐谱);
        }
        return 已播放;
    }

    // ===== FP-J 分层键盘 split（O7）+ piano roll（O9）+ 弯音轮调制轮（O6）+ 表情控制（O10）+ 资源包 + mod 边界 =====

    // ----- FP-19 分层键盘 split（O7）-----

    @Override
    public void 设置分层模式(UUID 玩家标识, boolean 启用) {
        if (玩家标识 == null) {
            return;
        }
        分层配置 当前 = 玩家分层配置表.getOrDefault(玩家标识, 分层配置.默认配置());
        if (当前.是否启用分层() == 启用) {
            return;
        }
        玩家分层配置表.put(玩家标识, 启用 ? 当前.切换分层() : 当前.切换分层());
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            发送消息(玩家, 启用 ? 分层已开启键 : 分层已关闭键);
        }
    }

    @Override
    public boolean 是否分层模式(UUID 玩家标识) {
        if (玩家标识 == null) {
            return false;
        }
        return 玩家分层配置表.getOrDefault(玩家标识, 分层配置.默认配置()).是否启用分层();
    }

    @Override
    public void 设置分界点(UUID 玩家标识, int 分界点) {
        if (玩家标识 == null) {
            return;
        }
        分层配置 当前 = 玩家分层配置表.getOrDefault(玩家标识, 分层配置.默认配置());
        玩家分层配置表.put(玩家标识, 当前.设置分界点(分界点));
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            发送消息(玩家, 分界点已设置键, 当前.设置分界点(分界点).获取分界点());
        }
    }

    @Override
    public int 获取分界点(UUID 玩家标识) {
        if (玩家标识 == null) {
            return 分层配置.默认分界点;
        }
        return 玩家分层配置表.getOrDefault(玩家标识, 分层配置.默认配置()).获取分界点();
    }

    @Override
    public void 设置分层左乐器(UUID 玩家标识, 乐器定义 乐器) {
        if (玩家标识 == null) {
            return;
        }
        分层配置 当前 = 玩家分层配置表.getOrDefault(玩家标识, 分层配置.默认配置());
        玩家分层配置表.put(玩家标识, 当前.设置左乐器(乐器));
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            发送消息(玩家, 分层乐器已设置键);
        }
    }

    @Override
    public void 设置分层右乐器(UUID 玩家标识, 乐器定义 乐器) {
        if (玩家标识 == null) {
            return;
        }
        分层配置 当前 = 玩家分层配置表.getOrDefault(玩家标识, 分层配置.默认配置());
        玩家分层配置表.put(玩家标识, 当前.设置右乐器(乐器));
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            发送消息(玩家, 分层乐器已设置键);
        }
    }

    @Override
    public 分层配置 获取分层配置(UUID 玩家标识) {
        if (玩家标识 == null) {
            return 分层配置.默认配置();
        }
        return 玩家分层配置表.getOrDefault(玩家标识, 分层配置.默认配置());
    }

    @Override
    public boolean 切换分层模式(UUID 玩家标识) {
        if (玩家标识 == null) {
            return false;
        }
        分层配置 当前 = 玩家分层配置表.getOrDefault(玩家标识, 分层配置.默认配置());
        分层配置 新配置 = 当前.切换分层();
        玩家分层配置表.put(玩家标识, 新配置);
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            发送消息(玩家, 新配置.是否启用分层() ? 分层已开启键 : 分层已关闭键);
        }
        return 新配置.是否启用分层();
    }

    // ----- FP-20 piano roll 简化编辑器（O9）-----

    @Override
    public boolean 打开PianoRoll编辑器(Player 玩家, String 乐谱名) {
        if (玩家 == null || 乐谱名 == null || 乐谱名.isBlank()) {
            return false;
        }
        Optional<乐谱> 已加载 = 加载乐谱(玩家.getUniqueId(), 乐谱名);
        if (已加载.isEmpty()) {
            发送消息(玩家, 乐谱不存在键, 乐谱名);
            return false;
        }
        钢琴卷帘编辑器 编辑器 = 钢琴卷帘编辑器.打开(已加载.get());
        玩家PianoRoll编辑器表.put(玩家.getUniqueId(), 编辑器);
        发送消息(玩家, pianoRoll已打开键, 乐谱名);
        return true;
    }

    @Override
    public void 关闭PianoRoll编辑器(UUID 玩家标识) {
        if (玩家标识 == null) {
            return;
        }
        钢琴卷帘编辑器 编辑器 = 玩家PianoRoll编辑器表.remove(玩家标识);
        if (编辑器 != null) {
            Player 玩家 = Bukkit.getPlayer(玩家标识);
            if (玩家 != null) {
                发送消息(玩家, pianoRoll已关闭键);
            }
        }
    }

    @Override
    public Optional<钢琴卷帘编辑器> 获取PianoRoll编辑器(UUID 玩家标识) {
        if (玩家标识 == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(玩家PianoRoll编辑器表.get(玩家标识));
    }

    @Override
    public 钢琴卷帘编辑器.编辑结果 编辑PianoRoll音符(UUID 玩家标识, 钢琴卷帘编辑器.编辑操作 操作,
                                               int 音符索引, int 新音高, int 新时值Tick, double 新力度) {
        if (玩家标识 == null || 操作 == null) {
            return 钢琴卷帘编辑器.编辑结果.乐谱为空;
        }
        钢琴卷帘编辑器 编辑器 = 玩家PianoRoll编辑器表.get(玩家标识);
        if (编辑器 == null) {
            return 钢琴卷帘编辑器.编辑结果.乐谱为空;
        }
        钢琴卷帘编辑器.编辑操作配对 配对;
        switch (操作) {
            case 删除 -> 配对 = 编辑器.删除音符(音符索引);
            case 移动 -> 配对 = 编辑器.移动音符(音符索引, 新音高, 新时值Tick);
            case 复制 -> 配对 = 编辑器.复制音符(音符索引, 新音高, 新时值Tick);
            case 改力度 -> 配对 = 编辑器.改音符力度(音符索引, 新力度);
            case 新增 -> 配对 = 编辑器.新增音符(新音高, 新时值Tick, 新力度);
            default -> {
                return 钢琴卷帘编辑器.编辑结果.位置无效;
            }
        }
        if (配对.是否成功()) {
            玩家PianoRoll编辑器表.put(玩家标识, 配对.获取新编辑器());
        }
        return 配对.获取结果();
    }

    @Override
    public boolean 保存PianoRoll编辑(UUID 玩家标识, String 乐谱名) {
        if (玩家标识 == null) {
            return false;
        }
        钢琴卷帘编辑器 编辑器 = 玩家PianoRoll编辑器表.get(玩家标识);
        if (编辑器 == null || !编辑器.是否已打开()) {
            return false;
        }
        乐谱 当前乐谱 = 编辑器.获取编辑中乐谱();
        String 保存名 = (乐谱名 == null || 乐谱名.isBlank()) ? 当前乐谱.获取名称() : 乐谱名;
        if (保存名 == null || 保存名.isBlank()) {
            return false;
        }
        // 构造新乐谱（使用保存名），保留音符列表与元数据
        乐谱 保存乐谱 = new 乐谱(保存名, 当前乐谱.获取作者标识(), 当前乐谱.获取作者名(),
                当前乐谱.获取创建时间戳(), 当前乐谱.获取速度BPM(), 当前乐谱.获取音符列表());
        保存乐谱到文件(保存乐谱);
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            发送消息(玩家, pianoRoll保存成功键, 保存名);
        }
        return true;
    }

    @Override
    public boolean 撤销PianoRoll编辑(UUID 玩家标识) {
        if (玩家标识 == null) {
            return false;
        }
        钢琴卷帘编辑器 编辑器 = 玩家PianoRoll编辑器表.get(玩家标识);
        if (编辑器 == null) {
            return false;
        }
        钢琴卷帘编辑器 恢复后 = 编辑器.恢复原始();
        玩家PianoRoll编辑器表.put(玩家标识, 恢复后);
        Player 玩家 = Bukkit.getPlayer(玩家标识);
        if (玩家 != null) {
            发送消息(玩家, pianoRoll已撤销键);
        }
        return true;
    }

    // ----- FP-21 弯音轮 + 调制轮（mod 独享，O6）-----

    @Override
    public void 处理弯音轮(Player 玩家, int 弯音值) {
        if (玩家 == null) {
            return;
        }
        UUID 玩家标识 = 玩家.getUniqueId();
        弯音调制状态 当前 = 玩家弯音调制状态表.getOrDefault(玩家标识, 弯音调制状态.默认状态());
        玩家弯音调制状态表.put(玩家标识, 当前.设置弯音值(弯音值));
    }

    @Override
    public void 处理调制轮(Player 玩家, int 调制值) {
        if (玩家 == null) {
            return;
        }
        UUID 玩家标识 = 玩家.getUniqueId();
        弯音调制状态 当前 = 玩家弯音调制状态表.getOrDefault(玩家标识, 弯音调制状态.默认状态());
        玩家弯音调制状态表.put(玩家标识, 当前.设置调制值(调制值));
    }

    @Override
    public 弯音调制状态 获取弯音调制状态(UUID 玩家标识) {
        if (玩家标识 == null) {
            return 弯音调制状态.默认状态();
        }
        return 玩家弯音调制状态表.getOrDefault(玩家标识, 弯音调制状态.默认状态());
    }

    @Override
    public void 设置弯音范围(UUID 玩家标识, int 半音数) {
        if (玩家标识 == null) {
            return;
        }
        int 限制 = Math.max(1, Math.min(12, 半音数));
        玩家弯音范围表.put(玩家标识, 限制);
    }

    @Override
    public int 获取弯音范围(UUID 玩家标识) {
        if (玩家标识 == null) {
            return 默认弯音范围半音值;
        }
        return 玩家弯音范围表.getOrDefault(玩家标识, 默认弯音范围半音值);
    }

    // ----- FP-22 表情控制（mod 独享，O10）-----

    @Override
    public void 处理表情控制(Player 玩家, int cc编号, int 值) {
        if (玩家 == null) {
            return;
        }
        UUID 玩家标识 = 玩家.getUniqueId();
        表情控制状态 当前 = 玩家表情控制状态表.getOrDefault(玩家标识, 表情控制状态.默认状态());
        玩家表情控制状态表.put(玩家标识, 当前.设置CC(cc编号, 值));
    }

    @Override
    public 表情控制状态 获取表情控制状态(UUID 玩家标识) {
        if (玩家标识 == null) {
            return 表情控制状态.默认状态();
        }
        return 玩家表情控制状态表.getOrDefault(玩家标识, 表情控制状态.默认状态());
    }

    // ----- FP-14 资源包（保底方案）-----

    @Override
    public boolean 应用资源包(Player 玩家) {
        if (玩家 == null) {
            return false;
        }
        boolean 已发送 = 资源包管理器.应用资源包(玩家);
        if (已发送) {
            发送消息(玩家, 资源包应用提示键);
        }
        return 已发送;
    }

    @Override
    public boolean 生成资源包() {
        return 资源包管理器.生成资源包();
    }

    @Override
    public boolean 资源包已配置() {
        return 资源包管理器.资源包已配置();
    }

    // ----- FP-12 mod 边界 -----

    @Override
    public boolean 注册mod输入通道() {
        if (mod输入通道Provider == null) {
            return false;
        }
        return mod输入通道Provider.get().注册通道();
    }

    @Override
    public boolean mod输入通道已注册() {
        if (mod输入通道Provider == null) {
            return false;
        }
        return mod输入通道Provider.get().是否已注册();
    }


    /**
     * FP-09 量化算法（非破坏性）：将音符列表的累计起始 tick 吸附到最近的量化网格点，
     * 然后根据量化后的起始 tick 重算每个音符的时值。保留原始音高与力度。
     * <p>
     * swing 算法：仅对非首拍位置应用（首拍 = 起始 tick 落在拍起点上）。
     * 当 swing > 0.50 时，将 1/8 网格的弱拍位置（半拍处）向后推迟到 swing × 每拍tick。
     * swing = 0.50 时不影响；三连音档位不应用 swing（已是三连音感）。
     *
     * @param 原始 原始音符列表
     * @param 档位 量化档位
     * @param 速度BPM 乐谱 BPM
     * @param swing比例 swing 比例（0.50-0.65）
     * @return 量化后音符列表
     */
    private List<音符> 量化音符列表(List<音符> 原始, 量化档位 档位, int 速度BPM, double swing比例) {
        if (原始 == null || 原始.isEmpty()) {
            return new ArrayList<>();
        }
        int 数量 = 原始.size();
        // 1. 计算每个音符的累计起始 tick
        int[] 起始tick数组 = new int[数量];
        int 累计 = 0;
        for (int i = 0; i < 数量; i++) {
            起始tick数组[i] = 累计;
            累计 += 原始.get(i).获取时值Tick();
        }
        // 2. 量化每个起始 tick 到网格
        int[] 量化后起始 = new int[数量];
        for (int i = 0; i < 数量; i++) {
            量化后起始[i] = 档位.量化Tick(起始tick数组[i], 速度BPM);
        }
        // 3. 应用 swing（仅非三连音档位 + swing > 0.50）
        if (!档位.是否三连音() && swing比例 > swing比例下限 + 1e-6) {
            long 每拍tick = Math.max(1L, Math.round(1200.0 / 速度BPM));
            long 半拍 = 每拍tick / 2;
            long swing偏移 = Math.round(swing比例 * 每拍tick);
            for (int i = 0; i < 数量; i++) {
                long 偏移 = 量化后起始[i] % 每拍tick;
                // 仅对半拍位置（8分音符弱拍）应用 swing
                if (半拍 > 0 && 偏移 == 半拍) {
                    量化后起始[i] = (int) (量化后起始[i] - 偏移 + swing偏移);
                }
            }
        }
        // 4. 根据量化后起始 tick 重算时值（最后一个音符保留原始时值）
        List<音符> 结果 = new ArrayList<>(数量);
        for (int i = 0; i < 数量; i++) {
            int 当前起始 = 量化后起始[i];
            int 下一起始 = (i + 1 < 数量) ? 量化后起始[i + 1] : 当前起始 + 原始.get(i).获取时值Tick();
            int 新时值 = Math.max(1, 下一起始 - 当前起始);
            音符 原音符 = 原始.get(i);
            结果.add(音符.of(原音符.获取音高(), 新时值, 原音符.获取力度()));
        }
        return 结果;
    }

    /**
     * FP-10 录制指示器：玩家开始录制时（count-in 结束后）显示"录制中"标题。
     * 与节拍器拍号显示互不冲突：录制中标题仅在录制开始时一次性显示。
     */
    // Purpur 26.2 的 Player 仅提供已过时的定时 sendTitle（无 adventure Title 重载），保留使用并抑制告警。
    @SuppressWarnings({"deprecation", "removal"})
    private void 显示录制中标题(Player 玩家) {
        if (玩家 == null) {
            return;
        }
        String 标题 = 翻译服务.获取(录制中标题键);
        玩家.sendTitle(标题, "", 录制中标题淡入tick, 录制中标题停留tick, 录制中标题淡出tick);
    }

    /**
     * FP-04 延音重触发内部播放（不含 pitch 微调、不含录制、含粒子）。
     * 受 MC playSound「一击即逝」限制，通过周期性重触发模拟自然衰减尾音。
     */
    private void 播放延音音符(Player 玩家, 乐器定义 乐器, int 音高, float 音量) {
        if (玩家 == null || 乐器 == null) {
            return;
        }
        float pitch = 乐器.音高转Pitch(音高);
        float 限制音量 = Math.max(0.0f, Math.min(1.0f, 音量)) * 默认音量;
        玩家.playSound(玩家.getLocation(), 乐器.获取原版音色(), 限制音量, pitch);
        for (Player 附近玩家 : 玩家.getWorld().getPlayers()) {
            if (附近玩家.equals(玩家)) {
                continue;
            }
            if (附近玩家.getLocation().distanceSquared(玩家.getLocation()) <= 听觉范围 * 听觉范围) {
                附近玩家.playSound(玩家.getLocation(), 乐器.获取原版音色(), 限制音量, pitch);
            }
        }
        播放音高粒子(玩家, 音高);
    }

    public static class 演奏界面持有者 implements InventoryHolder {
        private final 乐器定义 乐器;
        private int 当前八度;

        public 演奏界面持有者(乐器定义 乐器) {
            this.乐器 = 乐器;
            this.当前八度 = 乐器.获取默认音域();
        }

        public 乐器定义 获取乐器() {
            return 乐器;
        }

        public int 获取当前八度() {
            return 当前八度;
        }

        public void 设置当前八度(int 八度) {
            this.当前八度 = Math.max(0, Math.min(2, 八度));
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static class 录制状态 {
        final long 开始时间戳;
        final List<音符> 音符序列 = new ArrayList<>();
        // GAP-03: 按下时间戳与力度，按下时存入，松开时计算差值生成真实时值
        final Map<Integer, Long> 按下时间戳 = new ConcurrentHashMap<>();
        final Map<Integer, Double> 按下力度 = new ConcurrentHashMap<>();

        录制状态(long 开始时间戳) {
            this.开始时间戳 = 开始时间戳;
        }
    }

    private static class 合奏状态内部 {
        final UUID 队长标识;
        final 乐谱 乐谱;
        final Set<UUID> 参与成员;
        final Set<UUID> 已确认集合 = ConcurrentHashMap.newKeySet();
        boolean 已开始 = false;

        合奏状态内部(UUID 队长标识, 乐谱 乐谱, Set<UUID> 参与成员) {
            this.队长标识 = 队长标识;
            this.乐谱 = 乐谱;
            this.参与成员 = 参与成员;
        }
    }
}
