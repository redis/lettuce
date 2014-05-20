package com.lambdaworks.redis;

import java.util.Date;
import java.util.List;

/**
 * Synchronous executed commands for Server Control.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:32
 */
public interface RedisServerConnection<K, V> extends BaseRedisConnection<K, V> {
    String bgrewriteaof();

    String bgsave();

    K clientGetname();

    String clientSetname(K name);

    String clientKill(String addr);

    String clientPause(long timeout);

    String clientList();

    List<String> configGet(String parameter);

    String configResetstat();

    String configRewrite();

    String configSet(String parameter, String value);

    Long dbsize();

    String debugObject(K key);

    String flushall();

    String flushdb();

    String info();

    String info(String section);

    Date lastsave();

    String save();

    void shutdown(boolean save);

    String slaveof(String host, int port);

    String slaveofNoOne();

    List<Object> slowlogGet();

    List<Object> slowlogGet(int count);

    Long slowlogLen();

    String slowlogReset();

    String sync();

    List<V> time();
}
