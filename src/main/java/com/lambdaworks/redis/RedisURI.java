package com.lambdaworks.redis;

import static com.lambdaworks.redis.LettuceStrings.isEmpty;
import static com.lambdaworks.redis.LettuceStrings.isNotEmpty;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.lambdaworks.redis.internal.HostAndPort;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.internal.LettuceSets;
import com.lambdaworks.redis.protocol.LettuceCharsets;

/**
 * Redis URI. Contains connection details for the Redis/Sentinel connections. You can provide the database, password and
 * timeouts within the RedisURI.
 *
 * You have following possibilities to create a {@link RedisURI}:
 *
 * <ul>
 * <li>Use an URI:
 * <p>
 * {@code RedisURI.create("redis://localhost/");}
 * </p>
 * See {@link #create(String)} for more options</li>
 * <li>Use the Builder:
 * <p>
 * {@code RedisURI.Builder.redis("localhost", 6379).auth("password").database(1).build(); }
 * </p>
 * See {@link com.lambdaworks.redis.RedisURI.Builder#redis(String)} and
 * {@link com.lambdaworks.redis.RedisURI.Builder#sentinel(String)} for more options.</li>
 * <li>Construct your own instance:
 * <p>
 * {@code new RedisURI("localhost", 6379, 60, TimeUnit.SECONDS);}
 * </p>
 * or
 * <p>
 *     {@code RedisURI uri = new RedisURI();
 *     uri.setHost("localhost");
 *     }
 * </p>
 * </li>
 * </ul>
 *
 * <h3>URI syntax</h3>
 *
 * <b>Redis Standalone</b> <blockquote> <i>redis</i><b>{@code ://}</b>[<i>password@</i>]<i>host</i> [<b>{@code :}
 * </b><i>port</i>][<b>{@code /}</b><i>database</i>][<b>{@code ?}</b>
 * [<i>timeout=timeout</i>[<i>d|h|m|s|ms|us|ns</i>]] [<i>&database=database</i>]] </blockquote>
 *
 * <b>Redis Standalone (SSL)</b> <blockquote> <i>rediss</i><b>{@code ://}</b>[<i>password@</i>]<i>host</i> [<b>{@code :}
 * </b><i>port</i>][<b>{@code /}</b><i>database</i>][<b>{@code ?}</b>
 * [<i>timeout=timeout</i>[<i>d|h|m|s|ms|us|ns</i>]] [<i>&database=database</i>]] </blockquote>
 *
 * Redis Standalone (Unix Domain Sockets)</b> <blockquote> <i>redis-socket</i><b>{@code ://}
 * </b>[<i>password@</i>]<i>path</i>[<b>{@code ?}</b>[<i>timeout=timeout</i>[<i>d|h|m|s|ms|us|ns</i>]][<i>&database=database</i>]]
 * </blockquote>
 *
 * <b>Redis Sentinel</b> <blockquote> <i>redis-sentinel</i><b>{@code ://}</b>[<i>password@</i>]<i>host1</i> [<b>{@code :}
 * </b><i>port1</i>][, <i>host2</i> [<b>{@code :}</b><i>port2</i>]][, <i>hostN</i> [<b>{@code :}</b><i>portN</i>]][<b>{@code /}
 * </b><i>database</i>][<b>{@code ?}
 * </b>[<i>timeout=timeout</i>[<i>d|h|m|s|ms|us|ns</i>]] [<i>&sentinelMasterId=sentinelMasterId</i>] [<i>&database=database</i>]]
 * </blockquote>
 *
 * <p>
 * <b>Schemes</b>
 * </p>
 * <ul>
 * <li><b>redis</b> Redis Standalone</li>
 * <li><b>rediss</b> Redis Standalone SSL</li>
 * <li><b>redis-socket</b> Redis Standalone Unix Domain Socket</li>
 * <li><b>redis-sentinel</b> Redis Sentinel</li>
 * </ul>
 *
 * <p>
 * <b>Timeout units</b>
 * </p>
 * <ul>
 * <li><b>d</b> Days</li>
 * <li><b>h</b> Hours</li>
 * <li><b>m</b> Minutes</li>
 * <li><b>s</b> Seconds</li>
 * <li><b>ms</b> Milliseconds</li>
 * <li><b>us</b> Microseconds</li>
 * <li><b>ns</b> Nanoseconds</li>
 * </ul>
 *
 * <p>
 * Hint: The database parameter within the query part has higher precedence than the database in the path.
 * </p>
 * 
 * 
 * RedisURI supports Redis Standalone, Redis Sentinel and Redis Cluster with plain, SSL, TLS and unix domain socket connections.
 * 
 * @author Mark Paluch
 * @since 3.0
 */
@SuppressWarnings("serial")
public class RedisURI implements Serializable, ConnectionPoint {

    public static final String URI_SCHEME_REDIS_SENTINEL = "redis-sentinel";
    public static final String URI_SCHEME_REDIS = "redis";
    public static final String URI_SCHEME_REDIS_SECURE = "rediss";
    public static final String URI_SCHEME_REDIS_SECURE_ALT = "redis+ssl";
    public static final String URI_SCHEME_REDIS_TLS_ALT = "redis+tls";
    public static final String URI_SCHEME_REDIS_SOCKET = "redis-socket";
    public static final String URI_SCHEME_REDIS_SOCKET_ALT = "redis+socket";
    public static final String PARAMETER_NAME_TIMEOUT = "timeout";
    public static final String PARAMETER_NAME_DATABASE = "database";
    public static final String PARAMETER_NAME_DATABASE_ALT = "db";
    public static final String PARAMETER_NAME_SENTINEL_MASTER_ID = "sentinelMasterId";

    public static final Map<String, TimeUnit> TIME_UNIT_MAP;

    static {
        Map<String, TimeUnit> unitMap = new HashMap<String, TimeUnit>();
        unitMap.put("ns", TimeUnit.NANOSECONDS);
        unitMap.put("us", TimeUnit.MICROSECONDS);
        unitMap.put("ms", TimeUnit.MILLISECONDS);
        unitMap.put("s", TimeUnit.SECONDS);
        unitMap.put("m", TimeUnit.MINUTES);
        unitMap.put("h", TimeUnit.HOURS);
        unitMap.put("d", TimeUnit.DAYS);
        TIME_UNIT_MAP = Collections.unmodifiableMap(unitMap);
    }

    /**
     * The default sentinel port.
     */
    public static final int DEFAULT_SENTINEL_PORT = 26379;

    /**
     * The default redis port.
     */
    public static final int DEFAULT_REDIS_PORT = 6379;

    /**
     * Default timeout: 60 sec
     */
    public static final long DEFAULT_TIMEOUT = 60;
    public static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

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
    private final List<RedisURI> sentinels = new ArrayList<>();

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
     * Create a Redis URI from host and port.
     *
     * @param host the host
     * @param port the port
     * @return An instance of {@link RedisURI} containing details from the {@code host} and {@code port}.
     */
    public static RedisURI create(String host, int port) {
        return new Builder().redis(host, port).build();
    }

    /**
     * Create a Redis URI from an URI string.
     *
     * The uri must follow conventions of {@link java.net.URI}
     *
     * @param uri The URI string.
     * @return An instance of {@link RedisURI} containing details from the URI.
     */
    public static RedisURI create(String uri) {
        LettuceAssert.notEmpty(uri, "uri must not be empty");
        return create(URI.create(uri));
    }

    /**
     * Create a Redis URI from an URI string:
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
            } else {

                int index = password.indexOf(':');
                if (index > 0) {
                    password = password.substring(index + 1);
                }
            }
            if (password != null && !password.equals("")) {
                builder.withPassword(password);
            }
        }

        if (isNotEmpty(uri.getPath()) && builder.socket == null) {
            String pathSuffix = uri.getPath().substring(1);

            if (isNotEmpty(pathSuffix)) {
                builder.withDatabase(Integer.parseInt(pathSuffix));
            }
        }

        if (isNotEmpty(uri.getQuery())) {
            StringTokenizer st = new StringTokenizer(uri.getQuery(), "&;");
            while (st.hasMoreTokens()) {
                String queryParam = st.nextToken();
                String forStartWith = queryParam.toLowerCase();
                if (forStartWith.startsWith(PARAMETER_NAME_TIMEOUT + "=")) {
                    parseTimeout(builder, queryParam.toLowerCase());
                }

                if (forStartWith.startsWith(PARAMETER_NAME_DATABASE + "=")
                        || queryParam.startsWith(PARAMETER_NAME_DATABASE_ALT + "=")) {
                    parseDatabase(builder, queryParam);
                }

                if (forStartWith.startsWith(PARAMETER_NAME_SENTINEL_MASTER_ID.toLowerCase() + "=")) {
                    parseSentinelMasterId(builder, queryParam);
                }
            }
        }

        if (uri.getScheme().equals(URI_SCHEME_REDIS_SENTINEL)) {
            LettuceAssert.notEmpty(builder.sentinelMasterId, "URI must contain the sentinelMasterId");
        }

        return builder.build();

    }

    private static void parseTimeout(Builder builder, String queryParam) {
        int index = queryParam.indexOf('=');
        if (index < 0) {
            return;
        }

        String timeoutString = queryParam.substring(index + 1);

        int numbersEnd = 0;
        while (numbersEnd < timeoutString.length() && Character.isDigit(timeoutString.charAt(numbersEnd))) {
            numbersEnd++;
        }

        if (numbersEnd == 0) {
            if (timeoutString.startsWith("-")) {
                builder.withTimeout(0, TimeUnit.MILLISECONDS);
            } else {
                // no-op, leave defaults
            }
        } else {
            String timeoutValueString = timeoutString.substring(0, numbersEnd);
            long timeoutValue = Long.parseLong(timeoutValueString);
            builder.withTimeout(timeoutValue, TimeUnit.MILLISECONDS);

            String suffix = timeoutString.substring(numbersEnd);
            TimeUnit timeoutUnit = TIME_UNIT_MAP.get(suffix);
            if (timeoutUnit == null) {
                timeoutUnit = TimeUnit.MILLISECONDS;
            }

            builder.withTimeout(timeoutValue, timeoutUnit);
        }
    }

    private static void parseDatabase(Builder builder, String queryParam) {
        int index = queryParam.indexOf('=');
        if (index < 0) {
            return;
        }

        String databaseString = queryParam.substring(index + 1);

        int numbersEnd = 0;
        while (numbersEnd < databaseString.length() && Character.isDigit(databaseString.charAt(numbersEnd))) {
            numbersEnd++;
        }

        if (numbersEnd != 0) {
            String databaseValueString = databaseString.substring(0, numbersEnd);
            int value = Integer.parseInt(databaseValueString);
            builder.withDatabase(value);
        }
    }

    private static void parseSentinelMasterId(Builder builder, String queryParam) {
        int index = queryParam.indexOf('=');
        if (index < 0) {
            return;
        }

        String masterIdString = queryParam.substring(index + 1);
        if (isNotEmpty(masterIdString)) {
            builder.withSentinelMasterId(masterIdString);
        }
    }

    private static Builder configureStandalone(URI uri) {
        Builder builder;
        Set<String> allowedSchemes = LettuceSets.unmodifiableSet(URI_SCHEME_REDIS, URI_SCHEME_REDIS_SECURE, URI_SCHEME_REDIS_SOCKET,
                URI_SCHEME_REDIS_SOCKET_ALT, URI_SCHEME_REDIS_SECURE_ALT, URI_SCHEME_REDIS_TLS_ALT);

        if (!allowedSchemes.contains(uri.getScheme())) {
            throw new IllegalArgumentException("Scheme " + uri.getScheme() + " not supported");
        }

        if (URI_SCHEME_REDIS_SOCKET.equals(uri.getScheme()) || URI_SCHEME_REDIS_SOCKET_ALT.equals(uri.getScheme())) {
            builder = Builder.socket(uri.getPath());
        } else {
            if (uri.getPort() > 0) {
                builder = Builder.redis(uri.getHost(), uri.getPort());
            } else {
                builder = Builder.redis(uri.getHost());
            }
        }

        if (URI_SCHEME_REDIS_SECURE.equals(uri.getScheme()) || URI_SCHEME_REDIS_SECURE_ALT.equals(uri.getScheme())) {
            builder.withSsl(true);
        }

        if (URI_SCHEME_REDIS_TLS_ALT.equals(uri.getScheme())) {
            builder.withSsl(true);
            builder.withStartTls(true);
        }
        return builder;
    }

    private static RedisURI.Builder configureSentinel(URI uri) {
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
                HostAndPort hostAndPort = HostAndPort.parse(host);
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

        LettuceAssert.notNull(builder, "Invalid URI, cannot get host part");
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
     * Creates an URI based on the RedisURI.
     * 
     * @return URI based on the RedisURI
     */
    public URI toURI() {
        String scheme = getScheme();
        String authority = getAuthority(scheme);
        String queryString = getQueryString();
        String uri = scheme + "://" + authority;

        if (!queryString.isEmpty()) {
            uri += "?" + queryString;
        }

        return URI.create(uri);
    }

    private String getAuthority(final String scheme) {

        String authority = null;
        if (host != null) {
            authority = urlEncode(host) + getPortPart(port, scheme);
        }

        if (sentinels.size() != 0) {
            String joinedSentinels = sentinels.stream()
                    .map(redisURI -> urlEncode(redisURI.getHost()) + getPortPart(redisURI.getPort(), scheme))
                    .collect(Collectors.joining(","));

            authority = joinedSentinels;
        }

        if (socket != null) {
            authority = urlEncode(socket);
        }

        if (password != null && password.length != 0) {
            authority = urlEncode(new String(password)) + "@" + authority;
        }
        return authority;
    }

    private String getQueryString() {
        List<String> queryPairs = new ArrayList<>();

        if (database != 0) {
            queryPairs.add(PARAMETER_NAME_DATABASE + "=" + database);
        }

        if (sentinelMasterId != null) {
            queryPairs.add(PARAMETER_NAME_SENTINEL_MASTER_ID + "=" + urlEncode(sentinelMasterId));
        }

        if (timeout != 0 && unit != null && (timeout != DEFAULT_TIMEOUT && !unit.equals(DEFAULT_TIMEOUT_UNIT))) {
            queryPairs.add(PARAMETER_NAME_TIMEOUT + "=" + timeout + toQueryParamUnit(unit));
        }

        return queryPairs.stream().collect(Collectors.joining("&"));
    }

    private String getPortPart(int port, String scheme) {

        if (URI_SCHEME_REDIS_SENTINEL.equals(scheme) && port == DEFAULT_SENTINEL_PORT) {
            return "";
        }

        if (URI_SCHEME_REDIS.equals(scheme) && port == DEFAULT_REDIS_PORT) {
            return "";
        }

        return ":" + port;
    }

    private String getScheme() {
        String scheme = URI_SCHEME_REDIS;

        if (isSsl()) {
            if (isStartTls()) {
                scheme = URI_SCHEME_REDIS_TLS_ALT;
            } else {
                scheme = URI_SCHEME_REDIS_SECURE;
            }
        }

        if (socket != null) {
            scheme = URI_SCHEME_REDIS_SOCKET;
        }

        if (host == null && !sentinels.isEmpty()) {
            scheme = URI_SCHEME_REDIS_SENTINEL;
        }
        return scheme;
    }

    private String toQueryParamUnit(TimeUnit unit) {

        for (Map.Entry<String, TimeUnit> entry : TIME_UNIT_MAP.entrySet()) {
            if (entry.getValue().equals(unit)) {
                return entry.getKey();
            }
        }
        return "";
    }

    /**
     * URL encode the {@code str} without slash escaping {@code %2F}
     * 
     * @param str
     * @return the URL-encoded string
     */
    private String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, LettuceCharsets.UTF8.name()).replaceAll("%2F", "/");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     *
     * @return the resolved {@link SocketAddress} based either on host/port or the socket.
     */
    public SocketAddress getResolvedAddress() {
        if (getSocket() != null) {
            return EpollProvider.newSocketAddress(getSocket());
        }
        InetSocketAddress socketAddress = new InetSocketAddress(host, port);
        return socketAddress;
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
            sb.append("sentinels=").append(getSentinels());
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
        private final List<HostAndPort> sentinels = new ArrayList<>();

        /**
         * Set Redis socket. Creates a new builder.
         *
         * @param socket the host name
         * @return New builder with Redis socket.
         */
        public static Builder socket(String socket) {
            LettuceAssert.notNull(socket, "Socket must not be null");
            Builder builder = new Builder();
            builder.socket = socket;
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
            LettuceAssert.notNull(host, "Host must not be null");
            Builder builder = new Builder();
            builder.host = host;
            builder.port = port;
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
            LettuceAssert.notNull(host, "Host must not be null");
            Builder builder = new Builder();
            builder.sentinelMasterId = masterId;
            builder.withSentinel(host, port);

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
            LettuceAssert.assertState(this.host == null, "Cannot use with Redis mode.");
            LettuceAssert.notNull(host, "Host must not be null");
            sentinels.add(HostAndPort.of(host, port));
            return this;
        }

        /**
         * Adds port information to the builder. Does only affect Redis URI, cannot be used with Sentinel connections.
         *
         * @param port the port
         * @return the builder
         */
        public Builder withPort(int port) {
            LettuceAssert.assertState(this.host != null, "Host is null. Cannot use in Sentinel mode.");
            this.port = port;
            return this;
        }

        /**
         * Adds ssl information to the builder. Does only affect Redis URI, cannot be used with Sentinel connections.
         *
         * @param ssl {@literal true} if use SSL
         * @return the builder
         */
        public Builder withSsl(boolean ssl) {
            LettuceAssert.assertState(this.host != null, "Host is null. Cannot use in Sentinel mode.");
            this.ssl = ssl;
            return this;
        }

        /**
         * Enables/disables StartTLS when using SSL. Does only affect Redis URI, cannot be used with Sentinel connections.
         *
         * @param startTls {@literal true} if use StartTLS
         * @return the builder
         */
        public Builder withStartTls(boolean startTls) {
            LettuceAssert.assertState(this.host != null, "Host is null. Cannot use in Sentinel mode.");
            this.startTls = startTls;
            return this;
        }

        /**
         * Enables/disables peer verification. Does only affect Redis URI, cannot be used with Sentinel connections.
         *
         * @param verifyPeer {@literal true} to verify hosts when using SSL
         * @return the builder
         */
        public Builder withVerifyPeer(boolean verifyPeer) {
            LettuceAssert.assertState(this.host != null, "Host is null. Cannot use in Sentinel mode.");
            this.verifyPeer = verifyPeer;
            return this;
        }

        /**
         * Adds database selection.
         *
         * @param database the database number
         * @return the builder
         */
        public Builder withDatabase(int database) {
            LettuceAssert.isTrue(database >= 0 && database <= 15, "Invalid database number: " + database);
            this.database = database;
            return this;
        }

        /**
         * Adds authentication.
         *
         * @param password the password
         * @return the builder
         */
        public Builder withPassword(String password) {
            LettuceAssert.notNull(password, "Password must not be null");
            this.password = password.toCharArray();
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
            LettuceAssert.notNull(unit, "TimeUnit must not be null");
            LettuceAssert.isTrue(timeout >= 0, "Timeout must be greater or equal 0");
            this.timeout = timeout;
            this.unit = unit;
            return this;
        }

        /**
         * Adds a sentinel master Id.
         * 
         * @param sentinelMasterId sentinel master id
         * @return the builder
         */
        public Builder withSentinelMasterId(String sentinelMasterId) {
            LettuceAssert.notNull(sentinelMasterId, "Sentinel master id must not ne null");
            this.sentinelMasterId = sentinelMasterId;
            return this;
        }

        /**
         *
         * @return the RedisURI.
         */
        public RedisURI build() {

            RedisURI redisURI = new RedisURI();
            redisURI.setHost(host);
            redisURI.setPort(port);
            redisURI.password = password;
            redisURI.setDatabase(database);

            redisURI.setSentinelMasterId(sentinelMasterId);

            for (HostAndPort sentinel : sentinels) {
                redisURI.getSentinels().add(new RedisURI(sentinel.getHostText(), sentinel.getPort(), timeout, unit));
            }

            redisURI.setSocket(socket);
            redisURI.setSsl(ssl);
            redisURI.setStartTls(startTls);
            redisURI.setVerifyPeer(verifyPeer);

            redisURI.setTimeout(timeout);
            redisURI.setUnit(unit);

            return redisURI;
        }
    }
}
