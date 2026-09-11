package mljy.测试支持;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Keyed;
import org.bukkit.Registry;

/**
 * 测试用RegistryAccess实现，通过ServiceLoader自动注册。
 *
 * 背景：Bukkit的Registry.<clinit>通过RegistryAccess.registryAccess()获取注册表，
 * 该方法在无服务器环境下抛出IllegalStateException。本类通过ServiceLoader机制
 * 提供一个返回空注册表的实现，使Registry和Attribute等类能在测试环境中正常初始化。
 *
 * 注册方式：META-INF/services/io.papermc.paper.registry.RegistryAccess
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class 测试注册表访问 implements RegistryAccess {

    @Override
    @SuppressWarnings("removal")
    public <T extends Keyed> Registry<T> getRegistry(Class<T> type) {
        return new 空注册表<>();
    }

    @Override
    public <T extends Keyed> Registry<T> getRegistry(RegistryKey<T> registryKey) {
        return new 空注册表<>();
    }
}
