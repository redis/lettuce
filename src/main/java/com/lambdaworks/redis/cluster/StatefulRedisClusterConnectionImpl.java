package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.protocol.CommandType.AUTH;
import static com.lambdaworks.redis.protocol.CommandType.READONLY;
import static com.lambdaworks.redis.protocol.CommandType.READWRITE;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.common.reflect.AbstractInvocationHandler;
import com.lambdaworks.redis.AbstractRedisClient;
import com.lambdaworks.redis.LettuceFutures;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.rx.RedisAdvancedClusterReactiveCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CompleteableCommand;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.protocol.RedisCommand;
import io.netty.channel.ChannelHandler;

/**
 * A thread-safe connection to a Redis Cluster. Multiple threads may share one {@link StatefulRedisClusterConnectionImpl}
 *
 * A {@link ConnectionWatchdog} monitors each connection and reconnects automatically until {@link #close} is called. All
 * pending commands will be (re)sent after successful reconnection.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 4.0
 */
@ChannelHandler.Sharable
public class StatefulRedisClusterConnectionImpl<K, V> extends RedisChannelHandler<K, V> implements
        StatefulRedisClusterConnection<K, V> {

    private Partitions partitions;

    private char[] password;
    private boolean readOnly;

    protected RedisCodec<K, V> codec;
    protected RedisAdvancedClusterCommands<K, V> sync;
    protected RedisAdvancedClusterAsyncCommandsImpl<K, V> async;
    protected RedisAdvancedClusterReactiveCommandsImpl<K, V> reactive;

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     * @param unit Unit of time for the timeout.
     */
    public StatefulRedisClusterConnectionImpl(RedisChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout,
            TimeUnit unit) {
        super(writer, timeout, unit);
        this.codec = codec;
    }

    @Override
    public RedisAdvancedClusterCommands<K, V> sync() {
        if (sync == null) {
            InvocationHandler h = syncInvocationHandler();
            sync = (RedisAdvancedClusterCommands) Proxy.newProxyInstance(AbstractRedisClient.class.getClassLoader(),
                    new Class[] { RedisAdvancedClusterConnection.class, RedisAdvancedClusterCommands.class }, h);
        }

        return sync;
    }

    public InvocationHandler syncInvocationHandler() {
        return new ClusterFutureSyncInvocationHandler<>(this, async());
    }

    @Override
    public RedisAdvancedClusterAsyncCommands<K, V> async() {
        return getAsyncConnection();
    }

    protected RedisAdvancedClusterAsyncCommandsImpl<K, V> getAsyncConnection() {
        if (async == null) {
            async = new RedisAdvancedClusterAsyncCommandsImpl<>(this, codec);
        }
        return async;
    }

    @Override
    public RedisAdvancedClusterReactiveCommands<K, V> reactive() {
        return getReactiveCommands();
    }

    protected RedisAdvancedClusterReactiveCommandsImpl<K, V> getReactiveCommands() {
        if (reactive == null) {
            reactive = new RedisAdvancedClusterReactiveCommandsImpl<>(this, codec);
        }
        return reactive;
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
    public StatefulRedisConnection<K, V> getConnection(String nodeId) {
        RedisURI redisURI = lookup(nodeId);
        if (redisURI == null) {
            throw new RedisException("NodeId " + nodeId + " does not belong to the cluster");
        }

        return getConnection(redisURI.getHost(), redisURI.getPort());
    }

    @Override
    public StatefulRedisConnection<K, V> getConnection(String host, int port) {

        // there is currently no check whether the node belongs to the cluster or not.
        // A check against the partition table could be done, but this reflects only a particular
        // point of view. What if the cluster is multi-homed, proxied, natted...?

        StatefulRedisConnection<K, V> connection = getClusterDistributionChannelWriter().getClusterConnectionProvider()
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

        if (command instanceof CompleteableCommand) {
            CompleteableCommand<T> completeable = (CompleteableCommand<T>) command;
            completeable.onComplete(consumer);
        }
        return command;
    }

    public void setPartitions(Partitions partitions) {
        this.partitions = partitions;
    }

    public Partitions getPartitions() {
        return partitions;
    }

    /**
     * Invocation-handler to synchronize API calls which use Futures as backend. This class leverages the need to implement a
     * full sync class which just delegates every request.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
     * @since 3.0
     */
    private static class ClusterFutureSyncInvocationHandler<K, V> extends AbstractInvocationHandler {

        private final StatefulConnection<K, V> connection;
        private final Object asyncApi;

        public ClusterFutureSyncInvocationHandler(StatefulConnection<K, V> connection, Object asyncApi) {
            this.connection = connection;
            this.asyncApi = asyncApi;
        }

        /**
         *
         * @see AbstractInvocationHandler#handleInvocation(Object, Method, Object[])
         */
        @Override
        protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

            try {

                if (method.getName().equals("getConnection") && args.length > 0) {
                    Method targetMethod = connection.getClass().getMethod(method.getName(), method.getParameterTypes());
                    Object result = targetMethod.invoke(connection, args);
                    if (result instanceof StatefulRedisClusterConnection) {
                        StatefulRedisClusterConnection connection = (StatefulRedisClusterConnection) result;
                        return connection.sync();
                    }

                    if (result instanceof StatefulRedisConnection) {
                        StatefulRedisConnection connection = (StatefulRedisConnection) result;
                        return connection.sync();
                    }
                }

                Method targetMethod = asyncApi.getClass().getMethod(method.getName(), method.getParameterTypes());

                Object result = targetMethod.invoke(asyncApi, args);

                if (result instanceof RedisFuture) {
                    RedisFuture<?> command = (RedisFuture<?>) result;
                    if (!method.getName().equals("exec") && !method.getName().equals("multi")) {
                        if (connection instanceof StatefulRedisConnection && ((StatefulRedisConnection) connection).isMulti()) {
                            return null;
                        }
                    }
                    return LettuceFutures.await(command, connection.getTimeout(), connection.getTimeoutUnit());
                }

                return result;

            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }
}
