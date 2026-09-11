package mljy.基础设施层.Bukkit适配;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("实体适配器名称")
class 实体适配器测试 {

    @Test
    @DisplayName("有自定义名称时使用自定义名称")
    void 有自定义名称时使用自定义名称() {
        Entity 原始实体 = mock(Entity.class);
        when(原始实体.customName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("史莱姆王"));
        when(原始实体.getName()).thenReturn("Slime");

        assertEquals("史莱姆王", new 实体适配器(原始实体).获取名称());
    }

    @Test
    @DisplayName("没有有效自定义名称时使用客户端官方实体翻译")
    void 没有有效自定义名称时使用客户端官方实体翻译() {
        Entity 原始实体 = mock(Entity.class);
        when(原始实体.customName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("  "));
        when(原始实体.getName()).thenReturn("Slime");
        EntityType 类型 = mock(EntityType.class);
        when(类型.translationKey()).thenReturn("entity.minecraft.slime");
        when(原始实体.getType()).thenReturn(类型);

        assertEquals("<lang:entity.minecraft.slime>", new 实体适配器(原始实体).获取名称());
    }

    @Test
    @DisplayName("血条方块自定义名应被拒绝并回退客户端官方实体翻译")
    void 血条方块自定义名应回退实体翻译() {
        Entity 原始实体 = mock(Entity.class);
        when(原始实体.customName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("§c[████████████]"));
        when(原始实体.getName()).thenReturn("[████████████]");
        EntityType 类型 = mock(EntityType.class);
        when(类型.translationKey()).thenReturn("entity.minecraft.zombie");
        when(原始实体.getType()).thenReturn(类型);

        assertEquals("<lang:entity.minecraft.zombie>", new 实体适配器(原始实体).获取名称());
    }

    @Test
    @DisplayName("普通中文自定义名称不能被误判为血条")
    void 普通中文自定义名称不能被误判为血条() {
        Entity 原始实体 = mock(Entity.class);
        when(原始实体.customName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("腐化僵尸首领"));

        assertEquals("腐化僵尸首领", new 实体适配器(原始实体).获取名称());
    }
}
