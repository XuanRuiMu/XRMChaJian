package 暮澜纪元.数据;

import 暮澜纪元.职业.职业;

import java.util.UUID;

public class 玩家数据 {

    private final UUID 玩家ID;
    private String 玩家名字;
    private 职业 选择的职业;
    private int 等级;
    private boolean 已选择职业;

    public 玩家数据(UUID 玩家ID, String 名字) {
        this.玩家ID = 玩家ID;
        this.玩家名字 = 名字;
        this.已选择职业 = false;
        this.等级 = 1;
    }

    public UUID 获取UUID() {
        return this.玩家ID;
    }

    public String 获取名字() {
        return this.玩家名字;
    }

    public 职业 获取职业() {
        return this.选择的职业;
    }

    public void 设置职业(职业 职业) {
        this.选择的职业 = 职业;
        this.已选择职业 = 职业 != null;
    }

    public boolean 是否有职业() {
        return this.已选择职业;
    }

    public int 获取等级() {
        return this.等级;
    }

    public void 设置等级(int 等级) {
        this.等级 = 等级;
    }
}
