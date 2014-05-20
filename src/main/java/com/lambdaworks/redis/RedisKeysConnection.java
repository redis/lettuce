package com.lambdaworks.redis;

import java.util.Date;
import java.util.List;

import com.lambdaworks.redis.output.KeyStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingChannel;

/**
 * 
 * Synchronous executed commands for Keys (Key manipulation/querying).
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:10
 */
public interface RedisKeysConnection<K, V> extends BaseRedisConnection<K, V> {
    Long del(K... keys);

    byte[] dump(K key);

    Boolean exists(K key);

    Boolean expire(K key, long seconds);

    Boolean expireat(K key, Date timestamp);

    Boolean expireat(K key, long timestamp);

    List<K> keys(K pattern);

    Long keys(KeyStreamingChannel<K> channel, K pattern);

    String migrate(String host, int port, K key, int db, long timeout);

    Boolean move(K key, int db);

    String objectEncoding(K key);

    Long objectIdletime(K key);

    Long objectRefcount(K key);

    Boolean persist(K key);

    Boolean pexpire(K key, long milliseconds);

    Boolean pexpireat(K key, Date timestamp);

    Boolean pexpireat(K key, long timestamp);

    Long pttl(K key);

    V randomkey();

    String rename(K key, K newKey);

    Boolean renamenx(K key, K newKey);

    String restore(K key, long ttl, byte[] value);

    List<V> sort(K key);

    Long sort(ValueStreamingChannel<V> channel, K key);

    List<V> sort(K key, SortArgs sortArgs);

    Long sort(ValueStreamingChannel<V> channel, K key, SortArgs sortArgs);

    Long sortStore(K key, SortArgs sortArgs, K destination);

    Long ttl(K key);

    String type(K key);

    KeyScanCursor<K> scan();

    KeyScanCursor<K> scan(ScanArgs scanArgs);

    KeyScanCursor<K> scan(ScanCursor scanCursor, ScanArgs scanArgs);

    KeyScanCursor<K> scan(ScanCursor scanCursor);

    StreamScanCursor scan(KeyStreamingChannel<K> channel);

    StreamScanCursor scan(KeyStreamingChannel<K> channel, ScanArgs scanArgs);

    StreamScanCursor scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor, ScanArgs scanArgs);

    StreamScanCursor scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor);
}
