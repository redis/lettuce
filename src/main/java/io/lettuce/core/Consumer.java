package io.lettuce.core;

import java.util.Objects;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Value object representing a Stream consumer within a consumer group. Group name and consumer name are encoded as keys.
 *
 * @author Mark Paluch
 * @since 5.1
 * @see io.lettuce.core.codec.RedisCodec
 */
public class Consumer<K> {

    final K group;

    final K name;

    private Consumer(K group, K name) {

        this.group = group;
        this.name = name;
    }

    /**
     * Create a new consumer.
     *
     * @param group name of the consumer group, must not be {@code null} or empty.
     * @param name name of the consumer, must not be {@code null} or empty.
     * @return the consumer {@link Consumer} object.
     */
    public static <K> Consumer<K> from(K group, K name) {

        LettuceAssert.notNull(group, "Group must not be null");
        LettuceAssert.notNull(name, "Name must not be null");

        return new Consumer<>(group, name);
    }

    /**
     * @return name of the group.
     */
    public K getGroup() {
        return group;
    }

    /**
     * @return name of the consumer.
     */
    public K getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Consumer))
            return false;
        Consumer<?> consumer = (Consumer<?>) o;
        return Objects.equals(group, consumer.group) && Objects.equals(name, consumer.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, name);
    }

    @Override
    public String toString() {
        return String.format("%s:%s", group, name);
    }

}
