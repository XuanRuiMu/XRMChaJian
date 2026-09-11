package mljy.基础设施层;

import java.util.ArrayList;
import java.util.List;

public class 颜色码工具 {

    public static String 转换颜色码(String 输入) {
        if (输入 == null || 输入.isEmpty()) {
            return 输入;
        }

        StringBuilder 结果 = new StringBuilder();
        String 当前颜色标签 = null;
        List<String> 开启格式标签 = new ArrayList<>();

        int i = 0;
        while (i < 输入.length()) {
            char c = 输入.charAt(i);
            if (c == '§' && i + 1 < 输入.length()) {
                char 码 = 输入.charAt(i + 1);
                String 标签 = 获取颜色标签(码);
                if (标签 != null) {
                    if (是颜色码(码)) {
                        if (当前颜色标签 != null) {
                            结果.append("</").append(当前颜色标签).append(">");
                        }
                        当前颜色标签 = 标签;
                        结果.append("<").append(标签).append(">");
                    } else if (码 == 'r') {
                        for (int j = 开启格式标签.size() - 1; j >= 0; j--) {
                            结果.append("</").append(开启格式标签.get(j)).append(">");
                        }
                        开启格式标签.clear();
                        if (当前颜色标签 != null) {
                            结果.append("</").append(当前颜色标签).append(">");
                            当前颜色标签 = null;
                        }
                    } else {
                        开启格式标签.add(标签);
                        结果.append("<").append(标签).append(">");
                    }
                    i += 2;
                    continue;
                }
            }
            结果.append(c);
            i++;
        }

        for (int j = 开启格式标签.size() - 1; j >= 0; j--) {
            结果.append("</").append(开启格式标签.get(j)).append(">");
        }
        if (当前颜色标签 != null) {
            结果.append("</").append(当前颜色标签).append(">");
        }

        return 结果.toString();
    }

    private static boolean 是颜色码(char 码) {
        return (码 >= '0' && 码 <= '9') || (码 >= 'a' && 码 <= 'f');
    }

    private static String 获取颜色标签(char 码) {
        return switch (码) {
            case '0' -> "black";
            case '1' -> "dark_blue";
            case '2' -> "dark_green";
            case '3' -> "dark_aqua";
            case '4' -> "dark_red";
            case '5' -> "dark_purple";
            case '6' -> "gold";
            case '7' -> "gray";
            case '8' -> "dark_gray";
            case '9' -> "blue";
            case 'a' -> "green";
            case 'b' -> "aqua";
            case 'c' -> "red";
            case 'd' -> "light_purple";
            case 'e' -> "yellow";
            case 'f' -> "white";
            case 'l' -> "bold";
            case 'm' -> "strikethrough";
            case 'n' -> "underline";
            case 'o' -> "italic";
            case 'k' -> "obfuscated";
            case 'r' -> "reset";
            default -> null;
        };
    }
}
