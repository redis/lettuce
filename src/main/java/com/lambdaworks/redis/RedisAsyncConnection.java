package com.lambdaworks.redis;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 14.05.14 12:24
 */
public interface RedisAsyncConnection<K, V> {
    void setTimeout(long timeout, TimeUnit unit);

    Future<Long> append(K key, V value);

    String auth(String password);

    Future<String> bgrewriteaof();

    Future<String> bgsave();

    Future<Long> bitcount(K key);

    Future<Long> bitcount(K key, long start, long end);

    Future<Long> bitopAnd(K destination, K... keys);

    Future<Long> bitopNot(K destination, K source);

    Future<Long> bitopOr(K destination, K... keys);

    Future<Long> bitopXor(K destination, K... keys);

    Future<KeyValue<K, V>> blpop(long timeout, K... keys);

    Future<KeyValue<K, V>> brpop(long timeout, K... keys);

    Future<V> brpoplpush(long timeout, K source, K destination);

    Future<K> clientGetname();

    Future<String> clientSetname(K name);

    Future<String> clientKill(String addr);

    Future<String> clientList();

    Future<List<String>> configGet(String parameter);

    Future<String> configResetstat();

    Future<String> configSet(String parameter, String value);

    Future<Long> dbsize();

    Future<String> debugObject(K key);

    Future<Long> decr(K key);

    Future<Long> decrby(K key, long amount);

    Future<Long> del(K... keys);

    Future<String> discard();

    Future<byte[]> dump(K key);

    Future<V> echo(V msg);

    <T> Future<T> eval(V script, ScriptOutputType type, K... keys);

    <T> Future<T> eval(V script, ScriptOutputType type, K[] keys, V... values);

    <T> Future<T> evalsha(String digest, ScriptOutputType type, K... keys);

    <T> Future<T> evalsha(String digest, ScriptOutputType type, K[] keys, V... values);

    Future<Boolean> exists(K key);

    Future<Boolean> expire(K key, long seconds);

    Future<Boolean> expireat(K key, Date timestamp);

    Future<Boolean> expireat(K key, long timestamp);

    Future<List<Object>> exec();

    Future<String> flushall() throws Exception;

    Future<String> flushdb() throws Exception;

    Future<V> get(K key);

    Future<Long> getbit(K key, long offset);

    Future<V> getrange(K key, long start, long end);

    Future<V> getset(K key, V value);

    Future<Long> hdel(K key, K... fields);

    Future<Boolean> hexists(K key, K field);

    Future<V> hget(K key, K field);

    Future<Long> hincrby(K key, K field, long amount);

    Future<Double> hincrbyfloat(K key, K field, double amount);

    Future<Map<K, V>> hgetall(K key);

    Future<List<K>> hkeys(K key);

    Future<Long> hlen(K key);

    Future<List<V>> hmget(K key, K... fields);

    Future<String> hmset(K key, Map<K, V> map);

    Future<Boolean> hset(K key, K field, V value);

    Future<Boolean> hsetnx(K key, K field, V value);

    Future<List<V>> hvals(K key);

    Future<Long> incr(K key);

    Future<Long> incrby(K key, long amount);

    Future<Double> incrbyfloat(K key, double amount);

    Future<String> info();

    Future<String> info(String section);

    Future<List<K>> keys(K pattern);

    Future<Date> lastsave();

    Future<V> lindex(K key, long index);

    Future<Long> linsert(K key, boolean before, V pivot, V value);

    Future<Long> llen(K key);

    Future<V> lpop(K key);

    Future<Long> lpush(K key, V... values);

    Future<Long> lpushx(K key, V value);

    Future<List<V>> lrange(K key, long start, long stop);

    Future<Long> lrem(K key, long count, V value);

    Future<String> lset(K key, long index, V value);

    Future<String> ltrim(K key, long start, long stop);

    Future<String> migrate(String host, int port, K key, int db, long timeout);

    Future<List<V>> mget(K... keys);

    Future<Boolean> move(K key, int db);

    Future<String> multi();

    Future<String> mset(Map<K, V> map);

    Future<Boolean> msetnx(Map<K, V> map);

    Future<String> objectEncoding(K key);

    Future<Long> objectIdletime(K key);

    Future<Long> objectRefcount(K key);

    Future<Boolean> persist(K key);

    Future<Boolean> pexpire(K key, long milliseconds);

    Future<Boolean> pexpireat(K key, Date timestamp);

    Future<Boolean> pexpireat(K key, long timestamp);

    Future<String> ping();

    Future<Long> pttl(K key);

    Future<Long> publish(K channel, V message);

    Future<String> quit();

    Future<V> randomkey();

    Future<String> rename(K key, K newKey);

    Future<Boolean> renamenx(K key, K newKey);

    Future<String> restore(K key, long ttl, byte[] value);

    Future<V> rpop(K key);

    Future<V> rpoplpush(K source, K destination);

    Future<Long> rpush(K key, V... values);

    Future<Long> rpushx(K key, V value);

    Future<Long> sadd(K key, V... members);

    Future<String> save();

    Future<Long> scard(K key);

    Future<List<Boolean>> scriptExists(String... digests);

    Future<String> scriptFlush();

    Future<String> scriptKill();

    Future<String> scriptLoad(V script);

    Future<Set<V>> sdiff(K... keys);

    Future<Long> sdiffstore(K destination, K... keys);

    String select(int db);

    Future<String> set(K key, V value);

    Future<Long> setbit(K key, long offset, int value);

    Future<String> setex(K key, long seconds, V value);

    Future<Boolean> setnx(K key, V value);

    Future<Long> setrange(K key, long offset, V value);

    void shutdown(boolean save);

    Future<Set<V>> sinter(K... keys);

    Future<Long> sinterstore(K destination, K... keys);

    Future<Boolean> sismember(K key, V member);

    Future<Boolean> smove(K source, K destination, V member);

    Future<String> slaveof(String host, int port);

    Future<String> slaveofNoOne();

    Future<List<Object>> slowlogGet();

    Future<List<Object>> slowlogGet(int count);

    Future<Long> slowlogLen();

    Future<String> slowlogReset();

    Future<Set<V>> smembers(K key);

    Future<List<V>> sort(K key);

    Future<List<V>> sort(K key, SortArgs sortArgs);

    Future<Long> sortStore(K key, SortArgs sortArgs, K destination);

    Future<V> spop(K key);

    Future<V> srandmember(K key);

    Future<Set<V>> srandmember(K key, long count);

    Future<Long> srem(K key, V... members);

    Future<Set<V>> sunion(K... keys);

    Future<Long> sunionstore(K destination, K... keys);

    Future<String> sync();

    Future<Long> strlen(K key);

    Future<Long> ttl(K key);

    Future<String> type(K key);

    Future<String> watch(K... keys);

    Future<String> unwatch();

    Future<Long> zadd(K key, double score, V member);

    @SuppressWarnings("unchecked")
    Future<Long> zadd(K key, Object... scoresAndValues);

    Future<Long> zcard(K key);

    Future<Long> zcount(K key, double min, double max);

    Future<Long> zcount(K key, String min, String max);

    Future<Double> zincrby(K key, double amount, K member);

    Future<Long> zinterstore(K destination, K... keys);

    Future<Long> zinterstore(K destination, ZStoreArgs storeArgs, K... keys);

    Future<List<V>> zrange(K key, long start, long stop);

    Future<List<ScoredValue<V>>> zrangeWithScores(K key, long start, long stop);

    Future<List<V>> zrangebyscore(K key, double min, double max);

    Future<List<V>> zrangebyscore(K key, String min, String max);

    Future<List<V>> zrangebyscore(K key, double min, double max, long offset, long count);

    Future<List<V>> zrangebyscore(K key, String min, String max, long offset, long count);

    Future<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, double min, double max);

    Future<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, String min, String max);

    Future<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, double min, double max, long offset, long count);

    Future<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, String min, String max, long offset, long count);

    Future<Long> zrank(K key, V member);

    Future<Long> zrem(K key, V... members);

    Future<Long> zremrangebyrank(K key, long start, long stop);

    Future<Long> zremrangebyscore(K key, double min, double max);

    Future<Long> zremrangebyscore(K key, String min, String max);

    Future<List<V>> zrevrange(K key, long start, long stop);

    Future<List<ScoredValue<V>>> zrevrangeWithScores(K key, long start, long stop);

    Future<List<V>> zrevrangebyscore(K key, double max, double min);

    Future<List<V>> zrevrangebyscore(K key, String max, String min);

    Future<List<V>> zrevrangebyscore(K key, double max, double min, long offset, long count);

    Future<List<V>> zrevrangebyscore(K key, String max, String min, long offset, long count);

    Future<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, double max, double min);

    Future<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, String max, String min);

    Future<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, double max, double min, long offset, long count);

    Future<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, String max, String min, long offset, long count);

    Future<Long> zrevrank(K key, V member);

    Future<Double> zscore(K key, V member);

    Future<Long> zunionstore(K destination, K... keys);

    Future<Long> zunionstore(K destination, ZStoreArgs storeArgs, K... keys);

    void close();

    String digest(V script);
}
