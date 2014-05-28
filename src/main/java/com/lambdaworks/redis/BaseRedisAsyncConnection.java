package com.lambdaworks.redis;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 
 * Basic asynchronous executed commands.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:05
 */
public interface BaseRedisAsyncConnection<K, V> extends Closeable {

    RedisFuture<Long> publish(K channel, V message);

    RedisFuture<V> echo(V msg);

    RedisFuture<String> ping();

    RedisFuture<String> quit();

    void close();

    String digest(V script);

    RedisFuture<String> discard();

    RedisFuture<List<Object>> exec();

    RedisFuture<String> multi();

    RedisFuture<String> watch(K... keys);

    RedisFuture<String> unwatch();

    boolean isOpen();

}
