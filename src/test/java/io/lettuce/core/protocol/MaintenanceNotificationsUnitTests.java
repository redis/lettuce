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
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.protocol.MaintenanceNotification.Moving;
import io.lettuce.core.protocol.MaintenanceNotification.ShardScoped;
import io.lettuce.core.protocol.MaintenanceNotification.SlotMigration;
import io.lettuce.core.protocol.MaintenanceNotification.SlotRange;
import io.lettuce.core.protocol.MaintenanceNotification.SlotsMigrated;
import io.lettuce.core.protocol.MaintenanceNotification.SlotsMigrating;
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
    void shouldDecodeSlotsMigrating() {

        SlotsMigrating notification = (SlotsMigrating) MaintenanceNotifications
                .from(push("SMIGRATING", 93923L, bytes("123,456,789-1000")));

        assertThat(notification.getType()).isEqualTo(Type.SMIGRATING);
        assertThat(notification.getSeqId()).isEqualTo(93923L);
        assertThat(notification.getSlots()).extracting(SlotRange::toString).containsExactly("123", "456", "789-1000");
        assertThat(notification.getSlots().get(2).getFrom()).isEqualTo(789);
        assertThat(notification.getSlots().get(2).getTo()).isEqualTo(1000);
    }

    @Test
    void shouldDecodeSlotsMigratingWithMalformedSlots() {

        assertThat(slotsMigrating()).isEmpty();
        assertThat(slotsMigrating("not-a-bytebuffer")).isEmpty();
        assertThat(slotsMigrating(bytes("abc"))).isEmpty();
        assertThat(slotsMigrating(bytes("5-1"))).isEmpty();
        assertThat(slotsMigrating(bytes("16384"))).isEmpty();
        assertThat(slotsMigrating(bytes("1,,2"))).isEmpty();
        assertThat(slotsMigrating(bytes("0-16383"))).hasSize(1);
    }

    @Test
    void shouldDecodeSlotsMigrated() {

        SlotsMigrated notification = (SlotsMigrated) MaintenanceNotifications
                .from(push("SMIGRATED", 93923L, Arrays.asList(triplet("abc.com:6789", "abc.com:6790", "123,789-1000"),
                        triplet("abc.com:4545", "abc.com:4555", "1000-2000"))));

        assertThat(notification.getType()).isEqualTo(Type.SMIGRATED);
        assertThat(notification.getSeqId()).isEqualTo(93923L);
        assertThat(notification.getMigrations()).hasSize(2);

        SlotMigration first = notification.getMigrations().get(0);
        assertThat(first.getSource()).isEqualTo(HostAndPort.of("abc.com", 6789));
        assertThat(first.getDestination()).isEqualTo(HostAndPort.of("abc.com", 6790));
        assertThat(first.getSlots()).extracting(SlotRange::toString).containsExactly("123", "789-1000");

        SlotMigration second = notification.getMigrations().get(1);
        assertThat(second.getSource()).isEqualTo(HostAndPort.of("abc.com", 4545));
        assertThat(second.getDestination()).isEqualTo(HostAndPort.of("abc.com", 4555));
        assertThat(second.getSlots()).extracting(SlotRange::toString).containsExactly("1000-2000");
    }

    @Test
    void shouldSkipMalformedSlotMigrations() {

        SlotsMigrated notification = (SlotsMigrated) MaintenanceNotifications.from(push("SMIGRATED", 1L,
                Arrays.asList(triplet("a:1", "b:2", "1-2"), triplet("no-port", "b:2", "3"), triplet("a:1", "b:2", "x"),
                        Arrays.asList(bytes("a:1"), bytes("b:2")), "not-a-list", triplet("c:3", "d:4", "5"))));

        assertThat(notification.getMigrations()).extracting(SlotMigration::toString).containsExactly("a:1 -> b:2: [1-2]",
                "c:3 -> d:4: [5]");
    }

    @Test
    void shouldDecodeSlotsMigratedWithoutMigrations() {

        SlotsMigrated missing = (SlotsMigrated) MaintenanceNotifications.from(push("SMIGRATED", 1L));
        SlotsMigrated notAList = (SlotsMigrated) MaintenanceNotifications.from(push("SMIGRATED", 1L, bytes("a:1")));
        SlotsMigrated noSeqId = (SlotsMigrated) MaintenanceNotifications.from(push("SMIGRATED"));

        assertThat(missing.getMigrations()).isEmpty();
        assertThat(notAList.getMigrations()).isEmpty();
        assertThat(noSeqId.getSeqId()).isNull();
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
        assertThat(MaintenanceNotifications.from(push("UNKNOWN", 1L))).isNull();
    }

    private static InetSocketAddress movingEndpoint(String addressAndPort) {
        return ((Moving) MaintenanceNotifications.from(push("MOVING", 1L, 15L, bytes(addressAndPort)))).getEndpoint();
    }

    private static List<SlotRange> slotsMigrating(Object... slots) {

        Object[] arguments = new Object[slots.length + 1];
        arguments[0] = 1L;
        System.arraycopy(slots, 0, arguments, 1, slots.length);

        return ((SlotsMigrating) MaintenanceNotifications.from(push("SMIGRATING", arguments))).getSlots();
    }

    private static List<Object> triplet(String source, String destination, String slots) {
        return Arrays.asList(bytes(source), bytes(destination), bytes(slots));
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
