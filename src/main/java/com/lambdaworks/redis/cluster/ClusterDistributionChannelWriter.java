package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.internal.RedisChannelWriter;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandArgs;

/**
 * Channel writer for cluster operation. This writer looks up the right partition by hash/slot for the operation.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 17:46
 */
public class ClusterDistributionChannelWriter<K, V> implements RedisChannelWriter<K, V> {

    private RedisChannelWriter<K, V> defaultWriter;
    private ClusterConnectionProvider clusterConnectionProvider;
    private boolean closed = true;

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
            RedisAsyncConnectionImpl<K, V> connection = clusterConnectionProvider.getConnection(
                    ClusterConnectionProvider.Intent.WRITE, hash);

            RedisChannelWriter<K, V> channelWriter = connection.getChannelWriter();
            if (channelWriter instanceof ClusterDistributionChannelWriter) {
                ClusterDistributionChannelWriter writer = (ClusterDistributionChannelWriter) channelWriter;
                channelWriter = writer.defaultWriter;
            }

            if (channelWriter != this && channelWriter != defaultWriter) {
                channelWriter.write(command);
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

        if (closed) {
            return;
        }

        closed = true;

        if (defaultWriter != null) {
            defaultWriter.close();
            defaultWriter = null;
        }

        if (clusterConnectionProvider != null) {
            clusterConnectionProvider.close();
            clusterConnectionProvider = null;
        }

    }

    @Override
    public void setRedisChannelHandler(RedisChannelHandler<K, V> redisChannelHandler) {
        defaultWriter.setRedisChannelHandler(redisChannelHandler);
    }
}
