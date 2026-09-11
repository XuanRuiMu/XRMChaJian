package mljy.技能实现;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FP-04 资源技能声明审计")
class 资源技能声明审计测试 {

    private static final Pattern 技能注解 = Pattern.compile(
            "@技能定义\\((.*?)\\)\\s*public\\s+class",
            Pattern.DOTALL);
    private static final Pattern 资源标识 = Pattern.compile("资源标识\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern 资源消耗 = Pattern.compile("资源消耗\\s*=\\s*([-+]?\\d+(?:\\.\\d+)?)");

    @Test
    @DisplayName("所有声明资源技能必须同时声明资源标识和正数消耗")
    void 所有声明资源技能_字段完整且合法() throws IOException {
        Path 技能源码目录 = Path.of("src/main/java/mljy/技能实现");
        List<String> 违规 = new ArrayList<>();

        try (Stream<Path> 文件流 = Files.walk(技能源码目录)) {
            文件流.filter(文件 -> 文件.toString().endsWith(".java"))
                    .forEach(文件 -> 审计文件(文件, 违规));
        }

        assertTrue(违规.isEmpty(), String.join(System.lineSeparator(), 违规));
    }

    private void 审计文件(Path 文件, List<String> 违规) {
        try {
            String 源码 = Files.readString(文件);
            Matcher 注解匹配 = 技能注解.matcher(源码);
            while (注解匹配.find()) {
                String 注解内容 = 注解匹配.group(1);
                String 资源名称 = 匹配文本(资源标识, 注解内容);
                String 消耗文本 = 匹配文本(资源消耗, 注解内容);
                double 消耗 = 消耗文本 == null ? 0.0 : Double.parseDouble(消耗文本);
                boolean 声明资源 = 资源名称 != null && !资源名称.isBlank() || 消耗文本 != null && 消耗 != 0.0;
                if (声明资源 && (资源名称 == null || 资源名称.isBlank() || 消耗 <= 0.0)) {
                    违规.add(文件 + "：资源标识与资源消耗必须同时有效");
                }
            }
        } catch (IOException | NumberFormatException 异常) {
            违规.add(文件 + "：无法审计源码（" + 异常.getClass().getSimpleName() + "）");
        }
    }

    private String 匹配文本(Pattern 模式, String 文本) {
        Matcher 匹配 = 模式.matcher(文本);
        return 匹配.find() ? 匹配.group(1) : null;
    }
}
