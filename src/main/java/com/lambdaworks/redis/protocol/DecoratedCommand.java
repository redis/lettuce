package com.lambdaworks.redis.protocol;

/**
 * A decorated command allowing access to the underlying {@link #getDelegate()}.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public interface DecoratedCommand<K, V, T> {

    /**
     * The underlying command.
     * 
     * @return never {@literal null}.
     */
    RedisCommand<K, V, T> getDelegate();

}
