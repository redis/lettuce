/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;

import io.lettuce.core.protocol.MaintenanceNotification;
import io.lettuce.core.protocol.MaintenanceNotification.SlotMigration;
import io.lettuce.core.protocol.MaintenanceNotification.SlotsMigrated;
import io.lettuce.core.protocol.MaintenanceNotificationListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Reacts to the maintenance notifications received on any connection of a {@link RedisClusterClient}.
 * <p>
 * A completed slot migration is announced on every connection. Sequence ids only grow, so only the first {@code SMIGRATED} with
 * a sequence id above the highest one seen is acted upon; copies and older events are ignored. For that one a topology update
 * with the migrated slots is requested. The notification is received on an I/O thread, the update runs elsewhere.
 *
 * @since 8.0
 */
class ClusterMaintenanceCoordinator implements MaintenanceNotificationListener {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClusterMaintenanceCoordinator.class);

    private final BiPredicate<Long, List<SlotMigration>> updateRequest;

    private final AtomicLong lastSeqId = new AtomicLong(Long.MIN_VALUE);

    /**
     * @param updateRequest requests a topology update for the migrations of a sequence id, returns {@code true} if the request
     *        was accepted
     */
    ClusterMaintenanceCoordinator(BiPredicate<Long, List<SlotMigration>> updateRequest) {
        this.updateRequest = updateRequest;
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

        List<SlotMigration> migrations = notification instanceof SlotsMigrated ? ((SlotsMigrated) notification).getMigrations()
                : Collections.emptyList();

        boolean accepted = updateRequest.test(seqId, migrations);
        logger.debug("Topology update for SMIGRATED {} {}", seqId, accepted ? "requested" : "not accepted");
    }

}
