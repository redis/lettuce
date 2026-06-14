package io.lettuce.core.dynamic;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandInterruptedException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.dynamic.batch.BatchException;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Unit tests for {@link BatchExecutableCommand}.
 */
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
class BatchExecutableCommandUnitTests {

    @Mock
    private StatefulConnection<Object, Object> connection;

    @Test
    void synchronizeShouldIgnoreEmptyBatchWithoutReadingTimeout() {

        assertThat(BatchExecutableCommand.synchronize(BatchTasks.EMPTY, connection)).isNull();

        verifyNoInteractions(connection);
    }

    @Test
    void synchronizeShouldReturnWhenAllBatchCommandsCompleted() {

        when(connection.getTimeout()).thenReturn(Duration.ofMillis(10));

        AsyncCommand<Object, Object, Object> first = completedCommand();
        AsyncCommand<Object, Object, Object> second = completedCommand();

        assertThat(BatchExecutableCommand.synchronize(batchTasks(first, second), connection)).isNull();
    }

    @Test
    void synchronizeShouldAggregateAllCompletedCommandFailures() {

        when(connection.getTimeout()).thenReturn(Duration.ofMillis(10));

        AsyncCommand<Object, Object, Object> first = failedCommand(new RedisCommandExecutionException("first"));
        AsyncCommand<Object, Object, Object> second = completedCommand();
        AsyncCommand<Object, Object, Object> third = failedCommand(new RedisCommandExecutionException("third"));

        BatchException exception = catchThrowableOfType(
                () -> BatchExecutableCommand.synchronize(batchTasks(first, second, third), connection), BatchException.class);

        assertThat(exception.getFailedCommands()).containsExactly(first, third);
        assertThat(exception.getSuppressed()).hasSize(2);
        assertThat(exception.getSuppressed()[0]).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("first");
        assertThat(exception.getSuppressed()[1]).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("third");
    }

    @Test
    void synchronizeShouldConvertAwaitTimeoutIntoBatchException() {

        when(connection.getTimeout()).thenReturn(Duration.ofNanos(1));

        AsyncCommand<Object, Object, Object> pending = pendingCommand();

        BatchException exception = catchThrowableOfType(
                () -> BatchExecutableCommand.synchronize(batchTasks(pending), connection), BatchException.class);

        assertThat(exception.getFailedCommands()).containsExactly(pending);
        assertThat(exception.getSuppressed()).hasSize(1);
        assertThat(exception.getSuppressed()[0]).isInstanceOf(RedisCommandTimeoutException.class)
                .hasMessageContaining("Command timed out");
    }

    @Test
    void synchronizeShouldApplyTimeoutAcrossWholeBatch() {

        Duration timeout = Duration.ofMillis(500);
        when(connection.getTimeout()).thenReturn(timeout);

        DelayingCommand first = new DelayingCommand(Duration.ofMillis(25));
        TimingOutCommand second = new TimingOutCommand();

        BatchException exception = catchThrowableOfType(
                () -> BatchExecutableCommand.synchronize(batchTasks(first, second), connection), BatchException.class);

        assertThat(exception.getFailedCommands()).containsExactly(second);
        assertThat(second.timedGetCalls()).isEqualTo(1);
        assertThat(second.timeoutNs()).isPositive().isLessThan(timeout.toNanos());
    }

    @Test
    void synchronizeShouldNotBlockOnUnfinishedCommandsAfterBatchTimeout() {

        when(connection.getTimeout()).thenReturn(Duration.ofNanos(1));

        TimingOutCommand timedOut = new TimingOutCommand();
        AsyncCommand<Object, Object, Object> failedAfterTimeout = failedCommand(
                new RedisCommandExecutionException("already failed"));
        FailIfAwaitedCommand unfinishedAfterTimeout = new FailIfAwaitedCommand();

        BatchException exception = catchThrowableOfType(() -> BatchExecutableCommand
                .synchronize(batchTasks(timedOut, failedAfterTimeout, unfinishedAfterTimeout), connection),
                BatchException.class);

        assertThat(exception.getFailedCommands()).containsExactly(timedOut, failedAfterTimeout);
        assertThat(exception.getSuppressed()).hasSize(2);
        assertThat(exception.getSuppressed()[0]).isInstanceOf(RedisCommandTimeoutException.class);
        assertThat(exception.getSuppressed()[1]).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("already failed");
        assertThat(unfinishedAfterTimeout.wasAwaited()).isFalse();
    }

    @Test
    void synchronizeShouldCollectAlreadyCompletedFailuresAfterBatchTimeout() {

        when(connection.getTimeout()).thenReturn(Duration.ofNanos(1));

        TimingOutCommand timedOut = new TimingOutCommand();
        AsyncCommand<Object, Object, Object> completedAfterTimeout = completedCommand();
        AsyncCommand<Object, Object, Object> failedAfterTimeout = failedCommand(
                new RedisCommandExecutionException("done before scan"));

        BatchException exception = catchThrowableOfType(() -> BatchExecutableCommand
                .synchronize(batchTasks(timedOut, completedAfterTimeout, failedAfterTimeout), connection),
                BatchException.class);

        assertThat(exception.getFailedCommands()).containsExactly(timedOut, failedAfterTimeout);
        assertThat(exception.getSuppressed()).hasSize(2);
        assertThat(exception.getSuppressed()[0]).isInstanceOf(RedisCommandTimeoutException.class);
        assertThat(exception.getSuppressed()[1]).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("done before scan");
    }

    @Test
    void synchronizeShouldUseUnboundedWaitForZeroTimeout() {

        when(connection.getTimeout()).thenReturn(Duration.ZERO);

        CompletesOnUnboundedGetCommand command = new CompletesOnUnboundedGetCommand();

        assertThat(BatchExecutableCommand.synchronize(batchTasks(command), connection)).isNull();

        assertThat(command.unboundedGetCalls()).isEqualTo(1);
        assertThat(command.timedGetCalls()).isZero();
    }

    @Test
    void synchronizeShouldUseUnboundedWaitForNegativeTimeout() {

        when(connection.getTimeout()).thenReturn(Duration.ofNanos(-1));

        CompletesOnUnboundedGetCommand command = new CompletesOnUnboundedGetCommand();

        assertThat(BatchExecutableCommand.synchronize(batchTasks(command), connection)).isNull();

        assertThat(command.unboundedGetCalls()).isEqualTo(1);
        assertThat(command.timedGetCalls()).isZero();
    }

    @Test
    void synchronizeShouldUseUnboundedWaitForCompletedCommandWithPositiveTimeout() {

        when(connection.getTimeout()).thenReturn(Duration.ofSeconds(1));

        CompletedGetTrackingCommand command = new CompletedGetTrackingCommand();

        assertThat(BatchExecutableCommand.synchronize(batchTasks(command), connection)).isNull();

        assertThat(command.unboundedGetCalls()).isEqualTo(1);
        assertThat(command.timedGetCalls()).isZero();
    }

    @Test
    void synchronizeShouldUseUnboundedWaitForCompletedFailureWithPositiveTimeout() {

        when(connection.getTimeout()).thenReturn(Duration.ofSeconds(1));

        RedisCommandExecutionException failure = new RedisCommandExecutionException("completed failure");
        FailedCompletedGetTrackingCommand command = new FailedCompletedGetTrackingCommand(failure);

        BatchException exception = catchThrowableOfType(
                () -> BatchExecutableCommand.synchronize(batchTasks(command), connection), BatchException.class);

        assertThat(exception.getFailedCommands()).containsExactly(command);
        assertThat(exception.getSuppressed()).hasSize(1);
        assertThat(exception.getSuppressed()[0]).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("completed failure");
        assertThat(command.unboundedGetCalls()).isEqualTo(1);
        assertThat(command.timedGetCalls()).isZero();
    }

    @Test
    void synchronizeShouldExpireDeadlineBeforeAwaitingPendingCommand() {

        when(connection.getTimeout()).thenReturn(Duration.ofMillis(50));

        DelayingCommand completedAfterDeadline = new DelayingCommand(Duration.ofMillis(75));
        FailIfAwaitedCommand timedOutAfterDeadline = new FailIfAwaitedCommand();
        FailIfAwaitedCommand skippedAfterDeadline = new FailIfAwaitedCommand();

        BatchException exception = catchThrowableOfType(
                () -> BatchExecutableCommand.synchronize(
                        batchTasks(completedAfterDeadline, timedOutAfterDeadline, skippedAfterDeadline), connection),
                BatchException.class);

        assertThat(exception.getFailedCommands()).containsExactly(timedOutAfterDeadline);
        assertThat(exception.getSuppressed()).hasSize(1);
        assertThat(exception.getSuppressed()[0]).isInstanceOf(RedisCommandTimeoutException.class);
        assertThat(timedOutAfterDeadline.wasAwaited()).isFalse();
        assertThat(skippedAfterDeadline.wasAwaited()).isFalse();
    }

    @Test
    void synchronizeShouldCollectCompletedFailuresAfterDeadlineExpiredBeforeAwait() {

        when(connection.getTimeout()).thenReturn(Duration.ofMillis(50));

        DelayingCommand completedAfterDeadline = new DelayingCommand(Duration.ofMillis(75));
        FailIfAwaitedCommand timedOutAfterDeadline = new FailIfAwaitedCommand();
        AsyncCommand<Object, Object, Object> failedAfterDeadline = failedCommand(
                new RedisCommandExecutionException("completed after deadline"));
        FailIfAwaitedCommand skippedAfterDeadline = new FailIfAwaitedCommand();

        BatchException exception = catchThrowableOfType(() -> BatchExecutableCommand.synchronize(
                batchTasks(completedAfterDeadline, timedOutAfterDeadline, failedAfterDeadline, skippedAfterDeadline),
                connection), BatchException.class);

        assertThat(exception.getFailedCommands()).containsExactly(timedOutAfterDeadline, failedAfterDeadline);
        assertThat(exception.getSuppressed()).hasSize(2);
        assertThat(exception.getSuppressed()[0]).isInstanceOf(RedisCommandTimeoutException.class);
        assertThat(exception.getSuppressed()[1]).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("completed after deadline");
        assertThat(timedOutAfterDeadline.wasAwaited()).isFalse();
        assertThat(skippedAfterDeadline.wasAwaited()).isFalse();
    }

    @Test
    void synchronizeShouldAggregateInterruptedAwaitAsBatchFailure() {

        when(connection.getTimeout()).thenReturn(Duration.ofSeconds(1));

        InterruptingCommand command = new InterruptingCommand();

        try {
            BatchException exception = catchThrowableOfType(
                    () -> BatchExecutableCommand.synchronize(batchTasks(command), connection), BatchException.class);

            assertThat(exception.getFailedCommands()).containsExactly(command);
            assertThat(exception.getSuppressed()).hasSize(1);
            assertThat(exception.getSuppressed()[0]).isInstanceOf(RedisCommandInterruptedException.class);
            assertThat(command.timedGetCalls()).isEqualTo(1);
            assertThat(command.unboundedGetCalls()).isZero();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    private static BatchTasks batchTasks(RedisCommand<?, ?, ?>... commands) {
        return new BatchTasks(Arrays.asList(commands));
    }

    private static AsyncCommand<Object, Object, Object> pendingCommand() {
        return new AsyncCommand<>(new Command<>(CommandType.COMMAND, null, null));
    }

    private static AsyncCommand<Object, Object, Object> completedCommand() {

        AsyncCommand<Object, Object, Object> command = pendingCommand();
        command.complete();
        return command;
    }

    private static AsyncCommand<Object, Object, Object> failedCommand(Throwable throwable) {

        AsyncCommand<Object, Object, Object> command = pendingCommand();
        command.completeExceptionally(throwable);
        return command;
    }

    private static class DelayingCommand extends AsyncCommand<Object, Object, Object> {

        private final long delayNs;

        DelayingCommand(Duration delay) {
            super(new Command<>(CommandType.COMMAND, null, null));
            this.delayNs = delay.toNanos();
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

            parkAtLeast(delayNs);
            complete();
            return null;
        }

    }

    private static class TimingOutCommand extends AsyncCommand<Object, Object, Object> {

        private final AtomicInteger timedGetCalls = new AtomicInteger();

        private volatile long timeoutNs;

        TimingOutCommand() {
            super(new Command<>(CommandType.COMMAND, null, null));
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

            timedGetCalls.incrementAndGet();
            timeoutNs = unit.toNanos(timeout);
            throw new TimeoutException();
        }

        int timedGetCalls() {
            return timedGetCalls.get();
        }

        long timeoutNs() {
            return timeoutNs;
        }

    }

    private static class FailIfAwaitedCommand extends AsyncCommand<Object, Object, Object> {

        private final AtomicBoolean awaited = new AtomicBoolean();

        FailIfAwaitedCommand() {
            super(new Command<>(CommandType.COMMAND, null, null));
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {

            awaited.set(true);
            throw new AssertionError("Unfinished command must not be awaited after the batch timeout is reached");
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

            awaited.set(true);
            throw new AssertionError("Unfinished command must not be awaited after the batch timeout is reached");
        }

        boolean wasAwaited() {
            return awaited.get();
        }

    }

    private static class CompletesOnUnboundedGetCommand extends AsyncCommand<Object, Object, Object> {

        private final AtomicInteger unboundedGetCalls = new AtomicInteger();

        private final AtomicInteger timedGetCalls = new AtomicInteger();

        CompletesOnUnboundedGetCommand() {
            super(new Command<>(CommandType.COMMAND, null, null));
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {

            unboundedGetCalls.incrementAndGet();
            complete();
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

            timedGetCalls.incrementAndGet();
            throw new AssertionError("Zero timeout must use Future.get() without a timeout");
        }

        int unboundedGetCalls() {
            return unboundedGetCalls.get();
        }

        int timedGetCalls() {
            return timedGetCalls.get();
        }

    }

    private static class CompletedGetTrackingCommand extends AsyncCommand<Object, Object, Object> {

        private final AtomicInteger unboundedGetCalls = new AtomicInteger();

        private final AtomicInteger timedGetCalls = new AtomicInteger();

        CompletedGetTrackingCommand() {
            this(null);
        }

        CompletedGetTrackingCommand(Throwable throwable) {
            super(new Command<>(CommandType.COMMAND, null, null));

            if (throwable == null) {
                complete();
            } else {
                completeExceptionally(throwable);
            }
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {

            unboundedGetCalls.incrementAndGet();
            return super.get();
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

            timedGetCalls.incrementAndGet();
            throw new AssertionError("Completed command must use Future.get() without a timeout");
        }

        int unboundedGetCalls() {
            return unboundedGetCalls.get();
        }

        int timedGetCalls() {
            return timedGetCalls.get();
        }

    }

    private static class FailedCompletedGetTrackingCommand extends CompletedGetTrackingCommand {

        FailedCompletedGetTrackingCommand(Throwable throwable) {
            super(throwable);
        }

    }

    private static class InterruptingCommand extends AsyncCommand<Object, Object, Object> {

        private final AtomicInteger unboundedGetCalls = new AtomicInteger();

        private final AtomicInteger timedGetCalls = new AtomicInteger();

        InterruptingCommand() {
            super(new Command<>(CommandType.COMMAND, null, null));
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {

            unboundedGetCalls.incrementAndGet();
            throw new InterruptedException("interrupted");
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

            timedGetCalls.incrementAndGet();
            throw new InterruptedException("interrupted");
        }

        int unboundedGetCalls() {
            return unboundedGetCalls.get();
        }

        int timedGetCalls() {
            return timedGetCalls.get();
        }

    }

    private static void parkAtLeast(long nanos) {

        long deadline = System.nanoTime() + nanos;

        while (System.nanoTime() < deadline) {
            long remaining = deadline - System.nanoTime();
            LockSupport.parkNanos(Math.min(TimeUnit.MILLISECONDS.toNanos(1), remaining));
        }
    }

}
