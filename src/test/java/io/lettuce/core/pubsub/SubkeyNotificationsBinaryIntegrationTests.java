/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.pubsub;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.lettuce.core.support.CapturingPubSubListener;
import io.lettuce.core.support.CapturingPubSubListener.Notification;

/**
 * Binary integration tests for Redis 8.8 Subkey Notifications, exchanging keys, channels and payloads as raw {@code byte[]} via
 * {@link ByteArrayCodec} to cover bytes that would be lost or corrupted by {@code StringCodec.UTF8} — non-ASCII bytes and the
 * channel-internal {@code |} and {@code \n} separators. Server gating mirrors {@link SubkeyNotificationsIntegrationTests}.
 *
 * @author Aleksandar Todorov
 */
@Tag(INTEGRATION_TEST)
class SubkeyNotificationsBinaryIntegrationTests extends AbstractRedisClientTest {

    private StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection;

    private RedisPubSubCommands<byte[], byte[]> pubsub;

    private RedisCommands<byte[], byte[]> binaryRedis;

    private String originalNotifyConfig;

    @BeforeEach
    void openPubSubConnection() {
        pubSubConnection = client.connectPubSub(ByteArrayCodec.INSTANCE);
        pubsub = pubSubConnection.sync();
        binaryRedis = client.connect(ByteArrayCodec.INSTANCE).sync();

        originalNotifyConfig = redis.configGet("notify-keyspace-events").getOrDefault("notify-keyspace-events", "");
        try {
            redis.configSet("notify-keyspace-events", "AKEhSTIV");
        } catch (RedisCommandExecutionException e) {
            Assumptions.abort("Server does not support subkey notification flags (STIV): " + e.getMessage());
        }
    }

    @AfterEach
    void closePubSubConnection() {
        if (redis != null) {
            try {
                redis.configSet("notify-keyspace-events", originalNotifyConfig == null ? "" : originalNotifyConfig);
            } catch (Exception ignore) {
                // best-effort restore
            }
        }
        if (pubSubConnection != null) {
            pubSubConnection.close();
        }
    }

    @Test
    void subkeyspace_subkeyContainingPipeAndNonUtf8() {
        // Field contains '|' (the payload separator) plus a non-UTF-8 byte; length-prefixed parsing must round-trip it intact.
        byte[] hashKey = ("bin-subkeyspace-" + System.nanoTime()).getBytes(StandardCharsets.UTF_8);
        byte[] field = new byte[] { 'a', '|', 'b', (byte) 0xFE, 'c' };
        byte[] channel = concat(prefix("__subkeyspace@0__:"), hashKey);

        CapturingPubSubListener<byte[], byte[]> listener = new CapturingPubSubListener<>();
        pubSubConnection.addListener(listener);
        pubsub.subscribe(channel);

        binaryRedis.hset(hashKey, field, "v1".getBytes(StandardCharsets.UTF_8));

        Notification<byte[], byte[]> n = listener.expectMessageOn(channel);
        byte[] expected = concat("hset|".getBytes(StandardCharsets.UTF_8),
                (field.length + ":").getBytes(StandardCharsets.UTF_8), field);
        assertThat(n.getChannel()).isEqualTo(channel);
        assertThat(n.getPattern()).isNull();
        assertThat(n.getMessage()).isEqualTo(expected);
    }

    @Test
    void subkeyevent_keyAndSubkeyContainingPipe() {
        // Both key and subkey contain the '|' separator; only the length-prefixed format lets a parser recover the parts.
        byte[] hashKey = new byte[] { 'h', '|', 'k', (byte) 0x80 };
        byte[] field = new byte[] { 'f', '|', 'd' };
        byte[] channel = "__subkeyevent@0__:hset".getBytes(StandardCharsets.UTF_8);

        CapturingPubSubListener<byte[], byte[]> listener = new CapturingPubSubListener<>();
        pubSubConnection.addListener(listener);
        pubsub.subscribe(channel);

        binaryRedis.hset(hashKey, field, "v1".getBytes(StandardCharsets.UTF_8));

        Notification<byte[], byte[]> n = listener.expectMessageOn(channel);
        byte[] expected = concat((hashKey.length + ":").getBytes(StandardCharsets.UTF_8), hashKey,
                "|".getBytes(StandardCharsets.UTF_8), (field.length + ":").getBytes(StandardCharsets.UTF_8), field);
        assertThat(n.getChannel()).isEqualTo(channel);
        assertThat(n.getPattern()).isNull();
        assertThat(n.getMessage()).isEqualTo(expected);
    }

    @Test
    void subkeyspaceitem_channelContainsBinaryBytes() {
        // Channel name __subkeyspaceitem@0__:<key>\n<subkey> with arbitrary bytes in both key and subkey.
        byte[] hashKey = new byte[] { 'i', (byte) 0xC3, (byte) 0xA9 };
        byte[] field = new byte[] { 'f', (byte) 0x80, 'd' };
        byte[] pattern = concat(prefix("__subkeyspaceitem@0__:"), hashKey, "\n*".getBytes(StandardCharsets.UTF_8));
        byte[] expectedChannel = concat(prefix("__subkeyspaceitem@0__:"), hashKey, "\n".getBytes(StandardCharsets.UTF_8),
                field);

        CapturingPubSubListener<byte[], byte[]> listener = new CapturingPubSubListener<>();
        pubSubConnection.addListener(listener);
        pubsub.psubscribe(pattern);

        binaryRedis.hset(hashKey, field, "v1".getBytes(StandardCharsets.UTF_8));

        Notification<byte[], byte[]> n = listener.expectMessageOn(expectedChannel);
        assertThat(n.getChannel()).isEqualTo(expectedChannel);
        assertThat(n.getPattern()).isEqualTo(pattern);
        assertThat(n.getMessage()).isEqualTo("hset".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void subkeyevent_keyAndSubkeyContainingNewline() {
        // LF is a channel delimiter for subkeyspaceitem but is plain data for subkeyevent; both must round-trip intact.
        byte[] hashKey = new byte[] { 'h', '\n', 'k' };
        byte[] field = new byte[] { 'f', '\n', 'd' };
        byte[] channel = "__subkeyevent@0__:hset".getBytes(StandardCharsets.UTF_8);

        CapturingPubSubListener<byte[], byte[]> listener = new CapturingPubSubListener<>();
        pubSubConnection.addListener(listener);
        pubsub.subscribe(channel);

        binaryRedis.hset(hashKey, field, "v1".getBytes(StandardCharsets.UTF_8));

        Notification<byte[], byte[]> n = listener.expectMessageOn(channel);
        byte[] expected = concat((hashKey.length + ":").getBytes(StandardCharsets.UTF_8), hashKey,
                "|".getBytes(StandardCharsets.UTF_8), (field.length + ":").getBytes(StandardCharsets.UTF_8), field);
        assertThat(n.getChannel()).isEqualTo(channel);
        assertThat(n.getPattern()).isNull();
        assertThat(n.getMessage()).isEqualTo(expected);
    }

    @Test
    void subkeyspaceevent_keyContainingPipeInChannel() {
        // Key is embedded in the channel name and may contain '|'; only ByteArrayCodec preserves the exact bytes for SUBSCRIBE.
        byte[] hashKey = new byte[] { 'k', '|', 'k', (byte) 0xFF };
        byte[] field = new byte[] { 'f', 'l', 'd' };
        byte[] channel = concat("__subkeyspaceevent@0__:hset|".getBytes(StandardCharsets.UTF_8), hashKey);

        CapturingPubSubListener<byte[], byte[]> listener = new CapturingPubSubListener<>();
        pubSubConnection.addListener(listener);
        pubsub.subscribe(channel);

        binaryRedis.hset(hashKey, field, "v1".getBytes(StandardCharsets.UTF_8));

        Notification<byte[], byte[]> n = listener.expectMessageOn(channel);
        byte[] expected = concat((field.length + ":").getBytes(StandardCharsets.UTF_8), field);
        assertThat(n.getChannel()).isEqualTo(channel);
        assertThat(n.getPattern()).isNull();
        assertThat(n.getMessage()).isEqualTo(expected);
    }

    private static byte[] prefix(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) {
            total += p.length;
        }
        byte[] out = new byte[total];
        int off = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, off, p.length);
            off += p.length;
        }
        return out;
    }

}
