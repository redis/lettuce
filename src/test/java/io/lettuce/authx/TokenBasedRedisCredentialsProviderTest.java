package io.lettuce.authx;

import io.lettuce.core.RedisCredentials;
import io.lettuce.core.TestTokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import redis.clients.authentication.core.SimpleToken;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenBasedRedisCredentialsProviderTest {

    private TestTokenManager tokenManager;

    private TokenBasedRedisCredentialsProvider credentialsProvider;

    @BeforeEach
    public void setUp() {
        // Use TestToken manager to emit tokens/errors on request
        tokenManager = new TestTokenManager(null, null);
        credentialsProvider = new TokenBasedRedisCredentialsProvider(tokenManager);
    }

    @Test
    public void shouldReturnPreviouslyEmittedTokenWhenResolved() {
        tokenManager.emitToken(testToken("test-user", "token-1"));

        Mono<RedisCredentials> credentials = credentialsProvider.resolveCredentials();

        StepVerifier.create(credentials).assertNext(actual -> {
            assertThat(actual.getUsername()).isEqualTo("test-user");
            assertThat(new String(actual.getPassword())).isEqualTo("token-1");
        }).verifyComplete();
    }

    @Test
    public void shouldReturnLatestEmittedTokenWhenResolved() {
        tokenManager.emitToken(testToken("test-user", "token-2"));
        tokenManager.emitToken(testToken("test-user", "token-3")); // Latest token

        Mono<RedisCredentials> credentials = credentialsProvider.resolveCredentials();

        StepVerifier.create(credentials).assertNext(actual -> {
            assertThat(actual.getUsername()).isEqualTo("test-user");
            assertThat(new String(actual.getPassword())).isEqualTo("token-3");
        }).verifyComplete();
    }

    @Test
    public void shouldReturnTokenEmittedBeforeSubscription() {

        tokenManager.emitToken(testToken("test-user", "token-1"));

        // Test resolveCredentials
        Mono<RedisCredentials> credentials1 = credentialsProvider.resolveCredentials();

        StepVerifier.create(credentials1).assertNext(actual -> {
            assertThat(actual.getUsername()).isEqualTo("test-user");
            assertThat(new String(actual.getPassword())).isEqualTo("token-1");
        }).verifyComplete();

        // Emit second token and subscribe another
        tokenManager.emitToken(testToken("test-user", "token-2"));
        tokenManager.emitToken(testToken("test-user", "token-3"));
        Mono<RedisCredentials> credentials2 = credentialsProvider.resolveCredentials();
        StepVerifier.create(credentials2).assertNext(actual -> {
            assertThat(actual.getUsername()).isEqualTo("test-user");
            assertThat(new String(actual.getPassword())).isEqualTo("token-3");
        }).verifyComplete();
    }

    @Test
    public void shouldWaitForAndReturnTokenWhenEmittedLater() {
        Mono<RedisCredentials> result = credentialsProvider.resolveCredentials();

        tokenManager.emitTokenWithDelay(testToken("test-user", "delayed-token"), 100); // Emit token after 100ms
        StepVerifier.create(result)
                .assertNext(credentials -> assertThat(String.valueOf(credentials.getPassword())).isEqualTo("delayed-token"))
                .verifyComplete();
    }

    @Test
    public void shouldCompleteAllSubscribersOnStop() {
        Flux<RedisCredentials> credentialsFlux1 = credentialsProvider.credentials();
        Flux<RedisCredentials> credentialsFlux2 = credentialsProvider.credentials();

        Disposable subscription1 = credentialsFlux1.subscribe();
        Disposable subscription2 = credentialsFlux2.subscribe();

        tokenManager.emitToken(testToken("test-user", "token-1"));

        new Thread(() -> {
            try {
                Thread.sleep(100); // Delay of 100 milliseconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            credentialsProvider.close();
        }).start();

        StepVerifier.create(credentialsFlux1)
                .assertNext(credentials -> assertThat(String.valueOf(credentials.getPassword())).isEqualTo("token-1"))
                .verifyComplete();

        StepVerifier.create(credentialsFlux2)
                .assertNext(credentials -> assertThat(String.valueOf(credentials.getPassword())).isEqualTo("token-1"))
                .verifyComplete();
    }

    @Test
    public void shouldPropagateMultipleTokensOnStream() {

        Flux<RedisCredentials> result = credentialsProvider.credentials();
        StepVerifier.create(result).then(() -> tokenManager.emitToken(testToken("test-user", "token1")))
                .then(() -> tokenManager.emitToken(testToken("test-user", "token2")))
                .assertNext(credentials -> assertThat(String.valueOf(credentials.getPassword())).isEqualTo("token1"))
                .assertNext(credentials -> assertThat(String.valueOf(credentials.getPassword())).isEqualTo("token2"))
                .thenCancel().verify(Duration.ofMillis(100));
    }

    @Test
    public void shouldHandleTokenRequestErrorGracefully() {
        Exception simulatedError = new RuntimeException("Token request failed");
        tokenManager.emitError(simulatedError);

        Flux<RedisCredentials> result = credentialsProvider.credentials();

        StepVerifier.create(result).expectErrorMatches(
                throwable -> throwable instanceof RuntimeException && "Token request failed".equals(throwable.getMessage()))
                .verify();
    }

    private SimpleToken testToken(String username, String value) {
        return new SimpleToken(username, value, System.currentTimeMillis() + 5000, // expires in 5 seconds
                System.currentTimeMillis(), Collections.emptyMap());
    }

}
