package mljy.领域层.属性;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 属性注册表。
 * TD-4 解决方案：管理命令处理器从注册表读取属性元数据，不再硬编码属性名列表和 switch 映射。
 *
 * 设计目标（需求.txt第32行"松散性"与"可插入性"）：
 * 1. 松散性——注册标准属性() 中删除任一注册行，对应属性从命令系统消失，其余属性正常工作。
 * 2. 可插入性——外部模块注入本表后调用 注册() 即可新增属性（如反伤），无需修改命令处理器。
 *
 * 线程安全：使用 ConcurrentHashMap + CopyOnWriteArrayList，支持并发查询与注册。
 * 注册顺序：按调用 注册() 的先后顺序保留，用于属性查看的稳定显示顺序。
 */
public class 属性注册表 {
    private final ConcurrentHashMap<String, 属性元数据> 按命令名 = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<属性元数据> 注册顺序 = new CopyOnWriteArrayList<>();

    public 属性注册表() {
        注册标准属性();
    }

    private void 注册标准属性() {
        注册(new 属性元数据("生命值", "生命值上限", "管理员指令.属性.查看.生命值"));
        注册(new 属性元数据("公CD修正", "公共冷却时间", "管理员指令.属性.查看.公CD"));
        注册(new 属性元数据("急速", "急速", "管理员指令.属性.查看.急速"));
        注册(new 属性元数据("力量", "力量", "管理员指令.属性.查看.力量"));
        注册(new 属性元数据("敏捷", "敏捷", "管理员指令.属性.查看.敏捷"));
        注册(new 属性元数据("智力", "智力", "管理员指令.属性.查看.智力"));
        注册(new 属性元数据("法术暴击几率", "法术暴击几率", "管理员指令.属性.查看.法术暴击几率"));
        注册(new 属性元数据("法术暴击伤害", "法术暴击伤害", "管理员指令.属性.查看.法术暴击伤害"));
        注册(new 属性元数据("精通", "精通", "管理员指令.属性.查看.精通"));
        注册(new 属性元数据("全能", "全能", "管理员指令.属性.查看.全能"));
        注册(new 属性元数据("吸血", "吸血", "管理员指令.属性.查看.吸血"));
        注册(new 属性元数据("躲闪", "躲闪", "管理员指令.属性.查看.躲闪"));
        注册(new 属性元数据("恢复", "生命恢复", "管理员指令.属性.查看.生命恢复"));
        注册(new 属性元数据("移速", "移速", "管理员指令.属性.查看.移速"));
    }

    public void 注册(属性元数据 元数据) {
        if (元数据 == null || 元数据.命令名() == null) {
            return;
        }
        if (按命令名.putIfAbsent(元数据.命令名(), 元数据) == null) {
            注册顺序.add(元数据);
        }
    }

    public Optional<属性元数据> 查询(String 命令名) {
        return Optional.ofNullable(按命令名.get(命令名));
    }

    public boolean 包含(String 命令名) {
        return 按命令名.containsKey(命令名);
    }

    public List<String> 获取所有命令名() {
        return List.copyOf(按命令名.keySet());
    }

    public List<属性元数据> 获取所有元数据() {
        return List.copyOf(注册顺序);
    }
}
