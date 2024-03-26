package io.lettuce.core;

import reactor.core.publisher.Mono;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Static implementation of {@link RedisCredentialsProvider}.
 *
 * @author Mark Paluch
 * @since 6.2
 */
public class StaticCredentialsProvider
        implements RedisCredentialsProvider, RedisCredentialsProvider.ImmediateRedisCredentialsProvider {

    private final RedisCredentials credentials;

    private final Mono<RedisCredentials> mono;

    /**
     * Create a static {@link StaticCredentialsProvider} object from {@code username} and {@code password}.
     *
     * @param username can be {@code null}.
     * @param password can be {@code null}.
     */
    public StaticCredentialsProvider(String username, char[] password) {
        this(RedisCredentials.just(username, password));
    }

    /**
     * Create a static {@link StaticCredentialsProvider} object from {@link RedisCredentials}. The snapshot of the given
     * credentials is used to create a static representation to avoid credentials changes if the {@link RedisCredentials} change
     * over time.
     *
     * @param credentials must not be {@code null}.
     */
    public StaticCredentialsProvider(RedisCredentials credentials) {

        LettuceAssert.notNull(credentials, "RedisCredentials must not be null");

        this.credentials = RedisCredentials.just(credentials.getUsername(), credentials.getPassword());
        this.mono = Mono.just(credentials);
    }

    @Override
    public Mono<RedisCredentials> resolveCredentials() {
        return mono;
    }

    @Override
    public RedisCredentials resolveCredentialsNow() {
        return this.credentials;
    }

}
