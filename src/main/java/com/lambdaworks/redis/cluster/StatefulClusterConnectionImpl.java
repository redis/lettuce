package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.protocol.CommandType.AUTH;
import static com.lambdaworks.redis.protocol.CommandType.READONLY;
import static com.lambdaworks.redis.protocol.CommandType.READWRITE;

import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.lambdaworks.redis.AbstractRedisClient;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.api.StatefulClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.AsyncCommand;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class StatefulClusterConnectionImpl<K, V> extends RedisChannelHandler<K, V> implements StatefulClusterConnection<K, V> {

    private Partitions partitions;

    private char[] password;
    private boolean readOnly;

    protected RedisCodec<K, V> codec;
    protected RedisAdvancedClusterConnection<K, V> sync;
    protected RedisAdvancedClusterAsyncConnectionImpl<K, V> async;

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     * @param unit Unit of time for the timeout.
     */
    public StatefulClusterConnectionImpl(RedisChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
        super(writer, timeout, unit);
        this.codec = codec;
    }

    @Override
    public RedisAdvancedClusterAsyncConnection<K, V> async() {
        return getAsyncConnection();
    }

    protected RedisAdvancedClusterAsyncConnectionImpl<K, V> getAsyncConnection() {
        if (async == null) {
            async = new RedisAdvancedClusterAsyncConnectionImpl<>(this, codec);
        }
        return async;
    }

    private RedisURI lookup(String nodeId) {

        for (RedisClusterNode partition : partitions) {
            if (partition.getNodeId().equals(nodeId)) {
                return partition.getUri();
            }
        }
        return null;
    }

    @Override
    public RedisAdvancedClusterConnection<K, V> sync() {
        if (sync == null) {
            ClusterFutureSyncInvocationHandler<K, V> h = new ClusterFutureSyncInvocationHandler<>(this, async());
            sync = (RedisAdvancedClusterConnection) Proxy.newProxyInstance(AbstractRedisClient.class.getClassLoader(),
                    new Class[] { RedisAdvancedClusterConnection.class }, h);
        }

        return sync;
    }

    @Override
    public StatefulClusterConnection<K, V> getConnection(String nodeId) {
        RedisURI redisURI = lookup(nodeId);
        if (redisURI == null) {
            throw new RedisException("NodeId " + nodeId + " does not belong to the cluster");
        }

        return getConnection(redisURI.getHost(), redisURI.getPort());
    }

    @Override
    public StatefulClusterConnection<K, V> getConnection(String host, int port) {

        // there is currently no check whether the node belongs to the cluster or not.
        // A check against the partition table could be done, but this reflects only a particular
        // point of view. What if the cluster is multi-homed, proxied, natted...?

        StatefulClusterConnection<K, V> connection = getClusterDistributionChannelWriter().getClusterConnectionProvider()
                .getConnection(ClusterConnectionProvider.Intent.WRITE, host, port);

        return connection;
    }

    public ClusterDistributionChannelWriter<K, V> getClusterDistributionChannelWriter() {
        return (ClusterDistributionChannelWriter<K, V>) super.getChannelWriter();
    }

    @Override
    public void activated() {

        super.activated();
        // do not block in here, since the channel flow will be interrupted.
        if (password != null) {
            getAsyncConnection().authAsync(new String(password));
        }

        if (readOnly) {
            getAsyncConnection().readOnly();
        }
    }

    @Override
    public <T, C extends RedisCommand<K, V, T>> C dispatch(C cmd) {

        RedisCommand<K, V, T> local = cmd;

        if (local.getType().name().equals(AUTH.name())) {
            local = attachOnComplete(local, status -> {
                if (status.equals("OK")) {
                    this.password = cmd.getArgs().getStrings().get(0).toCharArray();
                }
            });
        }

        if (local.getType().name().equals(READONLY.name())) {
            local = attachOnComplete(local, status -> {
                if (status.equals("OK")) {
                    this.readOnly = true;
                }
            });
        }

        if (local.getType().name().equals(READWRITE.name())) {
            local = attachOnComplete(local, status -> {
                if (status.equals("OK")) {
                    this.readOnly = false;
                }
            });
        }

        return super.dispatch((C) local);
    }

    private <T> RedisCommand<K, V, T> attachOnComplete(RedisCommand<K, V, T> command, Consumer<T> consumer) {

        if (command instanceof AsyncCommand) {
            AsyncCommand<K, V, T> async = (AsyncCommand<K, V, T>) command;
            async.thenAccept(consumer);
        }
        return command;
    }

    public void setPartitions(Partitions partitions) {
        this.partitions = partitions;
    }
}
