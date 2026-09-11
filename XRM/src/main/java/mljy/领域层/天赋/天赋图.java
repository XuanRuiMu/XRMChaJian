package mljy.领域层.天赋;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class 天赋图 {
    private final String 专精;
    private final int 最大点数;
    private final Map<String, 天赋节点> 节点表;

    public 天赋图(String 专精, int 最大点数, Collection<天赋节点> 节点列表) {
        this.专精 = 专精;
        this.最大点数 = 最大点数;
        this.节点表 = new ConcurrentHashMap<>();
        for (天赋节点 节点 : 节点列表) {
            节点表.put(节点.天赋标识(), 节点);
        }
        验证前置存在();
        验证DAG();
    }

    private void 验证前置存在() {
        for (天赋节点 节点 : 节点表.values()) {
            for (String 前置标识 : 节点.前置天赋列表()) {
                if (!节点表.containsKey(前置标识)) {
                    throw new IllegalArgumentException(
                            "天赋节点「" + 节点.天赋标识() + "」的前置天赋「" + 前置标识 + "」不存在于天赋树中");
                }
            }
        }
    }

    private void 验证DAG() {
        Map<String, Set<String>> 邻接表 = new HashMap<>();
        Map<String, Integer> 入度表 = new HashMap<>();

        for (String 节点标识 : 节点表.keySet()) {
            邻接表.put(节点标识, new HashSet<>());
            入度表.put(节点标识, 0);
        }

        for (天赋节点 节点 : 节点表.values()) {
            for (String 前置标识 : 节点.前置天赋列表()) {
                邻接表.get(前置标识).add(节点.天赋标识());
                入度表.put(节点.天赋标识(), 入度表.get(节点.天赋标识()) + 1);
            }
        }

        Queue<String> 队列 = new LinkedList<>();
        for (Map.Entry<String, Integer> 条目 : 入度表.entrySet()) {
            if (条目.getValue() == 0) {
                队列.add(条目.getKey());
            }
        }

        int 已处理数 = 0;
        while (!队列.isEmpty()) {
            String 当前 = 队列.poll();
            已处理数++;
            for (String 后继 : 邻接表.get(当前)) {
                入度表.put(后继, 入度表.get(后继) - 1);
                if (入度表.get(后继) == 0) {
                    队列.add(后继);
                }
            }
        }

        if (已处理数 != 节点表.size()) {
            throw new IllegalArgumentException("专精「" + 专精 + "」的天赋树存在循环依赖，不是合法的有向无环图（DAG）");
        }
    }

    public String 获取专精() {
        return 专精;
    }

    public int 获取最大点数() {
        return 最大点数;
    }

    public Optional<天赋节点> 获取节点(String 天赋标识) {
        return Optional.ofNullable(节点表.get(天赋标识));
    }

    public Collection<天赋节点> 获取所有节点() {
        return Collections.unmodifiableCollection(节点表.values());
    }

    public boolean 存在节点(String 天赋标识) {
        return 节点表.containsKey(天赋标识);
    }

    public boolean 检查前置(String 天赋标识, Set<String> 已学天赋) {
        天赋节点 节点 = 节点表.get(天赋标识);
        if (节点 == null) {
            return false;
        }
        if (!节点.有前置()) {
            return true;
        }
        for (String 前置标识 : 节点.前置天赋列表()) {
            if (已学天赋.contains(前置标识)) {
                return true;
            }
        }
        return false;
    }

    public boolean 检查选择组(String 天赋标识, Set<String> 已学天赋) {
        天赋节点 节点 = 节点表.get(天赋标识);
        if (节点 == null || !节点.有选择组()) {
            return true;
        }
        for (天赋节点 其他 : 节点表.values()) {
            if (其他.天赋标识().equals(天赋标识)) {
                continue;
            }
            if (节点.选择组().equals(其他.选择组()) && 已学天赋.contains(其他.天赋标识())) {
                return false;
            }
        }
        return true;
    }

    public String 查找选择组冲突(String 天赋标识, Set<String> 已学天赋) {
        天赋节点 节点 = 节点表.get(天赋标识);
        if (节点 == null || !节点.有选择组()) {
            return "";
        }
        for (天赋节点 其他 : 节点表.values()) {
            if (其他.天赋标识().equals(天赋标识)) {
                continue;
            }
            if (节点.选择组().equals(其他.选择组()) && 已学天赋.contains(其他.天赋标识())) {
                return 其他.天赋标识();
            }
        }
        return "";
    }

    public Set<String> 获取依赖者(String 天赋标识, Set<String> 已学天赋) {
        Set<String> 依赖者 = new HashSet<>();
        for (天赋节点 节点 : 节点表.values()) {
            if (节点.天赋标识().equals(天赋标识)) {
                continue;
            }
            if (!已学天赋.contains(节点.天赋标识())) {
                continue;
            }
            if (节点.前置天赋列表().contains(天赋标识)) {
                依赖者.add(节点.天赋标识());
            }
        }
        return 依赖者;
    }

    public Set<String> 获取同组天赋(String 选择组) {
        Set<String> 同组 = new HashSet<>();
        if (选择组 == null || 选择组.isBlank()) {
            return 同组;
        }
        for (天赋节点 节点 : 节点表.values()) {
            if (选择组.equals(节点.选择组())) {
                同组.add(节点.天赋标识());
            }
        }
        return 同组;
    }
}
