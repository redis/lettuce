package com.lambdaworks.redis;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.output.KeyStreamingChannel;
import com.lambdaworks.redis.output.KeyValueStreamingChannel;
import com.lambdaworks.redis.output.ScoredValueStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingOutput;
import com.lambdaworks.redis.protocol.SetArgs;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 14.05.14 12:24
 */
public interface RedisAsyncConnection<K, V> extends AutoCloseable {
    void setTimeout(long timeout, TimeUnit unit);

    RedisFuture<Long> append(K key, V value);

    String auth(String password);

    RedisFuture<String> bgrewriteaof();

    RedisFuture<String> bgsave();

    RedisFuture<Long> bitcount(K key);

    RedisFuture<Long> bitcount(K key, long start, long end);

    RedisFuture<Long> bitopAnd(K destination, K... keys);

    RedisFuture<Long> bitopNot(K destination, K source);

    RedisFuture<Long> bitopOr(K destination, K... keys);

    RedisFuture<Long> bitopXor(K destination, K... keys);

    RedisFuture<KeyValue<K, V>> blpop(long timeout, K... keys);

    RedisFuture<KeyValue<K, V>> brpop(long timeout, K... keys);

    RedisFuture<V> brpoplpush(long timeout, K source, K destination);

    RedisFuture<K> clientGetname();

    RedisFuture<String> clientSetname(K name);

    RedisFuture<String> clientKill(String addr);

    RedisFuture<String> clientList();

    RedisFuture<List<String>> configGet(String parameter);

    RedisFuture<String> configResetstat();

    RedisFuture<String> configSet(String parameter, String value);

    RedisFuture<Long> dbsize();

    RedisFuture<String> debugObject(K key);

    RedisFuture<Long> decr(K key);

    RedisFuture<Long> decrby(K key, long amount);

    RedisFuture<Long> del(K... keys);

    RedisFuture<String> discard();

    RedisFuture<byte[]> dump(K key);

    RedisFuture<V> echo(V msg);

    <T> RedisFuture<T> eval(V script, ScriptOutputType type, K... keys);

    <T> RedisFuture<T> eval(V script, ScriptOutputType type, K[] keys, V... values);

    <T> RedisFuture<T> evalsha(String digest, ScriptOutputType type, K... keys);

    <T> RedisFuture<T> evalsha(String digest, ScriptOutputType type, K[] keys, V... values);

    RedisFuture<Boolean> exists(K key);

    RedisFuture<Boolean> expire(K key, long seconds);

    RedisFuture<Boolean> expireat(K key, Date timestamp);

    RedisFuture<Boolean> expireat(K key, long timestamp);

    RedisFuture<List<Object>> exec();

    RedisFuture<String> flushall() throws Exception;

    RedisFuture<String> flushdb() throws Exception;

    RedisFuture<V> get(K key);

    RedisFuture<Long> getbit(K key, long offset);

    RedisFuture<V> getrange(K key, long start, long end);

    RedisFuture<V> getset(K key, V value);

    RedisFuture<Long> hdel(K key, K... fields);

    RedisFuture<Boolean> hexists(K key, K field);

    RedisFuture<V> hget(K key, K field);

    RedisFuture<Long> hincrby(K key, K field, long amount);

    RedisFuture<Double> hincrbyfloat(K key, K field, double amount);

    RedisFuture<Map<K, V>> hgetall(K key);

    RedisFuture<Long> hgetall(KeyValueStreamingChannel<K, V> channel, K key);

    RedisFuture<List<K>> hkeys(K key);

    RedisFuture<Long> hkeys(KeyStreamingChannel<K> channel, K key);

    RedisFuture<Long> hlen(K key);

    RedisFuture<List<V>> hmget(K key, K... fields);

    RedisFuture<Long> hmget(ValueStreamingChannel<V> channel, K key, K... fields);

    RedisFuture<String> hmset(K key, Map<K, V> map);

    RedisFuture<Boolean> hset(K key, K field, V value);

    RedisFuture<Boolean> hsetnx(K key, K field, V value);

    RedisFuture<List<V>> hvals(K key);

    RedisFuture<Long> hvals(ValueStreamingChannel<V> channel, K key);

    RedisFuture<Long> incr(K key);

    RedisFuture<Long> incrby(K key, long amount);

    RedisFuture<Double> incrbyfloat(K key, double amount);

    RedisFuture<String> info();

    RedisFuture<String> info(String section);

    RedisFuture<List<K>> keys(K pattern);

    RedisFuture<Long> keys(KeyStreamingChannel<K> channel, K pattern);

    RedisFuture<Date> lastsave();

    RedisFuture<V> lindex(K key, long index);

    RedisFuture<Long> linsert(K key, boolean before, V pivot, V value);

    RedisFuture<Long> llen(K key);

    RedisFuture<V> lpop(K key);

    RedisFuture<Long> lpush(K key, V... values);

    RedisFuture<Long> lpushx(K key, V value);

    RedisFuture<List<V>> lrange(K key, long start, long stop);

    RedisFuture<Long> lrange(ValueStreamingChannel<V> channel, K key, long start, long stop);

    RedisFuture<Long> lrem(K key, long count, V value);

    RedisFuture<String> lset(K key, long index, V value);

    RedisFuture<String> ltrim(K key, long start, long stop);

    RedisFuture<String> migrate(String host, int port, K key, int db, long timeout);

    RedisFuture<List<V>> mget(K... keys);

    RedisFuture<Long> mget(ValueStreamingChannel<V> channel, K... keys);

    RedisFuture<Boolean> move(K key, int db);

    RedisFuture<String> multi();

    RedisFuture<String> mset(Map<K, V> map);

    RedisFuture<Boolean> msetnx(Map<K, V> map);

    RedisFuture<String> objectEncoding(K key);

    RedisFuture<Long> objectIdletime(K key);

    RedisFuture<Long> objectRefcount(K key);

    RedisFuture<Boolean> persist(K key);

    RedisFuture<Boolean> pexpire(K key, long milliseconds);

    RedisFuture<Boolean> pexpireat(K key, Date timestamp);

    RedisFuture<Boolean> pexpireat(K key, long timestamp);

    RedisFuture<String> ping();

    RedisFuture<Long> pttl(K key);

    RedisFuture<Long> publish(K channel, V message);

    RedisFuture<String> quit();

    RedisFuture<V> randomkey();

    RedisFuture<String> rename(K key, K newKey);

    RedisFuture<Boolean> renamenx(K key, K newKey);

    RedisFuture<String> restore(K key, long ttl, byte[] value);

    RedisFuture<V> rpop(K key);

    RedisFuture<V> rpoplpush(K source, K destination);

    RedisFuture<Long> rpush(K key, V... values);

    RedisFuture<Long> rpushx(K key, V value);

    RedisFuture<Long> sadd(K key, V... members);

    RedisFuture<String> save();

    RedisFuture<Long> scard(K key);

    RedisFuture<List<Boolean>> scriptExists(String... digests);

    RedisFuture<String> scriptFlush();

    RedisFuture<String> scriptKill();

    RedisFuture<String> scriptLoad(V script);

    RedisFuture<Set<V>> sdiff(K... keys);

    RedisFuture<Long> sdiff(ValueStreamingChannel<V> channel, K... keys);

    RedisFuture<Long> sdiffstore(K destination, K... keys);

    String select(int db);

    RedisFuture<String> set(K key, V value);

    RedisFuture<V> set(K key, V value, SetArgs setArgs);

    RedisFuture<Long> setbit(K key, long offset, int value);

    RedisFuture<String> setex(K key, long seconds, V value);

    RedisFuture<Boolean> setnx(K key, V value);

    RedisFuture<Long> setrange(K key, long offset, V value);

    void shutdown(boolean save);

    RedisFuture<Set<V>> sinter(K... keys);

    RedisFuture<Long> sinter(ValueStreamingChannel<V> channel, K... keys);

    RedisFuture<Long> sinterstore(K destination, K... keys);

    RedisFuture<Boolean> sismember(K key, V member);

    RedisFuture<Boolean> smove(K source, K destination, V member);

    RedisFuture<String> slaveof(String host, int port);

    RedisFuture<String> slaveofNoOne();

    RedisFuture<List<Object>> slowlogGet();

    RedisFuture<List<Object>> slowlogGet(int count);

    RedisFuture<Long> slowlogLen();

    RedisFuture<String> slowlogReset();

    RedisFuture<Set<V>> smembers(K key);

    RedisFuture<Long> smembers(ValueStreamingChannel<V> channel, K key);

    RedisFuture<List<V>> sort(K key);

    RedisFuture<Long> sort(ValueStreamingChannel<V> channel, K key);

    RedisFuture<List<V>> sort(K key, SortArgs sortArgs);

    RedisFuture<Long> sort(ValueStreamingChannel<V> channel, K key, SortArgs sortArgs);

    RedisFuture<Long> sortStore(K key, SortArgs sortArgs, K destination);

    RedisFuture<V> spop(K key);

    RedisFuture<V> srandmember(K key);

    RedisFuture<Set<V>> srandmember(K key, long count);

    RedisFuture<Long> srandmember(ValueStreamingChannel<V> channel, K key, long count);

    RedisFuture<Long> srem(K key, V... members);

    RedisFuture<Set<V>> sunion(K... keys);

    RedisFuture<Long> sunion(ValueStreamingChannel<V> channel, K... keys);

    RedisFuture<Long> sunionstore(K destination, K... keys);

    RedisFuture<String> sync();

    RedisFuture<Long> strlen(K key);

    RedisFuture<Long> ttl(K key);

    RedisFuture<String> type(K key);

    RedisFuture<String> watch(K... keys);

    RedisFuture<String> unwatch();

    RedisFuture<Long> zadd(K key, double score, V member);

    @SuppressWarnings("unchecked")
    RedisFuture<Long> zadd(K key, Object... scoresAndValues);

    RedisFuture<Long> zcard(K key);

    RedisFuture<Long> zcount(K key, double min, double max);

    RedisFuture<Long> zcount(K key, String min, String max);

    RedisFuture<Double> zincrby(K key, double amount, K member);

    RedisFuture<Long> zinterstore(K destination, K... keys);

    RedisFuture<Long> zinterstore(K destination, ZStoreArgs storeArgs, K... keys);

    RedisFuture<List<V>> zrange(K key, long start, long stop);

    RedisFuture<List<ScoredValue<V>>> zrangeWithScores(K key, long start, long stop);

    RedisFuture<List<V>> zrangebyscore(K key, double min, double max);

    RedisFuture<List<V>> zrangebyscore(K key, String min, String max);

    RedisFuture<List<V>> zrangebyscore(K key, double min, double max, long offset, long count);

    RedisFuture<List<V>> zrangebyscore(K key, String min, String max, long offset, long count);

    RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, double min, double max);

    RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, String min, String max);

    RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, double min, double max, long offset, long count);

    RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, String min, String max, long offset, long count);

    RedisFuture<Long> zrange(ValueStreamingChannel<V> channel, K key, long start, long stop);

    RedisFuture<Long> zrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop);

    RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max);

    RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max);

    RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max, long offset, long count);

    RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max, long offset, long count);

    RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max);

    RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max);

    RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max,
            long offset, long count);

    RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max,
            long offset, long count);

    RedisFuture<Long> zrank(K key, V member);

    RedisFuture<Long> zrem(K key, V... members);

    RedisFuture<Long> zremrangebyrank(K key, long start, long stop);

    RedisFuture<Long> zremrangebyscore(K key, double min, double max);

    RedisFuture<Long> zremrangebyscore(K key, String min, String max);

    RedisFuture<List<V>> zrevrange(K key, long start, long stop);

    RedisFuture<List<ScoredValue<V>>> zrevrangeWithScores(K key, long start, long stop);

    RedisFuture<List<V>> zrevrangebyscore(K key, double max, double min);

    RedisFuture<List<V>> zrevrangebyscore(K key, String max, String min);

    RedisFuture<List<V>> zrevrangebyscore(K key, double max, double min, long offset, long count);

    RedisFuture<List<V>> zrevrangebyscore(K key, String max, String min, long offset, long count);

    RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, double max, double min);

    RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, String max, String min);

    RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, double max, double min, long offset, long count);

    RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, String max, String min, long offset, long count);

    RedisFuture<Long> zrevrange(ValueStreamingChannel<V> channel, K key, long start, long stop);

    RedisFuture<Long> zrevrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop);

    RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min);

    RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min);

    RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min, long offset, long count);

    RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min, long offset, long count);

    RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min);

    RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min);

    RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min,
            long offset, long count);

    RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min,
            long offset, long count);

    RedisFuture<Long> zrevrank(K key, V member);

    RedisFuture<Double> zscore(K key, V member);

    RedisFuture<Long> zunionstore(K destination, K... keys);

    RedisFuture<Long> zunionstore(K destination, ZStoreArgs storeArgs, K... keys);

    RedisFuture<List<V>> time();

    void close();

    String digest(V script);
}
