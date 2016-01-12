package com.lambdaworks.redis;

import com.lambdaworks.redis.SimpleSerializers.JavaSerializer;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author <a href="mailto:a.abdelfatah@live.com">Ahmed Kamal</a>
 * @since 12.01.16 09:17
 */

/**
 * A simple redis connection for caching and retrieval from redis
 */
public class SimpleRedisConnection {

    private final RedisClient redisClient;
    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(SimpleRedisConnection.class);

    public SimpleRedisConnection(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    /**
     * Cache generic value in redis (The class of the value should implement Serializable)
     * <br/>
     * Sets field in the hash stored at key to value. If key does not exist, a new key holding a hash is created.
     * <br/>
     * If field already exists in the hash, it is overwritten.
     *
     * @param key
     * @param field
     * @param value
     * @param <T>
     * @return true if field is a new field in the hash and value was set.
     * <br/> false if field already exists in the hash and the value was updated.
     */
    public <T> boolean cache(String key, String field, T value) {

        RedisConnection<byte[], byte[]> redisCommands = null;
        try {
            redisCommands = getRedisConnection();

            byte[] serializedValues = new JavaSerializer().serializeObject(value);
            redisCommands.hset(key.getBytes(), field.getBytes(), serializedValues);

            return true;
        } catch (Exception e) {
            logger.error("Exception Occurred while caching a value in redis", e);
        } finally {
            closeConnection(redisCommands);
        }
        return false;
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
            T deserializeObject = new JavaSerializer().deserializeObject(serializedData);

            return deserializeObject;
        } catch (Exception e) {
            logger.error("Exception Occurred while getting a value from redis", e);
        } finally {
            closeConnection(redisCommands);
        }
        return null;
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

            int maxBytes = Integer.MIN_VALUE;

            for (int i = 0; i < fields.length; i++) {
                if (fields[i].getBytes().length > maxBytes)
                    maxBytes = fields[i].getBytes().length;
            }

            byte[][] fieldsInBytes = new byte[fields.length][maxBytes + 1];
            for (int i = 0; i < fields.length; i++) {
                fieldsInBytes[i] = fields[i].getBytes();
            }

            return redisCommands.hdel(key.getBytes(), fieldsInBytes);

        } catch (Exception e) {
            logger.error("Exception Occurred while clearing a value from redis", e);
        } finally {
            closeConnection(redisCommands);
        }
        return new Long(-1);
    }

    private synchronized RedisConnection getRedisConnection() {
        try {
            RedisConnection connection = redisClient.connect(new ByteArrayCodec());

            logger.info("Redis Connection is Opened");

            return connection;

        } catch (RedisConnectionException e) {
            logger.error("Exception Occurred while connecting to the server", e);
            throw e;

        } catch (Exception e) {
            logger.error("Exception Occurred while connecting to the server", e);
        }
        return null;
    }

    private <K, V> void closeConnection(RedisConnection<K, V> connection) {
        if (connection != null) {
            connection.close();
            logger.info("Redis Connection is Closed");
        }
    }
}