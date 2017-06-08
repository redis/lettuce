package com.lambdaworks.redis;

import com.lambdaworks.redis.SimpleSerializers.JavaSerializer;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.Serializable;

/**
 * A simple redis connection for caching and retrieval from redis
 *
 * @author <a href="mailto:a.abdelfatah@live.com">Ahmed Kamal</a>
 * @since 12.01.16 09:17
 */

public class SimpleRedisConnection {

    private final RedisClient redisClient;
    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(SimpleRedisConnection.class);
    private JavaSerializer codec;

    /**
     * creates a simple redis connection providing redis client
     * @param redisClient
     */
    public SimpleRedisConnection(RedisClient redisClient) {

        if (redisClient == null)
            throw new IllegalArgumentException("Redis Client argument shouldn't be null");

        this.redisClient = redisClient;
        this.codec = new JavaSerializer();
    }

    /**
     * Cache generic serializable value in redis
     * <br/>
     * Sets field in the hash stored at key to value. If key does not exist, a new key holding a hash is created.
     * <br/>
     * If field already exists in the hash, it is overwritten.
     *
     * @param key
     * @param field
     * @param value that implements serializable
     * @param <T>
     * @return true if field is a new field in the hash and value was set.
     * <br/> false if field already exists in the hash and the value was updated.
     */
    public <T extends Serializable> boolean cache(String key, String field, T value) {

        RedisConnection<byte[], byte[]> redisCommands = null;
        try {
            redisCommands = getRedisConnection();

            byte[] serializedValues = codec.serializeObject(value);
            return redisCommands.hset(key.getBytes(), field.getBytes(), serializedValues);

        } finally {
            closeConnection(redisCommands);
        }
    }

    /**
     * Get a generic cached value from redis
     * Returns the value associated with field in the hash stored at key.
     *
     * @param key
     * @param field
     * @param <T>
     * @return the value stored in a deserialized form or null when field is not present in the hash or key does not exist.
     */
    public <T> T get(String key, String field) {

        RedisConnection<byte[], byte[]> redisCommands = null;
        try {
            redisCommands = getRedisConnection();

            byte[] serializedData = redisCommands.hget(key.getBytes(), field.getBytes());
            T deserializeObject = codec.deserializeObject(serializedData);

            return deserializeObject;
        } finally {
            closeConnection(redisCommands);
        }
    }

    /**
     * Clear a cached value in redis
     * Removes the specified fields from the hash stored at key. Specified fields that do not exist within this hash are ignored.
     *
     * @param key
     * @param fields
     * @return the number of fields that were removed from the hash, not including specified but non existing fields.
     */
    public Long clear(String key, String... fields) {

        RedisConnection<byte[], byte[]> redisCommands = null;
        try {
            redisCommands = getRedisConnection();

            byte[][] fieldsInBytes = new byte[fields.length][];
            for (int i = 0; i < fields.length; i++) {
                fieldsInBytes[i] = fields[i].getBytes();
            }

            return redisCommands.hdel(key.getBytes(), fieldsInBytes);
        } finally {
            closeConnection(redisCommands);
        }
    }

    private synchronized RedisConnection getRedisConnection() {
            RedisConnection connection = redisClient.connect(new ByteArrayCodec());

            logger.info("Redis Connection is Opened");

            return connection;
    }

    private <K, V> void closeConnection(RedisConnection<K, V> connection) {
        if (connection != null) {
            connection.close();
            logger.info("Redis Connection is Closed");
        }
    }
}