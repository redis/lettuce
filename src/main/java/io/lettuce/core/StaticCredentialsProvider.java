package io.lettuce.core;

import reactor.core.publisher.Mono;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Static implementation of {@link RedisCredentialsProvider}.
 * <p>
 * In Lettuce 8.0, this class implements only {@link CredentialsProvider.ImmediateCredentialsProvider}: it is no longer a
 * {@link RedisCredentialsProvider} or a {@link RedisCredentialsProvider.ImmediateRedisCredentialsProvider}, and
 * {@link #resolveCredentials()} is removed. Refer to it as a {@link CredentialsProvider} or a
 * {@link CredentialsProvider.ImmediateCredentialsProvider}, and resolve credentials with {@link #resolveCredentialsAsync()} or
 * {@link #resolveCredentialsNow()}. {@link RedisURI#getCredentialsProvider()} then returns it wrapped in an adapter, so don't
 * cast the result of that method to {@code StaticCredentialsProvider}; use {@link RedisURI#getCredentialsProviderAsync()}.
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

    /**
     * @return a {@link Mono} emitting the static {@link RedisCredentials}
     * @deprecated since 7.9, use {@link #resolveCredentialsAsync()} — or {@link #resolveCredentialsNow()} for immediate,
     *             non-deferred access — instead; scheduled for removal in Lettuce 8.0.
     */
    @Deprecated
    @Override
    public Mono<RedisCredentials> resolveCredentials() {
        return mono;
    }

    @Override
    public RedisCredentials resolveCredentialsNow() {
        return this.credentials;
    }

}
