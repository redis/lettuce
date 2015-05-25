package com.lambdaworks.redis.cluster;

import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.RedisCodec;
import io.netty.channel.ChannelHandler;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@ChannelHandler.Sharable
public class RedisAdvancedClusterConnectionImpl<K, V> extends RedisAsyncConnectionImpl<K, V> implements
        RedisAdvancedClusterConnection<K, V> {

    private Partitions partitions;

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     * @param unit Unit of time for the timeout.
     */
    public RedisAdvancedClusterConnectionImpl(RedisChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout,
            TimeUnit unit) {
        super(writer, codec, timeout, unit);
    }

    @Override
    public NodeSelectionAsyncOperations<K, V> nodes(Predicate<RedisClusterNode> predicate) {
        return nodes(predicate, false);
    }

    @Override
    public NodeSelectionAsyncOperations<K, V> nodes(Predicate<RedisClusterNode> predicate, boolean dynamic) {

        NodeSelection<K, V> selection = new StaticNodeSelection<>(this, predicate);
        NodeSelectionInvocationHandler h = new NodeSelectionInvocationHandler(selection);
        return (NodeSelectionAsyncOperations<K, V>) Proxy.newProxyInstance(NodeSelection.class.getClassLoader(),
                new Class[] { NodeSelectionAsyncOperations.class }, h);
    }

    @Override
    public Partitions getPartitions() {
        return partitions;
    }

    public void setPartitions(Partitions partitions) {
        this.partitions = partitions;
    }
}
