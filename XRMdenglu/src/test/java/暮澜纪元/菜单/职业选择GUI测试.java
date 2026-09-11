package 暮澜纪元.菜单;

import 暮澜纪元.登录服插件;
import 暮澜纪元.配置.消息管理器;
import 暮澜纪元.测试工具.玩家输出捕获器;
import 暮澜纪元.测试工具.翻译文件加载器;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("职业选择GUI")
class 职业选择GUI测试 {

    private 登录服插件 mock插件;
    private 消息管理器 消息;

    @BeforeEach
    void 准备() throws Exception {
        mock插件 = mock(登录服插件.class);
        消息 = 翻译文件加载器.创建带磁盘翻译的消息管理器(mock插件, "菜单/职业选择GUI");
    }

    private String 渲染为字符串(String 文本) {
        return MiniMessage.miniMessage().serialize(消息.格式化文本(文本));
    }

    @Nested
    @DisplayName("翻译输出验证")
    class 翻译输出验证 {

        @Test
        @DisplayName("聊天栏应输出：正在进入暮澜纪元MMORPG服务器...")
        void 服务器欢迎_应显示正确中文(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            消息.发送类翻译(捕获器.获取玩家(), 职业选择GUI.class, "服务器欢迎");

            捕获器.发布到报告(报告);
            捕获器.断言包含("暮澜纪元", "服务器欢迎消息");
            捕获器.断言包含("MMORPG", "服务器欢迎消息");
        }

        @Test
        @DisplayName("聊天栏应输出：你选择了【魔法世界】的力量")
        void 世界和职业已选择_应包含世界名(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            消息.发送类翻译(捕获器.获取玩家(), 职业选择GUI.class, "世界和职业已选择", "魔法世界", "奥能法师");

            捕获器.发布到报告(报告);
            捕获器.断言包含("魔法世界", "选择世界后");
            捕获器.断言包含("奥能法师", "选择世界后");
        }

        @Test
        @DisplayName("聊天栏应输出：你已将专精更改为【奥能法师】！")
        void 职业已更改_应包含职业名(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            消息.发送类翻译(捕获器.获取玩家(), 职业选择GUI.class, "职业已更改", "奥能法师");

            捕获器.发布到报告(报告);
            捕获器.断言包含("奥能法师", "更改职业后");
        }

        @Test
        @DisplayName("英文玩家应看到：Entering Mulan Epoch MMORPG Server...")
        void 英文服务器欢迎_应显示英文(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            捕获器.设置语言("en");
            消息.发送类翻译(捕获器.获取玩家(), 职业选择GUI.class, "服务器欢迎");

            捕获器.发布到报告(报告);
            捕获器.断言包含("MMORPG Server", "英文玩家服务器欢迎");
        }

        @Test
        @DisplayName("英文玩家选择世界 → 聊天栏应输出含英文的世界选择提示")
        void 英文世界和职业已选择_应显示英文(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            捕获器.设置语言("en");
            消息.发送类翻译(捕获器.获取玩家(), 职业选择GUI.class, "世界和职业已选择", "Arcane World", "Arcane Mage");

            捕获器.发布到报告(报告);
            捕获器.断言包含("Arcane World", "英文玩家选择世界后");
            捕获器.断言包含("Arcane Mage", "英文玩家选择世界后");
        }

        @Test
        @DisplayName("聊天栏应输出：选择你的世界")
        void 世界选择标题_应显示正确中文(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            消息.发送类翻译(捕获器.获取玩家(), 职业选择GUI.class, "界面.世界选择标题");

            捕获器.发布到报告(报告);
            捕获器.断言包含("选择你的世界", "世界选择标题");
        }

        @Test
        @DisplayName("英文玩家世界选择标题 → 聊天栏应输出英文标题")
        void 英文世界选择标题_应显示英文(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            捕获器.设置语言("en");
            消息.发送类翻译(捕获器.获取玩家(), 职业选择GUI.class, "界面.世界选择标题");

            捕获器.发布到报告(报告);
            捕获器.断言包含("Choose Your World", "英文玩家世界选择标题");
        }

        @Test
        @DisplayName("英文玩家职业已更改 → 聊天栏应输出含英文的职业更改提示")
        void 英文职业已更改_应显示英文(TestReporter 报告) {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            捕获器.设置语言("en");
            消息.发送类翻译(捕获器.获取玩家(), 职业选择GUI.class, "职业已更改", "Arcane Mage");

            捕获器.发布到报告(报告);
            捕获器.断言包含("Arcane Mage", "英文玩家更改职业后");
        }
    }

    @Nested
    @DisplayName("职业描述颜色格式")
    class 职业描述颜色格式 {

        private void 断言不是关键词着色(String 描述, String 关键词) {
            String 错误消息 = "描述中 '" + 关键词 + "' 不应被着色为关键词";
            org.junit.jupiter.api.Assertions.assertFalse(描述.contains("<blue>" + 关键词 + "</blue>"), 错误消息);
            org.junit.jupiter.api.Assertions.assertFalse(描述.contains("<gray>" + 关键词 + "</gray>"), 错误消息);
        }

        private void 断言非伤害类型段落为灰色(String 描述, String 段落) {
            String 错误消息 = "非伤害类型段落 '" + 段落 + "' 应使用灰色而非红色";
            org.junit.jupiter.api.Assertions.assertFalse(描述.contains("<red>" + 段落 + "</red>"), 错误消息);
        }

        @Test
        @DisplayName("大魔导师描述中'魔法物质'的'魔法'不应作为关键词着色")
        void 大魔导师_魔法物质_不应着色() {
            String 中文描述 = 消息.获取类翻译(职业选择GUI.class, "职业描述.大魔导师", null);
            断言不是关键词着色(中文描述, "魔法");
        }

        @Test
        @DisplayName("Archmage description should not color 'magic' in 'magic projectiles' as keyword")
        void archmage_magicProjectiles_shouldNotColorMagic() {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            捕获器.设置语言("en");
            String 英文描述 = 消息.获取类翻译(职业选择GUI.class, "职业描述.大魔导师", 捕获器.获取玩家());
            断言不是关键词着色(英文描述, "magic");
        }

        @Test
        @DisplayName("女巫描述中伤害类型后的机制说明应为灰色")
        void 女巫_机制说明应为灰色() {
            String 中文描述 = 消息.获取类翻译(职业选择GUI.class, "职业描述.女巫", null);
            断言非伤害类型段落为灰色(中文描述, "间隔时间较长的DEBUFF出伤，范围伤害。");
        }

        @Test
        @DisplayName("Witch description should use gray for mechanic text after damage type")
        void witch_mechanicText_shouldBeGray() {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            捕获器.设置语言("en");
            String 英文描述 = 消息.获取类翻译(职业选择GUI.class, "职业描述.女巫", 捕获器.获取玩家());
            断言非伤害类型段落为灰色(英文描述, "Long-interval DEBUFF damage, AoE damage.");
        }

        @Test
        @DisplayName("沸血战士描述中不应有整段红色")
        void 沸血战士_不应有整段红色() {
            String 中文描述 = 消息.获取类翻译(职业选择GUI.class, "职业描述.沸血战士", null);
            org.junit.jupiter.api.Assertions.assertFalse(中文描述.contains("<red>"), "沸血战士描述不应包含红色整段");
        }

        @Test
        @DisplayName("Bloodboil Warrior description should not have entire red segments")
        void bloodboilWarrior_shouldNotHaveRedSegments() {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            捕获器.设置语言("en");
            String 英文描述 = 消息.获取类翻译(职业选择GUI.class, "职业描述.沸血战士", 捕获器.获取玩家());
            org.junit.jupiter.api.Assertions.assertFalse(英文描述.contains("<red>"), "Bloodboil Warrior description should not contain red segments");
        }

        @Test
        @DisplayName("雷电督军描述中仅伤害类型句可为红色")
        void 雷电督军_仅伤害类型为红色() {
            String 中文描述 = 消息.获取类翻译(职业选择GUI.class, "职业描述.雷电督军", null);
            String[] 段落 = 中文描述.split("\\|");
            String 第一段 = 渲染为字符串(段落[0]);
            org.junit.jupiter.api.Assertions.assertTrue(第一段.startsWith("<red>"), "第一段应为伤害类型描述，实际: " + 第一段);
            for (int i = 1; i < 段落.length; i++) {
                org.junit.jupiter.api.Assertions.assertFalse(渲染为字符串(段落[i]).startsWith("<red>"), "第" + (i + 1) + "段不应为红色");
            }
        }

        @Test
        @DisplayName("Thunder Warlord description should only have first segment red")
        void thunderWarlord_onlyFirstSegmentRed() {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            捕获器.设置语言("en");
            String 英文描述 = 消息.获取类翻译(职业选择GUI.class, "职业描述.雷电督军", 捕获器.获取玩家());
            String[] segments = 英文描述.split("\\|");
            String firstSegment = 渲染为字符串(segments[0]);
            org.junit.jupiter.api.Assertions.assertTrue(firstSegment.startsWith("<red>"), "First segment should be damage type, actual: " + firstSegment);
            for (int i = 1; i < segments.length; i++) {
                org.junit.jupiter.api.Assertions.assertFalse(渲染为字符串(segments[i]).startsWith("<red>"), "Segment " + (i + 1) + " should not be red");
            }
        }

        @Test
        @DisplayName("平衡术士描述中仅伤害类型句可为红色")
        void 平衡术士_仅伤害类型为红色() {
            String 中文描述 = 消息.获取类翻译(职业选择GUI.class, "职业描述.平衡术士", null);
            String[] 段落 = 中文描述.split("\\|");
            String 第一段 = 渲染为字符串(段落[0]);
            org.junit.jupiter.api.Assertions.assertTrue(第一段.startsWith("<red>"), "第一段应为伤害类型描述，实际: " + 第一段);
            for (int i = 1; i < 段落.length; i++) {
                org.junit.jupiter.api.Assertions.assertFalse(渲染为字符串(段落[i]).startsWith("<red>"), "第" + (i + 1) + "段不应为红色");
            }
        }

        @Test
        @DisplayName("Balance Adept description should only have first segment red")
        void balanceAdept_onlyFirstSegmentRed() {
            玩家输出捕获器 捕获器 = 玩家输出捕获器.创建();
            捕获器.设置语言("en");
            String 英文描述 = 消息.获取类翻译(职业选择GUI.class, "职业描述.平衡术士", 捕获器.获取玩家());
            String[] segments = 英文描述.split("\\|");
            String firstSegment = 渲染为字符串(segments[0]);
            org.junit.jupiter.api.Assertions.assertTrue(firstSegment.startsWith("<red>"), "First segment should be damage type, actual: " + firstSegment);
            for (int i = 1; i < segments.length; i++) {
                org.junit.jupiter.api.Assertions.assertFalse(渲染为字符串(segments[i]).startsWith("<red>"), "Segment " + (i + 1) + " should not be red");
            }
        }
    }
}
