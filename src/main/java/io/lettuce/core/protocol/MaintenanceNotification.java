/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.HostAndPort;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A decoded maintenance push notification. Each type holds its own wire format and validation.
 *
 * @see MaintenanceNotifications
 */
abstract class MaintenanceNotification {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MaintenanceNotification.class);

    private static final int SEQ_ID_INDEX = 1;

    private final Type type;

    private final Long seqId;

    private MaintenanceNotification(Type type, Long seqId) {
        this.type = type;
        this.seqId = seqId;
    }

    /**
     * @return the element at {@code index} if it is a {@link Long}, {@code null} otherwise
     */
    private static Long longAt(List<Object> content, int index) {

        if (content.size() > index && content.get(index) instanceof Long) {
            return (Long) content.get(index);
        }

        return null;
    }

    /**
     * @return the element at {@code index} decoded as string if it is a {@link ByteBuffer}, {@code null} otherwise
     */
    private static String stringAt(List<Object> content, int index) {

        if (content.size() > index && content.get(index) instanceof ByteBuffer) {
            return StringCodec.UTF8.decodeKey((ByteBuffer) content.get(index));
        }

        return null;
    }

    /**
     * @return the parsed {@code host:port}, or {@code null} if it is malformed or has no port
     */
    private static HostAndPort hostAndPort(String hostAndPort) {

        try {
            HostAndPort parsed = HostAndPort.parseCompat(hostAndPort);
            return parsed.hasPort() ? parsed : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return the ranges of a comma-separated list of slots and {@code from-to} ranges, or {@code null} if it is malformed
     */
    private static List<SlotRange> slotRanges(String slots) {

        List<SlotRange> ranges = new ArrayList<>();

        for (String token : slots.split(",")) {

            int dash = token.indexOf('-');

            try {
                int from = Integer.parseInt(dash < 0 ? token : token.substring(0, dash));
                int to = dash < 0 ? from : Integer.parseInt(token.substring(dash + 1));

                if (from < 0 || to >= SlotHash.SLOT_COUNT || from > to) {
                    return null;
                }

                ranges.add(new SlotRange(from, to));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return ranges;
    }

    private static boolean isNullOr(Object value, Class<?> type) {
        return value == null || type.isInstance(value);
    }

    private static String shards(List<Object> content, int shardsIndex, Type type) {

        String shards = stringAt(content, shardsIndex);

        if (shards == null) {
            logger.warn("Invalid {} message format, expected shards at index {}: {}", type, shardsIndex, content);
        }

        return shards;
    }

    Type getType() {
        return type;
    }

    /**
     * @return the sequence id, or {@code null} if absent
     */
    Long getSeqId() {
        return seqId;
    }

    enum Type {

        MOVING, MIGRATING, MIGRATED, FAILING_OVER, FAILED_OVER, SMIGRATING, SMIGRATED

    }

    /**
     * A notification that affects a set of shards.
     */
    interface ShardScoped {

        /**
         * @return the affected shards, or {@code null} if absent or malformed
         */
        String getShards();

    }

    /**
     * {@code MOVING <seq> <time> <endpoint>}: the endpoint is re-bound within the given time.
     */
    static final class Moving extends MaintenanceNotification {

        private static final int TIME_INDEX = 2;

        private static final int ADDRESS_INDEX = 3;

        private static final int SIZE = 4;

        private final Duration time;

        private final InetSocketAddress endpoint;

        private Moving(Long seqId, Duration time, InetSocketAddress endpoint) {
            super(Type.MOVING, seqId);
            this.time = time;
            this.endpoint = endpoint;
        }

        /**
         * @return the notification, or {@code null} if {@code content} is malformed
         */
        static Moving from(List<Object> content) {

            if (content.size() != SIZE) {
                logger.warn("Invalid re-bind message format, expected 4 elements, got {}", content.size());
                return null;
            }

            Long time = longAt(content, TIME_INDEX);

            if (time == null || !isNullOr(content.get(SEQ_ID_INDEX), Long.class)
                    || !isNullOr(content.get(ADDRESS_INDEX), ByteBuffer.class)) {
                logger.warn("Invalid re-bind message format: {}", content);
                return null;
            }

            String addressAndPort = stringAt(content, ADDRESS_INDEX);
            InetSocketAddress endpoint = null;

            // "null" announces a re-bind without a new endpoint
            if (addressAndPort != null && !"null".equals(addressAndPort)) {
                endpoint = endpoint(addressAndPort);
                if (endpoint == null) {
                    logger.warn("Invalid re-bind endpoint '{}'", addressAndPort);
                    return null;
                }
            }

            return new Moving(longAt(content, SEQ_ID_INDEX), Duration.ofSeconds(time), endpoint);
        }

        private static InetSocketAddress endpoint(String addressAndPort) {

            HostAndPort hostAndPort = hostAndPort(addressAndPort);

            return hostAndPort != null ? new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPort()) : null;
        }

        Duration getTime() {
            return time;
        }

        /**
         * @return the new endpoint, or {@code null} if none was announced
         */
        InetSocketAddress getEndpoint() {
            return endpoint;
        }

    }

    /**
     * {@code MIGRATING <seq> <time> <shards>}: a shard migration is about to start.
     */
    static final class Migrating extends MaintenanceNotification implements ShardScoped {

        private static final int SHARDS_INDEX = 3;

        private final String shards;

        private Migrating(Long seqId, String shards) {
            super(Type.MIGRATING, seqId);
            this.shards = shards;
        }

        static Migrating from(List<Object> content) {
            return new Migrating(longAt(content, SEQ_ID_INDEX), shards(content, SHARDS_INDEX, Type.MIGRATING));
        }

        @Override
        public String getShards() {
            return shards;
        }

    }

    /**
     * {@code MIGRATED <seq> <shards>}: a shard migration completed.
     */
    static final class Migrated extends MaintenanceNotification implements ShardScoped {

        private static final int SHARDS_INDEX = 2;

        private final String shards;

        private Migrated(Long seqId, String shards) {
            super(Type.MIGRATED, seqId);
            this.shards = shards;
        }

        static Migrated from(List<Object> content) {
            return new Migrated(longAt(content, SEQ_ID_INDEX), shards(content, SHARDS_INDEX, Type.MIGRATED));
        }

        @Override
        public String getShards() {
            return shards;
        }

    }

    /**
     * {@code FAILING_OVER <seq> <time> <shards>}: a shard failover is about to start.
     */
    static final class FailingOver extends MaintenanceNotification implements ShardScoped {

        private static final int SHARDS_INDEX = 3;

        private final String shards;

        private FailingOver(Long seqId, String shards) {
            super(Type.FAILING_OVER, seqId);
            this.shards = shards;
        }

        static FailingOver from(List<Object> content) {
            return new FailingOver(longAt(content, SEQ_ID_INDEX), shards(content, SHARDS_INDEX, Type.FAILING_OVER));
        }

        @Override
        public String getShards() {
            return shards;
        }

    }

    /**
     * {@code FAILED_OVER <seq> <shards>}: a shard failover completed.
     */
    static final class FailedOver extends MaintenanceNotification implements ShardScoped {

        private static final int SHARDS_INDEX = 2;

        private final String shards;

        private FailedOver(Long seqId, String shards) {
            super(Type.FAILED_OVER, seqId);
            this.shards = shards;
        }

        static FailedOver from(List<Object> content) {
            return new FailedOver(longAt(content, SEQ_ID_INDEX), shards(content, SHARDS_INDEX, Type.FAILED_OVER));
        }

        @Override
        public String getShards() {
            return shards;
        }

    }

    /**
     * An inclusive range of hash slots.
     */
    static final class SlotRange {

        private final int from;

        private final int to;

        private SlotRange(int from, int to) {
            this.from = from;
            this.to = to;
        }

        int getFrom() {
            return from;
        }

        int getTo() {
            return to;
        }

        @Override
        public String toString() {
            return from == to ? Integer.toString(from) : from + "-" + to;
        }

    }

    /**
     * Slots that moved from one node to another.
     */
    static final class SlotMigration {

        private final HostAndPort source;

        private final HostAndPort destination;

        private final List<SlotRange> slots;

        private SlotMigration(HostAndPort source, HostAndPort destination, List<SlotRange> slots) {
            this.source = source;
            this.destination = destination;
            this.slots = slots;
        }

        HostAndPort getSource() {
            return source;
        }

        HostAndPort getDestination() {
            return destination;
        }

        List<SlotRange> getSlots() {
            return slots;
        }

        @Override
        public String toString() {
            return source + " -> " + destination + ": " + slots;
        }

    }

    /**
     * {@code SMIGRATING <seq> <slots>}: a slot migration is about to start.
     */
    static final class SlotsMigrating extends MaintenanceNotification {

        private static final int SLOTS_INDEX = 2;

        private final List<SlotRange> slots;

        private SlotsMigrating(Long seqId, List<SlotRange> slots) {
            super(Type.SMIGRATING, seqId);
            this.slots = slots;
        }

        /**
         * @return the notification; malformed slots are left out so that the notification is still delivered
         */
        static SlotsMigrating from(List<Object> content) {

            List<SlotRange> slots = new ArrayList<>();

            for (int i = SLOTS_INDEX; i < content.size(); i++) {

                String value = stringAt(content, i);
                List<SlotRange> ranges = value != null ? slotRanges(value) : null;

                if (ranges == null) {
                    logger.warn("Invalid SMIGRATING slots at index {}: {}", i, content);
                    continue;
                }

                slots.addAll(ranges);
            }

            return new SlotsMigrating(longAt(content, SEQ_ID_INDEX), Collections.unmodifiableList(slots));
        }

        List<SlotRange> getSlots() {
            return slots;
        }

    }

    /**
     * {@code SMIGRATED <seq> [[<source>, <destination>, <slots>], ...]}: a slot migration completed.
     */
    static final class SlotsMigrated extends MaintenanceNotification {

        private static final int MIGRATIONS_INDEX = 2;

        private static final int MIGRATION_SIZE = 3;

        private final List<SlotMigration> migrations;

        private SlotsMigrated(Long seqId, List<SlotMigration> migrations) {
            super(Type.SMIGRATED, seqId);
            this.migrations = migrations;
        }

        /**
         * @return the notification; malformed migrations are left out so that the notification is still delivered
         */
        static SlotsMigrated from(List<Object> content) {

            List<SlotMigration> migrations = new ArrayList<>();

            if (content.size() > MIGRATIONS_INDEX && content.get(MIGRATIONS_INDEX) instanceof List) {

                for (Object entry : (List<?>) content.get(MIGRATIONS_INDEX)) {

                    SlotMigration migration = migration(entry);

                    if (migration == null) {
                        logger.warn("Invalid SMIGRATED migration: {}", entry);
                        continue;
                    }

                    migrations.add(migration);
                }
            } else {
                logger.warn("Invalid SMIGRATED message format, expected migrations at index {}: {}", MIGRATIONS_INDEX, content);
            }

            return new SlotsMigrated(longAt(content, SEQ_ID_INDEX), Collections.unmodifiableList(migrations));
        }

        @SuppressWarnings("unchecked")
        private static SlotMigration migration(Object entry) {

            if (!(entry instanceof List) || ((List<?>) entry).size() != MIGRATION_SIZE) {
                return null;
            }

            List<Object> triplet = (List<Object>) entry;
            String source = stringAt(triplet, 0);
            String destination = stringAt(triplet, 1);
            String slots = stringAt(triplet, 2);

            if (source == null || destination == null || slots == null) {
                return null;
            }

            HostAndPort sourceAddress = hostAndPort(source);
            HostAndPort destinationAddress = hostAndPort(destination);
            List<SlotRange> ranges = slotRanges(slots);

            if (sourceAddress == null || destinationAddress == null || ranges == null) {
                return null;
            }

            return new SlotMigration(sourceAddress, destinationAddress, Collections.unmodifiableList(ranges));
        }

        List<SlotMigration> getMigrations() {
            return migrations;
        }

    }

}
