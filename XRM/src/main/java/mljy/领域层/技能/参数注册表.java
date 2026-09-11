package mljy.领域层.技能;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class 参数注册表 {
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<参数条目>> 注册表 = new ConcurrentHashMap<>();

    public void 注册(String 技能或效果ID, 参数条目 条目) {
        if (技能或效果ID == null || 条目 == null || 条目.参数名() == null) {
            return;
        }
        CopyOnWriteArrayList<参数条目> 列表 = 注册表.computeIfAbsent(技能或效果ID, k -> new CopyOnWriteArrayList<>());
        for (参数条目 已存在 : 列表) {
            if (已存在.参数名().equals(条目.参数名())) {
                return;
            }
        }
        列表.add(条目);
    }

    public List<参数条目> 获取参数列表(String 技能或效果ID) {
        if (技能或效果ID == null) {
            return Collections.emptyList();
        }
        CopyOnWriteArrayList<参数条目> 列表 = 注册表.get(技能或效果ID);
        if (列表 == null) {
            return Collections.emptyList();
        }
        return List.copyOf(列表);
    }

    public boolean 是否已注册(String 技能或效果ID) {
        return 技能或效果ID != null && 注册表.containsKey(技能或效果ID);
    }

    public record 参数条目(String 参数名, String 默认值, String 描述) {
    }
}
