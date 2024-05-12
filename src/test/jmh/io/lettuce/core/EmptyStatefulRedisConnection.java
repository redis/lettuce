package io.lettuce.core;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ConnectionFacade;
import io.lettuce.core.protocol.RedisCommand;

/**
 * @author Mark Paluch
 */
public class EmptyStatefulRedisConnection extends RedisChannelHandler implements StatefulRedisConnection, ConnectionFacade {

    public static final EmptyStatefulRedisConnection INSTANCE = new EmptyStatefulRedisConnection(
            EmptyRedisChannelWriter.INSTANCE);

    public EmptyStatefulRedisConnection(RedisChannelWriter writer) {
        super(writer, Duration.ZERO);
    }

    @Override
    public boolean isMulti() {
        return false;
    }

    @Override
    public RedisCommands sync() {
        return null;
    }

    @Override
    public RedisAsyncCommands async() {
        return null;
    }

    @Override
    public RedisReactiveCommands reactive() {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public ClientOptions getOptions() {
        return null;
    }

    @Override
    public void activated() {
    }

    @Override
    public void deactivated() {
    }

    @Override
    public void reset() {
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
    }

    @Override
    public void flushCommands() {
    }

    @Override
    public void addListener(PushListener listener) {
    }

    @Override
    public void removeListener(PushListener listener) {
    }

    @Override
    public RedisCommand dispatch(RedisCommand command) {
        return null;
    }

    @Override
    public Collection<? extends RedisCommand> dispatch(Collection commands) {
        return commands;
    }
}
