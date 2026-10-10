package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * Unit tests for {@link RedisPublisher}.
 */
@Tag(UNIT_TEST)
class RedisPublisherUnitTests {

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
