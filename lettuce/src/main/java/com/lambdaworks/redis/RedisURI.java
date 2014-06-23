package com.lambdaworks.redis;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis URI. Contains connection details for the Redis/Sentinel connections. You can provide as well the database, password and
 * timeouts within the RedisURI. Either build your self the object
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 14.05.14 21:30
 */
public class RedisURI implements Serializable {

    private static final long serialVersionUID = 42L;

    /**
     * The default sentinel port.
     */
    public static final int DEFAULT_SENTINEL_PORT = 26379;

    /**
     * The default redis port.
     */
    public static final int DEFAULT_REDIS_PORT = 6379;

    private String host;
    private String sentinelMasterId;
    private int port;
    private int database;
    private char[] password;
    private long timeout = 60;
    private TimeUnit unit = TimeUnit.SECONDS;
    private final List<RedisURI> sentinels = new ArrayList<RedisURI>();
    private transient SocketAddress resolvedAddress;

    /**
     * Default empty constructor.
     */
    public RedisURI() {
    }

    /**
     * Constructor with host/port and timeout.
     * 
     * @param host
     * @param port
     * @param timeout
     * @param unit
     */
    public RedisURI(String host, int port, long timeout, TimeUnit unit) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.unit = unit;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getSentinelMasterId() {
        return sentinelMasterId;
    }

    public void setSentinelMasterId(String sentinelMasterId) {
        this.sentinelMasterId = sentinelMasterId;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password.toCharArray();
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public void setUnit(TimeUnit unit) {
        this.unit = unit;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public List<RedisURI> getSentinels() {
        return sentinels;
    }

    public SocketAddress getResolvedAddress() {
        if (resolvedAddress == null) {
            resolvedAddress = new InetSocketAddress(host, port);
        }
        return resolvedAddress;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [host='").append(host).append('\'');
        sb.append(", port=").append(port);
        sb.append(']');
        return sb.toString();
    }

    /**
     * Builder for Redis URI.
     */
    public static class Builder {

        private final RedisURI redisURI = new RedisURI();

        /**
         * Set Redis host. Creates a new builder.
         * 
         * @param host
         * @return New builder with Redis host/port.
         */
        public static Builder redis(String host) {
            return redis(host, DEFAULT_REDIS_PORT);
        }

        /**
         * Set Redis host and port. Creates a new builder
         * 
         * @param host
         * @param port
         * @return New builder with Redis host/port.
         */
        public static Builder redis(String host, int port) {
            checkNotNull(host, "host must not be null");
            Builder builder = new Builder();
            builder.redisURI.setHost(host);
            builder.redisURI.setPort(port);
            return builder;
        }

        /**
         * Set Sentinel host. Creates a new builder.
         * 
         * @param host
         * @return New builder with Sentinel host/port.
         */
        public static Builder sentinel(String host) {
            return sentinel(host, DEFAULT_SENTINEL_PORT, null);
        }

        /**
         * Set Sentinel host and port. Creates a new builder.
         * 
         * @param host
         * @param port
         * @return New builder with Sentinel host/port.
         */
        public static Builder sentinel(String host, int port) {
            return sentinel(host, port, null);
        }

        /**
         * Set Sentinel host and master id. Creates a new builder.
         * 
         * @param host
         * @param masterId
         * @return New builder with Sentinel host/port.
         */
        public static Builder sentinel(String host, String masterId) {
            return sentinel(host, DEFAULT_SENTINEL_PORT, masterId);
        }

        /**
         * Set Sentinel host, port and master id. Creates a new builder.
         * 
         * @param host
         * @param port
         * @param masterId
         * @return New builder with Sentinel host/port.
         */
        public static Builder sentinel(String host, int port, String masterId) {
            checkNotNull(host, "host must not be null");
            Builder builder = new Builder();
            builder.redisURI.setSentinelMasterId(masterId);

            builder.redisURI.sentinels.add(new RedisURI(host, port, 1, TimeUnit.SECONDS));

            return builder;
        }

        /**
         * Add a withSentinel host to the existing builder.
         * 
         * @param host
         * @return the builder
         */
        public Builder withSentinel(String host) {
            return withSentinel(host, DEFAULT_SENTINEL_PORT);
        }

        /**
         * Add a withSentinel host/port to the existing builder.
         * 
         * @param host
         * @param port
         * @return the builder
         */
        public Builder withSentinel(String host, int port) {
            checkState(redisURI.host == null, "Cannot use with Redis mode.");
            checkNotNull(host, "host must not be null");
            redisURI.sentinels.add(new RedisURI(host, port, 1, TimeUnit.SECONDS));
            return this;
        }

        /**
         * Adds port information to the builder. Does only affect Redis URI, cannot be used with Sentinel connections.
         * 
         * @param port
         * @return the builder
         */
        public Builder withPort(int port) {
            checkState(redisURI.host != null, "Host is null. Cannot use in Sentinel mode.");
            redisURI.setPort(port);
            return this;
        }

        /**
         * Adds database selection.
         * 
         * @param database
         * @return the builder
         */
        public Builder withDatabase(int database) {
            redisURI.setDatabase(database);
            return this;
        }

        /**
         * Adds authentication.
         * 
         * @param password
         * @return the builder
         */
        public Builder withPassword(String password) {
            checkNotNull(password, "password must not be null");
            redisURI.setPassword(password);
            return this;
        }

        /**
         * Adds timeout.
         * 
         * @param timeout
         * @param unit
         * @return the builder
         */
        public Builder withTimeout(long timeout, TimeUnit unit) {
            checkNotNull(unit, "unit must not be null");
            redisURI.setTimeout(timeout);
            redisURI.setUnit(unit);
            return this;
        }

        /**
         * 
         * @return the RedisURI.
         */
        public RedisURI build() {
            return redisURI;
        }

    }

}
