package com.lambdaworks.redis;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.reflect.AbstractInvocationHandler;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * Invocation-handler to synchronize API calls which use Futures as backend. This class leverages the need to implement a full
 * sync class which just delegates every request.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
class FutureSyncInvocationHandler<K, V> extends AbstractInvocationHandler {

    private final RedisChannelHandler<K, V> connection;
    protected long timeout;
    protected TimeUnit unit;
    private LoadingCache<Method, Method> methodCache;

    public FutureSyncInvocationHandler(final RedisChannelHandler<K, V> connection) {
        this.connection = connection;
        this.timeout = connection.timeout;
        this.unit = connection.unit;

        methodCache = CacheBuilder.newBuilder().build(new CacheLoader<Method, Method>() {
            @Override
            public Method load(Method key) throws Exception {
                return connection.getClass().getMethod(key.getName(), key.getParameterTypes());
            }
        });

    }

    /**
     * 
     * @see com.google.common.reflect.AbstractInvocationHandler#handleInvocation(java.lang.Object, java.lang.reflect.Method,
     *      java.lang.Object[])
     */
    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {

            // void setTimeout(long timeout, TimeUnit unit)
            if (method.getName().equals("setTimeout")) {
                setTimeout((Long) args[0], (TimeUnit) args[1]);
                return null;
            }

            Method targetMethod = methodCache.get(method);
            Object result = targetMethod.invoke(connection, args);

            if (result instanceof RedisCommand) {
                RedisCommand<?, ?, ?> redisCommand = (RedisCommand<?, ?, ?>) result;
                if (!method.getName().equals("exec") && !method.getName().equals("multi")) {
                    if (connection instanceof RedisAsyncConnectionImpl && ((RedisAsyncConnectionImpl) connection).isMulti()) {
                        return null;
                    }
                }

                Object awaitedResult = LettuceFutures.await(redisCommand, timeout, unit);

                if (redisCommand instanceof Command) {
                    Command<?, ?, ?> command = (Command<?, ?, ?>) redisCommand;
                    if (command.getException() != null) {
                        throw new RedisException(command.getException());
                    }
                }

                return awaitedResult;
            }

            if (result instanceof RedisClusterAsyncConnection) {
                return AbstractRedisClient.syncHandler((RedisChannelHandler) result, RedisConnection.class,
                        RedisClusterConnection.class);
            }

            return result;

        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }

    }

    private void setTimeout(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
    }
}
