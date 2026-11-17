/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.protocol.MaintenanceNotification;
import io.lettuce.core.protocol.MaintenanceNotification.Type;

/**
 * Unit tests for {@link ClusterMaintenanceCoordinator}.
 */
@Tag(UNIT_TEST)
class ClusterMaintenanceCoordinatorUnitTests {

    private final AtomicInteger refreshRequests = new AtomicInteger();

    private final AtomicBoolean accept = new AtomicBoolean(true);

    private final ClusterMaintenanceCoordinator sut = new ClusterMaintenanceCoordinator(() -> {
        refreshRequests.incrementAndGet();
        return accept.get();
    });

    @Test
    void shouldRequestRefreshOncePerSeqId() {

        for (int i = 0; i < 5; i++) {
            sut.onMaintenanceNotification(notification(Type.SMIGRATED, 1L));
        }

        assertThat(refreshRequests).hasValue(1);
    }

    @Test
    void shouldRequestRefreshForEachNewerSeqId() {

        sut.onMaintenanceNotification(notification(Type.SMIGRATED, 1L));
        sut.onMaintenanceNotification(notification(Type.SMIGRATED, 2L));
        sut.onMaintenanceNotification(notification(Type.SMIGRATED, 2L));

        assertThat(refreshRequests).hasValue(2);
    }

    @Test
    void shouldIgnoreOlderSeqId() {

        sut.onMaintenanceNotification(notification(Type.SMIGRATED, 6L));
        sut.onMaintenanceNotification(notification(Type.SMIGRATED, 5L));

        assertThat(refreshRequests).hasValue(1);
    }

    @Test
    void shouldIgnoreOtherNotificationTypes() {

        for (Type type : Type.values()) {
            if (type != Type.SMIGRATED) {
                sut.onMaintenanceNotification(notification(type, 1L));
            }
        }

        assertThat(refreshRequests).hasValue(0);
    }

    @Test
    void shouldIgnoreNotificationWithoutSeqId() {

        sut.onMaintenanceNotification(notification(Type.SMIGRATED, null));

        assertThat(refreshRequests).hasValue(0);
    }

    @Test
    void shouldNotRetryWhenRefreshWasNotAccepted() {

        accept.set(false);
        sut.onMaintenanceNotification(notification(Type.SMIGRATED, 1L));

        accept.set(true);
        sut.onMaintenanceNotification(notification(Type.SMIGRATED, 1L));

        assertThat(refreshRequests).hasValue(1);
    }

    @Test
    void shouldRequestRefreshOnceForConcurrentCopies() throws InterruptedException {

        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        MaintenanceNotification notification = notification(Type.SMIGRATED, 1L);

        try {
            for (int i = 0; i < threads; i++) {
                executor.execute(() -> {
                    try {
                        start.await();
                        sut.onMaintenanceNotification(notification);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(refreshRequests).hasValue(1);
    }

    private static MaintenanceNotification notification(Type type, Long seqId) {

        MaintenanceNotification notification = mock(MaintenanceNotification.class);
        when(notification.getType()).thenReturn(type);
        when(notification.getSeqId()).thenReturn(seqId);
        return notification;
    }

}
