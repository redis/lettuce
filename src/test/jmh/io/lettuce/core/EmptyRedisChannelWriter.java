package io.lettuce.core;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.protocol.ConnectionFacade;
import io.lettuce.core.protocol.EmptyClientResources;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;

/**
 * @author Mark Paluch
 */
public class EmptyRedisChannelWriter implements RedisChannelWriter {

    public static final EmptyRedisChannelWriter INSTANCE = new EmptyRedisChannelWriter();
    private static final CompletableFuture CLOSE_FUTURE = CompletableFuture.completedFuture(null);

    @Override
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {
        return null;
    }

    @Override
    public <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> redisCommands) {
        return (Collection) redisCommands;
    }

    @Override
    public void close() {

    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return CLOSE_FUTURE;
    }

    @Override
    public void reset() {

    }

    @Override
    public void setConnectionFacade(ConnectionFacade connection) {
    }

    @Override
    public ClientResources getClientResources() {
        return EmptyClientResources.INSTANCE;
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
    }

    @Override
    public void flushCommands() {
    }
}
