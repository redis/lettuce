/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.internal;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AsyncConnectionProvider}.
 */
@Tag(UNIT_TEST)
class AsyncConnectionProviderUnitTests {

    @Test
    void shouldShareAcquisitionWhenConnectionFactoryReentersGetConnectionForSameKey() {

        ManualExecutor connectStarter = new ManualExecutor();
        AtomicInteger creations = new AtomicInteger();
        CompletableFuture<TestConnection> actualConnection = new CompletableFuture<>();
        AtomicReference<CompletableFuture<TestConnection>> reentrantLookup = new AtomicReference<>();
        AtomicReference<AsyncConnectionProvider<String, TestConnection>> provider = new AtomicReference<>();

        provider.set(new AsyncConnectionProvider<>(key -> {
            creations.incrementAndGet();

            // Simulate the connection factory requesting the same key again while the first acquisition
            // is still in progress. Both callers should observe the same underlying connection attempt.
            reentrantLookup.set(provider.get().getConnection(key));
            return actualConnection;
        }, connectStarter));

        CompletableFuture<TestConnection> initialLookup = provider.get().getConnection("key");

        assertThat(creations).hasValue(0);

        connectStarter.runNext();

        assertThat(creations).hasValue(1);
        assertThat(reentrantLookup.get()).isNotNull();
        assertThat(reentrantLookup.get()).isNotSameAs(initialLookup);

        TestConnection connection = new TestConnection();
        actualConnection.complete(connection);

        assertThat(initialLookup).isCompletedWithValue(connection);
        assertThat(reentrantLookup.get()).isCompletedWithValue(connection);
    }

    @Test
    void shouldShareAcquisitionForConcurrentSameKeyCallers() throws Exception {

        ManualExecutor connectStarter = new ManualExecutor();
        AtomicInteger creations = new AtomicInteger();
        CompletableFuture<TestConnection> actualConnection = new CompletableFuture<>();

        AsyncConnectionProvider<String, TestConnection> sut = new AsyncConnectionProvider<>(key -> {
            creations.incrementAndGet();
            return actualConnection;
        }, connectStarter);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<CompletableFuture<TestConnection>> first = executor.submit(() -> sut.getConnection("key"));
            Future<CompletableFuture<TestConnection>> second = executor.submit(() -> sut.getConnection("key"));

            CompletableFuture<TestConnection> firstLookup = first.get(5, TimeUnit.SECONDS);
            CompletableFuture<TestConnection> secondLookup = second.get(5, TimeUnit.SECONDS);

            assertThat(firstLookup).isNotSameAs(secondLookup);
            assertThat(creations).hasValue(0);
            assertThat(connectStarter.size()).isEqualTo(1);

            connectStarter.runNext();

            assertThat(creations).hasValue(1);

            TestConnection connection = new TestConnection();
            actualConnection.complete(connection);

            assertThat(firstLookup).isCompletedWithValue(connection);
            assertThat(secondLookup).isCompletedWithValue(connection);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldRemovePlaceholderAfterSynchronousFailure() {

        ManualExecutor connectStarter = new ManualExecutor();
        AtomicInteger attempts = new AtomicInteger();
        TestConnection connection = new TestConnection();

        AsyncConnectionProvider<String, TestConnection> sut = new AsyncConnectionProvider<>(key -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("boom");
            }

            return CompletableFuture.completedFuture(connection);
        }, connectStarter);

        CompletableFuture<TestConnection> failedLookup = sut.getConnection("key");

        connectStarter.runNext();

        assertThatThrownBy(failedLookup::join).hasRootCauseInstanceOf(IllegalStateException.class).hasMessageContaining("boom");

        CompletableFuture<TestConnection> retriedLookup = sut.getConnection("key");
        connectStarter.runNext();

        assertThat(retriedLookup).isCompletedWithValue(connection);
        assertThat(attempts).hasValue(2);
        assertThat(sut.getConnectionCount()).isEqualTo(1);
    }

    @Test
    void shouldRemovePlaceholderAfterAsynchronousFailure() {

        ManualExecutor connectStarter = new ManualExecutor();
        AtomicInteger attempts = new AtomicInteger();
        TestConnection connection = new TestConnection();

        AsyncConnectionProvider<String, TestConnection> sut = new AsyncConnectionProvider<>(key -> {
            if (attempts.getAndIncrement() == 0) {
                CompletableFuture<TestConnection> failed = new CompletableFuture<>();
                failed.completeExceptionally(new IllegalStateException("boom"));
                return failed;
            }

            return CompletableFuture.completedFuture(connection);
        }, connectStarter);

        CompletableFuture<TestConnection> failedLookup = sut.getConnection("key");

        connectStarter.runNext();

        assertThatThrownBy(failedLookup::join).hasRootCauseInstanceOf(IllegalStateException.class).hasMessageContaining("boom");

        CompletableFuture<TestConnection> retriedLookup = sut.getConnection("key");
        connectStarter.runNext();

        assertThat(retriedLookup).isCompletedWithValue(connection);
        assertThat(attempts).hasValue(2);
        assertThat(sut.getConnectionCount()).isEqualTo(1);
    }

    @Test
    void shouldNotCancelSharedAcquisitionWhenCallerIsCancelled() {

        CompletableFuture<TestConnection> actualConnection = new CompletableFuture<>();
        AsyncConnectionProvider<String, TestConnection> sut = new AsyncConnectionProvider<>(key -> actualConnection,
                Runnable::run);

        CompletableFuture<TestConnection> first = sut.getConnection("key");
        CompletableFuture<TestConnection> second = sut.getConnection("key");

        assertThat(first.cancel(false)).isTrue();
        assertThat(actualConnection.isCancelled()).isFalse();

        TestConnection connection = new TestConnection();
        actualConnection.complete(connection);

        assertThat(first).isCancelled();
        assertThat(second).isCompletedWithValue(connection);
        assertThat(sut.getConnectionCount()).isEqualTo(1);
    }

    @Test
    void closeShouldCancelPendingConnectionAttemptWhenPossible() {

        CompletableFuture<TestConnection> actualConnection = new CompletableFuture<>();
        AsyncConnectionProvider<String, TestConnection> sut = new AsyncConnectionProvider<>(key -> actualConnection,
                Runnable::run);

        CompletableFuture<TestConnection> lookup = sut.getConnection("key");

        CompletableFuture<Void> closeFuture = sut.close();

        assertThat(actualConnection).isCancelled();
        assertThat(lookup).isCancelled();
        assertThat(closeFuture).isCompleted();
        assertThat(sut.getConnectionCount()).isEqualTo(0);
    }

    @Test
    void closeShouldCompleteIfConnectStartAbortsAfterMarkingStarted() {

        AsyncConnectionProvider.Sync<String, TestConnection> sync = new AsyncConnectionProvider.Sync<>("key");

        sync.markConnectStarted();
        sync.completeCloseWithoutConnection();

        CompletableFuture<Void> closeFuture = sync.close();

        assertThat(sync.getSharedConnection()).isCancelled();
        assertThat(closeFuture).isCompleted();
    }

    @Test
    void closeShouldWaitForPendingConnectionAndCloseIt() {

        NonCancellableFuture<TestConnection> actualConnection = new NonCancellableFuture<>();
        AsyncConnectionProvider<String, TestConnection> sut = new AsyncConnectionProvider<>(key -> actualConnection,
                Runnable::run);

        sut.getConnection("key");

        CompletableFuture<Void> closeFuture = sut.close();

        assertThat(closeFuture).isNotDone();

        TestConnection connection = new TestConnection();
        actualConnection.complete(connection);

        assertThat(closeFuture).isCompleted();
        assertThat(connection.closed).hasValue(1);
        assertThat(sut.getConnectionCount()).isEqualTo(0);
    }

    static class TestConnection implements AsyncCloseable {

        final AtomicInteger closed = new AtomicInteger();

        @Override
        public CompletableFuture<Void> closeAsync() {
            closed.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }

    }

    static class NonCancellableFuture<T> extends CompletableFuture<T> {

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

    }

    static class ManualExecutor implements Executor {

        private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        int size() {
            return tasks.size();
        }

        void runNext() {
            Runnable task = tasks.poll();
            assertThat(task).isNotNull();
            task.run();
        }

    }

}
