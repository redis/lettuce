package com.lambdaworks.redis.protocol;

import com.google.common.util.concurrent.ListenableFuture;
import com.lambdaworks.redis.RedisFuture;
import io.netty.buffer.ByteBuf;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 31.05.14 14:44
 */
public interface RedisCommand<K, V, T> extends ListenableFuture<T>, RedisFuture<T> {
    CommandOutput<K, V, T> getOutput();

    void complete();

    CommandArgs<K, V> getArgs();

    void encode(ByteBuf buf);
}
