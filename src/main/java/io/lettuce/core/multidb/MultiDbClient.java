package io.lettuce.core.multidb;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.CommandListenerWriter;
import io.lettuce.core.ConnectionBuilder;
import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.ConnectionState;
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SslConnectionBuilder;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.protocol.CommandExpiryWriter;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.protocol.PushHandler;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import reactor.core.publisher.Mono;

import java.io.Closeable;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.lettuce.core.RedisAuthenticationHandler.createHandler;

/**
 * A Redis client that supports multiple database endpoints with dynamic switching between them. This client manages connections
 * to multiple Redis endpoints and routes commands to the currently active endpoint using a {@link MultiDbChannelWriter}.
 * <p>
 * The client maintains a set of {@link RedisEndpoints} and allows switching the active endpoint at runtime. All connections
 * created by this client share the same endpoint configuration and will route commands to the currently active endpoint.
 * <p>
 * The MultiDbClient uses an internal {@link RedisClient} to create actual connections to individual endpoints, while the
 * top-level connection returned by {@link #connect()} uses a {@link MultiDbChannelWriter} to route commands.
 *
 * @author Ivo Gaydazhiev
 */
public class MultiDbClient extends AbstractRedisClient {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultiDbClient.class);

    // Internal RedisClient used to create actual connections to individual endpoints
    // private final RedisClient redisClient;

    // shared across all connections created via this MultiDb client
    private final RedisEndpoints endpoints;

    /**
     * Creates a new {@link MultiDbClient} with the specified client resources and endpoints.
     *
     * @param clientResources the client resources. If {@code null}, the client will create a new dedicated instance of client
     *        resources and keep track of them.
     * @param endpoints the Redis endpoints, must not be {@code null}
     */
    protected MultiDbClient(ClientResources clientResources, RedisEndpoints endpoints) {
        super(clientResources);

        LettuceAssert.notNull(endpoints, "RedisEndpoints must not be null");
        this.endpoints = endpoints;
    }

    /**
     * Create a new {@link MultiDbClient} with the specified set of initial Redis URIs and default {@link ClientResources}. The
     * first URI in the set will be set as the active endpoint.
     *
     * @param redisURIs the set of Redis URIs, must not be {@code null} or empty
     * @return a new instance of {@link MultiDbClient}
     */
    public static MultiDbClient create(Set<RedisURI> redisURIs) {
        LettuceAssert.notNull(redisURIs, "RedisURIs must not be null");
        LettuceAssert.isTrue(!redisURIs.isEmpty(), "RedisURIs must not be empty");

        RedisEndpoints endpoints = RedisEndpoints.create(redisURIs);
        return new MultiDbClient(null, endpoints);
    }

    /**
     * Create a new {@link MultiDbClient} with the specified set of initial Redis URIs and shared {@link ClientResources}. The
     * first URI in the set will be set as the active endpoint.
     *
     * @param clientResources the client resources, must not be {@code null}
     * @param redisURIs the set of Redis URIs, must not be {@code null} or empty
     * @return a new instance of {@link MultiDbClient}
     */
    public static MultiDbClient create(ClientResources clientResources, Set<RedisURI> redisURIs) {
        LettuceAssert.notNull(clientResources, "ClientResources must not be null");
        LettuceAssert.notNull(redisURIs, "RedisURIs must not be null");
        LettuceAssert.isTrue(!redisURIs.isEmpty(), "RedisURIs must not be empty");

        RedisEndpoints endpoints = RedisEndpoints.create(redisURIs);
        return new MultiDbClient(clientResources, endpoints);
    }

    /**
     * Open a new connection to the MultiDb setup that treats keys and values as UTF-8 strings. The connection will route
     * commands to the currently active endpoint.
     *
     * @return A new stateful MultiDb connection
     */
    public StatefulMultiDbConnection<String, String> connect() {
        return connect(newStringStringCodec());
    }

    protected StringCodec newStringStringCodec() {
        return StringCodec.UTF8;
    }

    /**
     * Open a new connection to the MultiDb setup. Use the supplied {@link RedisCodec codec} to encode/decode keys and values.
     * The connection will route commands to the currently active endpoint.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful MultiDb connection
     */
    public <K, V> StatefulMultiDbConnection<K, V> connect(RedisCodec<K, V> codec) {

        return getConnection(connectMultiDbAsync(codec), endpoints);
    }

    /**
     * Open a new connection to the MultiDb setup asynchronously. Use the supplied {@link RedisCodec codec} to encode/decode
     * keys and values. The connection will route commands to the currently active endpoint.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful MultiDb connection
     */
    public <K, V> CompletableFuture<StatefulMultiDbConnection<K, V>> connectMultiDbAsync(RedisCodec<K, V> codec) {
        LettuceAssert.notNull(codec, "RedisCodec must not be null");

        // Create MultiDb-specific connection provider and channel writer
        MultiDbChannelWriter multiDbWriter = new MultiDbChannelWriter(getResources());
        MultiDbConnectionProvider<K, V> connectionProvider = new MultiDbConnectionProvider<>(this, codec, endpoints,
                multiDbWriter);
        multiDbWriter.setMultiDbConnectionProvider(connectionProvider);

        // Create endpoint for push handler
        // TODO: ggivo How to handle push messages form multiple endpoints?
        PushHandler pushHandler = NoOpPushHandler.INSTANCE;

        // Use the active endpoint's timeout, or default timeout if not available
        java.time.Duration timeout = endpoints.getActive().getTimeout();

        // Create connection with MultiDbChannelWriter
        StatefulMultiDbConnectionImpl<K, V> connection = new StatefulMultiDbConnectionImpl<>(multiDbWriter, pushHandler, codec,
                timeout, getOptions().getJsonParser());

        connection.setOptions(getOptions());

        // Register the connection itself in closeableResources so it can be tracked
        closeableResources.add(connection);

        // Register the connection's closeab     les (writer, provider) so they get closed when the connection closes
        connection.registerCloseables(closeableResources, multiDbWriter, connectionProvider);

        return CompletableFuture.completedFuture((StatefulMultiDbConnection<K, V>) connection)
                .whenComplete((c,throwable) -> {
                    if (throwable == null) {
                        connection.registerCloseables(closeableResources, connection);
                    }
                } );
    }

    private static <T> T getConnection(CompletableFuture<T> connectionFuture, Object context) {

        try {
            return connectionFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw RedisConnectionException.create(context.toString(), e);
        } catch (Exception e) {

            if (e instanceof ExecutionException) {

                // filter intermediate RedisConnectionException exceptions that bloat the stack trace
                if (e.getCause() instanceof RedisConnectionException
                        && e.getCause().getCause() instanceof RedisConnectionException) {
                    throw RedisConnectionException.create(context.toString(), e.getCause().getCause());
                }

                throw RedisConnectionException.create(context.toString(), e.getCause());
            }

            throw RedisConnectionException.create(context.toString(), e);
        }
    }

    <K, V> ConnectionFuture<StatefulRedisConnection<K, V>> connectToEndpointNodeAsync(RedisCodec<K, V> codec,
            RedisURI endpointUri, RedisChannelWriter multiDbWriter) {

        assertNotNull(codec);
        assertNotNull(endpointUri);
        Mono<SocketAddress> socketAddressSupplier = getSocketAddress(endpointUri);

        LettuceAssert.notNull(socketAddressSupplier, "SocketAddressSupplier must not be null");

        // TODO: ggivo Should we use a multidb specific options endpoint type?
        MultiDbNodeEndpoint endpoint = new MultiDbNodeEndpoint(getOptions(), getResources(), multiDbWriter);

        RedisChannelWriter writer = endpoint;

        if (CommandExpiryWriter.isSupported(getOptions())) {
            writer = CommandExpiryWriter.buildCommandExpiryWriter(writer, getOptions(), getResources());
        }

        if (CommandListenerWriter.isSupported(getCommandListeners())) {
            writer = new CommandListenerWriter(writer, getCommandListeners());
        }

        StatefulRedisConnectionImpl<K, V> connection = newStatefulRedisConnection(writer, endpoint, codec,
                endpointUri.getTimeout(), getOptions().getJsonParser());

        connection
                .setAuthenticationHandler(createHandler(connection, endpointUri.getCredentialsProvider(), false, getOptions()));

        ConnectionFuture<StatefulRedisConnection<K, V>> connectionFuture = connectStatefulAsync(connection, endpoint,
                endpointUri, socketAddressSupplier, () -> new CommandHandler(getOptions(), getResources(), endpoint));

        return connectionFuture.whenComplete((conn, throwable) -> {
            if (throwable != null) {
                connection.closeAsync();
            }
        });
    }

    protected <K, V> StatefulRedisConnectionImpl<K, V> newStatefulRedisConnection(RedisChannelWriter channelWriter,
            PushHandler pushHandler, RedisCodec<K, V> codec, Duration timeout, Supplier<JsonParser> parser) {
        return new StatefulRedisConnectionImpl<>(channelWriter, pushHandler, codec, timeout, parser);
    }

    private <K, V, T extends StatefulRedisConnectionImpl<K, V>, S> ConnectionFuture<S> connectStatefulAsync(T connection,
            DefaultEndpoint endpoint, RedisURI connectionSettings, Mono<SocketAddress> socketAddressSupplier,
            Supplier<CommandHandler> commandHandlerSupplier) {

        ConnectionBuilder connectionBuilder = createConnectionBuilder(connection, connection.getConnectionState(), endpoint,
                connectionSettings, socketAddressSupplier, commandHandlerSupplier);

        ConnectionFuture<RedisChannelHandler<K, V>> future = initializeChannelAsync(connectionBuilder);

        return future.thenApply(channelHandler -> (S) connection);
    }

    private <K, V> ConnectionBuilder createConnectionBuilder(RedisChannelHandler<K, V> connection, ConnectionState state,
            DefaultEndpoint endpoint, RedisURI connectionSettings, Mono<SocketAddress> socketAddressSupplier,
            Supplier<CommandHandler> commandHandlerSupplier) {

        ConnectionBuilder connectionBuilder;
        if (connectionSettings.isSsl()) {
            SslConnectionBuilder sslConnectionBuilder = SslConnectionBuilder.sslConnectionBuilder();
            sslConnectionBuilder.ssl(connectionSettings);
            connectionBuilder = sslConnectionBuilder;
        } else {
            connectionBuilder = ConnectionBuilder.connectionBuilder();
        }

        state.apply(connectionSettings);

        connectionBuilder.connectionInitializer(createHandshake(state));

        // TODO: ggivo enable reconnect listener for multidb?
        // connectionBuilder.reconnectionListener(new ReconnectEventListener(topologyRefreshScheduler));
        connectionBuilder.clientOptions(getOptions());
        connectionBuilder.connection(connection);
        connectionBuilder.clientResources(getResources());
        connectionBuilder.endpoint(endpoint);
        connectionBuilder.commandHandler(commandHandlerSupplier);

        connectionBuilder(socketAddressSupplier, connectionBuilder, connection.getConnectionEvents(), connectionSettings);

        return connectionBuilder;
    }

    protected Mono<SocketAddress> getSocketAddress(RedisURI redisURI) {

        return Mono.defer(() -> {
            return Mono.fromCallable(() -> getResources().socketAddressResolver().resolve((redisURI)));
        });
    }

    /**
     * Get the {@link RedisEndpoints} used by this MultiDb client.
     *
     * @return the Redis endpoints
     */
    public RedisEndpoints getEndpoints() {
        return endpoints;
    }

    /**
     * Set the active endpoint for this MultiDb client. The endpoint must already be registered in the available endpoints. All
     * subsequent commands will be routed to this endpoint.
     *
     * @param redisURI the Redis URI to set as active, must not be {@code null}
     * @throws RedisException if the endpoint is not available
     */
    public void setActive(RedisURI redisURI) {
        endpoints.setActive(redisURI);
        // No need to notify connection providers - active endpoint change doesn't affect connections
        // todo : Gracefully close previous active connecion & notify connection?
        // Revisit in context of fastFailover?
    }

    /**
     * Get the currently active endpoint.
     *
     * @return the active Redis URI
     * @throws RedisException if no active endpoint is set
     */
    public RedisURI getActive() {
        return endpoints.getActive();
    }

    /**
     * Add a new endpoint to the available endpoints.
     *
     * @param redisURI the Redis URI to add, must not be {@code null}
     * @return {@code true} if the endpoint was added, {@code false} if it already exists
     */
    public boolean addEndpoint(RedisURI redisURI) {
        boolean added = endpoints.add(redisURI);
        if (added) {
            updateEndpointsInConnections();
        }
        return added;
    }

    /**
     * Remove an endpoint from the available endpoints. The active endpoint cannot be removed. To remove the currently active
     * endpoint, first switch to a different endpoint using {@link #setActive(RedisURI)}.
     *
     * @param redisURI the Redis URI to remove, must not be {@code null}
     * @return {@code true} if the endpoint was removed, {@code false} if it didn't exist
     * @throws RedisException if attempting to remove the last endpoint or the active endpoint
     */
    public boolean removeEndpoint(RedisURI redisURI) {
        boolean removed = endpoints.remove(redisURI);
        if (removed) {
            updateEndpointsInConnections();
        }
        return removed;
    }

    /**
     * Update endpoints in all active connections. This triggers cleanup of stale connections.
     */
    protected void updateEndpointsInConnections() {
        forEachMultiDbConnection(connection -> connection.setEndpoints(endpoints));
    }

    /**
     * Apply a {@link Consumer} of {@link StatefulMultiDbConnectionImpl} to all active connections.
     *
     * @param function the {@link Consumer}.
     */
    protected void forEachMultiDbConnection(Consumer<StatefulMultiDbConnectionImpl<?, ?>> function) {
        forEachCloseable(input -> input instanceof StatefulMultiDbConnectionImpl, function);
    }

    /**
     * Apply a {@link Consumer} of {@link Closeable} to all active connections.
     *
     * @param <T>
     * @param function the {@link Consumer}.
     */
    @SuppressWarnings("unchecked")
    protected <T extends Closeable> void forEachCloseable(Predicate<? super Closeable> selector, Consumer<T> function) {
        for (Closeable c : closeableResources) {
            if (selector.test(c)) {
                function.accept((T) c);
            }
        }
    }

    @Override
    public void setOptions(ClientOptions options) {
        super.setOptions(options);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    private static <K, V> void assertNotNull(RedisCodec<K, V> codec) {
        LettuceAssert.notNull(codec, "RedisCodec must not be null");
    }

    private static void assertNotNull(RedisURI redisURI) {
        LettuceAssert.notNull(redisURI, "RedisURI must not be null");
    }

    private static void assertNotEmpty(Iterable<RedisURI> redisURIs) {
        LettuceAssert.notNull(redisURIs, "RedisURIs must not be null");
        LettuceAssert.isTrue(redisURIs.iterator().hasNext(), "RedisURIs must not be empty");
    }

}
