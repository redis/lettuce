/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import io.lettuce.core.protocol.MaintenanceNotification;
import io.lettuce.core.protocol.MaintenanceNotificationListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Reacts to the maintenance notifications received on any connection of a {@link RedisClusterClient}.
 * <p>
 * A completed slot migration is announced on every connection. Sequence ids only grow, so the first {@code SMIGRATED} with a
 * sequence id above the highest one seen requests a topology refresh; copies and older events are ignored, the refresh covers
 * them.
 *
 * @since 8.0
 */
class ClusterMaintenanceCoordinator implements MaintenanceNotificationListener {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClusterMaintenanceCoordinator.class);

    private final BooleanSupplier refreshRequest;

    private final AtomicLong lastSeqId = new AtomicLong(Long.MIN_VALUE);

    /**
     * @param refreshRequest requests a topology refresh, returns {@code true} if the request was accepted
     */
    ClusterMaintenanceCoordinator(BooleanSupplier refreshRequest) {
        this.refreshRequest = refreshRequest;
    }

    @Override
    public void onMaintenanceNotification(MaintenanceNotification notification) {

        if (notification.getType() != MaintenanceNotification.Type.SMIGRATED) {
            return;
        }

        Long seqId = notification.getSeqId();

        if (seqId == null) {
            // malformed, reported by the parser
            return;
        }

        long previous = lastSeqId.getAndAccumulate(seqId, Math::max);

        if (seqId <= previous) {
            return;
        }

        boolean accepted = refreshRequest.getAsBoolean();
        logger.debug("Topology refresh for SMIGRATED {} {}", seqId, accepted ? "requested" : "not accepted");
    }

}
