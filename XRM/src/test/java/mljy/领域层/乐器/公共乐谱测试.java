package mljy.领域层.乐器;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FP-18 公共乐谱实体测试。
 * 验证状态转换、评分聚合、下载计数。
 */
@DisplayName("FP-18: 公共乐谱实体")
class 公共乐谱测试 {

    private 乐谱 创建测试乐谱() {
        return new 乐谱("测试乐谱", UUID.randomUUID(), "测试作者",
                System.currentTimeMillis(), 120, List.of(音符.of(0, 4, 1.0)));
    }

    @Nested
    @DisplayName("创建")
    class 创建测试 {

        @Test
        @DisplayName("创建 应生成 待审核 状态")
        void 创建应为待审核() {
            公共乐谱 乐谱 = 公共乐谱.创建("test-id", 创建测试乐谱(), UUID.randomUUID(), "上传者");
            assertEquals(公共乐谱.审核状态.待审核, 乐谱.获取状态());
        }

        @Test
        @DisplayName("创建 应初始化评分总和/评分人数/下载次数为 0")
        void 创建应初始化计数为零() {
            公共乐谱 乐谱 = 公共乐谱.创建("test-id", 创建测试乐谱(), UUID.randomUUID(), "上传者");
            assertEquals(0L, 乐谱.获取评分总和());
            assertEquals(0L, 乐谱.获取评分人数());
            assertEquals(0L, 乐谱.获取下载次数());
        }

        @Test
        @DisplayName("创建 应保留上传者信息")
        void 创建应保留上传者() {
            UUID 上传者 = UUID.randomUUID();
            公共乐谱 乐谱 = 公共乐谱.创建("test-id", 创建测试乐谱(), 上传者, "上传者名");
            assertEquals("test-id", 乐谱.获取乐谱ID());
            assertEquals(上传者, 乐谱.获取上传者标识());
            assertEquals("上传者名", 乐谱.获取上传者名());
            assertNotNull(乐谱.获取原乐谱());
        }
    }

    @Nested
    @DisplayName("审核状态")
    class 审核状态测试 {

        @Test
        @DisplayName("设置状态 应正确更新")
        void 设置状态应更新() {
            公共乐谱 乐谱 = 公共乐谱.创建("test-id", 创建测试乐谱(), UUID.randomUUID(), "上传者");
            乐谱.设置状态(公共乐谱.审核状态.已通过);
            assertEquals(公共乐谱.审核状态.已通过, 乐谱.获取状态());
        }

        @Test
        @DisplayName("审核状态枚举应有 3 个值")
        void 审核状态应有3个值() {
            assertEquals(3, 公共乐谱.审核状态.values().length);
        }
    }

    @Nested
    @DisplayName("评分聚合")
    class 评分聚合测试 {

        @Test
        @DisplayName("增加评分 应累加评分总和与人数")
        void 增加评分应累加() {
            公共乐谱 乐谱 = 公共乐谱.创建("test-id", 创建测试乐谱(), UUID.randomUUID(), "上传者");
            乐谱.增加评分(4);
            乐谱.增加评分(5);
            assertEquals(9L, 乐谱.获取评分总和());
            assertEquals(2L, 乐谱.获取评分人数());
        }

        @Test
        @DisplayName("获取平均评分 应为 总和 / 人数")
        void 平均评分计算() {
            公共乐谱 乐谱 = 公共乐谱.创建("test-id", 创建测试乐谱(), UUID.randomUUID(), "上传者");
            乐谱.增加评分(4);
            乐谱.增加评分(5);
            assertEquals(4.5, 乐谱.获取平均评分(), 0.001);
        }

        @Test
        @DisplayName("无评分时 平均评分应为 0")
        void 无评分平均为零() {
            公共乐谱 乐谱 = 公共乐谱.创建("test-id", 创建测试乐谱(), UUID.randomUUID(), "上传者");
            assertEquals(0.0, 乐谱.获取平均评分());
        }

        @Test
        @DisplayName("增加评分 越界应 clamp 到 [0, 5]")
        void 评分越界应clamp() {
            公共乐谱 乐谱 = 公共乐谱.创建("test-id", 创建测试乐谱(), UUID.randomUUID(), "上传者");
            乐谱.增加评分(10);
            assertEquals(5L, 乐谱.获取评分总和());
            乐谱.增加评分(-3);
            assertEquals(5L, 乐谱.获取评分总和());
        }
    }

    @Nested
    @DisplayName("下载计数")
    class 下载计数测试 {

        @Test
        @DisplayName("增加下载 应累加计数")
        void 增加下载应累加() {
            公共乐谱 乐谱 = 公共乐谱.创建("test-id", 创建测试乐谱(), UUID.randomUUID(), "上传者");
            乐谱.增加下载();
            乐谱.增加下载();
            assertEquals(2L, 乐谱.获取下载次数());
        }
    }
}
