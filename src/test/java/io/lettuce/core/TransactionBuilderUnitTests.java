/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
        builder = new TransactionBuilderImpl<>(connection, codec, null);
    }

    @Test
    void shouldStartEmpty() {
        assertThat(builder.isEmpty()).isTrue();
        assertThat(builder.size()).isEqualTo(0);
    }

    @Test
    void shouldTrackCommandCount() {
        builder.commands().set("key1", "value1");
        assertThat(builder.size()).isEqualTo(1);
        assertThat(builder.isEmpty()).isFalse();

        builder.commands().get("key2");
        assertThat(builder.size()).isEqualTo(2);

        builder.commands().incr("counter");
        assertThat(builder.size()).isEqualTo(3);
    }

    @Test
    void shouldSupportAllCommandsViaCommandsInterface() {
        // String commands
        builder.commands().set("k", "v");
        builder.commands().get("k");
        builder.commands().incr("num");
        builder.commands().decr("num");
        assertThat(builder.size()).isEqualTo(4);
    }

    @Test
    void shouldSupportStringCommands() {
        builder.commands().set("k", "v");
        builder.commands().get("k");
        builder.commands().append("k", "suffix");
        builder.commands().strlen("k");
        builder.commands().getrange("k", 0, 5);
        builder.commands().setrange("k", 0, "new");
        builder.commands().setnx("k2", "v2");
        builder.commands().setex("k3", 60, "v3");
        builder.commands().psetex("k4", 1000, "v4");
        builder.commands().incr("num");
        builder.commands().incrby("num", 10);
        builder.commands().incrbyfloat("num", 1.5);
        builder.commands().decr("num");
        builder.commands().decrby("num", 5);

        assertThat(builder.size()).isEqualTo(14);
    }

    @Test
    void shouldSupportKeyCommands() {
        builder.commands().del("k1", "k2");
        builder.commands().unlink("k3");
        builder.commands().exists("k1");
        builder.commands().expire("k1", 60);
        builder.commands().expire("k2", Duration.ofSeconds(120));
        builder.commands().ttl("k1");
        builder.commands().pttl("k1");
        builder.commands().persist("k1");
        builder.commands().type("k1");

        assertThat(builder.size()).isEqualTo(9);
    }

    @Test
    void shouldSupportHashCommands() {
        builder.commands().hset("hash", "field", "value");
        builder.commands().hget("hash", "field");
        builder.commands().hdel("hash", "field");
        builder.commands().hexists("hash", "field");
        builder.commands().hgetall("hash");
        builder.commands().hkeys("hash");
        builder.commands().hvals("hash");
        builder.commands().hlen("hash");
        builder.commands().hincrby("hash", "num", 1);
        builder.commands().hincrbyfloat("hash", "float", 0.5);

        assertThat(builder.size()).isEqualTo(10);
    }

    @Test
    void shouldSupportListCommands() {
        builder.commands().lpush("list", "v1", "v2");
        builder.commands().rpush("list", "v3");
        builder.commands().lpop("list");
        builder.commands().rpop("list");
        builder.commands().llen("list");
        builder.commands().lindex("list", 0);
        builder.commands().lrange("list", 0, -1);
        builder.commands().lset("list", 0, "new");
        builder.commands().lrem("list", 1, "old");
        builder.commands().ltrim("list", 0, 10);

        assertThat(builder.size()).isEqualTo(10);
    }

    @Test
    void shouldSupportSetCommands() {
        builder.commands().sadd("set", "m1", "m2");
        builder.commands().srem("set", "m1");
        builder.commands().sismember("set", "m2");
        builder.commands().smembers("set");
        builder.commands().scard("set");

        assertThat(builder.size()).isEqualTo(5);
    }

    @Test
    void shouldSupportSortedSetCommands() {
        builder.commands().zadd("zset", 1.0, "m1");
        builder.commands().zadd("zset", ZAddArgs.Builder.nx(), 2.0, "m2");
        builder.commands().zincrby("zset", 0.5, "m1");
        builder.commands().zrem("zset", "m1");
        builder.commands().zcard("zset");
        builder.commands().zscore("zset", "m2");
        builder.commands().zrange("zset", 0, -1);

        assertThat(builder.size()).isEqualTo(7);
    }

    @Test
    void shouldCreateTransactionBuilderWithWatchKeys() {
        TransactionBuilderImpl<String, String> watchBuilder = new TransactionBuilderImpl<>(connection, codec,
                new String[] { "watchKey1", "watchKey2" });

        watchBuilder.commands().set("key", "value");

        // The watch keys should be part of the bundle when created
        assertThat(watchBuilder.size()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldDispatchBundleOnExecuteAsync() {
        when(connection.dispatchTransactionBundle(any())).thenReturn(new AsyncCommand<>(mock(RedisCommand.class)));

        builder.commands().set("key", "value");
        builder.executeAsync();

        verify(connection, times(1)).dispatchTransactionBundle(any());
    }

}
