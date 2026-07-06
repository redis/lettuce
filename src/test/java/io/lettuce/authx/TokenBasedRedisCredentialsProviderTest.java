package io.lettuce.authx;

import io.lettuce.TestTags;
import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisCredentialsProvider.CredentialsSubscription;
import io.lettuce.core.TestTokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import redis.clients.authentication.core.SimpleToken;
import redis.clients.authentication.core.TokenManagerConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag(TestTags.UNIT_TEST)
public class TokenBasedRedisCredentialsProviderTest {

    private TestTokenManager tokenManager;

    private TokenBasedRedisCredentialsProvider credentialsProvider;

    @BeforeEach
    public void setUp() {
        // Use TestToken manager to emit tokens/errors on request
        TokenManagerConfig tokenManagerConfig = mock(TokenManagerConfig.class);
        when(tokenManagerConfig.getRetryPolicy()).thenReturn(mock(TokenManagerConfig.RetryPolicy.class));
        tokenManager = new TestTokenManager(null, tokenManagerConfig);
        credentialsProvider = TokenBasedRedisCredentialsProvider.create(tokenManager);
    }

    @Test
    public void shouldReturnPreviouslyEmittedTokenWhenResolved() {
        tokenManager.emitToken(testToken("test-user", "token-1"));

        Mono<RedisCredentials> credentials = Mono.fromCompletionStage(credentialsProvider.resolveCredentials());

        StepVerifier.create(credentials).assertNext(actual -> {
            assertThat(actual.getUsername()).isEqualTo("test-user");
            assertThat(new String(actual.getPassword())).isEqualTo("token-1");
        }).verifyComplete();
    }

    @Test
    public void shouldReturnLatestEmittedTokenWhenResolved() {
        tokenManager.emitToken(testToken("test-user", "token-2"));
        tokenManager.emitToken(testToken("test-user", "token-3")); // Latest token

        Mono<RedisCredentials> credentials = Mono.fromCompletionStage(credentialsProvider.resolveCredentials());

        StepVerifier.create(credentials).assertNext(actual -> {
            assertThat(actual.getUsername()).isEqualTo("test-user");
            assertThat(new String(actual.getPassword())).isEqualTo("token-3");
        }).verifyComplete();
    }

    @Test
    public void shouldReturnTokenEmittedBeforeSubscription() {

        tokenManager.emitToken(testToken("test-user", "token-1"));

        // Test resolveCredentials
        Mono<RedisCredentials> credentials1 = Mono.fromCompletionStage(credentialsProvider.resolveCredentials());

        StepVerifier.create(credentials1).assertNext(actual -> {
            assertThat(actual.getUsername()).isEqualTo("test-user");
            assertThat(new String(actual.getPassword())).isEqualTo("token-1");
        }).verifyComplete();

        // Emit second token and subscribe another
        tokenManager.emitToken(testToken("test-user", "token-2"));
        tokenManager.emitToken(testToken("test-user", "token-3"));
        Mono<RedisCredentials> credentials2 = Mono.fromCompletionStage(credentialsProvider.resolveCredentials());
        StepVerifier.create(credentials2).assertNext(actual -> {
            assertThat(actual.getUsername()).isEqualTo("test-user");
            assertThat(new String(actual.getPassword())).isEqualTo("token-3");
        }).verifyComplete();
    }

    @Test
    public void shouldWaitForAndReturnTokenWhenEmittedLater() {
        Mono<RedisCredentials> result = Mono.fromCompletionStage(credentialsProvider.resolveCredentials());

        tokenManager.emitTokenWithDelay(testToken("test-user", "delayed-token"), 100); // Emit token after 100ms
        StepVerifier.create(result)
                .assertNext(credentials -> assertThat(String.valueOf(credentials.getPassword())).isEqualTo("delayed-token"))
                .verifyComplete();
    }

    @Test
    public void shouldStopDeliveringToSubscribersOnClose() throws InterruptedException {
        List<RedisCredentials> received1 = new CopyOnWriteArrayList<>();
        List<RedisCredentials> received2 = new CopyOnWriteArrayList<>();
        CountDownLatch firstTokenLatch = new CountDownLatch(2);

        CredentialsSubscription sub1 = credentialsProvider.subscribeToCredentials(c -> {
            received1.add(c);
            firstTokenLatch.countDown();
        }, t -> {
        });
        CredentialsSubscription sub2 = credentialsProvider.subscribeToCredentials(c -> {
            received2.add(c);
            firstTokenLatch.countDown();
        }, t -> {
        });

        tokenManager.emitToken(testToken("test-user", "token-1"));

        assertThat(firstTokenLatch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(received1).hasSize(1);
        assertThat(String.valueOf(received1.get(0).getPassword())).isEqualTo("token-1");
        assertThat(received2).hasSize(1);
        assertThat(String.valueOf(received2.get(0).getPassword())).isEqualTo("token-1");

        credentialsProvider.close();
        // Emissions after close must not reach subscribers.
        tokenManager.emitToken(testToken("test-user", "token-2"));
        Thread.sleep(50);

        assertThat(received1).hasSize(1);
        assertThat(received2).hasSize(1);

        sub1.close();
        sub2.close();
    }

    @Test
    public void shouldPropagateMultipleTokensOnStream() throws InterruptedException {
        List<RedisCredentials> received = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        CredentialsSubscription sub = credentialsProvider.subscribeToCredentials(c -> {
            received.add(c);
            latch.countDown();
        }, t -> {
        });

        tokenManager.emitToken(testToken("test-user", "token1"));
        tokenManager.emitToken(testToken("test-user", "token2"));

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        List<String> passwords = new ArrayList<>();
        for (RedisCredentials c : received) {
            passwords.add(String.valueOf(c.getPassword()));
        }
        assertThat(passwords).containsExactly("token1", "token2");

        sub.close();
    }

    @Test
    public void shouldReplayLatestTokenToNewSubscriber() throws InterruptedException {
        tokenManager.emitToken(testToken("test-user", "token-1"));

        AtomicReference<RedisCredentials> received = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        CredentialsSubscription sub = credentialsProvider.subscribeToCredentials(c -> {
            received.set(c);
            latch.countDown();
        }, t -> {
        });

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(String.valueOf(received.get().getPassword())).isEqualTo("token-1");

        sub.close();
    }

    @Test
    public void shouldPropagateTokenRequestErrorsToSubscribers() throws InterruptedException {
        Exception simulatedError = new RuntimeException("Token request failed");

        List<RedisCredentials> received = new CopyOnWriteArrayList<>();
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        CountDownLatch tokensLatch = new CountDownLatch(2);
        CountDownLatch errorLatch = new CountDownLatch(1);

        CredentialsSubscription sub = credentialsProvider.subscribeToCredentials(c -> {
            received.add(c);
            tokensLatch.countDown();
        }, t -> {
            errors.add(t);
            errorLatch.countDown();
        });

        tokenManager.emitToken(testToken("test-user", "token1"));
        tokenManager.emitError(simulatedError);
        tokenManager.emitToken(testToken("test-user", "token2"));

        assertThat(tokensLatch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(errorLatch.await(1, TimeUnit.SECONDS)).isTrue();

        List<String> passwords = new ArrayList<>();
        for (RedisCredentials c : received) {
            passwords.add(String.valueOf(c.getPassword()));
        }
        assertThat(passwords).containsExactly("token1", "token2");
        assertThat(errors).containsExactly(simulatedError);

        sub.close();
    }

    private SimpleToken testToken(String username, String value) {
        return new SimpleToken(username, value, System.currentTimeMillis() + 5000, // expires in 5 seconds
                System.currentTimeMillis(), Collections.emptyMap());
    }

}
