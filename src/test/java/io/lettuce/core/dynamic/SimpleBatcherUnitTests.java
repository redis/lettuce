package io.lettuce.core.dynamic;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.dynamic.batch.CommandBatching;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;

/**
 * @author Mark Paluch
 * @author Ivo Gaydajiev
 */
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
class SimpleBatcherUnitTests {

    @Mock
    private StatefulConnection<Object, Object> connection;

    @Test
    void shouldBatchWithDefaultSize() {

        RedisCommand<Object, Object, Object> c1 = createCommand();
        RedisCommand<Object, Object, Object> c2 = createCommand();
        RedisCommand<Object, Object, Object> c3 = createCommand();
        RedisCommand<Object, Object, Object> c4 = createCommand();

        SimpleBatcher batcher = new SimpleBatcher(connection, 2);

        assertThat(batcher.batch(c1, null)).isEqualTo(BatchTasks.EMPTY);
        verifyNoInteractions(connection);

        BatchTasks batch = batcher.batch(c2, null);
        verify(connection).dispatch(Arrays.asList(c1, c2));
        assertThat(batch).contains(c1, c2);

        batcher.batch(c3, null);
        verifyNoMoreInteractions(connection);

        batcher.batch(c4, null);
        verify(connection).dispatch(Arrays.asList(c3, c4));
    }

    @Test
    void shouldBatchWithoutSize() {

        RedisCommand<Object, Object, Object> c1 = createCommand();
        RedisCommand<Object, Object, Object> c2 = createCommand();

        SimpleBatcher batcher = new SimpleBatcher(connection, -1);

        batcher.batch(c1, null);

        verify(connection).dispatch(c1);

        batcher.batch(c2, null);

        verify(connection).dispatch(c2);
    }

    @Test
    void shouldBatchWithBatchControlQueue() {

        RedisCommand<Object, Object, Object> c1 = createCommand();
        RedisCommand<Object, Object, Object> c2 = createCommand();
        RedisCommand<Object, Object, Object> c3 = createCommand();
        RedisCommand<Object, Object, Object> c4 = createCommand();

        SimpleBatcher batcher = new SimpleBatcher(connection, 2);

        batcher.batch(c1, CommandBatching.queue());
        batcher.batch(c2, CommandBatching.queue());
        verifyNoInteractions(connection);

        batcher.batch(c3, null);

        verify(connection).dispatch(Arrays.asList(c1, c2));
    }

    @Test
    void shouldBatchWithBatchControlQueueOverqueue() {

        RedisCommand<Object, Object, Object> c1 = createCommand();
        RedisCommand<Object, Object, Object> c2 = createCommand();
        RedisCommand<Object, Object, Object> c3 = createCommand();
        RedisCommand<Object, Object, Object> c4 = createCommand();
        RedisCommand<Object, Object, Object> c5 = createCommand();

        SimpleBatcher batcher = new SimpleBatcher(connection, 2);

        batcher.batch(c1, CommandBatching.queue());
        batcher.batch(c2, CommandBatching.queue());
        batcher.batch(c3, CommandBatching.queue());
        batcher.batch(c4, CommandBatching.queue());
        verifyNoInteractions(connection);

        batcher.batch(c5, null);

        verify(connection).dispatch(Arrays.asList(c1, c2));
        verify(connection).dispatch(Arrays.asList(c3, c4));
    }

    @Test
    void shouldBatchWithBatchControlFlush() {

        RedisCommand<Object, Object, Object> c1 = createCommand();
        RedisCommand<Object, Object, Object> c2 = createCommand();
        RedisCommand<Object, Object, Object> c3 = createCommand();

        SimpleBatcher batcher = new SimpleBatcher(connection, 4);

        batcher.batch(c1, null);
        batcher.batch(c2, CommandBatching.flush());

        verify(connection).dispatch(Arrays.asList(c1, c2));
    }

    @Test
    void shouldDispatchCommandsQueuedDuringOngoingFlush() throws InterruptedException {
        RedisCommand<Object, Object, Object> c1 = createCommand();
        RedisCommand<Object, Object, Object> c2 = createCommand();

        CountDownLatch batchFlushLatch1 = new CountDownLatch(1);
        CountDownLatch batchFlushLatch2 = new CountDownLatch(1);

        when(connection.dispatch((RedisCommand<Object, Object, Object>) any())).thenAnswer(invocation -> {
            batchFlushLatch1.countDown();
            batchFlushLatch2.await();

            return null;
        });

        SimpleBatcher batcher = new SimpleBatcher(connection, 4);

        Thread batchThread1 = new Thread(() -> {
            batcher.batch(c1, CommandBatching.flush());
        });
        batchThread1.start();

        Thread batchThread2 = new Thread(() -> {
            try {
                batchFlushLatch1.await();
            } catch (InterruptedException ignored) {
            }
            batcher.batch(c2, CommandBatching.flush());
            batchFlushLatch2.countDown();
        });
        batchThread2.start();

        batchThread1.join();
        batchThread2.join();
        verify(connection, times(1)).dispatch(c1);
        verify(connection, times(1)).dispatch(c2);
    }

    private static RedisCommand<Object, Object, Object> createCommand() {
        return new AsyncCommand<>(new Command<>(CommandType.COMMAND, null, null));
    }

}
