package mljy.领域层.技能;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class 参数读取器 {
    private static final Logger 日志器 = Logger.getLogger(参数读取器.class.getName());

    private final Map<String, String> 覆盖表;

    public 参数读取器() {
        this.覆盖表 = new HashMap<>();
    }

    public 参数读取器(Map<String, String> 覆盖表) {
        this.覆盖表 = 覆盖表 == null ? new HashMap<>() : new HashMap<>(覆盖表);
    }

    public void 添加覆盖(String 键, String 值) {
        if (键 == null) {
            return;
        }
        覆盖表.put(键, 值);
    }

    public boolean 是否有覆盖(String 键) {
        return 覆盖表.containsKey(键);
    }

    public double 获取双精度(String 键, double 默认值) {
        String 值 = 剥离尖括号(覆盖表.get(键));
        if (值 == null) {
            return 默认值;
        }
        try {
            return Double.parseDouble(值);
        } catch (NumberFormatException e) {
            日志器.warning("参数覆盖解析失败：键=" + 键 + " 值=" + 值 + " 类型=double 回退默认值=" + 默认值);
            return 默认值;
        }
    }

    public int 获取整数(String 键, int 默认值) {
        String 值 = 剥离尖括号(覆盖表.get(键));
        if (值 == null) {
            return 默认值;
        }
        try {
            return Integer.parseInt(值);
        } catch (NumberFormatException e) {
            日志器.warning("参数覆盖解析失败：键=" + 键 + " 值=" + 值 + " 类型=int 回退默认值=" + 默认值);
            return 默认值;
        }
    }

    public String 获取字符串(String 键, String 默认值) {
        String 值 = 剥离尖括号(覆盖表.get(键));
        return 值 == null ? 默认值 : 值;
    }

    public boolean 获取布尔值(String 键, boolean 默认值) {
        String 值 = 剥离尖括号(覆盖表.get(键));
        if (值 == null) {
            return 默认值;
        }
        if ("true".equalsIgnoreCase(值)) {
            return true;
        }
        if ("false".equalsIgnoreCase(值)) {
            return false;
        }
        日志器.warning("参数覆盖解析失败：键=" + 键 + " 值=" + 值 + " 类型=boolean 回退默认值=" + 默认值);
        return 默认值;
    }

    public long 获取长整数(String 键, long 默认值) {
        String 值 = 剥离尖括号(覆盖表.get(键));
        if (值 == null) {
            return 默认值;
        }
        try {
            return Long.parseLong(值);
        } catch (NumberFormatException e) {
            日志器.warning("参数覆盖解析失败：键=" + 键 + " 值=" + 值 + " 类型=long 回退默认值=" + 默认值);
            return 默认值;
        }
    }

    private static String 剥离尖括号(String 值) {
        if (值 != null && 值.length() > 2 && 值.startsWith("<") && 值.endsWith(">")) {
            return 值.substring(1, 值.length() - 1);
        }
        return 值;
    }

    public Map<String, String> 获取覆盖表() {
        return Collections.unmodifiableMap(覆盖表);
    }

    public static 参数读取器 从参数数组解析(String[] 参数, int 起始位置) {
        参数读取器 读取器 = new 参数读取器();
        if (参数 == null) {
            return 读取器;
        }
        int 起点 = Math.max(0, 起始位置);
        for (int i = 起点; i < 参数.length; i++) {
            String 项 = 参数[i];
            if (项 == null) {
                continue;
            }
            int 等号位置 = 项.indexOf('=');
            if (等号位置 <= 0) {
                continue;
            }
            String 键;
            if (项.startsWith("--")) {
                键 = 项.substring(2, 等号位置);
            } else {
                键 = 项.substring(0, 等号位置);
            }
            if (键.isEmpty()) {
                continue;
            }
            String 值 = 项.substring(等号位置 + 1);
            读取器.添加覆盖(键, 值);
        }
        return 读取器;
    }
}
