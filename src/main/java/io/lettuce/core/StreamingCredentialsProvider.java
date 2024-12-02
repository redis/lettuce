package io.lettuce.core;

import reactor.core.publisher.Flux;

public interface StreamingCredentialsProvider extends RedisCredentialsProvider {

    /**
     * Returns a {@link Flux} emitting {@link RedisCredentials} that can be used to authorize a Redis connection. This
     * credential provider supports streaming credentials, meaning that it can emit multiple credentials over time.
     *
     * @return
     */
    Flux<RedisCredentials> credentials();

}
