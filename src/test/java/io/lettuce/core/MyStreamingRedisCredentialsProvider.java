package io.lettuce.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

public class MyStreamingRedisCredentialsProvider implements StreamingCredentialsProvider {

    private final Sinks.Many<RedisCredentials> credentialsSink = Sinks.many().replay().latest();

    @Override
    public Mono<RedisCredentials> resolveCredentials() {

        return credentialsSink.asFlux().next();
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
