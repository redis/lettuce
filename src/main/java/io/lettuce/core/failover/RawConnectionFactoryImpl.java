package io.lettuce.core.failover;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * Implementation of {@link RawConnectionFactory} for creating raw connections to databases.
 * <p>
 * This factory is used by health check strategies to create dedicated connections for health checking, separate from the main
 * application connections.
 * <p>
 * There is room to improve this factory since it hides the underlying {@link ClientOptions}, which might need to mutate or to
 * be exposed depending on the use case.
 */
class RawConnectionFactoryImpl implements RawConnectionFactory {

    private final ClientOptions clientOptions;

    private final MultiDbClientImpl client;

    /**
     * Creates a new raw connection factory.
     *
     * @param clientOptions the client options to use for connections
     * @param client the multi-database client
     */
    public RawConnectionFactoryImpl(ClientOptions clientOptions, MultiDbClientImpl client) {
        this.clientOptions = clientOptions;
        this.client = client;
    }

    @Override
    public StatefulRedisConnection<?, ?> create(RedisURI endpoint) {
        client.setOptions(clientOptions);
        try {
            return client.connect(endpoint);
        } finally {
            client.resetOptions();
        }
    }

}
