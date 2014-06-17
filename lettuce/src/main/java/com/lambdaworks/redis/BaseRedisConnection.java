package com.lambdaworks.redis;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

/**
 * 
 * Basic synchronous executed commands.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:05
 */
public interface BaseRedisConnection<K, V> extends Closeable {

    Long publish(K channel, V message);

    List<K> pubsubChannels();

    List<K> pubsubChannels(K channel);

    Map<K, Long> pubsubNumsub(K... channels);

    Long pubsubNumpat();

    V echo(V msg);

    String ping();

    String quit();

    @Override
    void close();

    String digest(V script);

    String discard();

    List<Object> exec();

    String multi();

    String watch(K... keys);

    String unwatch();

    Long waitForReplication(int replicas, long timeout);

    boolean isOpen();

}
