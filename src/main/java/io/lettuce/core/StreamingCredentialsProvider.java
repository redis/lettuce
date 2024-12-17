package io.lettuce.core;

import reactor.core.publisher.Flux;

/**
 * A provider for streaming credentials that can be used to authorize a Redis connection and re-authenticate the connection when
 * new credentials are received.
 *
 * @author Ivo Gaydajiev
 * @since 6.6.0
 */
public interface StreamingCredentialsProvider extends RedisCredentialsProvider {

    /**
     * Returns a {@link Flux} emitting {@link RedisCredentials} that can be used to authorize a Redis connection. This
     * credential provider supports streaming credentials, meaning that it can emit multiple credentials over time.
     *
     * @return
     */
    Flux<RedisCredentials> credentials();

    default boolean supportsStreaming() {
        return true;
    }

}
