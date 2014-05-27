package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.internal.RedisChannelWriter;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandArgs;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 17:46
 */
public class ClusterDistributionChannelWriter<K, V> implements RedisChannelWriter<K, V> {
    private RedisChannelWriter<K, V> defaultWriter;
    private ClusterConnectionProvider clusterConnectionProvider;

    public ClusterDistributionChannelWriter(RedisChannelWriter<K, V> defaultWriter,
            ClusterConnectionProvider clusterConnectionProvider) {
        this.defaultWriter = defaultWriter;
        this.clusterConnectionProvider = clusterConnectionProvider;
    }

    @Override
    public void write(Command<K, V, ?> command) {

        CommandArgs<K, V> args = command.getArgs();
        if (args != null && !args.getKeys().isEmpty()) {

            int hash = getHash(args.getEncodedKey(0));
            RedisAsyncConnectionImpl<K, V> connection = clusterConnectionProvider.getConnection(hash, null);

            if (connection.getChannelWriter() != this) {
                connection.getChannelWriter().write(command);
                return;
            }
        }

        defaultWriter.write(command);
    }

    protected int getHash(byte[] encodedKey) {
        return SlotHash.getSlot(encodedKey);
    }

    @Override
    public void close() {
        defaultWriter.close();

    }

    @Override
    public void setRedisChannelHandler(RedisChannelHandler<K, V> redisChannelHandler) {
        defaultWriter.setRedisChannelHandler(redisChannelHandler);
    }
}
