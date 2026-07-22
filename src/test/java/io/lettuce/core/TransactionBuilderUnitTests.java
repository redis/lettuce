/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import reactor.core.publisher.Mono;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Unit tests for {@link TransactionBuilderImpl}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class TransactionBuilderUnitTests {

    private static final StringCodec codec = StringCodec.UTF8;

    @Mock
    private StatefulRedisConnectionImpl<String, String> connection;

    private TransactionBuilderImpl<String, String> builder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(connection.getCodec()).thenReturn(codec);
        // executeAsync() consults ClientOptions#getMaxTransactionBundleSize(); default options disable the guard.
        when(connection.getOptions()).thenReturn(ClientOptions.create());
        builder = new TransactionBuilderImpl<>(connection, codec, null);
    }

    @Test
    void shouldStartEmpty() {
        assertThat(builder.isEmpty()).isTrue();
        assertThat(builder.size()).isEqualTo(0);
    }

    @Test
    void shouldTrackCommandCount() {
        builder.queue().set("key1", "value1");
        assertThat(builder.size()).isEqualTo(1);
        assertThat(builder.isEmpty()).isFalse();

        builder.queue().get("key2");
        assertThat(builder.size()).isEqualTo(2);

        builder.queue().incr("counter");
        assertThat(builder.size()).isEqualTo(3);
    }

    @Test
    void shouldSupportAllCommandsViaCommandsInterface() {
        // String commands
        builder.queue().set("k", "v");
        builder.queue().get("k");
        builder.queue().incr("num");
        builder.queue().decr("num");
        assertThat(builder.size()).isEqualTo(4);
    }

    @Test
    void shouldSupportStringCommands() {
        builder.queue().set("k", "v");
        builder.queue().get("k");
        builder.queue().append("k", "suffix");
        builder.queue().strlen("k");
        builder.queue().getrange("k", 0, 5);
        builder.queue().setrange("k", 0, "new");
        builder.queue().setnx("k2", "v2");
        builder.queue().setex("k3", 60, "v3");
        builder.queue().psetex("k4", 1000, "v4");
        builder.queue().incr("num");
        builder.queue().incrby("num", 10);
        builder.queue().incrbyfloat("num", 1.5);
        builder.queue().decr("num");
        builder.queue().decrby("num", 5);

        assertThat(builder.size()).isEqualTo(14);
    }

    @Test
    void shouldSupportKeyCommands() {
        builder.queue().del("k1", "k2");
        builder.queue().unlink("k3");
        builder.queue().exists("k1");
        builder.queue().expire("k1", 60);
        builder.queue().expire("k2", Duration.ofSeconds(120));
        builder.queue().ttl("k1");
        builder.queue().pttl("k1");
        builder.queue().persist("k1");
        builder.queue().type("k1");

        assertThat(builder.size()).isEqualTo(9);
    }

    @Test
    void shouldSupportHashCommands() {
        builder.queue().hset("hash", "field", "value");
        builder.queue().hget("hash", "field");
        builder.queue().hdel("hash", "field");
        builder.queue().hexists("hash", "field");
        builder.queue().hgetall("hash");
        builder.queue().hkeys("hash");
        builder.queue().hvals("hash");
        builder.queue().hlen("hash");
        builder.queue().hincrby("hash", "num", 1);
        builder.queue().hincrbyfloat("hash", "float", 0.5);

        assertThat(builder.size()).isEqualTo(10);
    }

    @Test
    void shouldSupportListCommands() {
        builder.queue().lpush("list", "v1", "v2");
        builder.queue().rpush("list", "v3");
        builder.queue().lpop("list");
        builder.queue().rpop("list");
        builder.queue().llen("list");
        builder.queue().lindex("list", 0);
        builder.queue().lrange("list", 0, -1);
        builder.queue().lset("list", 0, "new");
        builder.queue().lrem("list", 1, "old");
        builder.queue().ltrim("list", 0, 10);

        assertThat(builder.size()).isEqualTo(10);
    }

    @Test
    void shouldSupportSetCommands() {
        builder.queue().sadd("set", "m1", "m2");
        builder.queue().srem("set", "m1");
        builder.queue().sismember("set", "m2");
        builder.queue().smembers("set");
        builder.queue().scard("set");

        assertThat(builder.size()).isEqualTo(5);
    }

    @Test
    void shouldSupportSortedSetCommands() {
        builder.queue().zadd("zset", 1.0, "m1");
        builder.queue().zadd("zset", ZAddArgs.Builder.nx(), 2.0, "m2");
        builder.queue().zincrby("zset", 0.5, "m1");
        builder.queue().zrem("zset", "m1");
        builder.queue().zcard("zset");
        builder.queue().zscore("zset", "m2");
        builder.queue().zrange("zset", 0, -1);

        assertThat(builder.size()).isEqualTo(7);
    }

    @Test
    void shouldCreateTransactionBuilderWithWatchKeys() {
        TransactionBuilderImpl<String, String> watchBuilder = new TransactionBuilderImpl<>(connection, codec,
                new String[] { "watchKey1", "watchKey2" });

        watchBuilder.queue().set("key", "value");

        // The watch keys should be part of the bundle when created
        assertThat(watchBuilder.size()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldDispatchBundleOnExecuteAsync() {
        when(connection.dispatchTransactionBundle(any())).thenReturn(new AsyncCommand<>(mock(RedisCommand.class)));

        builder.queue().set("key", "value");
        builder.executeAsync();

        verify(connection, times(1)).dispatchTransactionBundle(any());
    }

    @Test // WI-5/D2: executeReactive() must be cold - no dispatch until the returned Mono is subscribed
    @SuppressWarnings("unchecked")
    void executeReactiveDoesNotDispatchUntilSubscribe() {
        when(connection.dispatchTransactionBundle(any())).thenReturn(new AsyncCommand<>(mock(RedisCommand.class)));

        builder.queue().set("key", "value");
        Mono<TransactionResult> mono = builder.executeReactive();

        // Assembly must not have dispatched anything yet.
        verify(connection, never()).dispatchTransactionBundle(any());

        mono.subscribe();
        verify(connection, times(1)).dispatchTransactionBundle(any());
    }

    @Test // WI-5: a builder is one-shot; re-executing (e.g. a second subscription) fails fast instead of dispatching empty
    @SuppressWarnings("unchecked")
    void executeAsyncIsOneShot() {
        when(connection.dispatchTransactionBundle(any())).thenReturn(new AsyncCommand<>(mock(RedisCommand.class)));

        builder.queue().set("key", "value");
        builder.executeAsync();

        assertThatThrownBy(() -> builder.executeAsync()).isInstanceOf(IllegalStateException.class);
    }

    @Test // WI-6/A2/A3: sync execute() honors the connection timeout and throws RedisCommandTimeoutException
    @SuppressWarnings("unchecked")
    void executeTimesOutWithRedisCommandTimeoutException() {
        when(connection.getTimeout()).thenReturn(Duration.ofMillis(100));
        // A never-completing future simulates a stuck transaction.
        when(connection.dispatchTransactionBundle(any())).thenReturn(new AsyncCommand<>(mock(RedisCommand.class)));

        builder.queue().set("key", "value");

        assertThatThrownBy(() -> builder.execute()).isInstanceOf(RedisCommandTimeoutException.class);
    }

    @Test // WI-10/P2: the configurable bundle size guard fails fast before dispatch
    void executeAsyncFailsFastWhenExceedingBundleSizeGuard() {
        when(connection.getOptions()).thenReturn(ClientOptions.builder().maxTransactionBundleSize(1).build());

        builder.queue().set("k1", "v1");
        builder.queue().set("k2", "v2"); // 2 > limit of 1

        assertThatThrownBy(() -> builder.executeAsync()).isInstanceOf(RedisException.class)
                .hasMessageContaining("exceeds the configured maximum");

        // guard runs before dispatch
        verify(connection, never()).dispatchTransactionBundle(any());
    }

}
