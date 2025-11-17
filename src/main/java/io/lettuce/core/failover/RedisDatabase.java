package io.lettuce.core.failover;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.failover.CircuitBreaker.CircuitBreakerConfig;

/**
 * Represents a Redis database with a weight and a connection.
 *
 * @param <C> Connection type.
 * 
 * @author Ali Takavci
 * @since 7.1
 */
public class RedisDatabase<C extends StatefulRedisConnection<?, ?>> {

    private final float weight;

    private final C connection;

    private final RedisURI redisURI;

    private final DatabaseEndpoint databaseEndpoint;

    private final CircuitBreaker circuitBreaker;

    public RedisDatabase(RedisDatabaseConfig config, C connection, DatabaseEndpoint databaseEndpoint) {
        this.redisURI = config.redisURI;
        this.weight = config.weight;
        this.connection = connection;
        this.databaseEndpoint = databaseEndpoint;
        this.circuitBreaker = new CircuitBreaker(config.circuitBreakerConfig);
        databaseEndpoint.setCircuitBreaker(circuitBreaker);
    }

    public float getWeight() {
        return weight;
    }

    public C getConnection() {
        return connection;
    }

    public RedisURI getRedisURI() {
        return redisURI;
    }

    public DatabaseEndpoint getDatabaseEndpoint() {
        return databaseEndpoint;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public static class RedisDatabaseConfig {

        private RedisURI redisURI;

        private float weight;

        private CircuitBreakerConfig circuitBreakerConfig;

        public RedisDatabaseConfig(RedisURI redisURI, float weight) {
            this.redisURI = redisURI;
            this.weight = weight;
            this.circuitBreakerConfig = CircuitBreakerConfig.DEFAULT;
        }

        public RedisDatabaseConfig(RedisURI redisURI, float weight, CircuitBreakerConfig circuitBreakerConfig) {
            this.redisURI = redisURI;
            this.weight = weight;
            this.circuitBreakerConfig = circuitBreakerConfig;
        }

    }

}
