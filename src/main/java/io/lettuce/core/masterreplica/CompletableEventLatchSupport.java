/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.masterreplica;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Completable latch support expecting an number of inbound events to trigger an outbound event signalled though a
 * {@link CompletionStage}. This latch is created by specifying a number of expected events of type {@code T} or exceptions and
 * synchronized through either a timeout or receiving the matching number of events.
 * <p>
 * Inbound events can be consumed through callback hook methods. Events arriving after synchronization are dropped.
 *
 * @author Mark Paluch
 */
abstract class CompletableEventLatchSupport<T, V> {

    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<CompletableEventLatchSupport> NOTIFICATIONS_UPDATER = AtomicIntegerFieldUpdater
            .newUpdater(CompletableEventLatchSupport.class, "notifications");

    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<CompletableEventLatchSupport> GATE_UPDATER = AtomicIntegerFieldUpdater
            .newUpdater(CompletableEventLatchSupport.class, "gate");

    private static final int GATE_OPEN = 0;

    private static final int GATE_CLOSED = 1;

    private final int expectedCount;

    private final CompletableFuture<V> selfFuture = new CompletableFuture<>();

    private volatile ScheduledFuture<?> timeoutScheduleFuture;

    // accessed via UPDATER
    @SuppressWarnings("unused")
    private volatile int notifications = 0;

    @SuppressWarnings("unused")
    private volatile int gate = GATE_OPEN;

    /**
     * Construct a new {@link CompletableEventLatchSupport} class expecting {@code expectedCount} notifications.
     *
     * @param expectedCount
     */
    public CompletableEventLatchSupport(int expectedCount) {
        this.expectedCount = expectedCount;
    }

    public final int getExpectedCount() {
        return expectedCount;
    }

    /**
     * Notification callback method accepting a connection for a value. Triggers emission if the gate is open and the current
     * call to this method is the last expected notification.
     */
    public final void accept(T value) {

        if (GATE_UPDATER.get(this) == GATE_CLOSED) {
            onDrop(value);
            return;
        }

        onAccept(value);
        onNotification();
    }

    /**
     * Notification callback method accepting a connection error. Triggers emission if the gate is open and the current call to
     * this method is the last expected notification.
     */
    public final void accept(Throwable throwable) {

        if (GATE_UPDATER.get(this) == GATE_CLOSED) {
            onDrop(throwable);
            return;
        }

        onError(throwable);
        onNotification();
    }

    private void onNotification() {

        if (NOTIFICATIONS_UPDATER.incrementAndGet(this) == expectedCount) {

            ScheduledFuture<?> timeoutScheduleFuture = this.timeoutScheduleFuture;
            this.timeoutScheduleFuture = null;

            if (timeoutScheduleFuture != null) {
                timeoutScheduleFuture.cancel(false);
            }

            emit();
        }
    }

    private void emit() {

        if (GATE_UPDATER.compareAndSet(this, GATE_OPEN, GATE_CLOSED)) {

            onEmit(new Emission<V>() {

                @Override
                public void success(V value) {
                    selfFuture.complete(value);
                }

                @Override
                public void error(Throwable exception) {
                    selfFuture.completeExceptionally(exception);
                }

            });
        }
    }

    // Callback hooks

    protected void onAccept(T value) {

    }

    protected void onError(Throwable value) {

    }

    protected void onDrop(T value) {

    }

    protected void onDrop(Throwable value) {

    }

    protected void onEmit(Emission<V> emission) {

    }

    /**
     * Retrieve a {@link CompletionStage} that is notified upon completion or timeout.
     *
     * @param timeout
     * @param timeoutExecutor
     * @return
     */
    public final CompletionStage<V> getOrTimeout(Duration timeout, ScheduledExecutorService timeoutExecutor) {

        if (GATE_UPDATER.get(this) == GATE_OPEN && timeoutScheduleFuture == null) {
            this.timeoutScheduleFuture = timeoutExecutor.schedule(this::emit, timeout.toNanos(), TimeUnit.NANOSECONDS);
        }

        return selfFuture;
    }

    /**
     * Interface to signal emission of a value or an {@link Exception}.
     *
     * @param <T>
     */
    public interface Emission<T> {

        /**
         * Complete emission successfully.
         *
         * @param value the actual value to emit.
         */
        void success(T value);

        /**
         * Complete emission with an {@link Throwable exception}.
         *
         * @param exception the error to emit.
         */
        void error(Throwable exception);

    }

}
