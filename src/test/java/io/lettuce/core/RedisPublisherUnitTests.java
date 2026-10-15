package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Subscriber;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.CompleteableCommand;
import io.lettuce.core.protocol.RedisCommand;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * Unit tests for {@link RedisPublisher}.
 */
@Tag(UNIT_TEST)
class RedisPublisherUnitTests {

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void reactiveCancellationNotifiesTheBufferedCommandWrapper() {
        StatefulConnection<String, String> connection = mock(StatefulConnection.class);
        Command<String, String, String> delegate = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        RedisPublisher.RedisSubscription<String> subscription = new RedisPublisher.RedisSubscription<>(connection, delegate,
                false, ImmediateEventExecutor.INSTANCE);
        Subscriber<String> subscriber = mock(Subscriber.class);
        subscription.subscribe(subscriber);
        subscription.request(1);

        ArgumentCaptor<RedisCommand> dispatched = ArgumentCaptor.forClass(RedisCommand.class);
        verify(connection).dispatch(dispatched.capture());
        RedisCommand<?, ?, ?> buffered = dispatched.getValue();
        AtomicBoolean completed = new AtomicBoolean();
        ((CompleteableCommand<?>) buffered).onComplete((result, error) -> completed.set(true));

        subscription.cancel();

        assertThat(delegate.isCancelled()).isTrue();
        assertThat(completed.get()).isTrue();
        verify(subscriber).onSubscribe(any());
        verifyNoMoreInteractions(subscriber);
    }

    @Test
    void shouldSignalErrorWhenStateChangesConcurrently() {

        Command<String, String, String> command = new Command<>(CommandType.EVALSHA, new StatusOutput<>(StringCodec.UTF8),
                null);

        // Simulates the subscribing thread flipping DEMAND -> READING right before the error's CAS to COMPLETED.
        RedisPublisher.RedisSubscription<String> subscription = new RedisPublisher.RedisSubscription<String>(
                mock(StatefulConnection.class), command, false, ImmediateEventExecutor.INSTANCE) {

            private boolean raced;

            @Override
            boolean changeState(RedisPublisher.State oldState, RedisPublisher.State newState) {

                if (!raced && oldState == RedisPublisher.State.DEMAND && newState == RedisPublisher.State.COMPLETED) {
                    raced = true;
                    super.changeState(RedisPublisher.State.DEMAND, RedisPublisher.State.READING);
                }
                return super.changeState(oldState, newState);
            }

        };

        Subscriber<String> subscriber = mock(Subscriber.class);
        subscription.subscribe(subscriber);
        subscription.changeState(RedisPublisher.State.NO_DEMAND, RedisPublisher.State.DEMAND);

        RedisCommandExecutionException error = new RedisCommandExecutionException("NOSCRIPT No matching script");
        subscription.onError(error);

        verify(subscriber).onSubscribe(any());
        verify(subscriber).onError(error);
        verifyNoMoreInteractions(subscriber);
    }

}
