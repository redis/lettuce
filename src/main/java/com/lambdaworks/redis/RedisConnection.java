package com.lambdaworks.redis;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 14.05.14 12:21
 */
public interface RedisConnection<K, V> extends AutoCloseable {
    Long append(K key, V value);

    String auth(String password);

    String bgrewriteaof();

    String bgsave();

    Long bitcount(K key);

    Long bitcount(K key, long start, long end);

    Long bitopAnd(K destination, K... keys);

    Long bitopNot(K destination, K source);

    Long bitopOr(K destination, K... keys);

    Long bitopXor(K destination, K... keys);

    KeyValue<K, V> blpop(long timeout, K... keys);

    KeyValue<K, V> brpop(long timeout, K... keys);

    V brpoplpush(long timeout, K source, K destination);

    K clientGetname();

    String clientSetname(K name);

    String clientKill(String addr);

    String clientList();

    List<String> configGet(String parameter);

    String configResetstat();

    String configSet(String parameter, String value);

    Long dbsize();

    String debugObject(K key);

    Long decr(K key);

    Long decrby(K key, long amount);

    Long del(K... keys);

    String discard();

    byte[] dump(K key);

    V echo(V msg);

    @SuppressWarnings("unchecked")
    <T> T eval(V script, ScriptOutputType type, K... keys);

    @SuppressWarnings("unchecked")
    <T> T eval(V script, ScriptOutputType type, K[] keys, V... values);

    @SuppressWarnings("unchecked")
    <T> T evalsha(String digest, ScriptOutputType type, K... keys);

    @SuppressWarnings("unchecked")
    <T> T evalsha(String digest, ScriptOutputType type, K[] keys, V... values);

    Boolean exists(K key);

    Boolean expire(K key, long seconds);

    Boolean expireat(K key, Date timestamp);

    Boolean expireat(K key, long timestamp);

    List<Object> exec();

    String flushall() throws Exception;

    String flushdb() throws Exception;

    V get(K key);

    Long getbit(K key, long offset);

    V getrange(K key, long start, long end);

    V getset(K key, V value);

    Long hdel(K key, K... fields);

    Boolean hexists(K key, K field);

    V hget(K key, K field);

    Long hincrby(K key, K field, long amount);

    Double hincrbyfloat(K key, K field, double amount);

    Map<K, V> hgetall(K key);

    List<K> hkeys(K key);

    Long hlen(K key);

    List<V> hmget(K key, K... fields);

    String hmset(K key, Map<K, V> map);

    Boolean hset(K key, K field, V value);

    Boolean hsetnx(K key, K field, V value);

    List<V> hvals(K key);

    Long incr(K key);

    Long incrby(K key, long amount);

    Double incrbyfloat(K key, double amount);

    String info();

    String info(String section);

    List<K> keys(K pattern);

    Date lastsave();

    V lindex(K key, long index);

    Long linsert(K key, boolean before, V pivot, V value);

    Long llen(K key);

    V lpop(K key);

    Long lpush(K key, V... values);

    Long lpushx(K key, V value);

    List<V> lrange(K key, long start, long stop);

    Long lrem(K key, long count, V value);

    String lset(K key, long index, V value);

    String ltrim(K key, long start, long stop);

    String migrate(String host, int port, K key, int db, long timeout);

    List<V> mget(K... keys);

    Boolean move(K key, int db);

    String multi();

    String mset(Map<K, V> map);

    Boolean msetnx(Map<K, V> map);

    String objectEncoding(K key);

    Long objectIdletime(K key);

    Long objectRefcount(K key);

    Boolean persist(K key);

    Boolean pexpire(K key, long milliseconds);

    Boolean pexpireat(K key, Date timestamp);

    Boolean pexpireat(K key, long timestamp);

    String ping();

    Long pttl(K key);

    Long publish(K channel, V message);

    String quit();

    V randomkey();

    String rename(K key, K newKey);

    Boolean renamenx(K key, K newKey);

    String restore(K key, long ttl, byte[] value);

    V rpop(K key);

    V rpoplpush(K source, K destination);

    Long rpush(K key, V... values);

    Long rpushx(K key, V value);

    Long sadd(K key, V... members);

    String save();

    Long scard(K key);

    List<Boolean> scriptExists(String... digests);

    String scriptFlush();

    String scriptKill();

    String scriptLoad(V script);

    Set<V> sdiff(K... keys);

    Long sdiffstore(K destination, K... keys);

    String select(int db);

    String set(K key, V value);

    Long setbit(K key, long offset, int value);

    String setex(K key, long seconds, V value);

    Boolean setnx(K key, V value);

    Long setrange(K key, long offset, V value);

    void shutdown(boolean save);

    Set<V> sinter(K... keys);

    Long sinterstore(K destination, K... keys);

    Boolean sismember(K key, V member);

    Boolean smove(K source, K destination, V member);

    String slaveof(String host, int port);

    String slaveofNoOne();

    List<Object> slowlogGet();

    List<Object> slowlogGet(int count);

    Long slowlogLen();

    String slowlogReset();

    Set<V> smembers(K key);

    List<V> sort(K key);

    List<V> sort(K key, SortArgs sortArgs);

    Long sortStore(K key, SortArgs sortArgs, K destination);

    V spop(K key);

    V srandmember(K key);

    Set<V> srandmember(K key, long count);

    Long srem(K key, V... members);

    Set<V> sunion(K... keys);

    Long sunionstore(K destination, K... keys);

    String sync();

    Long strlen(K key);

    Long ttl(K key);

    String type(K key);

    String watch(K... keys);

    String unwatch();

    Long zadd(K key, double score, V member);

    Long zadd(K key, Object... scoresAndValues);

    Long zcard(K key);

    Long zcount(K key, double min, double max);

    Long zcount(K key, String min, String max);

    Double zincrby(K key, double amount, K member);

    Long zinterstore(K destination, K... keys);

    Long zinterstore(K destination, ZStoreArgs storeArgs, K... keys);

    List<V> zrange(K key, long start, long stop);

    List<ScoredValue<V>> zrangeWithScores(K key, long start, long stop);

    List<V> zrangebyscore(K key, double min, double max);

    List<V> zrangebyscore(K key, String min, String max);

    List<V> zrangebyscore(K key, double min, double max, long offset, long count);

    List<V> zrangebyscore(K key, String min, String max, long offset, long count);

    List<ScoredValue<V>> zrangebyscoreWithScores(K key, double min, double max);

    List<ScoredValue<V>> zrangebyscoreWithScores(K key, String min, String max);

    List<ScoredValue<V>> zrangebyscoreWithScores(K key, double min, double max, long offset, long count);

    List<ScoredValue<V>> zrangebyscoreWithScores(K key, String min, String max, long offset, long count);

    Long zrank(K key, V member);

    Long zrem(K key, V... members);

    Long zremrangebyrank(K key, long start, long stop);

    Long zremrangebyscore(K key, double min, double max);

    Long zremrangebyscore(K key, String min, String max);

    List<V> zrevrange(K key, long start, long stop);

    List<ScoredValue<V>> zrevrangeWithScores(K key, long start, long stop);

    List<V> zrevrangebyscore(K key, double max, double min);

    List<V> zrevrangebyscore(K key, String max, String min);

    List<V> zrevrangebyscore(K key, double max, double min, long offset, long count);

    List<V> zrevrangebyscore(K key, String max, String min, long offset, long count);

    List<ScoredValue<V>> zrevrangebyscoreWithScores(K key, double max, double min);

    List<ScoredValue<V>> zrevrangebyscoreWithScores(K key, String max, String min);

    List<ScoredValue<V>> zrevrangebyscoreWithScores(K key, double max, double min, long offset, long count);

    List<ScoredValue<V>> zrevrangebyscoreWithScores(K key, String max, String min, long offset, long count);

    Long zrevrank(K key, V member);

    Double zscore(K key, V member);

    Long zunionstore(K destination, K... keys);

    Long zunionstore(K destination, ZStoreArgs storeArgs, K... keys);

    void close();

    String digest(V script);

    void setTimeout(long timeout, TimeUnit unit);
}
