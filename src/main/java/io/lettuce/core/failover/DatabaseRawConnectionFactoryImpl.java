package io.lettuce.core.failover;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * Implementation of {@link DatabaseRawConnectionFactory} for creating raw connections to databases.
 * <p>
 * This factory is used by health check strategies to create dedicated connections for health checking, separate from the main
 * application connections.
 */
class DatabaseRawConnectionFactoryImpl implements DatabaseRawConnectionFactory {

    private final io.lettuce.core.ClientOptions clientOptions;

    private final MultiDbClientImpl client;

    /**
     * Creates a new raw connection factory.
     *
     * @param clientOptions the client options to use for connections
     * @param client the multi-database client
     */
    public DatabaseRawConnectionFactoryImpl(io.lettuce.core.ClientOptions clientOptions, MultiDbClientImpl client) {
        this.clientOptions = clientOptions;
        this.client = client;
    }

    @Override
    public StatefulRedisConnection<?, ?> connectToDatabase(RedisURI endpoint) {
        client.setOptions(clientOptions);
        try {
            return client.connect(endpoint);
        } finally {
            client.resetOptions();
        }
    }

}
