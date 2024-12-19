/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.authx;

import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisCredentialsProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import redis.clients.authentication.core.Token;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.core.TokenListener;
import redis.clients.authentication.core.TokenManager;

public class TokenBasedRedisCredentialsProvider implements RedisCredentialsProvider, AutoCloseable {

    private final TokenManager tokenManager;

    private final Sinks.Many<RedisCredentials> credentialsSink = Sinks.many().replay().latest();

    public TokenBasedRedisCredentialsProvider(TokenAuthConfig tokenAuthConfig) {
        this(new TokenManager(tokenAuthConfig.getIdentityProviderConfig().getProvider(),
                tokenAuthConfig.getTokenManagerConfig()));

    }

    public TokenBasedRedisCredentialsProvider(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
        initializeTokenManager();
    }

    /**
     * Initialize the TokenManager and subscribe to token renewal events.
     */
    private void initializeTokenManager() {
        TokenListener listener = new TokenListener() {

            @Override
            public void onTokenRenewed(Token token) {
                try {
                    String username = token.getUser();
                    char[] pass = token.getValue().toCharArray();
                    RedisCredentials credentials = RedisCredentials.just(username, pass);
                    credentialsSink.tryEmitNext(credentials);
                } catch (Exception e) {
                    credentialsSink.emitError(e, Sinks.EmitFailureHandler.FAIL_FAST);
                }
            }

            @Override
            public void onError(Exception exception) {
                credentialsSink.tryEmitError(exception);
            }

        };

        try {
            tokenManager.start(listener, false);
        } catch (Exception e) {
            credentialsSink.tryEmitError(e);
        }
    }

    /**
     * Resolve the latest available credentials as a Mono.
     * <p>
     * This method returns a Mono that emits the most recent set of Redis credentials. The Mono will complete once the
     * credentials are emitted. If no credentials are available at the time of subscription, the Mono will wait until
     * credentials are available.
     *
     * @return a Mono that emits the latest Redis credentials
     */
    @Override
    public Mono<RedisCredentials> resolveCredentials() {

        return credentialsSink.asFlux().next();
    }

    /**
     * Expose the Flux for all credential updates.
     * <p>
     * This method returns a Flux that emits all updates to the Redis credentials. Subscribers will receive the latest
     * credentials whenever they are updated. The Flux will continue to emit updates until the provider is shut down.
     *
     * @return a Flux that emits all updates to the Redis credentials
     */
    @Override
    public Flux<RedisCredentials> credentials() {

        return credentialsSink.asFlux().onBackpressureLatest(); // Provide a continuous stream of credentials
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    /**
     * Stop the credentials provider and clean up resources.
     * <p>
     * This method stops the TokenManager and completes the credentials sink, ensuring that all resources are properly released.
     * It should be called when the credentials provider is no longer needed.
     */
    @Override
    public void close() {
        credentialsSink.tryEmitComplete();
        tokenManager.stop();
    }

}
