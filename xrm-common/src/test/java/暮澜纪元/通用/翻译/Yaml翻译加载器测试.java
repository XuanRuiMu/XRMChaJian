package 暮澜纪元.通用.翻译;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class Yaml翻译加载器测试 {

    private Yaml翻译加载器 加载器;

    @BeforeEach
    void 准备() {
        加载器 = new Yaml翻译加载器();
    }

    private void 加载资源(String 资源路径, String 语言码) {
        try (InputStream 流 = getClass().getClassLoader().getResourceAsStream(资源路径)) {
            assertNotNull(流, "测试资源应存在: " + 资源路径);
            加载器.加载翻译流(流, 语言码);
        } catch (Exception e) {
            fail("加载翻译资源失败: " + 资源路径 + " - " + e.getMessage());
        }
    }

    @Test
    void 加载翻译流后应能获取翻译() {
        加载资源("翻译/zh.yml", "zh");
        String 值 = 加载器.获取("通用.欢迎", Locale.SIMPLIFIED_CHINESE);
        assertEquals("欢迎来到暮澜纪元", 值);
    }

    @Test
    void 获取不存在的键应返回键本身() {
        加载资源("翻译/zh.yml", "zh");
        String 值 = 加载器.获取("不存在的键", Locale.SIMPLIFIED_CHINESE);
        assertEquals("不存在的键", 值);
    }

    @Test
    void 获取时应支持多语言fallback到默认zh() {
        加载资源("翻译/zh.yml", "zh");
        加载资源("翻译/en.yml", "en");
        String 值 = 加载器.获取("通用.欢迎", Locale.US);
        assertEquals("Welcome to Mulan Era", 值);

        String 中文值 = 加载器.获取("通用.欢迎", Locale.SIMPLIFIED_CHINESE);
        assertEquals("欢迎来到暮澜纪元", 中文值);
    }

    @Test
    void 英文缺失的键应fallback到中文() {
        加载资源("翻译/zh.yml", "zh");
        加载资源("翻译/en.yml", "en");
        String 值 = 加载器.获取("仅中文存在的键", Locale.US);
        assertEquals("仅中文存在的键", 值);
    }

    @Test
    void null语言应使用默认语言码() {
        加载资源("翻译/zh.yml", "zh");
        String 值 = 加载器.获取("通用.欢迎", null);
        assertEquals("欢迎来到暮澜纪元", 值);
    }

    @Test
    void 嵌套键应被扁平化为点分隔() {
        加载资源("翻译/嵌套.yml", "zh");
        String 值 = 加载器.获取("菜单.主菜单.标题", Locale.SIMPLIFIED_CHINESE);
        assertEquals("主菜单", 值);
        String 深层值 = 加载器.获取("菜单.子菜单.设置.音量", Locale.SIMPLIFIED_CHINESE);
        assertEquals("音量设置", 深层值);
    }

    @Test
    void UTF8编码的翻译应正确读取() {
        加载资源("翻译/zh.yml", "zh");
        String 值 = 加载器.获取("错误.权限不足", Locale.SIMPLIFIED_CHINESE);
        assertEquals("你没有权限执行此操作", 值);
    }

    @Test
    void 多次加载同一语言应合并键值() {
        加载资源("翻译/zh.yml", "zh");
        String 之前值 = 加载器.获取("通用.欢迎", Locale.SIMPLIFIED_CHINESE);
        assertEquals("欢迎来到暮澜纪元", 之前值);

        try (InputStream 流 = getClass().getClassLoader().getResourceAsStream("翻译/zh.yml")) {
            assertNotNull(流);
            加载器.加载翻译流(流, "zh");
        } catch (Exception e) {
            fail("重复加载不应抛出异常: " + e.getMessage());
        }
        String 之后值 = 加载器.获取("通用.欢迎", Locale.SIMPLIFIED_CHINESE);
        assertEquals("欢迎来到暮澜纪元", 之后值);
    }
}
