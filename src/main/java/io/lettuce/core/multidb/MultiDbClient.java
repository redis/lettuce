package io.lettuce.core.multidb;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.PushHandler;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
    private final RedisClient redisClient;

    // shared across all connections created via this MultiDb client
    private final RedisEndpoints endpoints;

    private final boolean sharedResources;

    /**
     * Creates a new {@link MultiDbClient} with the specified client resources and endpoints.
     *
     * @param clientResources the client resources, must not be {@code null}
     * @param endpoints the Redis endpoints, must not be {@code null}
     * @param sharedResources whether the client resources are shared
     */
    protected MultiDbClient(ClientResources clientResources, RedisEndpoints endpoints, boolean sharedResources) {
        super(clientResources);

        LettuceAssert.notNull(endpoints, "RedisEndpoints must not be null");
        this.endpoints = endpoints;
        this.sharedResources = sharedResources;
        this.redisClient = RedisClient.create(clientResources);
    }

    /**
     * Create a new {@link MultiDbClient} with the specified set of initial Redis URIs. The first URI in the set will be set as
     * the active endpoint.
     *
     * @param redisURIs the set of Redis URIs, must not be {@code null} or empty
     * @return a new instance of {@link MultiDbClient}
     */
    public static MultiDbClient create(Set<RedisURI> redisURIs) {
        LettuceAssert.notNull(redisURIs, "RedisURIs must not be null");
        LettuceAssert.isTrue(!redisURIs.isEmpty(), "RedisURIs must not be empty");

        RedisEndpoints endpoints = RedisEndpoints.create(redisURIs);
        ClientResources resources = ClientResources.create();
        return new MultiDbClient(resources, endpoints, false);
    }

    /**
     * Open a new connection to the MultiDb setup that treats keys and values as UTF-8 strings. The connection will route
     * commands to the currently active endpoint.
     *
     * @return A new stateful Redis connection
     */
    public StatefulRedisConnection<String, String> connect() {
        return connect(newStringStringCodec());
    }

    protected StringCodec newStringStringCodec() {
        return StringCodec.UTF8;
    }

    public <K, V> StatefulRedisConnection<K, V> connect(RedisCodec<K, V> codec) {

        return getConnection(connectMultiDbAsync(codec), endpoints);
    }

    /**
     * Open a new connection to the MultiDb setup. Use the supplied {@link RedisCodec codec} to encode/decode keys and values.
     * The connection will route commands to the currently active endpoint.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful Redis connection
     */
    public <K, V> CompletableFuture<StatefulRedisConnection<K, V>> connectMultiDbAsync(RedisCodec<K, V> codec) {
        LettuceAssert.notNull(codec, "RedisCodec must not be null");

        // Create MultiDb-specific connection provider and channel writer
        MultiDbConnectionProvider<K, V> connectionProvider = new MultiDbConnectionProvider<>(redisClient, codec, endpoints);
        MultiDbChannelWriter multiDbWriter = new MultiDbChannelWriter(connectionProvider, getResources());

        // Create endpoint for push handler
        // TODO: ggivo How to handle push messages form multiple endpoints?
        PushHandler pushHandler = NoOpPushHandler.INSTANCE;

        // Use the active endpoint's timeout, or default timeout if not available
        java.time.Duration timeout = endpoints.getActive().getTimeout();

        // Create connection with MultiDbChannelWriter
        StatefulRedisConnectionImpl<K, V> connection = new StatefulRedisConnectionImpl<>(multiDbWriter, pushHandler, codec,
                timeout, getOptions().getJsonParser());

        connection.setOptions(getOptions());

        return CompletableFuture.completedFuture(connection);
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
        return endpoints.add(redisURI);
    }

    /**
     * Remove an endpoint from the available endpoints. If the removed endpoint is currently active, the active endpoint will be
     * set to {@code null}.
     *
     * @param redisURI the Redis URI to remove, must not be {@code null}
     * @return {@code true} if the endpoint was removed, {@code false} if it didn't exist
     * @throws RedisException if attempting to remove the last endpoint
     */
    public boolean removeEndpoint(RedisURI redisURI) {
        return endpoints.remove(redisURI);
    }

    @Override
    public void setOptions(ClientOptions options) {
        super.setOptions(options);
        redisClient.setOptions(options);
    }

    @Override
    public void shutdown() {
        if (redisClient != null) {
            redisClient.shutdown();
        }
        super.shutdown();
    }

}
