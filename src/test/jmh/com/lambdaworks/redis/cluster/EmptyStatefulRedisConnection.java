package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.protocol.ConnectionFacade;
import com.lambdaworks.redis.protocol.RedisCommand;

import java.util.concurrent.TimeUnit;

/**
 * @author Mark Paluch
 */
public class EmptyStatefulRedisConnection implements StatefulRedisConnection, ConnectionFacade {
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
    public void setTimeout(long timeout, TimeUnit unit) {

    }

    @Override
    public TimeUnit getTimeoutUnit() {
        return null;
    }

    @Override
    public long getTimeout() {
        return 0;
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
    public RedisCommand dispatch(RedisCommand command) {
        return null;
    }
}
