/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.MaintenanceNotification.Moving;
import io.lettuce.core.protocol.MaintenanceNotification.ShardScoped;
import io.lettuce.core.protocol.MaintenanceNotification.Type;

/**
 * Unit tests for {@link MaintenanceNotifications} and {@link MaintenanceNotification}.
 */
@Tag(UNIT_TEST)
class MaintenanceNotificationsUnitTests {

    @Test
    void shouldDecodeMoving() {

        MaintenanceNotification notification = MaintenanceNotifications.from(push("MOVING", 7L, 15L, bytes("10.0.0.1:6380")));

        assertThat(notification).isInstanceOf(Moving.class);
        assertThat(notification.getType()).isEqualTo(Type.MOVING);
        assertThat(notification.getSeqId()).isEqualTo(7L);

        Moving moving = (Moving) notification;
        assertThat(moving.getTime()).isEqualTo(Duration.ofSeconds(15));
        assertThat(moving.getEndpoint()).isEqualTo(new InetSocketAddress("10.0.0.1", 6380));
    }

    @Test
    void shouldDecodeMovingWithoutEndpoint() {

        Moving nullElement = (Moving) MaintenanceNotifications.from(push("MOVING", 1L, 15L, null));
        Moving nullLiteral = (Moving) MaintenanceNotifications.from(push("MOVING", 1L, 15L, bytes("null")));

        assertThat(nullElement.getEndpoint()).isNull();
        assertThat(nullLiteral.getEndpoint()).isNull();
    }

    @Test
    void shouldDecodeMovingEndpointForms() {

        assertThat(movingEndpoint("redis.example.com:6380")).isEqualTo(new InetSocketAddress("redis.example.com", 6380));
        assertThat(movingEndpoint("[2001:db8::1]:6380")).isEqualTo(new InetSocketAddress("2001:db8::1", 6380));
        assertThat(movingEndpoint("2001:db8::1:6380")).isEqualTo(new InetSocketAddress("2001:db8::1", 6380));
    }

    @Test
    void shouldRejectMalformedMoving() {

        assertThat(MaintenanceNotifications.from(push("MOVING", 1L))).isNull();
        assertThat(MaintenanceNotifications.from(push("MOVING", 1L, 15L, bytes("a:1"), "extra"))).isNull();
        assertThat(MaintenanceNotifications.from(push("MOVING", "seq", 15L, bytes("a:1")))).isNull();
        assertThat(MaintenanceNotifications.from(push("MOVING", 1L, 15L, "not-a-bytebuffer"))).isNull();
        assertThat(MaintenanceNotifications.from(push("MOVING", 1L, 15L, bytes("no-port")))).isNull();
        assertThat(MaintenanceNotifications.from(push("MOVING", 1L, 15L, bytes("host:port")))).isNull();
        assertThat(MaintenanceNotifications.from(push("MOVING", 1L, 15L, bytes("host:70000")))).isNull();
    }

    @Test
    void shouldDecodeStartedShardNotifications() {

        assertShards(MaintenanceNotifications.from(push("MIGRATING", 2L, 5L, bytes("1,2"))), Type.MIGRATING, 2L, "1,2");
        assertShards(MaintenanceNotifications.from(push("FAILING_OVER", 3L, 5L, bytes("4"))), Type.FAILING_OVER, 3L, "4");
    }

    @Test
    void shouldDecodeCompletedShardNotifications() {

        assertShards(MaintenanceNotifications.from(push("MIGRATED", 2L, bytes("1,2"))), Type.MIGRATED, 2L, "1,2");
        assertShards(MaintenanceNotifications.from(push("FAILED_OVER", 3L, bytes("4"))), Type.FAILED_OVER, 3L, "4");
    }

    @Test
    void shouldDecodeEachTypeToItsOwnClass() {

        assertThat(MaintenanceNotifications.from(push("MIGRATING", 1L, 5L, bytes("1"))))
                .isInstanceOf(MaintenanceNotification.Migrating.class);
        assertThat(MaintenanceNotifications.from(push("MIGRATED", 1L, bytes("1"))))
                .isInstanceOf(MaintenanceNotification.Migrated.class);
        assertThat(MaintenanceNotifications.from(push("FAILING_OVER", 1L, 5L, bytes("1"))))
                .isInstanceOf(MaintenanceNotification.FailingOver.class);
        assertThat(MaintenanceNotifications.from(push("FAILED_OVER", 1L, bytes("1"))))
                .isInstanceOf(MaintenanceNotification.FailedOver.class);
    }

    @Test
    void shouldDecodeShardNotificationWithoutShards() {

        assertShards(MaintenanceNotifications.from(push("MIGRATING", 2L)), Type.MIGRATING, 2L, null);
        assertShards(MaintenanceNotifications.from(push("MIGRATED", 2L)), Type.MIGRATED, 2L, null);
        assertShards(MaintenanceNotifications.from(push("FAILING_OVER", 2L, 5L, "not-a-bytebuffer")), Type.FAILING_OVER, 2L,
                null);
        assertShards(MaintenanceNotifications.from(push("FAILED_OVER", 2L, 5L)), Type.FAILED_OVER, 2L, null);
    }

    @Test
    void shouldDecodeShardNotificationWithoutSeqId() {

        assertThat(MaintenanceNotifications.from(push("MIGRATED")).getSeqId()).isNull();
        assertThat(MaintenanceNotifications.from(push("MIGRATED", "seq", bytes("1"))).getSeqId()).isNull();
    }

    @Test
    void shouldSupportEveryType() {

        for (Type type : Type.values()) {
            MaintenanceNotification notification = MaintenanceNotifications
                    .from(push(type.name(), 1L, 5L, bytes("10.0.0.1:6380")));

            assertThat(notification).describedAs(type.name()).isNotNull();
            assertThat(notification.getType()).isEqualTo(type);
        }
    }

    @Test
    void shouldIgnoreOtherPushMessages() {

        assertThat(MaintenanceNotifications.from(push("message", bytes("channel"), bytes("payload")))).isNull();
        assertThat(MaintenanceNotifications.from(push("SMIGRATED", 1L))).isNull();
    }

    private static InetSocketAddress movingEndpoint(String addressAndPort) {
        return ((Moving) MaintenanceNotifications.from(push("MOVING", 1L, 15L, bytes(addressAndPort)))).getEndpoint();
    }

    private static void assertShards(MaintenanceNotification notification, Type type, Long seqId, String shards) {

        assertThat(notification).isInstanceOf(ShardScoped.class);
        assertThat(notification.getType()).isEqualTo(type);
        assertThat(notification.getSeqId()).isEqualTo(seqId);
        assertThat(((ShardScoped) notification).getShards()).isEqualTo(shards);
    }

    private static ByteBuffer bytes(String value) {
        return StringCodec.UTF8.encodeKey(value);
    }

    private static PushMessage push(String type, Object... arguments) {

        Object[] content = new Object[arguments.length + 1];
        content[0] = type;
        System.arraycopy(arguments, 0, content, 1, arguments.length);

        return new PushMessage() {

            @Override
            public String getType() {
                return type;
            }

            @Override
            public List<Object> getContent() {
                return Arrays.asList(content);
            }

            @Override
            public List<Object> getContent(Function<ByteBuffer, Object> decodeFunction) {
                return getContent();
            }

        };
    }

}
