package com.lambdaworks.redis.masterslave;

import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.ProtocolKeyword;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * Channel writer/dispatcher that dispatches commands based on the intent to different connections.
 * 
 * @author Mark Paluch
 */
class MasterSlaveChannelWriter<K, V> implements RedisChannelWriter<K, V> {

    private MasterSlaveConnectionProvider masterSlaveConnectionProvider;
    private boolean closed = false;

    public MasterSlaveChannelWriter(MasterSlaveConnectionProvider masterSlaveConnectionProvider) {
        this.masterSlaveConnectionProvider = masterSlaveConnectionProvider;
    }

    @Override
    public <T, C extends RedisCommand<K, V, T>> C write(C command) {
        LettuceAssert.notNull(command, "command must not be null");

        if (closed) {
            throw new RedisException("Connection is closed");
        }

        MasterSlaveConnectionProvider.Intent intent = getIntent(command.getType());
        StatefulRedisConnection<K, V> connection = masterSlaveConnectionProvider.getConnection(intent);

        return connection.dispatch(command);
    }

    private MasterSlaveConnectionProvider.Intent getIntent(ProtocolKeyword type) {
        for (ProtocolKeyword readOnlyCommand : ReadOnlyCommands.READ_ONLY_COMMANDS) {
            if (readOnlyCommand == type) {
                return MasterSlaveConnectionProvider.Intent.READ;
            }
        }
        return MasterSlaveConnectionProvider.Intent.WRITE;
    }

    @Override
    public void close() {

        if (closed) {
            return;
        }

        closed = true;

        if (masterSlaveConnectionProvider != null) {
            masterSlaveConnectionProvider.close();
            masterSlaveConnectionProvider = null;
        }
    }

    public MasterSlaveConnectionProvider getMasterSlaveConnectionProvider() {
        return masterSlaveConnectionProvider;
    }

    @Override
    public void setRedisChannelHandler(RedisChannelHandler<K, V> redisChannelHandler) {

    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        masterSlaveConnectionProvider.setAutoFlushCommands(autoFlush);
    }

    @Override
    public void flushCommands() {
        masterSlaveConnectionProvider.flushCommands();
    }

    @Override
    public void reset() {
        masterSlaveConnectionProvider.reset();
    }

    /**
     * Set from which nodes data is read. The setting is used as default for read operations on this connection. See the
     * documentation for {@link ReadFrom} for more information.
     *
     * @param readFrom the read from setting, must not be {@literal null}
     */
    public void setReadFrom(ReadFrom readFrom) {
        masterSlaveConnectionProvider.setReadFrom(readFrom);
    }

    /**
     * Gets the {@link ReadFrom} setting for this connection. Defaults to {@link ReadFrom#MASTER} if not set.
     *
     * @return the read from setting
     */
    public ReadFrom getReadFrom() {
        return masterSlaveConnectionProvider.getReadFrom();
    }

}
