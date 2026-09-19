/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;

/**
 * Guards the compatibility contract of the credentials provider surface that downstream libraries (for example Spring Data
 * Redis) compile against.
 * <p>
 * A binary compiled against an earlier release records each call as an exact {@code name + descriptor} reference and the JVM
 * resolves it verbatim at link time. The deprecated reactive {@link RedisCredentialsProvider} accessors on {@link RedisURI}
 * must therefore remain present with unchanged signatures, alongside the reactor-free {@link CredentialsProvider} replacements,
 * so those pre-existing references keep resolving.
 */
@Tag(UNIT_TEST)
class CredentialsProviderCompatibilityUnitTests {

    @Test
    @SuppressWarnings("deprecation")
    void redisUriRetainsDeprecatedReactiveCredentialsSurface() throws NoSuchMethodException {

        // getCredentialsProvider():RedisCredentialsProvider -- Spring Data Redis LettuceConverters binds to this descriptor.
        Method getter = RedisURI.class.getMethod("getCredentialsProvider");
        assertThat(getter.getReturnType()).isEqualTo(RedisCredentialsProvider.class);

        // The pre-7.8 mutator descriptors must survive so callers compiled against them keep linking.
        RedisURI.class.getMethod("setCredentialsProvider", RedisCredentialsProvider.class);
        RedisURI.Builder.class.getMethod("withAuthentication", RedisCredentialsProvider.class);
    }

    @Test
    void redisUriExposesReactorFreeCredentialsSurface() throws NoSuchMethodException {

        Method asyncGetter = RedisURI.class.getMethod("getCredentialsProviderAsync");
        assertThat(asyncGetter.getReturnType()).isEqualTo(CredentialsProvider.class);

        RedisURI.class.getMethod("setCredentialsProvider", CredentialsProvider.class);
        RedisURI.Builder.class.getMethod("withAuthentication", CredentialsProvider.class);
    }

    @Test
    @SuppressWarnings("deprecation")
    void redisCredentialsProviderRemainsReactiveAndExtendsCredentialsProvider() throws NoSuchMethodException {

        // The inheritance that makes RedisCredentialsProvider a CredentialsProvider is source- and binary-compatible.
        assertThat(CredentialsProvider.class).isAssignableFrom(RedisCredentialsProvider.class);

        // The reactive method must keep returning Mono (Spring Data Redis binds resolveCredentials():Mono).
        Method resolve = RedisCredentialsProvider.class.getMethod("resolveCredentials");
        assertThat(resolve.getReturnType()).isEqualTo(Mono.class);

        // The reactor-free method resolves as a CompletionStage.
        Method resolveAsync = CredentialsProvider.class.getMethod("resolveCredentialsAsync");
        assertThat(resolveAsync.getReturnType()).isEqualTo(CompletionStage.class);
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedBuilderAndSetterAcceptRedisCredentialsProviderAndRoundTrip() {

        // Mirrors Spring Data Redis usage: a RedisCredentialsProvider handed to the builder and setter, read back reactively.
        RedisCredentialsProvider provider = RedisCredentialsProvider
                .from(() -> RedisCredentials.just("alice", "secret".toCharArray()));

        RedisURI viaBuilder = RedisURI.builder().withHost("localhost").withAuthentication(provider).build();
        RedisCredentials fromBuilder = viaBuilder.getCredentialsProvider().resolveCredentials().block();
        assertThat(fromBuilder.getUsername()).isEqualTo("alice");

        RedisURI viaSetter = RedisURI.create("localhost", 6379);
        viaSetter.setCredentialsProvider(provider);
        RedisCredentials fromSetter = viaSetter.getCredentialsProvider().resolveCredentials().block();
        assertThat(new String(fromSetter.getPassword())).isEqualTo("secret");
    }

}
