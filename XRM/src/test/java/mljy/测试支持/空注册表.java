package mljy.测试支持;

import net.kyori.adventure.key.Key;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Registry的最小空实现，用于在无服务器环境下提供Registry实例。
 * 所有方法返回null或空值。getOrThrow返回null（而非抛出异常），
 * 使Attribute等接口的静态初始化器能正常完成。
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class 空注册表<T extends Keyed> implements Registry<T> {

    @Override
    public T get(NamespacedKey key) {
        return null;
    }

    @Override
    public T getOrThrow(Key key) {
        return null;
    }

    @Override
    public NamespacedKey getKey(T value) {
        return null;
    }

    @Override
    public boolean hasTag(TagKey tagKey) {
        return false;
    }

    @Override
    public Tag getTag(TagKey tagKey) {
        return null;
    }

    @Override
    public Collection getTags() {
        return Collections.emptyList();
    }

    @Override
    public Stream stream() {
        return Stream.empty();
    }

    @Override
    public Stream keyStream() {
        return Stream.empty();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Iterator iterator() {
        return Collections.emptyIterator();
    }
}
