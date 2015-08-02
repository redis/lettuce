package com.lambdaworks.redis;

import static com.google.common.base.Preconditions.*;
import static com.lambdaworks.redis.LettuceStrings.*;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableSet;
import com.google.common.net.HostAndPort;

/**
 * Redis URI. Contains connection details for the Redis/Sentinel connections. You can provide the database, password and
 * timeouts within the RedisURI.
 *
 * You have following possibilities to create a {@link RedisURI}:
 *
 * <ul>
 * <li>Use an URI:
 * <p>
 * {@code RedisURI.create("redis://localhost/")}
 * </p>
 * See {@link #create(String)} for more options</li>
 * <li>Use an the Builder:
 * <p>
 * {@code RedisURI.Builder.redis("localhost", 6379).auth("password").database(1).build() }
 * </p>
 * See {@link com.lambdaworks.redis.RedisURI.Builder#redis(String)} and
 * {@link com.lambdaworks.redis.RedisURI.Builder#sentinel(String)} for more options.</li>
 * <li>Construct your own instance:
 * <p>
 * {@code new RedisURI("localhost", 6379, 60, TimeUnit.SECONDS)}
 * </p>
 * or
 * <p>
 * <code>RedisURI uri = new RedisURI();
 uri.setHost("localhost")</code>
 * </p>
 * </li>
 * </ul>
 *
 * RedisURI supports Redis Standalone, Redis Sentinel and Redis Cluster with plain, SSL, TLS and unix domain socket connections.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class RedisURI implements Serializable, ConnectionPoint {

    public static final String URI_SCHEME_REDIS_SENTINEL = "redis-sentinel";
    public static final String URI_SCHEME_REDIS = "redis";
    public static final String URI_SCHEME_REDIS_SECURE = "rediss";
    public static final String URI_SCHEME_REDIS_SOCKET = "redis-socket";

    /**
     * The default sentinel port.
     */
    public static final int DEFAULT_SENTINEL_PORT = 26379;

    /**
     * The default redis port.
     */
    public static final int DEFAULT_REDIS_PORT = 6379;

    private String host;
    private String socket;
    private String sentinelMasterId;
    private int port;
    private int database;
    private char[] password;
    private boolean ssl = false;
    private boolean verifyPeer = true;
    private boolean startTls = false;
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
     * @param host the host
     * @param port the port
     * @param timeout timeout value
     * @param unit unit of the timeout value
     */
    public RedisURI(String host, int port, long timeout, TimeUnit unit) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.unit = unit;
    }

    /**
     * Create a Redis URI from an URI string. Supported formats are:
     * <ul>
     * <li>redis-sentinel://[password@]host[:port][,host2[:port2]][/databaseNumber]#sentinelMasterId</li>
     * <li>redis://[password@]host[:port][/databaseNumber]</li>
     * </ul>
     *
     * The uri must follow conventions of {@link java.net.URI}
     * 
     * @param uri The URI string.
     * @return An instance of {@link RedisURI} containing details from the URI.
     */
    public static RedisURI create(String uri) {
        return create(URI.create(uri));
    }

    /**
     * Create a Redis URI from an URI string. Supported formats are:
     * <ul>
     * <li>redis-sentinel://[password@]host[:port][,host2[:port2]][/databaseNumber]#sentinelMasterId</li>
     * <li>redis://[password@]host[:port][/databaseNumber]</li>
     * </ul>
     *
     * The uri must follow conventions of {@link java.net.URI}
     *
     * @param uri The URI.
     * @return An instance of {@link RedisURI} containing details from the URI.
     */
    public static RedisURI create(URI uri) {

        RedisURI.Builder builder;
        if (uri.getScheme().equals(URI_SCHEME_REDIS_SENTINEL)) {
            builder = configureSentinel(uri);
        } else {
            builder = configureStandalone(uri);
        }

        String userInfo = uri.getUserInfo();

        if (isEmpty(userInfo) && isNotEmpty(uri.getAuthority()) && uri.getAuthority().indexOf('@') > 0) {
            userInfo = uri.getAuthority().substring(0, uri.getAuthority().indexOf('@'));
        }

        if (isNotEmpty(userInfo)) {
            String password = userInfo;
            if (password.startsWith(":")) {
                password = password.substring(1);
            }
            builder.withPassword(password);
        }

        if (isNotEmpty(uri.getPath()) && builder.redisURI.getSocket() == null) {
            String pathSuffix = uri.getPath().substring(1);

            if (isNotEmpty(pathSuffix)) {
                builder.withDatabase(Integer.parseInt(pathSuffix));
            }
        }

        return builder.build();

    }

    private static Builder configureStandalone(URI uri) {
        Builder builder;
        Set<String> allowedSchemes = ImmutableSet.of(URI_SCHEME_REDIS, URI_SCHEME_REDIS_SECURE, URI_SCHEME_REDIS_SOCKET);

        if (!allowedSchemes.contains(uri.getScheme())) {
            throw new IllegalArgumentException("Scheme " + uri.getScheme() + " not supported");
        }

        if (URI_SCHEME_REDIS_SOCKET.equals(uri.getScheme())) {
            builder = Builder.socket(uri.getPath());
        } else {
            if (uri.getPort() > 0) {
                builder = Builder.redis(uri.getHost(), uri.getPort());
            } else {
                builder = Builder.redis(uri.getHost());
            }
        }

        if (URI_SCHEME_REDIS_SECURE.equals(uri.getScheme())) {
            builder.withSsl(true);
        }
        return builder;
    }

    private static RedisURI.Builder configureSentinel(URI uri) {
        checkArgument(isNotEmpty(uri.getFragment()), "URI Fragment must contain the sentinelMasterId");
        String masterId = uri.getFragment();

        RedisURI.Builder builder = null;

        if (isNotEmpty(uri.getHost())) {
            if (uri.getPort() != -1) {
                builder = RedisURI.Builder.sentinel(uri.getHost(), uri.getPort(), masterId);
            } else {
                builder = RedisURI.Builder.sentinel(uri.getHost(), masterId);
            }
        }

        if (builder == null && isNotEmpty(uri.getAuthority())) {
            String authority = uri.getAuthority();
            if (authority.indexOf('@') > -1) {
                authority = authority.substring(authority.indexOf('@') + 1);
            }

            String[] hosts = authority.split("\\,");
            for (String host : hosts) {
                HostAndPort hostAndPort = HostAndPort.fromString(host);
                if (builder == null) {
                    if (hostAndPort.hasPort()) {
                        builder = RedisURI.Builder.sentinel(hostAndPort.getHostText(), hostAndPort.getPort(), masterId);
                    } else {
                        builder = RedisURI.Builder.sentinel(hostAndPort.getHostText(), masterId);
                    }
                } else {
                    if (hostAndPort.hasPort()) {
                        builder.withSentinel(hostAndPort.getHostText(), hostAndPort.getPort());
                    } else {
                        builder.withSentinel(hostAndPort.getHostText());
                    }
                }
            }

        }

        checkArgument(builder != null, "Invalid URI, cannot get host part");
        return builder;
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

    public String getSocket() {
        return socket;
    }

    public void setSocket(String socket) {
        this.socket = socket;
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

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public boolean isVerifyPeer() {
        return verifyPeer;
    }

    public void setVerifyPeer(boolean verifyPeer) {
        this.verifyPeer = verifyPeer;
    }

    public boolean isStartTls() {
        return startTls;
    }

    public void setStartTls(boolean startTls) {
        this.startTls = startTls;
    }

    public List<RedisURI> getSentinels() {
        return sentinels;
    }

    /**
     * 
     * @return the resolved {@link SocketAddress} based either on host/port or the socket.
     */
    public SocketAddress getResolvedAddress() {
        if (resolvedAddress == null) {
            resolveAddress();
        }

        return resolvedAddress;
    }

    /**
     * Resolve the address.
     */
    private void resolveAddress() {
        if (getSocket() != null) {
            resolvedAddress = EpollProvider.newSocketAddress(getSocket());
        } else {
            resolvedAddress = new InetSocketAddress(host, port);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());

        sb.append(" [");

        if (host != null) {
            sb.append("host='").append(host).append('\'');
            sb.append(", port=").append(port);
        }

        if (socket != null) {
            sb.append("socket='").append(socket).append('\'');
        }

        if (sentinelMasterId != null) {
            sb.append(", sentinelMasterId=").append(sentinelMasterId);
        }

        sb.append(']');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RedisURI))
            return false;

        RedisURI redisURI = (RedisURI) o;

        if (port != redisURI.port)
            return false;
        if (database != redisURI.database)
            return false;
        if (host != null ? !host.equals(redisURI.host) : redisURI.host != null)
            return false;
        if (socket != null ? !socket.equals(redisURI.socket) : redisURI.socket != null)
            return false;
        if (sentinelMasterId != null ? !sentinelMasterId.equals(redisURI.sentinelMasterId) : redisURI.sentinelMasterId != null)
            return false;
        return !(sentinels != null ? !sentinels.equals(redisURI.sentinels) : redisURI.sentinels != null);

    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + (socket != null ? socket.hashCode() : 0);
        result = 31 * result + (sentinelMasterId != null ? sentinelMasterId.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + database;
        result = 31 * result + (sentinels != null ? sentinels.hashCode() : 0);
        return result;
    }

    /**
     * Builder for Redis URI.
     */
    public static class Builder {

        private final RedisURI redisURI = new RedisURI();

        /**
         * Set Redis socket. Creates a new builder.
         * 
         * @param socket the host name
         * @return New builder with Redis socket.
         */
        public static Builder socket(String socket) {
            checkNotNull(socket, "Socket must not be null");
            Builder builder = new Builder();
            builder.redisURI.setSocket(socket);
            return builder;
        }

        /**
         * Set Redis host. Creates a new builder.
         *
         * @param host the host name
         * @return New builder with Redis host/port.
         */
        public static Builder redis(String host) {
            return redis(host, DEFAULT_REDIS_PORT);
        }

        /**
         * Set Redis host and port. Creates a new builder
         * 
         * @param host the host name
         * @param port the port
         * @return New builder with Redis host/port.
         */
        public static Builder redis(String host, int port) {
            checkNotNull(host, "Host must not be null");
            Builder builder = new Builder();
            builder.redisURI.setHost(host);
            builder.redisURI.setPort(port);
            return builder;
        }

        /**
         * Set Sentinel host. Creates a new builder.
         * 
         * @param host the host name
         * @return New builder with Sentinel host/port.
         */
        public static Builder sentinel(String host) {
            return sentinel(host, DEFAULT_SENTINEL_PORT, null);
        }

        /**
         * Set Sentinel host and port. Creates a new builder.
         * 
         * @param host the host name
         * @param port the port
         * @return New builder with Sentinel host/port.
         */
        public static Builder sentinel(String host, int port) {
            return sentinel(host, port, null);
        }

        /**
         * Set Sentinel host and master id. Creates a new builder.
         * 
         * @param host the host name
         * @param masterId sentinel master id
         * @return New builder with Sentinel host/port.
         */
        public static Builder sentinel(String host, String masterId) {
            return sentinel(host, DEFAULT_SENTINEL_PORT, masterId);
        }

        /**
         * Set Sentinel host, port and master id. Creates a new builder.
         * 
         * @param host the host name
         * @param port the port
         * @param masterId sentinel master id
         * @return New builder with Sentinel host/port.
         */
        public static Builder sentinel(String host, int port, String masterId) {
            checkNotNull(host, "Host must not be null");
            Builder builder = new Builder();
            builder.redisURI.setSentinelMasterId(masterId);

            builder.redisURI.sentinels.add(new RedisURI(host, port, 1, TimeUnit.SECONDS));

            return builder;
        }

        /**
         * Add a withSentinel host to the existing builder.
         * 
         * @param host the host name
         * @return the builder
         */
        public Builder withSentinel(String host) {
            return withSentinel(host, DEFAULT_SENTINEL_PORT);
        }

        /**
         * Add a withSentinel host/port to the existing builder.
         * 
         * @param host the host name
         * @param port the port
         * @return the builder
         */
        public Builder withSentinel(String host, int port) {
            checkState(redisURI.host == null, "Cannot use with Redis mode.");
            checkNotNull(host, "Host must not be null");
            redisURI.sentinels.add(new RedisURI(host, port, 1, TimeUnit.SECONDS));
            return this;
        }

        /**
         * Adds port information to the builder. Does only affect Redis URI, cannot be used with Sentinel connections.
         * 
         * @param port the port
         * @return the builder
         */
        public Builder withPort(int port) {
            checkState(redisURI.host != null, "Host is null. Cannot use in Sentinel mode.");
            redisURI.setPort(port);
            return this;
        }

        /**
         * Adds ssl information to the builder. Does only affect Redis URI, cannot be used with Sentinel connections.
         *
         * @param ssl {@literal true} if use SSL
         * @return the builder
         */
        public Builder withSsl(boolean ssl) {
            checkState(redisURI.host != null, "Host is null. Cannot use in Sentinel mode.");
            redisURI.setSsl(ssl);
            return this;
        }

        /**
         * Enables/disables StartTLS when using SSL. Does only affect Redis URI, cannot be used with Sentinel connections.
         *
         * @param startTls {@literal true} if use StartTLS
         * @return the builder
         */
        public Builder withStartTls(boolean startTls) {
            checkState(redisURI.host != null, "Host is null. Cannot use in Sentinel mode.");
            redisURI.setStartTls(startTls);
            return this;
        }

        /**
         * Enables/disables peer verification. Does only affect Redis URI, cannot be used with Sentinel connections.
         *
         * @param verifyPeer {@literal true} to verify hosts when using SSL
         * @return the builder
         */
        public Builder withVerifyPeer(boolean verifyPeer) {
            checkState(redisURI.host != null, "Host is null. Cannot use in Sentinel mode.");
            redisURI.setVerifyPeer(verifyPeer);
            return this;
        }

        /**
         * Adds database selection.
         * 
         * @param database the database number
         * @return the builder
         */
        public Builder withDatabase(int database) {
            redisURI.setDatabase(database);
            return this;
        }

        /**
         * Adds authentication.
         * 
         * @param password the password
         * @return the builder
         */
        public Builder withPassword(String password) {
            checkNotNull(password, "Password must not be null");
            redisURI.setPassword(password);
            return this;
        }

        /**
         * Adds timeout.
         * 
         * @param timeout must be greater or equal 0"
         * @param unit the timeout time unit.
         * @return the builder
         */
        public Builder withTimeout(long timeout, TimeUnit unit) {
            checkNotNull(unit, "TimeUnit must not be null");
            checkArgument(timeout >= 0, "Timeout must be greater or equal 0");
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
