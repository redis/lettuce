/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.HostAndPort;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Decodes Redis maintenance push notifications
 *
 * Smart client hand-off uses the following RESP3 push notifications for standalone clients:
 *
 * - MOVING: A connection moves from endpoint A to B within the given time - FAILING_OVER: A shard failover is happening -
 * FAILED_OVER: A shard failover completed - MIGRATING: A shard is migrating - MIGRATED: A shard got migrated
 *
 *
 * For OSS cluster clients, the following push notifications can be received:
 *
 * - SMIGRATING: Slots are migrating - SMIGRATED: Slots migrated and there is a new updated cluster topology
 *
 * @since 7.0
 */
public abstract class MaintenanceNotification {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MaintenanceNotification.class);

    // Message indexes for easier parsing
    private static final int MSG_SEQ_NUM_INDEX = 1;

    private static final int MSG_TIME_INDEX = 2;

    private static final int MOVING_TO_INDEX = 3;

    private static final int MIGRATING_SHARDS_INDEX = 3;

    private static final int MIGRATED_SHARDS_INDEX = 2;

    private static final int SMIGRATING_IMPACTED_SLOTS = 2;

    private static final int SMIGRATED_SLOTS_INDEX = 2;

    private final Type type;

    private final Long seqNum;

    private MaintenanceNotification(Type type, Long seqNum) {
        this.type = type;
        this.seqNum = seqNum;
    }

    /**
     * Decode a Redis maintenance push notification.
     *
     * @param message the push message.
     * @return the decoded notification, or {@code null} if the message is not a maintenance notification or cannot be decoded.
     */
    public static MaintenanceNotification from(PushMessage message) {

        Type type = Type.from(message.getType());
        if (type == null) {
            return null;
        }

        List<Object> content = message.getContent();

        try {
            switch (type) {
                case MOVING:
                    return moving(content);
                case SMIGRATING:
                    return sMigrating(content);
                case MIGRATING:
                    // Same handler as FAILING_OVER
                case FAILING_OVER:
                    return failingOverOrMigrating(type, content);
                case SMIGRATED:
                    return sMigrated(content);
                case MIGRATED:
                    // Same handler as FAILED_OVER
                case FAILED_OVER:
                    return failedOverOrMigrated(type, content);
                default:
                    return null;
            }
        } catch (RuntimeException e) {
            logger.error("Invalid maintenance message format", e);
            return null;
        }
    }

    private static MovingNotification moving(List<Object> content) {

        if (content.size() != 4) {
            logger.warn("Invalid re-bind message format, expected 4 elements, got {}", content.size());
            return null;
        }

        Long seqNum = asLong(content.get(MSG_SEQ_NUM_INDEX));
        Duration time = Duration.ofSeconds(asLong(content.get(MSG_TIME_INDEX)));

        return new MovingNotification(seqNum, time, getEndpoint(content.get(MOVING_TO_INDEX)));
    }

    private static MaintenanceNotification failingOverOrMigrating(Type type, List<Object> content) {

        Long seqNum = content.size() > MSG_SEQ_NUM_INDEX ? safeLong(content.get(MSG_SEQ_NUM_INDEX)) : null;
        Long timeInSeconds = content.size() > MSG_TIME_INDEX ? safeLong(content.get(MSG_TIME_INDEX)) : null;
        Duration time = timeInSeconds != null ? Duration.ofSeconds(timeInSeconds) : null;
        String shards = getShards(content, MIGRATING_SHARDS_INDEX, type);

        switch (type) {
            case MIGRATING:
                return new MigrationStartedNotification(seqNum, time, shards);
            case FAILING_OVER:
                return new FailoverStartedNotification(seqNum, time, shards);
            default:
                throw new IllegalArgumentException("Unsupported started maintenance event type: " + type);
        }
    }

    private static MaintenanceNotification failedOverOrMigrated(Type type, List<Object> content) {

        Long seqNum = content.size() > MSG_SEQ_NUM_INDEX ? safeLong(content.get(MSG_SEQ_NUM_INDEX)) : null;
        String shards = getShards(content, MIGRATED_SHARDS_INDEX, type);

        switch (type) {
            case MIGRATED:
                return new MigrationCompletedNotification(seqNum, shards);
            case FAILED_OVER:
                return new FailoverCompletedNotification(seqNum, shards);
            default:
                throw new IllegalArgumentException("Unsupported completed maintenance event type: " + type);
        }
    }

    private static SlotsMigrationStartedNotification sMigrating(List<Object> content) {

        Long seqNum = content.size() > MSG_SEQ_NUM_INDEX ? safeLong(content.get(MSG_SEQ_NUM_INDEX)) : null;

        return new SlotsMigrationStartedNotification(seqNum, joinValues(content, SMIGRATING_IMPACTED_SLOTS));
    }

    private static SlotsMigrationCompletedNotification sMigrated(List<Object> content) {

        Long seqNum = content.size() > MSG_SEQ_NUM_INDEX ? safeLong(content.get(MSG_SEQ_NUM_INDEX)) : null;

        if (content.size() <= SMIGRATED_SLOTS_INDEX) {
            logger.warn("Invalid maintenance message format, expected at least {} elements, got {} for {} maintenance event",
                    SMIGRATED_SLOTS_INDEX + 1, content.size(), Type.SMIGRATED.pushMessageType);
            return new SlotsMigrationCompletedNotification(seqNum, null, Collections.emptyList());
        }

        List<SlotMigration> migrations = parseSlotMigrations(decode(content.get(SMIGRATED_SLOTS_INDEX)));

        return new SlotsMigrationCompletedNotification(seqNum, slotRanges(migrations), migrations);
    }

    private static String getShards(List<Object> content, int shardsIndex, Type maintenanceEvent) {

        if (content.size() <= shardsIndex) {
            logger.warn("Invalid maintenance message format, expected at least {} elements, got {} for {} maintenance event",
                    shardsIndex + 1, content.size(), maintenanceEvent.pushMessageType);
            return null;
        }

        return decodeStringValue(content.get(shardsIndex), maintenanceEvent);
    }

    private static String joinValues(List<Object> content, int fromIndex) {

        if (content.size() <= fromIndex) {
            return null;
        }

        StringJoiner joiner = new StringJoiner(",");

        for (int i = fromIndex; i < content.size(); i++) {
            String value = decodeStringValue(content.get(i), Type.SMIGRATING);
            if (value != null) {
                joiner.add(value);
            }
        }

        return joiner.toString();
    }

    private static String slotRanges(List<SlotMigration> migrations) {

        StringJoiner slots = new StringJoiner(",");
        migrations.forEach(migration -> slots.add(migration.getSlots()));

        return slots.toString();
    }

    private static String decodeStringValue(Object value, Type maintenanceEvent) {

        if (value == null) {
            return null;
        }

        if (value instanceof ByteBuffer) {
            return decodeString((ByteBuffer) value);
        }

        if (value instanceof String) {
            return (String) value;
        }

        logger.warn("Invalid string value format, expected ByteBuffer, got {} for {} maintenance event", value.getClass(),
                maintenanceEvent.pushMessageType);
        return null;
    }

    private static List<SlotMigration> parseSlotMigrations(Object decodedValue) {

        if (!(decodedValue instanceof List)) {
            logger.warn("Invalid SMIGRATED topology format, expected List, got {}",
                    decodedValue != null ? decodedValue.getClass() : "null");
            return Collections.emptyList();
        }

        List<SlotMigration> migrations = new ArrayList<>();

        for (Object entry : (List<?>) decodedValue) {

            if (!(entry instanceof List) || ((List<?>) entry).size() != 3) {
                logger.warn("Invalid SMIGRATED migration entry format: {}", entry);
                continue;
            }

            List<?> migration = (List<?>) entry;
            String source = String.valueOf(migration.get(0));
            String destination = String.valueOf(migration.get(1));
            String slots = String.valueOf(migration.get(2));

            migrations.add(new SlotMigration(source, destination, slots));
        }

        return migrations;
    }

    public static InetSocketAddress getEndpoint(Object endpointObject) {

        if (endpointObject == null) {
            return null;
        }

        String addressAndPort;
        if (endpointObject instanceof ByteBuffer) {
            addressAndPort = decodeString((ByteBuffer) endpointObject);
        } else if (endpointObject instanceof String) {
            addressAndPort = (String) endpointObject;
        } else {
            logger.warn("Invalid re-bind address format, expected ByteBuffer, got {}", endpointObject.getClass());
            return null;
        }

        if (addressAndPort == null || "null".equals(addressAndPort) || "none".equals(addressAndPort)) {
            return null;
        }

        HostAndPort hostAndPort = HostAndPort.parseCompat(addressAndPort);
        return new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPort());
    }

    private static Long asLong(Object value) {

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return Long.parseLong(value.toString());
    }

    private static Long safeLong(Object value) {

        try {
            return asLong(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Object decode(Object value) {

        if (value instanceof ByteBuffer) {
            return decodeString((ByteBuffer) value);
        }

        if (value instanceof List) {

            List<Object> result = new ArrayList<>(((List<?>) value).size());

            for (Object entry : (List<?>) value) {
                result.add(decode(entry));
            }

            return result;
        }

        if (value instanceof Set) {

            Set<Object> result = new LinkedHashSet<>(((Set<?>) value).size());

            for (Object entry : (Set<?>) value) {
                result.add(decode(entry));
            }

            return result;
        }

        if (value instanceof Map) {

            Map<Object, Object> result = new LinkedHashMap<>(((Map<?, ?>) value).size());

            ((Map<?, ?>) value).forEach((key, mappedValue) -> result.put(decode(key), decode(mappedValue)));

            return result;
        }

        return value;
    }

    private static String decodeString(ByteBuffer byteBuffer) {
        return StringCodec.UTF8.decodeKey(byteBuffer.asReadOnlyBuffer());
    }

    public static boolean matches(String endpoint, InetSocketAddress socketAddress) {

        if (endpoint == null || socketAddress == null) {
            return false;
        }

        HostAndPort hostAndPort = HostAndPort.parseCompat(endpoint);

        if (hostAndPort.getPort() != socketAddress.getPort()) {
            return false;
        }

        String expectedHost = hostAndPort.getHostText();

        if (expectedHost.equalsIgnoreCase(socketAddress.getHostString())) {
            return true;
        }

        return socketAddress.getAddress() != null && expectedHost.equals(socketAddress.getAddress().getHostAddress());
    }

    public Type getType() {
        return type;
    }

    public Long getSeqNum() {
        return seqNum;
    }

    public enum Type {

        MOVING("MOVING"),

        MIGRATING("MIGRATING"),

        MIGRATED("MIGRATED"),

        SMIGRATING("SMIGRATING"),

        SMIGRATED("SMIGRATED"),

        FAILING_OVER("FAILING_OVER"),

        FAILED_OVER("FAILED_OVER");

        private final String pushMessageType;

        Type(String pushMessageType) {
            this.pushMessageType = pushMessageType;
        }

        private static Type from(String pushMessageType) {

            for (Type type : values()) {
                if (type.pushMessageType.equals(pushMessageType)) {
                    return type;
                }
            }

            return null;
        }

    }

    public static final class MovingNotification extends MaintenanceNotification {

        private final Duration time;

        private final InetSocketAddress endpoint;

        private MovingNotification(Long seqNum, Duration time, InetSocketAddress endpoint) {
            super(Type.MOVING, seqNum);
            this.time = time;
            this.endpoint = endpoint;
        }

        public Duration getTime() {
            return time;
        }

        public InetSocketAddress getEndpoint() {
            return endpoint;
        }

    }

    public static abstract class ShardNotification extends MaintenanceNotification {

        private final String shards;

        private ShardNotification(Type type, Long seqNum, String shards) {
            super(type, seqNum);
            this.shards = shards;
        }

        public String getShards() {
            return shards;
        }

    }

    public static abstract class TimedShardNotification extends ShardNotification {

        private final Duration time;

        private TimedShardNotification(Type type, Long seqNum, Duration time, String shards) {
            super(type, seqNum, shards);
            this.time = time;
        }

        public Duration getTime() {
            return time;
        }

    }

    public static final class MigrationStartedNotification extends TimedShardNotification {

        private MigrationStartedNotification(Long seqNum, Duration time, String shards) {
            super(Type.MIGRATING, seqNum, time, shards);
        }

    }

    public static final class MigrationCompletedNotification extends ShardNotification {

        private MigrationCompletedNotification(Long seqNum, String shards) {
            super(Type.MIGRATED, seqNum, shards);
        }

    }

    public static final class SlotsMigrationStartedNotification extends ShardNotification {

        private SlotsMigrationStartedNotification(Long seqNum, String shards) {
            super(Type.SMIGRATING, seqNum, shards);
        }

    }

    public static final class SlotsMigrationCompletedNotification extends ShardNotification {

        private final List<SlotMigration> slotMigrations;

        private SlotsMigrationCompletedNotification(Long seqNum, String shards, List<SlotMigration> slotMigrations) {
            super(Type.SMIGRATED, seqNum, shards);
            this.slotMigrations = slotMigrations;
        }

        public List<SlotMigration> getSlotMigrations() {
            return slotMigrations;
        }

        public boolean hasSource(SocketAddress socketAddress) {

            if (!(socketAddress instanceof InetSocketAddress)) {
                return false;
            }

            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;

            for (SlotMigration slotMigration : slotMigrations) {
                if (matches(slotMigration.getSource(), inetSocketAddress)) {
                    return true;
                }
            }

            return false;
        }

    }

    public static final class FailoverStartedNotification extends TimedShardNotification {

        private FailoverStartedNotification(Long seqNum, Duration time, String shards) {
            super(Type.FAILING_OVER, seqNum, time, shards);
        }

    }

    public static final class FailoverCompletedNotification extends ShardNotification {

        private FailoverCompletedNotification(Long seqNum, String shards) {
            super(Type.FAILED_OVER, seqNum, shards);
        }

    }

    public static final class SlotMigration {

        private final String source;

        private final String destination;

        private final String slots;

        private SlotMigration(String source, String destination, String slots) {
            this.source = source;
            this.destination = destination;
            this.slots = slots;
        }

        public String getSource() {
            return source;
        }

        public String getDestination() {
            return destination;
        }

        public String getSlots() {
            return slots;
        }

    }

}
