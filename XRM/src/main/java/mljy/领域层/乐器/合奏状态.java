package mljy.领域层.乐器;

/**
 * 合奏状态领域对象。
 * 描述玩家当前所在合奏的基本信息。
 */
public record 合奏状态(String 曲目名, int BPM, boolean 是否演奏中, int 已准备人数, int 总人数) {
}
