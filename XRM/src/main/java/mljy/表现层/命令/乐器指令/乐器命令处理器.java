package mljy.表现层.命令.乐器指令;

import com.google.inject.Inject;
import 暮澜纪元.通用.文本.关键词解析器;
import mljy.业务层.乐器服务;
import mljy.业务层.乐器服务.乐谱保存结果;
import mljy.业务层.乐器.公共乐谱服务.上传结果;
import mljy.业务层.乐器.公共乐谱服务.下载结果;
import mljy.业务层.乐器.公共乐谱服务.评分结果;
import mljy.业务层.乐器.公共乐谱服务.审核结果;
import mljy.翻译服务;
import mljy.表现层.命令.抽象命令处理器;
import mljy.领域层.乐器.乐器定义;
import mljy.领域层.乐器.乐器注册表;
import mljy.领域层.乐器.合奏状态;
import mljy.领域层.乐器.力度档;
import mljy.领域层.乐器.量化档位;
import mljy.领域层.乐器.延音模式;
import mljy.领域层.乐器.和弦类型;
import mljy.领域层.乐器.公共乐谱;
import mljy.领域层.乐器.公共乐谱.审核状态;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class 乐器命令处理器 extends 抽象命令处理器 {
    private static final String 未知子命令键 = "乐器命令.错误.未知子命令";
    private static final String 缺少参数键 = "乐器命令.错误.缺少参数";
    private static final String 音色不存在键 = "乐器命令.错误.音色不存在";
    private static final String 玩家不在线键 = "乐器命令.错误.玩家不在线";
    private static final String 合奏用法键 = "乐器命令.合奏.用法";
    private static final String 给予用法键 = "乐器命令.给予.用法";
    private static final String 乐谱列表为空键 = "乐器命令.乐谱.列表为空";
    private static final String 乐谱不存在键 = "乐器命令.乐谱.不存在";
    private static final String 乐谱删除成功键 = "乐器命令.乐谱.删除成功";
    private static final String 你的乐谱键 = "乐器命令.乐谱.你的乐谱";
    private static final String 曲目条目键 = "乐器命令.乐谱.曲目条目";
    private static final String 音符计数后缀键 = "乐器命令.乐谱.音符计数后缀";
    private static final String 给予成功键 = "乐器命令.乐器.给予成功";
    private static final String 重载成功键 = "乐器命令.配置.重载成功";
    private static final String 合奏准备检查已发送键 = "乐器命令.合奏.准备检查已发送";
    private static final String 合奏已确认准备键 = "乐器命令.合奏.已确认准备";
    private static final String 合奏已停止键 = "乐器命令.合奏.已停止";
    private static final String 合奏无进行中键 = "乐器命令.合奏.无进行中合奏";
    private static final String 合奏状态标题键 = "乐器命令.合奏.状态标题";
    private static final String 合奏曲目键 = "乐器命令.合奏.曲目";
    private static final String 合奏BPM键 = "乐器命令.合奏.BPM";
    private static final String 合奏状态键 = "乐器命令.合奏.状态";
    private static final String 合奏准备键 = "乐器命令.合奏.准备";
    private static final String 合奏无曲目键 = "乐器命令.合奏.无曲目";
    private static final String 合奏演奏中键 = "乐器命令.合奏.演奏中";
    private static final String 合奏准备中键 = "乐器命令.合奏.准备中";
    private static final String 合奏准备进度键 = "乐器服务.合奏状态.准备进度";
    private static final String 合奏无法开始错误键 = "乐器命令.合奏.无法开始错误";
    private static final String 合奏无待确认错误键 = "乐器命令.合奏.无待确认错误";
    private static final String 合奏无待拒绝错误键 = "乐器命令.合奏.无待拒绝错误";
    private static final String 合奏无可停止错误键 = "乐器命令.合奏.无可停止错误";

    private static final String 节拍器开关已开启键 = "乐器命令.节拍器.开关.已开启";
    private static final String 节拍器开关已关闭键 = "乐器命令.节拍器.开关.已关闭";
    private static final String 节拍器CountIn已设置键 = "乐器命令.节拍器.countIn.已设置";
    private static final String 节拍器CountIn非法值错误键 = "乐器命令.节拍器.countIn.非法值错误";
    private static final String 节拍器用法键 = "乐器命令.节拍器.用法";
    private static final String 节拍器CountIn用法键 = "乐器命令.节拍器.countIn.用法";

    // FP-16 新增翻译键
    private static final String 力度用法键 = "乐器命令.力度.用法";
    private static final String 力度已设置键 = "乐器命令.力度.已设置";
    private static final String 力度档无效键 = "乐器命令.力度.档位无效";
    private static final String 力度真实开关无效键 = "乐器命令.力度.真实开关无效";
    private static final String 音域用法键 = "乐器命令.音域.用法";
    private static final String 音域已设置键 = "乐器命令.音域.已设置";
    private static final String 音域越界键 = "乐器命令.音域.越界";

    private static final String 量化用法键 = "乐器命令.量化.用法";
    private static final String 量化档位无效键 = "乐器命令.量化.档位无效";
    private static final String 量化预览已量化键 = "乐器命令.量化.预览已量化";
    private static final String 量化预览未量化键 = "乐器命令.量化.预览未量化";

    private static final String swing用法键 = "乐器命令.swing.用法";
    private static final String swing值越界键 = "乐器命令.swing.值越界";
    private static final String swing已开启键 = "乐器命令.swing.已开启";
    private static final String swing已关闭键 = "乐器命令.swing.已关闭";
    private static final String swing值无效键 = "乐器命令.swing.值无效";

    private static final String 延音用法键 = "乐器命令.延音.用法";
    private static final String 延音已设置键 = "乐器命令.延音.已设置";
    private static final String 延音模式无效键 = "乐器命令.延音.模式无效";

    private static final String 和弦用法键 = "乐器命令.和弦.用法";
    private static final String 和弦根音无效键 = "乐器命令.和弦.根音无效";
    private static final String 和弦类型无效键 = "乐器命令.和弦.类型无效";
    private static final String 和弦列表标题键 = "乐器命令.和弦.列表标题";
    private static final String 和弦列表项键 = "乐器命令.和弦.列表项";

    private static final String 录音用法键 = "乐器命令.录音.用法";
    private static final String 录音已开始键 = "乐器命令.录音.已开始";
    private static final String 录音已停止键 = "乐器命令.录音.已停止";
    private static final String 录音未在进行键 = "乐器命令.录音.未在进行";
    private static final String 录音保存用法键 = "乐器命令.录音.保存用法";
    private static final String 录音预览进行中键 = "乐器命令.录音.预览进行中";
    private static final String 录音预览未进行键 = "乐器命令.录音.预览未进行";

    private static final String 公共库用法键 = "乐器命令.公共库.用法";
    private static final String 公共库上传用法键 = "乐器命令.公共库.上传用法";
    private static final String 公共库下载用法键 = "乐器命令.公共库.下载用法";
    private static final String 公共库评分用法键 = "乐器命令.公共库.评分用法";
    private static final String 公共库审核用法键 = "乐器命令.公共库.审核用法";
    private static final String 公共库搜索用法键 = "乐器命令.公共库.搜索用法";
    private static final String 公共库评分值无效键 = "乐器命令.公共库.评分值无效";
    private static final String 公共库审核状态无效键 = "乐器命令.公共库.审核状态无效";
    private static final String 公共库搜索结果为空键 = "乐器命令.公共库.搜索结果为空";

    private static final String 分层用法键 = "乐器命令.分层.用法";
    private static final String 分界点越界键 = "乐器命令.分层.分界点越界";
    private static final String 分层方向无效键 = "乐器命令.分层.方向无效";

    private static final String 钢琴卷帘用法键 = "乐器命令.钢琴卷帘.用法";

    private static final String 资源包用法键 = "乐器命令.资源包.用法";
    private static final String 资源包应用失败键 = "乐器命令.资源包.应用失败";
    private static final String 资源包生成成功键 = "乐器命令.资源包.生成成功";
    private static final String 资源包生成失败键 = "乐器命令.资源包.生成失败";

    private static final String 弯音用法键 = "乐器命令.弯音.用法";
    private static final String 弯音范围已设置键 = "乐器命令.弯音.范围已设置";
    private static final String 弯音范围越界键 = "乐器命令.弯音.范围越界";
    private static final String 默认乐器未配置键 = "乐器命令.错误.默认乐器未配置";

    // 乐器服务层翻译键（直接复用）
    private static final String 服务力度切换键 = "乐器服务.力度切换";
    private static final String 服务音域切换键 = "乐器服务.音域切换";
    private static final String 服务真实力度已开启键 = "乐器服务.真实力度已开启";
    private static final String 服务真实力度已关闭键 = "乐器服务.真实力度已关闭";
    private static final String 服务量化已应用键 = "乐器服务.量化已应用";
    private static final String 服务量化已撤销键 = "乐器服务.量化已撤销";
    private static final String 服务无可量化乐谱键 = "乐器服务.无可量化乐谱";
    private static final String 服务swing已设置键 = "乐器服务.swing已设置";
    private static final String 服务和弦已播放键 = "乐器服务.和弦已播放";
    private static final String 服务和弦记忆已启动键 = "乐器服务.和弦记忆已启动";
    private static final String 服务和弦记忆已停止键 = "乐器服务.和弦记忆已停止";
    private static final String 服务公共乐谱上传成功键 = "乐器服务.公共乐谱上传成功";
    private static final String 服务公共乐谱上传重名键 = "乐器服务.公共乐谱上传重名";
    private static final String 服务公共乐谱上传参数无效键 = "乐器服务.公共乐谱上传参数无效";
    private static final String 服务公共乐谱下载成功键 = "乐器服务.公共乐谱下载成功";
    private static final String 服务公共乐谱不存在键 = "乐器服务.公共乐谱不存在";
    private static final String 服务公共乐谱未通过审核键 = "乐器服务.公共乐谱未通过审核";
    private static final String 服务公共乐谱评分成功键 = "乐器服务.公共乐谱评分成功";
    private static final String 服务公共乐谱已评分键 = "乐器服务.公共乐谱已评分";
    private static final String 服务公共乐谱评分越界键 = "乐器服务.公共乐谱评分越界";
    private static final String 服务公共乐谱审核成功键 = "乐器服务.公共乐谱审核成功";
    private static final String 服务公共乐谱状态无效键 = "乐器服务.公共乐谱状态无效";
    private static final String 服务公共乐谱列表为空键 = "乐器服务.公共乐谱列表为空";
    private static final String 服务公共乐谱列表项键 = "乐器服务.公共乐谱列表项";
    private static final String 服务NBS导入成功键 = "乐器服务.NBS导入成功";
    private static final String 服务NBS导入失败键 = "乐器服务.NBS导入失败";
    private static final String 服务NBS导出成功键 = "乐器服务.NBS导出成功";
    private static final String 服务NBS导出失败键 = "乐器服务.NBS导出失败";
    private static final String 服务MIDI导入成功键 = "乐器服务.MIDI导入成功";
    private static final String 服务MIDI导入失败键 = "乐器服务.MIDI导入失败";
    private static final String 服务MIDI导出成功键 = "乐器服务.MIDI导出成功";
    private static final String 服务MIDI导出失败键 = "乐器服务.MIDI导出失败";
    private static final String 服务文件不可读键 = "乐器服务.文件不可读";
    private static final String 服务乐谱不存在键 = "乐器服务.乐谱不存在";
    private static final String 服务分层已开启键 = "乐器服务.分层已开启";
    private static final String 服务分层已关闭键 = "乐器服务.分层已关闭";
    private static final String 服务分界点已设置键 = "乐器服务.分界点已设置";
    private static final String 服务分层乐器已设置键 = "乐器服务.分层乐器已设置";
    private static final String 服务pianoRoll已打开键 = "乐器服务.pianoRoll已打开";
    private static final String 服务pianoRoll编辑失败键 = "乐器服务.pianoRoll编辑失败";
    private static final String 服务资源包应用提示键 = "乐器服务.资源包应用提示";
    private static final String 服务乐谱保存成功键 = "乐器服务.乐谱保存成功";
    private static final String 服务乐谱已存在键 = "乐器服务.乐谱已存在";
    private static final String 服务乐谱超限键 = "乐器服务.乐谱超限";
    private static final String 服务无录制内容键 = "乐器服务.无录制内容";

    private static final String 子命令演奏 = "演奏";
    private static final String 子命令切换 = "切换";
    private static final String 子命令乐谱 = "乐谱";
    private static final String 子命令合奏 = "合奏";
    private static final String 子命令给予 = "给予";
    private static final String 子命令重载 = "重载";
    private static final String 子命令节拍器 = "节拍器";
    private static final String 子命令力度 = "力度";
    private static final String 子命令音域 = "音域";
    private static final String 子命令量化 = "量化";
    private static final String 子命令Swing = "swing";
    private static final String 子命令延音 = "延音";
    private static final String 子命令和弦 = "和弦";
    private static final String 子命令录音 = "录音";
    private static final String 子命令公共库 = "公共库";
    private static final String 子命令分层 = "分层";
    private static final String 子命令钢琴卷帘 = "钢琴卷帘";
    private static final String 子命令资源包 = "资源包";
    private static final String 子命令弯音 = "弯音";

    private static final String 乐谱子命令列表 = "列表";
    private static final String 乐谱子命令播放 = "播放";
    private static final String 乐谱子命令删除 = "删除";
    private static final String 乐谱子命令导入 = "导入";
    private static final String 乐谱子命令导出 = "导出";
    private static final String 乐谱子命令Midi导入 = "midi导入";
    private static final String 乐谱子命令Midi导出 = "midi导出";

    private static final String 合奏子命令开始 = "开始";
    private static final String 合奏子命令确认 = "确认";
    private static final String 合奏子命令拒绝 = "拒绝";
    private static final String 合奏子命令停止 = "停止";
    private static final String 合奏子命令状态 = "状态";

    private static final String 节拍器子命令开 = "开";
    private static final String 节拍器子命令关 = "关";
    private static final String 节拍器子命令CountIn = "count-in";

    private static final String 力度子命令真实 = "真实";
    private static final String 力度子命令音域 = "音域";
    private static final String 开关参数开 = "开";
    private static final String 开关参数关 = "关";

    private static final String 量子命令撤销 = "撤销";
    private static final String 量子命令预览 = "预览";

    private static final String 延音参数按住 = "按住";
    private static final String 延音参数切换 = "切换";

    private static final String 和弦子命令列表 = "列表";
    private static final String 和弦子命令清除 = "清除";
    private static final String 和弦参数记忆 = "记忆";

    private static final String 录音子命令开始 = "开始";
    private static final String 录音子命令停止 = "停止";
    private static final String 录音子命令保存 = "保存";
    private static final String 录音子命令预览 = "预览";
    private static final String 录音子命令撤销量化 = "撤销量化";

    private static final String 公共库子命令列表 = "列表";
    private static final String 公共库子命令搜索 = "搜索";
    private static final String 公共库子命令下载 = "下载";
    private static final String 公共库子命令上传 = "上传";
    private static final String 公共库子命令评分 = "评分";
    private static final String 公共库子命令审核 = "审核";

    private static final String 公共库审核通过 = "通过";
    private static final String 公共库审核拒绝 = "拒绝";

    private static final String 分层子命令开 = "开";
    private static final String 分层子命令关 = "关";
    private static final String 分层子命令切换 = "切换";
    private static final String 分层子命令分界 = "分界";
    private static final String 分层子命令乐器 = "乐器";
    private static final String 分层方向左 = "左";
    private static final String 分层方向右 = "右";

    private static final String 钢琴卷帘子命令打开 = "打开";

    private static final String 资源包子命令应用 = "应用";
    private static final String 资源包子命令生成 = "生成";

    private static final String 弯音子命令灵敏度 = "灵敏度";

    private static final String 乐器管理权限 = "xrmm.乐器.管理";
    private static final String 乐器演奏权限 = "xrm.乐器.演奏";
    private static final String 乐器录制权限 = "xrm.乐器.录制";
    private static final String 公共库上传权限 = "xrm.乐器.公共库.上传";
    private static final String 公共库下载权限 = "xrm.乐器.公共库.下载";
    private static final String 公共库评分权限 = "xrm.乐器.公共库.评分";
    private static final String 公共库审核权限 = "xrm.乐器.公共库.审核";
    private static final String 资源包应用权限 = "xrm.乐器.资源包.应用";
    private static final String 资源包生成权限 = "xrm.乐器.资源包.生成";
    private static final String 乐器管理员权限 = "xrm.乐器.管理员";

    private static final String 插件名称 = "XRM";
    private static final String 乐谱目录名 = "乐谱";
    private static final String Nbs导入目录名 = "nbs导入";
    private static final String Nbs导出目录名 = "nbs导出";
    private static final String Midi导入目录名 = "midi导入";
    private static final String Midi导出目录名 = "midi导出";
    private static final String Nbs扩展名 = ".nbs";
    private static final String Midi扩展名 = ".mid";

    private static final int 音域下限 = 0;
    private static final int 音域上限 = 4;
    private static final int 音高下限 = 0;
    private static final int 音高上限 = 59;
    private static final int 弯音范围下限 = 1;
    private static final int 弯音范围上限 = 12;
    private static final int swing百分比下限 = 50;
    private static final int swing百分比上限 = 65;
    private static final double swing开启默认比例 = 0.55;
    private static final double swing关闭比例 = 0.50;
    private static final int 和弦记忆默认间隔 = 10;
    private static final Set<String> 有效力度档标识 = Set.of("pp", "p", "mf", "f", "ff");

    private final 乐器服务 乐器服务;
    private final 乐器注册表 乐器注册表;

    @Inject
    public 乐器命令处理器(翻译服务 翻译服务, 关键词解析器 关键词解析器,
                        乐器服务 乐器服务, 乐器注册表 乐器注册表) {
        super(翻译服务, 关键词解析器);
        this.乐器服务 = 乐器服务;
        this.乐器注册表 = 乐器注册表;
    }

    @Override
    public String 获取命令名() {
        return "乐器";
    }

    /**
     * 检查发送者是否拥有任一权限（任一通过即放行）。
     * 仅当两个权限都没有时才发送"无权限"错误消息，避免有权限时误发错误消息。
     */
    private boolean 检查任一权限(CommandSender 发送者, String 权限A, String 权限B) {
        if (发送者.hasPermission(权限A) || 发送者.hasPermission(权限B)) {
            return true;
        }
        发送错误(发送者, 无权限键);
        return false;
    }

    @Override
    public boolean onCommand(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (!检查玩家(发送者)) {
            return true;
        }

        if (参数.length == 0) {
            发送错误(发送者, 缺少参数键);
            return true;
        }

        String 子命令 = 参数[0];
        return switch (子命令) {
            case 子命令演奏 -> 处理演奏(发送者, 参数);
            case 子命令切换 -> 处理切换(发送者, 参数);
            case 子命令乐谱 -> 处理乐谱(发送者, 参数);
            case 子命令合奏 -> 处理合奏(发送者, 参数);
            case 子命令给予 -> 处理给予(发送者, 参数);
            case 子命令重载 -> 处理重载(发送者);
            case 子命令节拍器 -> 处理节拍器(发送者, 参数);
            case 子命令力度 -> 处理力度(发送者, 参数);
            case 子命令音域 -> 处理音域(发送者, 参数);
            case 子命令量化 -> 处理量化(发送者, 参数);
            case 子命令Swing -> 处理Swing(发送者, 参数);
            case 子命令延音 -> 处理延音(发送者, 参数);
            case 子命令和弦 -> 处理和弦(发送者, 参数);
            case 子命令录音 -> 处理录音(发送者, 参数);
            case 子命令公共库 -> 处理公共库(发送者, 参数);
            case 子命令分层 -> 处理分层(发送者, 参数);
            case 子命令钢琴卷帘 -> 处理钢琴卷帘(发送者, 参数);
            case 子命令资源包 -> 处理资源包(发送者, 参数);
            case 子命令弯音 -> 处理弯音(发送者, 参数);
            default -> {
                发送错误(发送者, 未知子命令键, 子命令);
                yield true;
            }
        };
    }

    private boolean 处理演奏(CommandSender 发送者, String[] 参数) {
        if (!检查任一权限(发送者, 乐器演奏权限, 乐器管理权限)) {
            return true;
        }
        Player 玩家 = (Player) 发送者;
        乐器定义 乐器 = 获取玩家乐器(玩家);
        if (乐器 == null) {
            发送错误(发送者, 默认乐器未配置键);
            return true;
        }
        乐器服务.打开演奏界面(玩家, 乐器);
        return true;
    }

    private boolean 处理切换(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送错误(发送者, 缺少参数键);
            return true;
        }
        Optional<乐器定义> 乐器可选 = 乐器注册表.按名称查找(参数[1]);
        if (乐器可选.isEmpty()) {
            发送错误(发送者, 音色不存在键, 参数[1]);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        乐器服务.切换乐器(玩家, 乐器可选.get());
        return true;
    }

    private boolean 处理乐谱(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送乐谱列表(发送者);
            return true;
        }
        String 乐谱子命令 = 参数[1];
        return switch (乐谱子命令) {
            case 乐谱子命令列表 -> {
                发送乐谱列表(发送者);
                yield true;
            }
            case 乐谱子命令播放 -> 处理乐谱播放(发送者, 参数);
            case 乐谱子命令删除 -> 处理乐谱删除(发送者, 参数);
            case 乐谱子命令导入 -> 处理Nbs导入(发送者, 参数);
            case 乐谱子命令导出 -> 处理Nbs导出(发送者, 参数);
            case 乐谱子命令Midi导入 -> 处理Midi导入(发送者, 参数);
            case 乐谱子命令Midi导出 -> 处理Midi导出(发送者, 参数);
            default -> {
                发送错误(发送者, 未知子命令键, 乐谱子命令);
                yield true;
            }
        };
    }

    private void 发送乐谱列表(CommandSender 发送者) {
        Player 玩家 = (Player) 发送者;
        List<String> 乐谱列表 = 乐器服务.获取乐谱列表(玩家.getUniqueId());
        if (乐谱列表.isEmpty()) {
            发送消息(发送者, 乐谱列表为空键);
            return;
        }
        StringBuilder 列表文本 = new StringBuilder();
        String 后缀 = 翻译服务.获取(音符计数后缀键);
        for (int i = 0; i < 乐谱列表.size(); i++) {
            String 乐谱名 = 乐谱列表.get(i);
            if (i > 0) {
                列表文本.append(", ");
            }
            int 音符数 = 乐器服务.加载乐谱(玩家.getUniqueId(), 乐谱名)
                    .map(乐谱 -> 乐谱.获取音符数量())
                    .orElse(0);
            String 条目 = 翻译服务.获取(曲目条目键, 乐谱名, 音符数, 后缀);
            列表文本.append(条目);
        }
        发送消息(发送者, 你的乐谱键, 列表文本.toString());
    }

    private boolean 处理乐谱播放(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送错误(发送者, 缺少参数键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 乐谱名 = 参数[2];
        var 乐谱可选 = 乐器服务.加载乐谱(玩家.getUniqueId(), 乐谱名);
        if (乐谱可选.isEmpty()) {
            发送错误(发送者, 乐谱不存在键, 乐谱名);
            return true;
        }
        乐器服务.播放乐谱(玩家, 乐谱可选.get());
        return true;
    }

    private boolean 处理乐谱删除(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送错误(发送者, 缺少参数键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 乐谱名 = 参数[2];
        if (!乐器服务.删除乐谱(玩家.getUniqueId(), 乐谱名)) {
            发送错误(发送者, 乐谱不存在键, 乐谱名);
            return true;
        }
        发送消息(发送者, 乐谱删除成功键, 乐谱名);
        return true;
    }

    private boolean 处理合奏(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送消息(发送者, 合奏用法键);
            return true;
        }
        String 合奏子命令 = 参数[1];
        return switch (合奏子命令) {
            case 合奏子命令开始 -> 处理合奏开始(发送者, 参数);
            case 合奏子命令确认 -> 处理合奏确认(发送者);
            case 合奏子命令拒绝 -> 处理合奏拒绝(发送者);
            case 合奏子命令停止 -> 处理合奏停止(发送者);
            case 合奏子命令状态 -> 处理合奏状态(发送者);
            default -> {
                发送错误(发送者, 未知子命令键, 合奏子命令);
                yield true;
            }
        };
    }

    private boolean 处理合奏开始(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送消息(发送者, 合奏用法键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 乐谱名 = 参数[2];
        if (!乐器服务.开始合奏(玩家.getUniqueId(), 乐谱名)) {
            发送错误(发送者, 合奏无法开始错误键);
            return true;
        }
        发送消息(发送者, 合奏准备检查已发送键);
        return true;
    }

    private boolean 处理合奏确认(CommandSender 发送者) {
        Player 玩家 = (Player) 发送者;
        if (!乐器服务.确认合奏(玩家.getUniqueId())) {
            发送错误(发送者, 合奏无待确认错误键);
            return true;
        }
        发送消息(发送者, 合奏已确认准备键);
        return true;
    }

    private boolean 处理合奏拒绝(CommandSender 发送者) {
        Player 玩家 = (Player) 发送者;
        if (!乐器服务.拒绝合奏(玩家.getUniqueId())) {
            发送错误(发送者, 合奏无待拒绝错误键);
            return true;
        }
        return true;
    }

    private boolean 处理合奏停止(CommandSender 发送者) {
        Player 玩家 = (Player) 发送者;
        if (!乐器服务.停止合奏(玩家.getUniqueId())) {
            发送错误(发送者, 合奏无可停止错误键);
            return true;
        }
        发送消息(发送者, 合奏已停止键);
        return true;
    }

    private boolean 处理合奏状态(CommandSender 发送者) {
        Player 玩家 = (Player) 发送者;
        Optional<合奏状态> 状态可选 = 乐器服务.获取合奏状态(玩家.getUniqueId());
        if (状态可选.isEmpty()) {
            发送消息(发送者, 合奏无进行中键);
            return true;
        }
        合奏状态 状态 = 状态可选.get();
        发送消息(发送者, 合奏状态标题键);
        String 曲目名 = 状态.曲目名();
        if (曲目名 == null || 曲目名.isBlank()) {
            曲目名 = 翻译服务.获取(合奏无曲目键);
        }
        发送消息(发送者, 合奏曲目键, 曲目名);
        发送消息(发送者, 合奏BPM键, 状态.BPM());
        String 状态文本 = 状态.是否演奏中() ? 翻译服务.获取(合奏演奏中键) : 翻译服务.获取(合奏准备中键);
        发送消息(发送者, 合奏状态键, 状态文本);
        String 准备进度 = 翻译服务.获取(合奏准备进度键, 状态.已准备人数(), 状态.总人数());
        发送消息(发送者, 合奏准备键, 准备进度, "");
        return true;
    }

    private boolean 处理给予(CommandSender 发送者, String[] 参数) {
        if (!检查任一权限(发送者, 乐器管理权限, 乐器管理员权限)) {
            return true;
        }
        if (参数.length < 3) {
            发送消息(发送者, 给予用法键);
            return true;
        }
        String 目标名 = 参数[1];
        String 乐器名 = 参数[2];
        Player 目标 = Bukkit.getPlayerExact(目标名);
        if (目标 == null) {
            发送错误(发送者, 玩家不在线键, 目标名);
            return true;
        }
        Optional<乐器定义> 乐器可选 = 乐器注册表.按名称查找(乐器名);
        if (乐器可选.isEmpty()) {
            发送错误(发送者, 音色不存在键, 乐器名);
            return true;
        }
        乐器定义 乐器 = 乐器可选.get();
        乐器服务.给予乐器(目标, 乐器);
        发送消息(发送者, 给予成功键, 目标名, 乐器.获取显示名称());
        return true;
    }

    private boolean 处理重载(CommandSender 发送者) {
        if (!检查任一权限(发送者, 乐器管理权限, 乐器管理员权限)) {
            return true;
        }
        乐器服务.重载配置();
        发送消息(发送者, 重载成功键);
        return true;
    }

    private boolean 处理节拍器(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送消息(发送者, 节拍器用法键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 节拍器子命令 = 参数[1];
        return switch (节拍器子命令) {
            case 节拍器子命令开 -> {
                乐器服务.设置节拍器开关(玩家.getUniqueId(), true);
                发送消息(发送者, 节拍器开关已开启键);
                yield true;
            }
            case 节拍器子命令关 -> {
                乐器服务.设置节拍器开关(玩家.getUniqueId(), false);
                发送消息(发送者, 节拍器开关已关闭键);
                yield true;
            }
            case 节拍器子命令CountIn -> 处理节拍器CountIn(发送者, 参数);
            default -> {
                发送错误(发送者, 未知子命令键, 节拍器子命令);
                yield true;
            }
        };
    }

    private boolean 处理节拍器CountIn(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送消息(发送者, 节拍器CountIn用法键);
            return true;
        }
        int 小节数;
        try {
            小节数 = Integer.parseInt(参数[2]);
        } catch (NumberFormatException e) {
            发送错误(发送者, 节拍器CountIn非法值错误键, 参数[2]);
            return true;
        }
        if (小节数 < 0 || 小节数 > 2) {
            发送错误(发送者, 节拍器CountIn非法值错误键, String.valueOf(小节数));
            return true;
        }
        乐器服务.设置CountIn小节数(小节数);
        发送消息(发送者, 节拍器CountIn已设置键, 小节数);
        return true;
    }

    // ===== FP-16 新增子命令处理 =====

    private boolean 处理力度(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送消息(发送者, 力度用法键);
            return true;
        }
        String 子参数 = 参数[1];
        return switch (子参数) {
            case 力度子命令真实 -> 处理真实力度(发送者, 参数);
            case 力度子命令音域 -> 处理力度音域(发送者, 参数);
            default -> {
                String 小写 = 子参数.trim().toLowerCase();
                if (!有效力度档标识.contains(小写)) {
                    发送错误(发送者, 力度档无效键, 子参数);
                    yield true;
                }
                力度档 档 = 力度档.按标识(小写);
                Player 玩家 = (Player) 发送者;
                乐器服务.设置玩家力度档(玩家.getUniqueId(), 档);
                发送消息(发送者, 力度已设置键, 档.获取标识());
                yield true;
            }
        };
    }

    private boolean 处理真实力度(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送错误(发送者, 力度真实开关无效键, "");
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 开关值 = 参数[2];
        return switch (开关值) {
            case 开关参数开 -> {
                乐器服务.设置玩家真实力度开关(玩家.getUniqueId(), true);
                发送消息(发送者, 服务真实力度已开启键);
                yield true;
            }
            case 开关参数关 -> {
                乐器服务.设置玩家真实力度开关(玩家.getUniqueId(), false);
                发送消息(发送者, 服务真实力度已关闭键);
                yield true;
            }
            default -> {
                发送错误(发送者, 力度真实开关无效键, 开关值);
                yield true;
            }
        };
    }

    private boolean 处理力度音域(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送错误(发送者, 音域越界键, "");
            return true;
        }
        return 设置音域(发送者, 参数[2]);
    }

    private boolean 处理音域(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送消息(发送者, 音域用法键);
            return true;
        }
        return 设置音域(发送者, 参数[1]);
    }

    private boolean 设置音域(CommandSender 发送者, String 值) {
        int 音域;
        try {
            音域 = Integer.parseInt(值);
        } catch (NumberFormatException e) {
            发送错误(发送者, 音域越界键, 值);
            return true;
        }
        if (音域 < 音域下限 || 音域 > 音域上限) {
            发送错误(发送者, 音域越界键, String.valueOf(音域));
            return true;
        }
        Player 玩家 = (Player) 发送者;
        乐器服务.设置玩家音域(玩家.getUniqueId(), 音域);
        发送消息(发送者, 音域已设置键, 音域);
        return true;
    }

    private boolean 处理量化(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送消息(发送者, 量化用法键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 子参数 = 参数[1];
        return switch (子参数) {
            case 量子命令撤销 -> {
                if (!乐器服务.撤销量化(玩家.getUniqueId())) {
                    发送错误(发送者, 服务无可量化乐谱键);
                } else {
                    发送消息(发送者, 服务量化已撤销键);
                }
                yield true;
            }
            case 量子命令预览 -> {
                boolean 已量化 = 乐器服务.是否已量化(玩家.getUniqueId());
                if (已量化) {
                    发送消息(发送者, 量化预览已量化键);
                } else {
                    发送消息(发送者, 量化预览未量化键);
                }
                yield true;
            }
            default -> {
                量化档位 档 = 查找量化档位(子参数);
                if (档 == null) {
                    发送错误(发送者, 量化档位无效键, 子参数);
                    yield true;
                }
                乐器服务.量化乐谱(玩家.getUniqueId(), 档);
                发送消息(发送者, 服务量化已应用键, 档.获取标识());
                yield true;
            }
        };
    }

    private 量化档位 查找量化档位(String 标识) {
        if (标识 == null || 标识.isBlank()) {
            return null;
        }
        String 修剪 = 标识.trim();
        for (量化档位 档 : 量化档位.values()) {
            if (档.获取标识().equals(修剪)) {
                return 档;
            }
        }
        return null;
    }

    private boolean 处理Swing(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送消息(发送者, swing用法键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 子参数 = 参数[1];
        return switch (子参数) {
            case 开关参数开 -> {
                乐器服务.设置Swing(玩家.getUniqueId(), swing开启默认比例);
                发送消息(发送者, swing已开启键);
                yield true;
            }
            case 开关参数关 -> {
                乐器服务.设置Swing(玩家.getUniqueId(), swing关闭比例);
                发送消息(发送者, swing已关闭键);
                yield true;
            }
            default -> {
                int 百分比;
                try {
                    百分比 = Integer.parseInt(子参数);
                } catch (NumberFormatException e) {
                    发送错误(发送者, swing值无效键, 子参数);
                    yield true;
                }
                if (百分比 < swing百分比下限 || 百分比 > swing百分比上限) {
                    发送错误(发送者, swing值越界键, String.valueOf(百分比));
                    yield true;
                }
                double 比例 = 百分比 / 100.0;
                乐器服务.设置Swing(玩家.getUniqueId(), 比例);
                发送消息(发送者, 服务swing已设置键, String.valueOf(百分比));
                yield true;
            }
        };
    }

    private boolean 处理延音(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送消息(发送者, 延音用法键);
            return true;
        }
        String 模式参数 = 参数[1];
        延音模式 模式;
        return switch (模式参数) {
            case 延音参数按住 -> {
                模式 = 延音模式.按住;
                Player 玩家 = (Player) 发送者;
                乐器服务.设置延音模式(玩家.getUniqueId(), 模式);
                发送消息(发送者, 延音已设置键, 模式参数);
                yield true;
            }
            case 延音参数切换 -> {
                模式 = 延音模式.切换;
                Player 玩家 = (Player) 发送者;
                乐器服务.设置延音模式(玩家.getUniqueId(), 模式);
                发送消息(发送者, 延音已设置键, 模式参数);
                yield true;
            }
            default -> {
                发送错误(发送者, 延音模式无效键, 模式参数);
                yield true;
            }
        };
    }

    private boolean 处理和弦(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送消息(发送者, 和弦用法键);
            return true;
        }
        String 子参数 = 参数[1];
        if (子参数.equals(和弦子命令列表)) {
            return 处理和弦列表(发送者);
        }
        if (子参数.equals(和弦子命令清除)) {
            Player 玩家 = (Player) 发送者;
            乐器服务.停止和弦记忆(玩家.getUniqueId());
            发送消息(发送者, 服务和弦记忆已停止键);
            return true;
        }
        // 格式: /乐器 和弦 <根音> <类型> [记忆]
        return 处理和弦播放(发送者, 参数);
    }

    private boolean 处理和弦列表(CommandSender 发送者) {
        发送消息(发送者, 和弦列表标题键);
        for (和弦类型 类型 : 和弦类型.values()) {
            发送消息(发送者, 和弦列表项键, 类型.获取标识(), 类型.获取音程().toString());
        }
        return true;
    }

    private boolean 处理和弦播放(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送消息(发送者, 和弦用法键);
            return true;
        }
        int 根音;
        try {
            根音 = Integer.parseInt(参数[1]);
        } catch (NumberFormatException e) {
            发送错误(发送者, 和弦根音无效键, 参数[1]);
            return true;
        }
        if (根音 < 音高下限 || 根音 > 音高上限) {
            发送错误(发送者, 和弦根音无效键, String.valueOf(根音));
            return true;
        }
        和弦类型 类型 = 和弦类型.按标识查找(参数[2]);
        if (类型 == null) {
            发送错误(发送者, 和弦类型无效键, 参数[2]);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        乐器定义 乐器 = 获取玩家乐器(玩家);
        if (乐器 == null) {
            发送错误(发送者, 默认乐器未配置键);
            return true;
        }
        力度档 档 = 乐器服务.获取玩家力度档(玩家.getUniqueId());
        boolean 启动记忆 = 参数.length >= 4 && 参数[3].equals(和弦参数记忆);
        if (启动记忆) {
            乐器服务.启动和弦记忆(玩家, 乐器, 根音, 类型, 档, 和弦记忆默认间隔);
            发送消息(发送者, 服务和弦记忆已启动键, 类型.获取标识());
        } else {
            乐器服务.播放和弦(玩家, 乐器, 根音, 类型, 档);
            发送消息(发送者, 服务和弦已播放键, 类型.获取标识());
        }
        return true;
    }

    private boolean 处理录音(CommandSender 发送者, String[] 参数) {
        if (!检查任一权限(发送者, 乐器录制权限, 乐器管理权限)) {
            return true;
        }
        if (参数.length < 2) {
            发送消息(发送者, 录音用法键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 子参数 = 参数[1];
        return switch (子参数) {
            case 录音子命令开始 -> {
                乐器定义 乐器 = 获取玩家乐器(玩家);
                if (乐器 == null) {
                    发送错误(发送者, 默认乐器未配置键);
                    yield true;
                }
                乐器服务.开始录制(玩家.getUniqueId(), 乐器);
                发送消息(发送者, 录音已开始键);
                yield true;
            }
            case 录音子命令停止 -> {
                乐器服务.停止录制(玩家.getUniqueId());
                发送消息(发送者, 录音已停止键);
                yield true;
            }
            case 录音子命令保存 -> 处理录音保存(发送者, 参数);
            case 录音子命令预览 -> {
                boolean 进行中 = 乐器服务.是否录制中(玩家.getUniqueId());
                if (进行中) {
                    发送消息(发送者, 录音预览进行中键);
                } else {
                    发送消息(发送者, 录音预览未进行键);
                }
                yield true;
            }
            case 录音子命令撤销量化 -> {
                if (!乐器服务.撤销量化(玩家.getUniqueId())) {
                    发送错误(发送者, 服务无可量化乐谱键);
                } else {
                    发送消息(发送者, 服务量化已撤销键);
                }
                yield true;
            }
            default -> {
                发送错误(发送者, 未知子命令键, 子参数);
                yield true;
            }
        };
    }

    private boolean 处理录音保存(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送消息(发送者, 录音保存用法键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 乐谱名 = 参数[2];
        乐谱保存结果 结果 = 乐器服务.保存乐谱带结果(玩家.getUniqueId(), 乐谱名);
        return switch (结果) {
            case 成功 -> {
                发送消息(发送者, 服务乐谱保存成功键, 乐谱名);
                yield true;
            }
            case 重名 -> {
                发送消息(发送者, 服务乐谱已存在键, 乐谱名);
                yield true;
            }
            case 超限 -> {
                发送错误(发送者, 服务乐谱超限键, String.valueOf(乐器服务.获取最大乐谱存储数()));
                yield true;
            }
            case 无录制 -> {
                发送错误(发送者, 服务无录制内容键);
                yield true;
            }
            case 参数无效 -> {
                发送错误(发送者, 缺少参数键);
                yield true;
            }
        };
    }

    private boolean 处理公共库(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送消息(发送者, 公共库用法键);
            return true;
        }
        String 子参数 = 参数[1];
        return switch (子参数) {
            case 公共库子命令列表 -> 处理公共库列表(发送者);
            case 公共库子命令搜索 -> 处理公共库搜索(发送者, 参数);
            case 公共库子命令上传 -> 处理公共库上传(发送者, 参数);
            case 公共库子命令下载 -> 处理公共库下载(发送者, 参数);
            case 公共库子命令评分 -> 处理公共库评分(发送者, 参数);
            case 公共库子命令审核 -> 处理公共库审核(发送者, 参数);
            default -> {
                发送错误(发送者, 未知子命令键, 子参数);
                yield true;
            }
        };
    }

    private boolean 处理公共库列表(CommandSender 发送者) {
        List<公共乐谱> 列表 = 乐器服务.列出公共乐谱();
        if (列表.isEmpty()) {
            发送消息(发送者, 服务公共乐谱列表为空键);
            return true;
        }
        for (公共乐谱 公共 : 列表) {
            发送消息(发送者, 服务公共乐谱列表项键,
                    公共.获取原乐谱().获取名称(),
                    公共.获取上传者名(),
                    String.format("%.1f", 公共.获取平均评分()),
                    公共.获取下载次数());
        }
        return true;
    }

    private boolean 处理公共库搜索(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送消息(发送者, 公共库搜索用法键);
            return true;
        }
        String 关键词 = 参数[2].toLowerCase();
        List<公共乐谱> 全部 = 乐器服务.列出公共乐谱();
        List<公共乐谱> 匹配 = new ArrayList<>();
        for (公共乐谱 公共 : 全部) {
            String 名称 = 公共.获取原乐谱().获取名称();
            String 上传者 = 公共.获取上传者名();
            if ((名称 != null && 名称.toLowerCase().contains(关键词)) ||
                    (上传者 != null && 上传者.toLowerCase().contains(关键词))) {
                匹配.add(公共);
            }
        }
        if (匹配.isEmpty()) {
            发送消息(发送者, 公共库搜索结果为空键);
            return true;
        }
        for (公共乐谱 公共 : 匹配) {
            发送消息(发送者, 服务公共乐谱列表项键,
                    公共.获取原乐谱().获取名称(),
                    公共.获取上传者名(),
                    String.format("%.1f", 公共.获取平均评分()),
                    公共.获取下载次数());
        }
        return true;
    }

    private boolean 处理公共库上传(CommandSender 发送者, String[] 参数) {
        if (!检查任一权限(发送者, 公共库上传权限, 乐器管理权限)) {
            return true;
        }
        if (参数.length < 4) {
            发送消息(发送者, 公共库上传用法键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 本地乐谱名 = 参数[2];
        String 公开名 = 参数[3];
        上传结果 结果 = 乐器服务.上传公共乐谱(玩家, 本地乐谱名, 公开名);
        return switch (结果) {
            case 成功 -> {
                发送消息(发送者, 服务公共乐谱上传成功键, 公开名);
                yield true;
            }
            case 重名 -> {
                发送错误(发送者, 服务公共乐谱上传重名键);
                yield true;
            }
            case 参数无效 -> {
                发送错误(发送者, 服务公共乐谱上传参数无效键);
                yield true;
            }
        };
    }

    private boolean 处理公共库下载(CommandSender 发送者, String[] 参数) {
        if (!检查任一权限(发送者, 公共库下载权限, 乐器管理权限)) {
            return true;
        }
        if (参数.length < 3) {
            发送消息(发送者, 公共库下载用法键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 乐谱ID = 参数[2];
        下载结果 结果 = 乐器服务.下载公共乐谱(玩家, 乐谱ID);
        return switch (结果) {
            case 成功 -> {
                发送消息(发送者, 服务公共乐谱下载成功键, 乐谱ID);
                yield true;
            }
            case 不存在 -> {
                发送错误(发送者, 服务公共乐谱不存在键, 乐谱ID);
                yield true;
            }
            case 未通过审核 -> {
                发送错误(发送者, 服务公共乐谱未通过审核键, 乐谱ID);
                yield true;
            }
            case 参数无效 -> {
                发送错误(发送者, 缺少参数键);
                yield true;
            }
        };
    }

    private boolean 处理公共库评分(CommandSender 发送者, String[] 参数) {
        if (!检查任一权限(发送者, 公共库评分权限, 乐器管理权限)) {
            return true;
        }
        if (参数.length < 4) {
            发送消息(发送者, 公共库评分用法键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 乐谱ID = 参数[2];
        int 评分;
        try {
            评分 = Integer.parseInt(参数[3]);
        } catch (NumberFormatException e) {
            发送错误(发送者, 公共库评分值无效键, 参数[3]);
            return true;
        }
        评分结果 结果 = 乐器服务.评分公共乐谱(玩家, 乐谱ID, 评分);
        return switch (结果) {
            case 成功 -> {
                发送消息(发送者, 服务公共乐谱评分成功键, 乐谱ID);
                yield true;
            }
            case 不存在 -> {
                发送错误(发送者, 服务公共乐谱不存在键, 乐谱ID);
                yield true;
            }
            case 未通过审核 -> {
                发送错误(发送者, 服务公共乐谱未通过审核键, 乐谱ID);
                yield true;
            }
            case 已评分 -> {
                发送错误(发送者, 服务公共乐谱已评分键, 乐谱ID);
                yield true;
            }
            case 评分越界 -> {
                发送错误(发送者, 服务公共乐谱评分越界键);
                yield true;
            }
            case 参数无效 -> {
                发送错误(发送者, 缺少参数键);
                yield true;
            }
        };
    }

    private boolean 处理公共库审核(CommandSender 发送者, String[] 参数) {
        if (!检查任一权限(发送者, 公共库审核权限, 乐器管理权限)) {
            return true;
        }
        if (参数.length < 4) {
            发送消息(发送者, 公共库审核用法键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 乐谱ID = 参数[2];
        String 审核动作 = 参数[3];
        审核状态 目标状态;
        return switch (审核动作) {
            case 公共库审核通过 -> {
                目标状态 = 审核状态.已通过;
                yield 执行审核(发送者, 玩家, 乐谱ID, 目标状态);
            }
            case 公共库审核拒绝 -> {
                目标状态 = 审核状态.已拒绝;
                yield 执行审核(发送者, 玩家, 乐谱ID, 目标状态);
            }
            default -> {
                发送错误(发送者, 公共库审核状态无效键, 审核动作);
                yield true;
            }
        };
    }

    private boolean 执行审核(CommandSender 发送者, Player 玩家, String 乐谱ID, 审核状态 状态) {
        审核结果 结果 = 乐器服务.审核公共乐谱(玩家, 乐谱ID, 状态);
        return switch (结果) {
            case 成功 -> {
                发送消息(发送者, 服务公共乐谱审核成功键, 乐谱ID, 状态.name());
                yield true;
            }
            case 不存在 -> {
                发送错误(发送者, 服务公共乐谱不存在键, 乐谱ID);
                yield true;
            }
            case 状态无效 -> {
                发送错误(发送者, 服务公共乐谱状态无效键, 乐谱ID);
                yield true;
            }
            case 参数无效 -> {
                发送错误(发送者, 缺少参数键);
                yield true;
            }
        };
    }

    private boolean 处理分层(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送消息(发送者, 分层用法键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 子参数 = 参数[1];
        return switch (子参数) {
            case 分层子命令开 -> {
                乐器服务.设置分层模式(玩家.getUniqueId(), true);
                发送消息(发送者, 服务分层已开启键);
                yield true;
            }
            case 分层子命令关 -> {
                乐器服务.设置分层模式(玩家.getUniqueId(), false);
                发送消息(发送者, 服务分层已关闭键);
                yield true;
            }
            case 分层子命令切换 -> {
                boolean 启用 = 乐器服务.切换分层模式(玩家.getUniqueId());
                if (启用) {
                    发送消息(发送者, 服务分层已开启键);
                } else {
                    发送消息(发送者, 服务分层已关闭键);
                }
                yield true;
            }
            case 分层子命令分界 -> 处理分层分界(发送者, 参数);
            case 分层子命令乐器 -> 处理分层乐器(发送者, 参数);
            default -> {
                发送错误(发送者, 未知子命令键, 子参数);
                yield true;
            }
        };
    }

    private boolean 处理分层分界(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送错误(发送者, 分界点越界键, "");
            return true;
        }
        int 分界点;
        try {
            分界点 = Integer.parseInt(参数[2]);
        } catch (NumberFormatException e) {
            发送错误(发送者, 分界点越界键, 参数[2]);
            return true;
        }
        if (分界点 < 音高下限 || 分界点 > 音高上限) {
            发送错误(发送者, 分界点越界键, String.valueOf(分界点));
            return true;
        }
        Player 玩家 = (Player) 发送者;
        乐器服务.设置分界点(玩家.getUniqueId(), 分界点);
        发送消息(发送者, 服务分界点已设置键, 分界点);
        return true;
    }

    private boolean 处理分层乐器(CommandSender 发送者, String[] 参数) {
        if (参数.length < 4) {
            发送错误(发送者, 分层方向无效键, "");
            return true;
        }
        String 方向 = 参数[2];
        String 乐器名 = 参数[3];
        Optional<乐器定义> 乐器可选 = 乐器注册表.按名称查找(乐器名);
        if (乐器可选.isEmpty()) {
            发送错误(发送者, 音色不存在键, 乐器名);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        return switch (方向) {
            case 分层方向左 -> {
                乐器服务.设置分层左乐器(玩家.getUniqueId(), 乐器可选.get());
                发送消息(发送者, 服务分层乐器已设置键);
                yield true;
            }
            case 分层方向右 -> {
                乐器服务.设置分层右乐器(玩家.getUniqueId(), 乐器可选.get());
                发送消息(发送者, 服务分层乐器已设置键);
                yield true;
            }
            default -> {
                发送错误(发送者, 分层方向无效键, 方向);
                yield true;
            }
        };
    }

    private boolean 处理钢琴卷帘(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送消息(发送者, 钢琴卷帘用法键);
            return true;
        }
        if (!参数[1].equals(钢琴卷帘子命令打开)) {
            发送错误(发送者, 未知子命令键, 参数[1]);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 乐谱名 = 参数[2];
        if (!乐器服务.打开PianoRoll编辑器(玩家, 乐谱名)) {
            发送错误(发送者, 服务乐谱不存在键, 乐谱名);
            return true;
        }
        发送消息(发送者, 服务pianoRoll已打开键, 乐谱名);
        return true;
    }

    private boolean 处理资源包(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送消息(发送者, 资源包用法键);
            return true;
        }
        String 子参数 = 参数[1];
        Player 玩家 = (Player) 发送者;
        return switch (子参数) {
            case 资源包子命令应用 -> {
                if (!检查任一权限(发送者, 资源包应用权限, 乐器管理权限)) {
                    yield true;
                }
                if (乐器服务.应用资源包(玩家)) {
                    发送消息(发送者, 服务资源包应用提示键);
                } else {
                    发送错误(发送者, 资源包应用失败键);
                }
                yield true;
            }
            case 资源包子命令生成 -> {
                if (!检查任一权限(发送者, 资源包生成权限, 乐器管理权限)) {
                    yield true;
                }
                if (乐器服务.生成资源包()) {
                    发送消息(发送者, 资源包生成成功键);
                } else {
                    发送错误(发送者, 资源包生成失败键);
                }
                yield true;
            }
            default -> {
                发送错误(发送者, 未知子命令键, 子参数);
                yield true;
            }
        };
    }

    private boolean 处理弯音(CommandSender 发送者, String[] 参数) {
        if (参数.length < 2) {
            发送消息(发送者, 弯音用法键);
            return true;
        }
        if (!参数[1].equals(弯音子命令灵敏度)) {
            发送错误(发送者, 未知子命令键, 参数[1]);
            return true;
        }
        if (参数.length < 3) {
            发送错误(发送者, 弯音范围越界键, "");
            return true;
        }
        int 半音数;
        try {
            半音数 = Integer.parseInt(参数[2]);
        } catch (NumberFormatException e) {
            发送错误(发送者, 弯音范围越界键, 参数[2]);
            return true;
        }
        if (半音数 < 弯音范围下限 || 半音数 > 弯音范围上限) {
            发送错误(发送者, 弯音范围越界键, String.valueOf(半音数));
            return true;
        }
        Player 玩家 = (Player) 发送者;
        乐器服务.设置弯音范围(玩家.getUniqueId(), 半音数);
        发送消息(发送者, 弯音范围已设置键, 半音数);
        return true;
    }

    private boolean 处理Nbs导入(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送错误(发送者, 缺少参数键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 文件名 = 参数[2];
        File 文件 = 构建导入文件(玩家, Nbs导入目录名, 文件名, Nbs扩展名);
        if (文件 == null || !文件.canRead()) {
            发送错误(发送者, 服务文件不可读键, 文件名);
            return true;
        }
        String 乐谱名 = 文件名;
        Optional<mljy.领域层.乐器.乐谱> 结果 = 乐器服务.导入NBS(玩家, 文件, 乐谱名);
        if (结果.isEmpty()) {
            发送错误(发送者, 服务NBS导入失败键, 文件名);
        } else {
            发送消息(发送者, 服务NBS导入成功键, 乐谱名);
        }
        return true;
    }

    private boolean 处理Nbs导出(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送错误(发送者, 缺少参数键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 乐谱名 = 参数[2];
        File 文件 = 构建导出文件(玩家, Nbs导出目录名, 乐谱名, Nbs扩展名);
        if (文件 == null) {
            发送错误(发送者, 服务NBS导出失败键, 乐谱名);
            return true;
        }
        if (乐器服务.导出NBS(玩家.getUniqueId(), 乐谱名, 文件)) {
            发送消息(发送者, 服务NBS导出成功键, 文件.getAbsolutePath());
        } else {
            发送错误(发送者, 服务NBS导出失败键, 乐谱名);
        }
        return true;
    }

    private boolean 处理Midi导入(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送错误(发送者, 缺少参数键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 文件名 = 参数[2];
        File 文件 = 构建导入文件(玩家, Midi导入目录名, 文件名, Midi扩展名);
        if (文件 == null || !文件.canRead()) {
            发送错误(发送者, 服务文件不可读键, 文件名);
            return true;
        }
        String 乐谱名 = 文件名;
        Optional<mljy.领域层.乐器.乐谱> 结果 = 乐器服务.导入MIDI(玩家, 文件, 乐谱名);
        if (结果.isEmpty()) {
            发送错误(发送者, 服务MIDI导入失败键, 文件名);
        } else {
            发送消息(发送者, 服务MIDI导入成功键, 乐谱名);
        }
        return true;
    }

    private boolean 处理Midi导出(CommandSender 发送者, String[] 参数) {
        if (参数.length < 3) {
            发送错误(发送者, 缺少参数键);
            return true;
        }
        Player 玩家 = (Player) 发送者;
        String 乐谱名 = 参数[2];
        File 文件 = 构建导出文件(玩家, Midi导出目录名, 乐谱名, Midi扩展名);
        if (文件 == null) {
            发送错误(发送者, 服务MIDI导出失败键, 乐谱名);
            return true;
        }
        if (乐器服务.导出MIDI(玩家.getUniqueId(), 乐谱名, 文件)) {
            发送消息(发送者, 服务MIDI导出成功键, 文件.getAbsolutePath());
        } else {
            发送错误(发送者, 服务MIDI导出失败键, 乐谱名);
        }
        return true;
    }

    private 乐器定义 获取玩家乐器(Player 玩家) {
        乐器定义 乐器 = 乐器服务.获取玩家当前乐器(玩家.getUniqueId()).orElse(null);
        if (乐器 == null) {
            乐器 = 乐器注册表.默认乐器().orElse(null);
        }
        return 乐器;
    }

    private File 构建导入文件(Player 玩家, String 子目录名, String 文件名, String 扩展名) {
        if (!是安全文件名(文件名)) {
            return null;
        }
        File 玩家目录 = 获取玩家乐谱目录(玩家);
        if (玩家目录 == null) {
            return null;
        }
        File 导入目录 = new File(玩家目录, 子目录名);
        String 实际文件名 = 文件名.endsWith(扩展名) ? 文件名 : 文件名 + 扩展名;
        File 目标文件 = new File(导入目录, 实际文件名);
        if (!是路径在目录内(目标文件, 导入目录)) {
            return null;
        }
        return 目标文件;
    }

    private File 构建导出文件(Player 玩家, String 子目录名, String 乐谱名, String 扩展名) {
        if (!是安全文件名(乐谱名)) {
            return null;
        }
        File 玩家目录 = 获取玩家乐谱目录(玩家);
        if (玩家目录 == null) {
            return null;
        }
        File 导出目录 = new File(玩家目录, 子目录名);
        if (!导出目录.exists() && !导出目录.mkdirs()) {
            return null;
        }
        String 实际文件名 = 乐谱名.endsWith(扩展名) ? 乐谱名 : 乐谱名 + 扩展名;
        File 目标文件 = new File(导出目录, 实际文件名);
        if (!是路径在目录内(目标文件, 导出目录)) {
            return null;
        }
        return 目标文件;
    }

    /**
     * 校验文件名是否安全：仅允许字母、数字、下划线、连字符、点；
     * 禁止路径分隔符、连续点（防 ../ 穿越）、绝对路径前缀。
     */
    private boolean 是安全文件名(String 文件名) {
        if (文件名 == null || 文件名.isBlank()) {
            return false;
        }
        if (文件名.contains("..") || 文件名.contains("/") || 文件名.contains("\\")
                || 文件名.contains(File.separator)) {
            return false;
        }
        for (int i = 0; i < 文件名.length(); i++) {
            char c = 文件名.charAt(i);
            boolean 合法 = Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.';
            if (!合法) {
                return false;
            }
        }
        return true;
    }

    /**
     * 二次校验：目标文件的规范路径必须以父目录的规范路径为前缀（防符号链接/相对路径穿越）。
     */
    private boolean 是路径在目录内(File 目标文件, File 父目录) {
        try {
            String 目标规范路径 = 目标文件.getCanonicalPath();
            String 父目录规范路径 = 父目录.getCanonicalPath();
            return 目标规范路径.startsWith(父目录规范路径 + File.separator);
        } catch (java.io.IOException e) {
            return false;
        }
    }

    private File 获取玩家乐谱目录(Player 玩家) {
        Plugin 插件 = Bukkit.getPluginManager().getPlugin(插件名称);
        if (插件 == null) {
            return null;
        }
        return new File(new File(插件.getDataFolder(), 乐谱目录名), 玩家.getUniqueId().toString());
    }

    @Override
    public List<String> onTabComplete(CommandSender 发送者, Command 命令, String 标签, String[] 参数) {
        if (参数.length == 1) {
            return Arrays.asList(子命令演奏, 子命令切换, 子命令乐谱, 子命令合奏, 子命令给予,
                    子命令重载, 子命令节拍器, 子命令力度, 子命令音域, 子命令量化,
                    子命令Swing, 子命令延音, 子命令和弦, 子命令录音, 子命令公共库,
                    子命令分层, 子命令钢琴卷帘, 子命令资源包, 子命令弯音);
        }
        String 主命令 = 参数[0];
        if (参数.length == 2) {
            return 补全二级子命令(主命令);
        }
        if (参数.length >= 3) {
            return 补全三级及以上(主命令, 参数, 发送者);
        }
        return Collections.emptyList();
    }

    private List<String> 补全二级子命令(String 主命令) {
        return switch (主命令) {
            case 子命令乐谱 -> Arrays.asList(乐谱子命令列表, 乐谱子命令播放, 乐谱子命令删除,
                    乐谱子命令导入, 乐谱子命令导出, 乐谱子命令Midi导入, 乐谱子命令Midi导出);
            case 子命令合奏 -> Arrays.asList(合奏子命令开始, 合奏子命令确认, 合奏子命令拒绝,
                    合奏子命令停止, 合奏子命令状态);
            case 子命令节拍器 -> Arrays.asList(节拍器子命令开, 节拍器子命令关, 节拍器子命令CountIn);
            case 子命令力度 -> Arrays.asList("pp", "p", "mf", "f", "ff", 力度子命令真实, 力度子命令音域);
            case 子命令量化 -> Arrays.asList("1/8", "1/16", "1/32", "1/8T", "1/16T", "1/32T",
                    量子命令撤销, 量子命令预览);
            case 子命令Swing -> Arrays.asList(开关参数开, 开关参数关, "50", "55", "60", "65");
            case 子命令延音 -> Arrays.asList(延音参数按住, 延音参数切换);
            case 子命令和弦 -> {
                List<String> 选项 = new ArrayList<>(Arrays.asList(和弦子命令列表, 和弦子命令清除));
                选项.addAll(Arrays.asList("maj", "min", "7", "maj7", "m7", "dim",
                        "aug", "sus2", "sus4", "add9", "6", "9"));
                yield 选项;
            }
            case 子命令录音 -> Arrays.asList(录音子命令开始, 录音子命令停止, 录音子命令保存,
                    录音子命令预览, 录音子命令撤销量化);
            case 子命令公共库 -> Arrays.asList(公共库子命令列表, 公共库子命令搜索, 公共库子命令下载,
                    公共库子命令上传, 公共库子命令评分, 公共库子命令审核);
            case 子命令分层 -> Arrays.asList(分层子命令开, 分层子命令关, 分层子命令切换,
                    分层子命令分界, 分层子命令乐器);
            case 子命令钢琴卷帘 -> Collections.singletonList(钢琴卷帘子命令打开);
            case 子命令资源包 -> Arrays.asList(资源包子命令应用, 资源包子命令生成);
            case 子命令弯音 -> Collections.singletonList(弯音子命令灵敏度);
            case 子命令切换 -> 乐器注册表.全部显示名称();
            case 子命令给予 -> null;
            default -> Collections.emptyList();
        };
    }

    private List<String> 补全三级及以上(String 主命令, String[] 参数, CommandSender 发送者) {
        return switch (主命令) {
            case 子命令乐谱 -> 补全乐谱三级(参数, 发送者);
            case 子命令合奏 -> 补全合奏三级(参数, 发送者);
            case 子命令节拍器 -> 补全节拍器三级(参数);
            case 子命令力度 -> 补全力度三级(参数);
            case 子命令和弦 -> 补全和弦三级(参数);
            case 子命令录音 -> 补全录音三级(发送者, 参数);
            case 子命令公共库 -> 补全公共库三级(参数);
            case 子命令分层 -> 补全分层三级(参数);
            case 子命令钢琴卷帘 -> 补全钢琴卷帘三级(发送者, 参数);
            case 子命令弯音 -> Collections.emptyList();
            case 子命令给予 -> 补全给予三级(参数);
            default -> Collections.emptyList();
        };
    }

    private List<String> 补全乐谱三级(String[] 参数, CommandSender 发送者) {
        String 子 = 参数[1];
        if (子.equals(乐谱子命令播放) || 子.equals(乐谱子命令删除) || 子.equals(乐谱子命令导出)
                || 子.equals(乐谱子命令Midi导出)) {
            return 获取发送者乐谱列表(发送者);
        }
        return Collections.emptyList();
    }

    private List<String> 补全合奏三级(String[] 参数, CommandSender 发送者) {
        if (参数[1].equals(合奏子命令开始)) {
            return 获取发送者乐谱列表(发送者);
        }
        return Collections.emptyList();
    }

    private List<String> 补全节拍器三级(String[] 参数) {
        if (参数[1].equals(节拍器子命令CountIn)) {
            return Arrays.asList("0", "1", "2");
        }
        return Collections.emptyList();
    }

    private List<String> 补全力度三级(String[] 参数) {
        if (参数[1].equals(力度子命令真实)) {
            return Arrays.asList(开关参数开, 开关参数关);
        }
        if (参数[1].equals(力度子命令音域)) {
            return Arrays.asList("0", "1", "2", "3", "4");
        }
        return Collections.emptyList();
    }

    private List<String> 补全和弦三级(String[] 参数) {
        // /乐器 和弦 <根音> <类型> [记忆]
        if (是数字参数位置(参数[1])) {
            if (参数.length == 3) {
                return Arrays.asList("maj", "min", "7", "maj7", "m7", "dim",
                        "aug", "sus2", "sus4", "add9", "6", "9");
            }
            if (参数.length == 4) {
                return Collections.singletonList(和弦参数记忆);
            }
        }
        return Collections.emptyList();
    }

    private boolean 是数字参数位置(String 参数) {
        try {
            Integer.parseInt(参数);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private List<String> 补全录音三级(CommandSender 发送者, String[] 参数) {
        if (参数[1].equals(录音子命令保存)) {
            return 获取发送者乐谱列表(发送者);
        }
        return Collections.emptyList();
    }

    private List<String> 补全公共库三级(String[] 参数) {
        String 子 = 参数[1];
        if (子.equals(公共库子命令审核) && 参数.length == 4) {
            return Arrays.asList(公共库审核通过, 公共库审核拒绝);
        }
        return Collections.emptyList();
    }

    private List<String> 补全分层三级(String[] 参数) {
        if (参数[1].equals(分层子命令乐器)) {
            if (参数.length == 3) {
                return Arrays.asList(分层方向左, 分层方向右);
            }
            if (参数.length == 4) {
                return 乐器注册表.全部显示名称();
            }
        }
        return Collections.emptyList();
    }

    private List<String> 补全钢琴卷帘三级(CommandSender 发送者, String[] 参数) {
        if (参数[1].equals(钢琴卷帘子命令打开)) {
            return 获取发送者乐谱列表(发送者);
        }
        return Collections.emptyList();
    }

    private List<String> 补全给予三级(String[] 参数) {
        if (参数.length == 3) {
            return 乐器注册表.全部显示名称();
        }
        return Collections.emptyList();
    }

    private List<String> 获取发送者乐谱列表(CommandSender 发送者) {
        if (!(发送者 instanceof Player 玩家)) {
            return Collections.emptyList();
        }
        List<String> 列表 = 乐器服务.获取乐谱列表(玩家.getUniqueId());
        if (列表 == null) {
            return Collections.emptyList();
        }
        return 列表;
    }
}
