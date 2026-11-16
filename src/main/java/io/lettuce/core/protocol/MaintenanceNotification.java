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
import java.util.List;

import io.lettuce.core.codec.StringCodec;
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

        MOVING, MIGRATING, MIGRATED, FAILING_OVER, FAILED_OVER

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

            String[] parts = addressAndPort.split(":");
            if (parts.length < 2) {
                return null;
            }

            try {
                return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
            } catch (IllegalArgumentException e) {
                return null;
            }
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

}
