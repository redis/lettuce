package io.lettuce.core;

import java.util.concurrent.CompletionStage;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * A provider for streaming credentials that can be used to authorize a Redis connection
 *
 * @author Ivo Gaydajiev
 * @since 6.6.0
 */
public class MyStreamingRedisCredentialsProvider implements RedisCredentialsProvider {

    private final Sinks.Many<RedisCredentials> credentialsSink = Sinks.many().replay().latest();

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public CompletionStage<RedisCredentials> resolveCredentials() {

        return credentialsSink.asFlux().next().toFuture();
    }

    public Flux<RedisCredentials> credentials() {

        return credentialsSink.asFlux().onBackpressureLatest(); // Provide a continuous stream of credentials
    }

    public void shutdown() {
        credentialsSink.tryEmitComplete();
    }

    public void emitCredentials(RedisCredentials credentials) {
        credentialsSink.tryEmitNext(credentials);
    }

    public void emitCredentials(String username, char[] password) {
        credentialsSink.tryEmitNext(new StaticRedisCredentials(username, password));
    }

    public void tryEmitError(RuntimeException testError) {
        credentialsSink.tryEmitError(testError);
    }

}
