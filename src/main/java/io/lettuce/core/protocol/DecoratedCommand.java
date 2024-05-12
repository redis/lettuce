package io.lettuce.core.protocol;

/**
 * A decorated command allowing access to the underlying {@link #getDelegate()}.
 *
 * @author Mark Paluch
 */
public interface DecoratedCommand<K, V, T> {

    /**
     * The underlying command.
     *
     * @return never {@code null}.
     */
    RedisCommand<K, V, T> getDelegate();

}
