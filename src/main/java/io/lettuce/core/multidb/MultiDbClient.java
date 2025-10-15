package io.lettuce.core.multidb;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceLists;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

// TODO : ggivo Should we extend from AbstractRedisClient instead requiring RedisClient as a parameter?
public class MultiDbClient {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultiDbClient.class);

    private final RedisClient redisClient;

    // shared across all connections created via this MultiDb client
    private final RedisEndpoints endpoints;

    protected MultiDbClient(RedisClient redisClient, RedisEndpoints endpoints) {

        assertNotNull(redisClient);
        LettuceAssert.notNull(endpoints, "RedisEndpoints must not be null");

        this.redisClient = redisClient;
        this.endpoints = endpoints;
    }

    public StatefulRedisConnection<String, String> connect() {
        return connect(newStringStringCodec());
    }

    public <K, V> StatefulRedisConnection<K, V> connect(RedisCodec<K, V> codec) {

        return getConnection(connectMultiDbAsync(codec), endpoints);
    }

    protected RedisCodec<String, String> newStringStringCodec() {
        return StringCodec.UTF8;
    }

    private <K, V> CompletableFuture<StatefulRedisConnection<K, V>> connectMultiDbAsync(RedisCodec<K, V> codec) {

        MultiDbConnectionProvider<K, V> connectionProvider = new MultiDbConnectionProvider<>(redisClient, codec, endpoints);

        MultiDbChannelWriter multiDbWriter = new MultiDbChannelWriter(connectionProvider, redisClient.getResources());

        // TODO: ggivo - Implement proper connection initialization
        RedisURI activeUri = endpoints.getActive();
        Duration timeout = activeUri.getTimeout();
        StatefulRedisConnectionImpl<K, V> connection =
                new StatefulRedisConnectionImpl<>(multiDbWriter, NoOpPushHandler.INSTANCE, codec, timeout);

        // Set client options for timeout handling
        connection.setOptions(redisClient.getOptions());

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

    private static <T> CompletableFuture<T> transformAsyncConnectionException(CompletionStage<T> future, Object context) {

        return ConnectionFuture.from(null, future.toCompletableFuture()).thenCompose((v, e) -> {

            if (e != null) {

                // filter intermediate RedisConnectionException exceptions that bloat the stack trace
                if (e.getCause() instanceof RedisConnectionException
                        && e.getCause().getCause() instanceof RedisConnectionException) {
                    return Futures.failed(RedisConnectionException.create(context.toString(), e.getCause()));
                }
                return Futures.failed(RedisConnectionException.create(context.toString(), e));
            }

            return CompletableFuture.completedFuture(v);
        }).toCompletableFuture();
    }

    public static MultiDbClient create(RedisClient redisClient, Set<RedisURI> redisURI) {
        assertNotNull(redisClient);
        LettuceAssert.notNull(redisURI, "RedisURI must not be null");

        RedisEndpoints endpoints = RedisEndpoints.create(redisURI);
        return new MultiDbClient(redisClient, endpoints);
    }

    /**
     * Create a new MultiDb client that connects to the supplied {@link RedisURI uri}s. You can connect to different Redis
     * servers but you must supply a {@link RedisURI} on connecting.
     *
     * @param redisClient the Redis client, must not be {@code null}
     * @param redisURIs one or more Redis URI, must not be {@code null} and not empty
     * @return a new instance of {@link MultiDbClient}
     */
    public static MultiDbClient create(RedisClient redisClient, Iterable<RedisURI> redisURIs) {
        assertNotNull(redisClient);
        assertNotEmpty(redisURIs);

        List<RedisURI> uriList = LettuceLists.newList(redisURIs);
        RedisEndpoints endpoints = RedisEndpoints.create(uriList);
        return new MultiDbClient(redisClient, endpoints);
    }

    /**
     * Create a new MultiDb client with pre-configured {@link RedisEndpoints}.
     *
     * @param redisClient the Redis client, must not be {@code null}
     * @param endpoints the Redis endpoints, must not be {@code null}
     * @return a new instance of {@link MultiDbClient}
     */
    public static MultiDbClient create(RedisClient redisClient, RedisEndpoints endpoints) {
        assertNotNull(redisClient);
        LettuceAssert.notNull(endpoints, "RedisEndpoints must not be null");

        return new MultiDbClient(redisClient, endpoints);
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
     * Set the active endpoint for this MultiDb client. The endpoint must already be registered in the available endpoints.
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

    private static void assertNotEmpty(Iterable<RedisURI> redisURIs) {
        LettuceAssert.notNull(redisURIs, "RedisURIs must not be null");
        LettuceAssert.isTrue(redisURIs.iterator().hasNext(), "RedisURIs must not be empty");
    }

    private static void assertNotNull(RedisClient redisClient) {
        LettuceAssert.notNull(redisClient, "RedisClient must not be null");
    }

}
