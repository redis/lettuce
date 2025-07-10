/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import static io.lettuce.core.internal.LettuceStrings.*;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceSets;
import io.lettuce.core.internal.LettuceStrings;
import reactor.core.publisher.Mono;

/**
 * Redis URI. Contains connection details for the Redis/Sentinel connections. You can provide the database, client name,
 * password and timeouts within the RedisURI.
 * <p>
 * You have the following possibilities to create a {@link RedisURI}:
 *
 * <ul>
 * <li>Use an URI:
 * <p>
 * {@code RedisURI.create("redis://localhost/");}
 * </p>
 * See {@link #create(String)} for more options</li>
 * <li>Use the Builder:
 * <p>
 * {@code RedisURI.Builder.redis("localhost", 6379).withPassword("password").withDatabase(1).build(); }
 * </p>
 * See {@link io.lettuce.core.RedisURI.Builder#redis(String)} and {@link io.lettuce.core.RedisURI.Builder#sentinel(String)} for
 * more options.</li>
 * <li>Construct your own instance:
 * <p>
 * {@code new RedisURI("localhost", 6379, Duration.ofSeconds(60));}
 * </p>
 * or
 * <p>
 * {@code RedisURI uri = new RedisURI(); uri.setHost("localhost");
 *     }
 * </p>
 * </li>
 * </ul>
 *
 * <h3>URI syntax</h3>
 *
 * <b>Redis Standalone</b> <blockquote> <i>redis</i><b>{@code ://}</b>[[<i>username</i>{@code :}]<i>password@</i>]<i>host</i>
 * [<b>{@code :} </b> <i>port</i>][<b>{@code /}</b><i>database</i>][<b>{@code ?}</b>
 * [<i>timeout=timeout</i>[<i>d|h|m|s|ms|us|ns</i>]] [ <i>&amp;database=database</i>] [<i>&amp;clientName=clientName</i>]
 * [<i>&amp;libraryName=libraryName</i>] [<i>&amp;libraryVersion=libraryVersion</i>] [<i>&amp;verifyPeer=NONE|CA|FULL</i>]]
 * </blockquote>
 *
 * <b>Redis Standalone (SSL)</b> <blockquote>
 * <i>rediss</i><b>{@code ://}</b>[[<i>username</i>{@code :}]<i>password@</i>]<i>host</i> [<b>{@code :} </b>
 * <i>port</i>][<b>{@code /}</b><i>database</i>][<b>{@code ?}</b> [<i>timeout=timeout</i>[<i>d|h|m|s|ms|us|ns</i>]] [
 * <i>&amp;database=database</i>] [<i>&amp;clientName=clientName</i>] [<i>&amp;libraryName=libraryName</i>]
 * [<i>&amp;libraryVersion=libraryVersion</i>] [<i>&amp;verifyPeer=NONE|CA|FULL</i>]] </blockquote>
 *
 * Redis Standalone (Unix Domain Sockets)</b> <blockquote> <i>redis-socket</i><b>{@code ://}
 * </b>[[<i>username</i>{@code :}]<i>password@</i>]<i>path</i>[
 * <b>{@code ?}</b>[<i>timeout=timeout</i>[<i>d|h|m|s|ms|us|ns</i>]][<i>&amp;database=database</i>]
 * [<i>&amp;clientName=clientName</i>] [<i>&amp;libraryName=libraryName</i>] [<i>&amp;libraryVersion=libraryVersion</i>]
 * [<i>&amp;verifyPeer=NONE|CA|FULL</i>]] </blockquote>
 *
 * <b>Redis Sentinel</b> <blockquote>
 * <i>redis-sentinel</i><b>{@code ://}</b>[[<i>username</i>{@code :}]<i>password@</i>]<i>host1</i> [<b>{@code :} </b>
 * <i>port1</i>][, <i>host2</i> [<b>{@code :}</b><i>port2</i>]][, <i>hostN</i> [<b>{@code :}</b><i>portN</i>]][<b>{@code /} </b>
 * <i>database</i>][<b>{@code ?} </b>[<i>timeout=timeout</i>[<i>d|h|m|s|ms|us|ns</i>]] [
 * <i>&amp;sentinelMasterId=sentinelMasterId</i>] [<i>&amp;database=database</i>] [<i>&amp;clientName=clientName</i>]
 * [<i>&amp;libraryName=libraryName</i>] [<i>&amp;libraryVersion=libraryVersion</i>] [<i>&amp;verifyPeer=NONE|CA|FULL</i>]]
 * </blockquote>
 *
 *
 * <h3><b>Schemes</b>
 * </p>
 * <ul>
 * <li><b>redis</b> Redis Standalone</li>
 * <li><b>rediss</b> Redis Standalone SSL</li>
 * <li><b>redis-socket</b> Redis Standalone Unix Domain Socket</li>
 * <li><b>redis-sentinel</b> Redis Sentinel</li>
 * <li><b>rediss-sentinel</b> Redis Sentinel SSL</li>
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
 * <p>
 * {@code RedisURI} supports Redis Standalone, Redis Sentinel and Redis Cluster with plain, SSL, TLS and unix domain socket
 * connections.
 * </p>
 *
 * <h3>Authentication</h3> Redis URIs may contain authentication details that effectively lead to usernames with passwords,
 * password-only, or no authentication. Connections are authenticated by using information provided through
 * {@link RedisCredentials}. Credentials are obtained at connection time from {@link RedisCredentialsProvider}. When configuring
 * username/password on the URI statically, then a {@link StaticCredentialsProvider} holds the configured information.
 *
 * <p>
 * <b>Notes</b>
 * </p>
 * <ul>
 * <li>When using Redis Sentinel, the password from the URI applies to the data nodes only. Sentinel authentication must be
 * configured for each {@link #getSentinels() sentinel node}.</li>
 * <li>Usernames are supported as of Redis 6.</li>
 * <li>Library name and library version are automatically set on Redis 7.2 or greater defaulting to
 * {@link LettuceVersion#getVersion()}.</li>
 * </ul>
 *
 * @author Mark Paluch
 * @author Guy Korland
 * @author Johnny Lim
 * @author Jon Iantosca
 * @author Jacob Halsey
 * @author Tihomir Mateev
 * @author Kim Sung Hyun
 * @since 3.0
 */
public class RedisURI implements Serializable, ConnectionPoint {

    public static final String URI_SCHEME_REDIS_SENTINEL = "redis-sentinel";

    public static final String URI_SCHEME_REDIS_SENTINEL_SECURE = "rediss-sentinel";

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

    public static final String PARAMETER_NAME_CLIENT_NAME = "clientName";

    public static final String PARAMETER_NAME_LIBRARY_NAME = "libraryName";

    public static final String PARAMETER_NAME_LIBRARY_VERSION = "libraryVersion";

    public static final String PARAMETER_NAME_VERIFY_PEER = "verifyPeer";

    public static final Map<String, LongFunction<Duration>> CONVERTER_MAP;

    static {
        Map<String, LongFunction<Duration>> unitMap = new HashMap<>();
        unitMap.put("ns", Duration::ofNanos);
        unitMap.put("us", us -> Duration.ofNanos(us * 1000));
        unitMap.put("ms", Duration::ofMillis);
        unitMap.put("s", Duration::ofSeconds);
        unitMap.put("m", Duration::ofMinutes);
        unitMap.put("h", Duration::ofHours);
        unitMap.put("d", Duration::ofDays);
        CONVERTER_MAP = Collections.unmodifiableMap(unitMap);
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

    public static final Duration DEFAULT_TIMEOUT_DURATION = Duration.ofSeconds(DEFAULT_TIMEOUT);

    private String host;

    private String socket;

    private String sentinelMasterId;

    private int port;

    private int database;

    private String clientName;

    private String libraryName = LettuceVersion.getName();

    private String libraryVersion = LettuceVersion.getVersion();

    private RedisCredentialsProvider credentialsProvider = new StaticCredentialsProvider(null, null);;

    private boolean ssl = false;

    private SslVerifyMode verifyMode = SslVerifyMode.FULL;

    private boolean startTls = false;

    private Duration timeout = DEFAULT_TIMEOUT_DURATION;

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
     */
    public RedisURI(String host, int port, Duration timeout) {

        LettuceAssert.notEmpty(host, "Host must not be empty");
        LettuceAssert.notNull(timeout, "Timeout duration must not be null");
        LettuceAssert.isTrue(!timeout.isNegative(), "Timeout duration must be greater or equal to zero");

        setHost(host);
        setPort(port);
        setTimeout(timeout);
    }

    /**
     * Return a new {@link RedisURI.Builder} to construct a {@link RedisURI}.
     *
     * @return a new {@link RedisURI.Builder} to construct a {@link RedisURI}.
     */
    public static RedisURI.Builder builder() {
        return new Builder();
    }

    /**
     * Create a Redis URI from host and port.
     *
     * @param host the host
     * @param port the port
     * @return An instance of {@link RedisURI} containing details from the {@code host} and {@code port}.
     */
    public static RedisURI create(String host, int port) {
        return Builder.redis(host, port).build();
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
        LettuceAssert.notEmpty(uri, "URI must not be empty");
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
        return buildRedisUriFromUri(uri);
    }

    /**
     * Create a new {@link RedisURI.Builder} that is initialized from a plain {@link RedisURI}.
     *
     * @param source the initialization source, must not be {@code null}.
     * @return the initialized builder.
     * @since 6.0
     */
    public static Builder builder(RedisURI source) {

        LettuceAssert.notNull(source, "Source RedisURI must not be null");

        Builder builder = builder();
        builder.withSsl(source).withAuthentication(source).withTimeout(source.getTimeout()).withDatabase(source.getDatabase());

        if (source.getClientName() != null) {
            builder.withClientName(source.getClientName());
        }

        if (source.getLibraryName() != null) {
            builder.withLibraryName(source.getLibraryName());
        }

        if (source.getLibraryVersion() != null) {
            builder.withLibraryVersion(source.getLibraryVersion());
        }

        if (source.socket != null) {
            builder.socket = source.getSocket();
        } else {

            if (source.getHost() != null) {
                builder.withHost(source.getHost());
                builder.withPort(source.getPort());
            }
        }

        return builder;
    }

    /**
     * Returns the host.
     *
     * @return the host.
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the Redis host.
     *
     * @param host the host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Returns the Sentinel Master Id.
     *
     * @return the Sentinel Master Id.
     */
    public String getSentinelMasterId() {
        return sentinelMasterId;
    }

    /**
     * Sets the Sentinel Master Id.
     *
     * @param sentinelMasterId the Sentinel Master Id.
     */
    public void setSentinelMasterId(String sentinelMasterId) {
        this.sentinelMasterId = sentinelMasterId;
    }

    /**
     * Returns the Redis port.
     *
     * @return the Redis port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the Redis port. Defaults to {@link #DEFAULT_REDIS_PORT}.
     *
     * @param port the Redis port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns the Unix Domain Socket path.
     *
     * @return the Unix Domain Socket path.
     */
    public String getSocket() {
        return socket;
    }

    /**
     * Sets the Unix Domain Socket path.
     *
     * @param socket the Unix Domain Socket path.
     */
    public void setSocket(String socket) {
        this.socket = socket;
    }

    /**
     * Apply authentication from another {@link RedisURI}. The authentication settings of the {@code source} URI will be applied
     * to this URI. That is in particular the {@link RedisCredentialsProvider}.
     *
     * @param source must not be {@code null}.
     * @since 6.0
     */
    public void applyAuthentication(RedisURI source) {

        LettuceAssert.notNull(source, "Source RedisURI must not be null");

        if (source.credentialsProvider != null) {
            setCredentialsProvider(source.getCredentialsProvider());
        }
    }

    /**
     * Sets the password to use to authenticate Redis connections.
     * <p>
     * This method effectively overwrites any existing {@link RedisCredentialsProvider} with a new one, containing an empty
     * username and the provided password.
     *
     * @param password the password to use to authenticate Redis connections.
     * @see #setCredentialsProvider(RedisCredentialsProvider)
     * @since 7.0
     */
    public void setAuthentication(CharSequence password) {
        LettuceAssert.notNull(password, "Password must not be null");

        this.setAuthentication(null, password);
    }

    /**
     * Sets the password to use to authenticate Redis connections.
     * <p>
     * This method effectively overwrites any existing {@link RedisCredentialsProvider} with a new one, containing an empty
     * username and the provided password.
     *
     * @param password the password to use to authenticate Redis connections.
     * @see #setCredentialsProvider(RedisCredentialsProvider)
     * @since 7.0
     */
    public void setAuthentication(char[] password) {
        LettuceAssert.notNull(password, "Password must not be null");

        this.setAuthentication(null, password);
    }

    /**
     * Sets the username and password to use to authenticate Redis connections.
     * <p>
     * This method effectively overwrites any existing {@link RedisCredentialsProvider} with a new one, containing the provided
     * username and password.
     *
     * @param username the username to use to authenticate Redis connections.
     * @param password the password to use to authenticate Redis connections.
     * @see #setCredentialsProvider(RedisCredentialsProvider)
     * @since 7.0
     */
    public void setAuthentication(String username, char[] password) {
        LettuceAssert.notNull(password, "Password must not be null");

        this.setCredentialsProvider(() -> Mono.just(RedisCredentials.just(username, password)));
    }

    /**
     * Sets the username and password to use to authenticate Redis connections.
     * <p>
     * This method effectively overwrites any existing {@link RedisCredentialsProvider} with a new one, containing the provided
     * username and password.
     *
     * @param username the username to use to authenticate Redis connections.
     * @param password the password to use to authenticate Redis connections.
     * @see #setCredentialsProvider(RedisCredentialsProvider)
     * @since 7.0
     */
    public void setAuthentication(String username, CharSequence password) {
        LettuceAssert.notNull(password, "Password must not be null");

        this.setCredentialsProvider(() -> Mono.just(RedisCredentials.just(username, password)));
    }

    /**
     * Returns the {@link RedisCredentialsProvider} to use to authenticate Redis connections. Returns a static credentials
     * provider no explicit {@link RedisCredentialsProvider} was configured.
     *
     * @return the {@link RedisCredentialsProvider} to use to authenticate Redis connections
     * @since 6.2
     */
    public RedisCredentialsProvider getCredentialsProvider() {
        return this.credentialsProvider;
    }

    /**
     * Sets the {@link RedisCredentialsProvider}. Configuring a credentials provider resets the configured static
     * username/password.
     *
     * @param credentialsProvider the credentials provider to use when authenticating a Redis connection.
     * @since 6.2
     */
    public void setCredentialsProvider(RedisCredentialsProvider credentialsProvider) {

        LettuceAssert.notNull(credentialsProvider, "RedisCredentialsProvider must not be null");

        this.credentialsProvider = credentialsProvider;
    }

    /**
     * Returns the command timeout for synchronous command execution.
     *
     * @return the Timeout
     * @since 5.0
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Sets the command timeout for synchronous command execution. A zero timeout value indicates to not time out.
     *
     * @param timeout the command timeout for synchronous command execution.
     * @since 5.0
     */
    public void setTimeout(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout must not be null");
        LettuceAssert.isTrue(!timeout.isNegative(), "Timeout must be greater or equal 0");

        this.timeout = timeout;
    }

    /**
     * Returns the Redis database number. Databases are only available for Redis Standalone and Redis Master/Slave.
     *
     * @return the Redis database number
     */
    public int getDatabase() {
        return database;
    }

    /**
     * Sets the Redis database number. Databases are only available for Redis Standalone and Redis Master/Slave.
     *
     * @param database the Redis database number.
     */
    public void setDatabase(int database) {

        LettuceAssert.isTrue(database >= 0, "Invalid database number: " + database);

        this.database = database;
    }

    /**
     * Returns the client name.
     *
     * @return the client name.
     * @since 4.4
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * Sets the client name to be applied on Redis connections.
     *
     * @param clientName the client name.
     * @since 4.4
     */
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    /**
     * Returns the library name.
     *
     * @return the library name.
     * @since 6.3
     */
    public String getLibraryName() {
        return libraryName;
    }

    /**
     * Sets the library name to be applied on Redis connections.
     *
     * @param libraryName the library name.
     * @since 6.3
     */
    public void setLibraryName(String libraryName) {
        if (libraryName != null && libraryName.indexOf(' ') != -1) {
            throw new IllegalArgumentException("Library name must not contain spaces");
        }
        this.libraryName = libraryName;
    }

    /**
     * Returns the library version.
     *
     * @return the library version.
     * @since 6.3
     */
    public String getLibraryVersion() {
        return libraryVersion;
    }

    /**
     * Sets the library version to be applied on Redis connections.
     *
     * @param libraryVersion the library version.
     * @since 6.3
     */
    public void setLibraryVersion(String libraryVersion) {
        if (libraryVersion != null && libraryVersion.indexOf(' ') != -1) {
            throw new IllegalArgumentException("Library version must not contain spaces");
        }
        this.libraryVersion = libraryVersion;
    }

    /**
     * Apply authentication from another {@link RedisURI}. The SSL settings of the {@code source} URI will be applied to this
     * URI. That is in particular SSL usage, peer verification and StartTLS.
     *
     * @param source must not be {@code null}.
     * @since 6.0
     */
    public void applySsl(RedisURI source) {

        LettuceAssert.notNull(source, "Source RedisURI must not be null");

        setSsl(source.isSsl());
        setVerifyPeer(source.getVerifyMode());
        setStartTls(source.isStartTls());
    }

    /**
     * Returns {@code true} if SSL mode is enabled.
     *
     * @return {@code true} if SSL mode is enabled.
     */
    public boolean isSsl() {
        return ssl;
    }

    /**
     * Sets whether to use SSL. Sets SSL also for already configured Redis Sentinel nodes.
     *
     * @param ssl
     */
    public void setSsl(boolean ssl) {
        this.ssl = ssl;
        this.sentinels.forEach(it -> it.setSsl(ssl));
    }

    /**
     * Returns whether to verify peers when using {@link #isSsl() SSL}.
     *
     * @return {@code true} to verify peers when using {@link #isSsl() SSL}.
     */
    public boolean isVerifyPeer() {
        return verifyMode != SslVerifyMode.NONE;
    }

    /**
     * Returns the mode to verify peers when using {@link #isSsl() SSL}.
     *
     * @return the verification mode
     * @since 6.1
     */
    public SslVerifyMode getVerifyMode() {
        return verifyMode;
    }

    /**
     * Sets whether to verify peers when using {@link #isSsl() SSL}. Sets peer verification also for already configured Redis
     * Sentinel nodes.
     *
     * @param verifyPeer {@code true} to verify peers when using {@link #isSsl() SSL}.
     */
    public void setVerifyPeer(boolean verifyPeer) {
        setVerifyPeer(verifyPeer ? SslVerifyMode.FULL : SslVerifyMode.NONE);
    }

    /**
     * Sets how to verify peers when using {@link #isSsl() SSL}. Sets peer verification also for already configured Redis
     * Sentinel nodes.
     *
     * @param verifyMode verification mode to use when using {@link #isSsl() SSL}.
     * @since 6.1
     */
    public void setVerifyPeer(SslVerifyMode verifyMode) {
        LettuceAssert.notNull(verifyMode, "VerifyMode must not be null");
        this.verifyMode = verifyMode;
        this.sentinels.forEach(it -> it.setVerifyPeer(this.verifyMode));
    }

    /**
     * Returns {@code true} if StartTLS is enabled.
     *
     * @return {@code true} if StartTLS is enabled.
     */
    public boolean isStartTls() {
        return startTls;
    }

    /**
     * Returns whether StartTLS is enabled. Sets StartTLS also for already configured Redis Sentinel nodes.
     *
     * @param startTls {@code true} if StartTLS is enabled.
     */
    public void setStartTls(boolean startTls) {
        this.startTls = startTls;
        this.sentinels.forEach(it -> it.setStartTls(startTls));
    }

    /**
     *
     * @return the list of {@link RedisURI Redis Sentinel URIs}.
     */
    public List<RedisURI> getSentinels() {
        return sentinels;
    }

    /**
     * Creates an URI based on the RedisURI if possible.
     * <p>
     * An URI an represent a Standalone address using host and port or socket addressing or a Redis Sentinel address using
     * host/port. A Redis Sentinel URI with multiple nodes using Unix Domain Sockets cannot be rendered to a {@link URI}.
     *
     * @return URI based on the RedisURI.
     * @throws IllegalStateException if the URI cannot be rendered.
     */
    public URI toURI() {
        try {
            return URI.create(createUriString(false));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot render URI for " + toString(), e);
        }
    }

    private String createUriString(boolean maskCredentials) {
        String scheme = getScheme();
        String authority = getAuthority(scheme, maskCredentials);
        String queryString = getQueryString();
        String uri = scheme + "://" + authority;

        if (!queryString.isEmpty()) {
            uri += "?" + queryString;
        }
        return uri;
    }

    private static RedisURI buildRedisUriFromUri(URI uri) {

        LettuceAssert.notNull(uri, "URI must not be null");
        LettuceAssert.notNull(uri.getScheme(), "URI scheme must not be null");

        Builder builder;
        if (isSentinel(uri.getScheme())) {
            builder = configureSentinel(uri);
        } else {
            builder = configureStandalone(uri);
        }

        String userInfo = uri.getUserInfo();

        if (isEmpty(userInfo) && isNotEmpty(uri.getAuthority()) && uri.getAuthority().lastIndexOf('@') > 0) {
            userInfo = uri.getAuthority().substring(0, uri.getAuthority().lastIndexOf('@'));
        }

        if (isNotEmpty(userInfo)) {
            String password = userInfo;
            String username = null;
            if (password.startsWith(":")) {
                password = password.substring(1);
            } else {

                int index = password.indexOf(':');
                if (index > 0) {
                    username = password.substring(0, index);
                    password = password.substring(index + 1);
                }
            }
            if (LettuceStrings.isNotEmpty(password)) {
                if (username == null) {
                    builder.withPassword(password);
                } else {
                    builder.withAuthentication(username, password);
                }
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

                if (forStartWith.startsWith(PARAMETER_NAME_CLIENT_NAME.toLowerCase() + "=")) {
                    parseClientName(builder, queryParam);
                }

                if (forStartWith.startsWith(PARAMETER_NAME_LIBRARY_NAME.toLowerCase() + "=")) {
                    parseLibraryName(builder, queryParam);
                }

                if (forStartWith.startsWith(PARAMETER_NAME_LIBRARY_VERSION.toLowerCase() + "=")) {
                    parseLibraryVersion(builder, queryParam);
                }

                if (forStartWith.startsWith(PARAMETER_NAME_VERIFY_PEER.toLowerCase() + "=")) {
                    parseVerifyPeer(builder, queryParam);
                }

                if (forStartWith.startsWith(PARAMETER_NAME_SENTINEL_MASTER_ID.toLowerCase() + "=")) {
                    parseSentinelMasterId(builder, queryParam);
                }
            }
        }

        if (isSentinel(uri.getScheme())) {
            LettuceAssert.notEmpty(builder.sentinelMasterId, "URI must contain the sentinelMasterId");
        }

        return builder.build();
    }

    private String getAuthority(String scheme, boolean maskCredentials) {

        String authority = null;

        if (host != null) {
            if (host.contains(",")) {
                authority = host;
            } else {
                authority = urlEncode(host) + getPortPart(port, scheme);
            }
        }

        if (!sentinels.isEmpty()) {

            authority = sentinels.stream().map(redisURI -> {
                if (LettuceStrings.isNotEmpty(redisURI.getSocket())) {
                    return String.format("[Socket %s]", redisURI.getSocket());
                }

                return urlEncode(redisURI.getHost()) + getPortPart(redisURI.getPort(), scheme);
            }).collect(Collectors.joining(","));
        }

        if (socket != null) {
            authority = urlEncode(socket);
        } else {
            if (database != 0) {
                authority += "/" + database;
            }
        }

        if (credentialsProvider != null) {
            if (!maskCredentials) {
                authority = urlEncode("**credentialsProvider**") + "@" + authority;
            } else {
                // compatibility with versions before 7.0 - in previous versions of the Lettuce driver there was an option to
                // have a username and password pair as part of the RedisURI; in these cases when we were masking credentials we
                // would get asterix for each character of the password.
                RedisCredentials creds = credentialsProvider.resolveCredentials().block();
                if (creds != null) {
                    String credentials = "";

                    if (creds.hasUsername() && !creds.getUsername().isEmpty()) {
                        credentials = urlEncode(creds.getUsername()) + ":";
                    }

                    if (creds.hasPassword()) {
                        credentials += IntStream.range(0, creds.getPassword().length).mapToObj(ignore -> "*")
                                .collect(Collectors.joining());
                    }

                    if (!credentials.isEmpty()) {
                        authority = credentials + "@" + authority;
                    }
                }
            }
        }
        return authority;
    }

    private String getQueryString() {

        List<String> queryPairs = new ArrayList<>();

        if (database != 0 && LettuceStrings.isNotEmpty(socket)) {
            queryPairs.add(PARAMETER_NAME_DATABASE + "=" + database);
        }

        if (clientName != null) {
            queryPairs.add(PARAMETER_NAME_CLIENT_NAME + "=" + urlEncode(clientName));
        }

        if (libraryName != null && !libraryName.equals(LettuceVersion.getName())) {
            queryPairs.add(PARAMETER_NAME_LIBRARY_NAME + "=" + urlEncode(libraryName));
        }

        if (libraryVersion != null && !libraryVersion.equals(LettuceVersion.getVersion())) {
            queryPairs.add(PARAMETER_NAME_LIBRARY_VERSION + "=" + urlEncode(libraryVersion));
        }

        if (isSsl() && getVerifyMode() != SslVerifyMode.FULL) {
            queryPairs.add(PARAMETER_NAME_VERIFY_PEER + "=" + verifyMode.name());
        }

        if (sentinelMasterId != null) {
            queryPairs.add(PARAMETER_NAME_SENTINEL_MASTER_ID + "=" + urlEncode(sentinelMasterId));
        }

        if (timeout.getSeconds() != DEFAULT_TIMEOUT) {

            if (timeout.getNano() == 0) {
                queryPairs.add(PARAMETER_NAME_TIMEOUT + "=" + timeout.getSeconds() + "s");
            } else {
                queryPairs.add(PARAMETER_NAME_TIMEOUT + "=" + timeout.toNanos() + "ns");
            }
        }

        return String.join("&", queryPairs);
    }

    private String getPortPart(int port, String scheme) {

        if (isSentinel(scheme) && port == DEFAULT_SENTINEL_PORT) {
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
            if (isSsl()) {
                scheme = URI_SCHEME_REDIS_SENTINEL_SECURE;
            } else {
                scheme = URI_SCHEME_REDIS_SENTINEL;
            }
        }
        return scheme;
    }

    /**
     * URL encode the {@code str} without slash escaping {@code %2F}.
     *
     * @param str the string to encode.
     * @return the URL-encoded string
     */
    private static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.name()).replaceAll("%2F", "/");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return the RedisURL in a URI-like form.
     */
    @Override
    public String toString() {
        return createUriString(true);
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
                builder.withTimeout(Duration.ZERO);
            } else {
                // no-op, leave defaults
            }
        } else {
            String timeoutValueString = timeoutString.substring(0, numbersEnd);
            long timeoutValue = Long.parseLong(timeoutValueString);
            builder.withTimeout(Duration.ofMillis(timeoutValue));

            String suffix = timeoutString.substring(numbersEnd);
            LongFunction<Duration> converter = CONVERTER_MAP.get(suffix);
            if (converter == null) {
                converter = Duration::ofMillis;
            }

            builder.withTimeout(converter.apply(timeoutValue));
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

    private static void parseClientName(Builder builder, String queryParam) {

        String clientName = getValuePart(queryParam);
        if (isNotEmpty(clientName)) {
            builder.withClientName(clientName);
        }
    }

    private static void parseLibraryName(Builder builder, String queryParam) {

        String libraryName = getValuePart(queryParam);
        if (isNotEmpty(libraryName)) {
            builder.withLibraryName(libraryName);
        }
    }

    private static void parseLibraryVersion(Builder builder, String queryParam) {

        String libraryVersion = getValuePart(queryParam);
        if (isNotEmpty(libraryVersion)) {
            builder.withLibraryVersion(libraryVersion);
        }
    }

    private static void parseVerifyPeer(Builder builder, String queryParam) {

        String verifyPeer = getValuePart(queryParam);
        if (isNotEmpty(verifyPeer)) {
            builder.withVerifyPeer(SslVerifyMode.valueOf(verifyPeer.toUpperCase()));
        }
    }

    private static void parseSentinelMasterId(Builder builder, String queryParam) {

        String masterIdString = getValuePart(queryParam);
        if (isNotEmpty(masterIdString)) {
            builder.withSentinelMasterId(masterIdString);
        }
    }

    private static String getValuePart(String queryParam) {
        int index = queryParam.indexOf('=');
        if (index < 0) {
            return null;
        }

        return queryParam.substring(index + 1);
    }

    private static Builder configureStandalone(URI uri) {

        Builder builder = null;
        Set<String> allowedSchemes = LettuceSets.unmodifiableSet(URI_SCHEME_REDIS, URI_SCHEME_REDIS_SECURE,
                URI_SCHEME_REDIS_SOCKET, URI_SCHEME_REDIS_SOCKET_ALT, URI_SCHEME_REDIS_SECURE_ALT, URI_SCHEME_REDIS_TLS_ALT);

        if (!allowedSchemes.contains(uri.getScheme())) {
            throw new IllegalArgumentException("Scheme " + uri.getScheme() + " not supported");
        }

        if (URI_SCHEME_REDIS_SOCKET.equals(uri.getScheme()) || URI_SCHEME_REDIS_SOCKET_ALT.equals(uri.getScheme())) {
            builder = Builder.socket(uri.getPath());
        } else {

            if (isNotEmpty(uri.getHost())) {

                if (uri.getPort() > 0) {
                    builder = Builder.redis(uri.getHost(), uri.getPort());
                } else {
                    builder = Builder.redis(uri.getHost());
                }
            } else {

                if (isNotEmpty(uri.getAuthority())) {
                    String authority = uri.getAuthority();
                    if (authority.indexOf('@') > -1) {
                        authority = authority.substring(authority.lastIndexOf('@') + 1);
                    }

                    builder = Builder.redis(authority);
                }
            }
        }

        LettuceAssert.notNull(builder, "Invalid URI, cannot get host or socket part");

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
                builder = RedisURI.Builder.sentinel(uri.getHost(), uri.getPort());
            } else {
                builder = RedisURI.Builder.sentinel(uri.getHost());
            }
        }

        if (builder == null && isNotEmpty(uri.getAuthority())) {
            String authority = uri.getAuthority();
            if (authority.indexOf('@') > -1) {
                authority = authority.substring(authority.lastIndexOf('@') + 1);
            }

            String[] hosts = authority.split(",");
            for (String host : hosts) {
                HostAndPort hostAndPort = HostAndPort.parse(host);
                if (builder == null) {
                    if (hostAndPort.hasPort()) {
                        builder = RedisURI.Builder.sentinel(hostAndPort.getHostText(), hostAndPort.getPort());
                    } else {
                        builder = RedisURI.Builder.sentinel(hostAndPort.getHostText());
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

        if (isNotEmpty(masterId)) {
            builder.withSentinelMasterId(masterId);
        }

        if (uri.getScheme().equals(URI_SCHEME_REDIS_SENTINEL_SECURE)) {
            builder.withSsl(true);
        }

        return builder;
    }

    private static boolean isSentinel(String scheme) {
        return URI_SCHEME_REDIS_SENTINEL.equals(scheme) || URI_SCHEME_REDIS_SENTINEL_SECURE.equals(scheme);
    }

    /**
     * Builder for Redis URI.
     */
    public static class Builder {

        private String host;

        private String socket;

        private String sentinelMasterId;

        private int port = DEFAULT_REDIS_PORT;

        private int database;

        private String clientName;

        private String libraryName = LettuceVersion.getName();

        private String libraryVersion = LettuceVersion.getVersion();

        private RedisCredentialsProvider credentialsProvider;

        private boolean ssl = false;

        private SslVerifyMode verifyMode = SslVerifyMode.FULL;

        private boolean startTls = false;

        private Duration timeout = DEFAULT_TIMEOUT_DURATION;

        private final List<RedisURI> sentinels = new ArrayList<>();

        private Builder() {
        }

        /**
         * Set Redis socket. Creates a new builder.
         *
         * @param socket the host name
         * @return new builder with Redis socket.
         */
        public static Builder socket(String socket) {

            LettuceAssert.notNull(socket, "Socket must not be null");

            Builder builder = RedisURI.builder();
            builder.socket = socket;
            return builder;
        }

        /**
         * Set Redis host. Creates a new builder.
         *
         * @param host the host name
         * @return new builder with Redis host/port.
         */
        public static Builder redis(String host) {
            return redis(host, DEFAULT_REDIS_PORT);
        }

        /**
         * Set Redis host and port. Creates a new builder
         *
         * @param host the host name
         * @param port the port
         * @return new builder with Redis host/port.
         */
        public static Builder redis(String host, int port) {

            LettuceAssert.notEmpty(host, "Host must not be empty");
            LettuceAssert.isTrue(isValidPort(port), () -> String.format("Port out of range: %s", port));

            Builder builder = RedisURI.builder();
            return builder.withHost(host).withPort(port);
        }

        /**
         * Set Sentinel host. Creates a new builder.
         *
         * @param host the host name
         * @return new builder with Sentinel host/port.
         */
        public static Builder sentinel(String host) {

            LettuceAssert.notEmpty(host, "Host must not be empty");

            Builder builder = RedisURI.builder();
            return builder.withSentinel(host);
        }

        /**
         * Set Sentinel host and port. Creates a new builder.
         *
         * @param host the host name
         * @param port the port
         * @return new builder with Sentinel host/port.
         */
        public static Builder sentinel(String host, int port) {

            LettuceAssert.notEmpty(host, "Host must not be empty");
            LettuceAssert.isTrue(isValidPort(port), () -> String.format("Port out of range: %s", port));

            Builder builder = RedisURI.builder();
            return builder.withSentinel(host, port);
        }

        /**
         * Set Sentinel host and master id. Creates a new builder.
         *
         * @param host the host name
         * @param masterId sentinel master id
         * @return new builder with Sentinel host/port.
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
         * @return new builder with Sentinel host/port.
         */
        public static Builder sentinel(String host, int port, String masterId) {
            return sentinel(host, port, masterId, null);
        }

        /**
         * Set Sentinel host, port, master id and Sentinel authentication. Creates a new builder.
         *
         * @param host the host name
         * @param port the port
         * @param masterId sentinel master id
         * @param password the Sentinel password (supported since Redis 5.0.1)
         * @return new builder with Sentinel host/port.
         */
        public static Builder sentinel(String host, int port, String masterId, CharSequence password) {

            LettuceAssert.notEmpty(host, "Host must not be empty");
            LettuceAssert.isTrue(isValidPort(port), () -> String.format("Port out of range: %s", port));

            return password == null ? RedisURI.builder().withSentinelMasterId(masterId).withSentinel(host, port)
                    : RedisURI.builder().withSentinelMasterId(masterId).withSentinel(host, port, password);
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

            return withSentinel(host, port, null);
        }

        /**
         * Add a withSentinel host/port and Sentinel authentication to the existing builder.
         *
         * @param host the host name
         * @param port the port
         * @param password the Sentinel password (supported since Redis 5.0.1)
         * @return the builder
         * @since 5.2
         */
        public Builder withSentinel(String host, int port, CharSequence password) {

            LettuceAssert.assertState(this.host == null, "Cannot use with Redis mode.");
            LettuceAssert.notEmpty(host, "Host must not be empty");
            LettuceAssert.isTrue(isValidPort(port), () -> String.format("Port out of range: %s", port));

            RedisURI redisURI = password == null ? RedisURI.builder().withHost(host).withPort(port).build()
                    : RedisURI.builder().withHost(host).withPort(port).withPassword(password).build();

            return withSentinel(redisURI);
        }

        /**
         * Add a withSentinel RedisURI to the existing builder.
         *
         * @param redisURI the sentinel URI
         * @return the builder
         * @since 5.2
         */
        public Builder withSentinel(RedisURI redisURI) {

            LettuceAssert.notNull(redisURI, "Redis URI must not be null");

            sentinels.add(redisURI);
            return this;
        }

        /**
         * Adds host information to the builder. Does only affect Redis URI, cannot be used with Sentinel connections.
         *
         * @param host the port
         * @return the builder
         */
        public Builder withHost(String host) {

            LettuceAssert.assertState(this.sentinels.isEmpty(), "Sentinels are non-empty. Cannot use in Sentinel mode.");
            LettuceAssert.notEmpty(host, "Host must not be empty");

            this.host = host;
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
            LettuceAssert.isTrue(isValidPort(port), () -> String.format("Port out of range: %s", port));

            this.port = port;
            return this;
        }

        /**
         * Apply authentication from another {@link RedisURI}. The SSL settings of the {@code source} URI will be applied to
         * this URI. That is in particular SSL usage, peer verification and StartTLS.
         *
         * @param source must not be {@code null}.
         * @since 6.0
         * @return the builder
         */
        public Builder withSsl(RedisURI source) {

            LettuceAssert.notNull(source, "Source RedisURI must not be null");

            withSsl(source.isSsl());
            withVerifyPeer(source.getVerifyMode());
            withStartTls(source.isStartTls());

            return this;
        }

        /**
         * Adds ssl information to the builder. Sets SSL also for already configured Redis Sentinel nodes.
         *
         * @param ssl {@code true} if use SSL
         * @return the builder
         */
        public Builder withSsl(boolean ssl) {

            this.ssl = ssl;
            this.sentinels.forEach(it -> it.setSsl(ssl));
            return this;
        }

        /**
         * Enables/disables StartTLS when using SSL. Sets StartTLS also for already configured Redis Sentinel nodes.
         *
         * @param startTls {@code true} if use StartTLS
         * @return the builder
         */
        public Builder withStartTls(boolean startTls) {

            this.startTls = startTls;
            this.sentinels.forEach(it -> it.setStartTls(startTls));
            return this;
        }

        /**
         * Enables/disables peer verification. Sets peer verification also for already configured Redis Sentinel nodes.
         *
         * @param verifyPeer {@code true} to verify hosts when using SSL
         * @return the builder
         */
        public Builder withVerifyPeer(boolean verifyPeer) {
            return withVerifyPeer(verifyPeer ? SslVerifyMode.FULL : SslVerifyMode.NONE);
        }

        /**
         * Configures peer verification mode. Sets peer verification also for already configured Redis Sentinel nodes.
         *
         * @param verifyMode the mode to verify hosts when using SSL
         * @return the builder
         * @since 6.1
         */
        public Builder withVerifyPeer(SslVerifyMode verifyMode) {

            LettuceAssert.notNull(verifyMode, "VerifyMode must not be null");

            this.verifyMode = verifyMode;
            this.sentinels.forEach(it -> it.setVerifyPeer(verifyMode));
            return this;
        }

        /**
         * Configures the database number.
         *
         * @param database the database number
         * @return the builder
         */
        public Builder withDatabase(int database) {

            LettuceAssert.isTrue(database >= 0, () -> "Invalid database number: " + database);

            this.database = database;
            return this;
        }

        /**
         * Configures a client name. Sets client name also for already configured Redis Sentinel nodes.
         *
         * @param clientName the client name
         * @return the builder
         */
        public Builder withClientName(String clientName) {

            LettuceAssert.notNull(clientName, "Client name must not be null");

            this.clientName = clientName;
            this.sentinels.forEach(it -> it.setClientName(clientName));
            return this;
        }

        /**
         * Configures a library name. Sets library name also for already configured Redis Sentinel nodes.
         *
         * @param libraryName the library name
         * @return the builder
         * @since 6.3
         */
        public Builder withLibraryName(String libraryName) {

            LettuceAssert.notNull(libraryName, "Library name must not be null");

            if (libraryName.indexOf(' ') != -1) {
                throw new IllegalArgumentException("Library name must not contain spaces");
            }

            this.libraryName = libraryName;
            this.sentinels.forEach(it -> it.setLibraryName(libraryName));
            return this;
        }

        /**
         * Configures a library version. Sets library version also for already configured Redis Sentinel nodes.
         *
         * @param libraryVersion the library version
         * @return the builder
         * @since 6.3
         */
        public Builder withLibraryVersion(String libraryVersion) {

            LettuceAssert.notNull(libraryVersion, "Library version must not be null");

            if (libraryVersion.indexOf(' ') != -1) {
                throw new IllegalArgumentException("Library version must not contain spaces");
            }

            this.libraryVersion = libraryVersion;
            this.sentinels.forEach(it -> it.setLibraryVersion(libraryVersion));
            return this;
        }

        /**
         * Configures authentication.
         *
         * @param username the user name
         * @param password the password name
         * @return the builder
         * @since 6.0
         */
        public Builder withAuthentication(String username, CharSequence password) {

            LettuceAssert.notNull(username, "User name must not be null");
            LettuceAssert.notNull(password, "Password must not be null");

            return withAuthentication(() -> Mono.just(RedisCredentials.just(username, password)));
        }

        /**
         * Apply authentication from another {@link RedisURI}. The authentication settings of the {@code source} URI will be
         * applied to this builder.
         *
         * @param source must not be {@code null}.
         * @since 6.0
         */
        public Builder withAuthentication(RedisURI source) {

            LettuceAssert.notNull(source, "Source RedisURI must not be null");

            return withAuthentication(source.getCredentialsProvider());
        }

        /**
         * Configures authentication. Empty password is supported (although not recommended for security reasons).
         *
         * @param username the user name
         * @param password the password name
         * @return the builder
         * @since 6.0
         */
        public Builder withAuthentication(String username, char[] password) {

            LettuceAssert.notNull(username, "User name must not be null");
            LettuceAssert.notNull(password, "Password must not be null");

            return withAuthentication(() -> Mono.just(RedisCredentials.just(username, password)));
        }

        /**
         * Configures authentication.
         *
         * @param credentialsProvider the credentials provider to use
         * @since 6.2
         */
        public Builder withAuthentication(RedisCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        /**
         * Configures authentication. Empty password is supported (although not recommended for security reasons).
         *
         * @param password the password
         * @return the builder
         * @since 6.0
         */
        public Builder withPassword(CharSequence password) {

            LettuceAssert.notNull(password, "Password must not be null");

            char[] chars = new char[password.length()];
            for (int i = 0; i < password.length(); i++) {
                chars[i] = password.charAt(i);
            }

            return withPassword(chars);
        }

        /**
         * Configures authentication. Empty password is supported (although not recommended for security reasons).
         *
         * @param password the password
         * @return the builder
         * @since 4.4
         */
        public Builder withPassword(char[] password) {
            return withAuthentication(() -> Mono.just(RedisCredentials.just(null, password)));
        }

        /**
         * Configures a timeout.
         *
         * @param timeout must not be {@code null} or negative.
         * @return the builder
         */
        public Builder withTimeout(Duration timeout) {

            LettuceAssert.notNull(timeout, "Timeout must not be null");
            LettuceAssert.notNull(!timeout.isNegative(), "Timeout must be greater or equal 0");

            this.timeout = timeout;
            return this;
        }

        /**
         * Configures a sentinel master Id.
         *
         * @param sentinelMasterId sentinel master id, must not be empty or {@code null}
         * @return the builder
         */
        public Builder withSentinelMasterId(String sentinelMasterId) {

            LettuceAssert.notEmpty(sentinelMasterId, "Sentinel master id must not empty");

            this.sentinelMasterId = sentinelMasterId;
            return this;
        }

        /**
         * Builds a new {@link RedisURI}.
         *
         * @return the RedisURI.
         */
        public RedisURI build() {

            if (sentinels.isEmpty() && LettuceStrings.isEmpty(host) && LettuceStrings.isEmpty(socket)) {
                throw new IllegalStateException(
                        "Cannot build a RedisURI. One of the following must be provided Host, Socket or Sentinel");
            }

            RedisURI redisURI = new RedisURI();
            redisURI.setHost(host);
            redisURI.setPort(port);

            if (credentialsProvider != null) {
                redisURI.setCredentialsProvider(credentialsProvider);
            }

            redisURI.setDatabase(database);
            redisURI.setClientName(clientName);
            redisURI.setLibraryName(libraryName);
            redisURI.setLibraryVersion(libraryVersion);

            redisURI.setSentinelMasterId(sentinelMasterId);

            for (RedisURI sentinel : sentinels) {

                sentinel.setTimeout(timeout);
                redisURI.getSentinels().add(sentinel);
            }

            redisURI.setSocket(socket);
            redisURI.setSsl(ssl);
            redisURI.setStartTls(startTls);
            redisURI.setVerifyPeer(verifyMode);
            redisURI.setTimeout(timeout);

            return redisURI;
        }

    }

    /** Return true for valid port numbers. */
    private static boolean isValidPort(int port) {
        return port >= 0 && port <= 65535;
    }

}
