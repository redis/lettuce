package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * Unit tests for {@link io.lettuce.core.RedisPublisher.SubscriptionCommand}.
 *
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class SubscriptionCommandUnitTests {

    private RedisCodec<String, String> codec = StringCodec.UTF8;

    private Command<String, String, String> command;

    private RedisPublisher.RedisSubscription<String> subscription;

    private Subscriber<String> subscriber = mock(Subscriber.class);

    @BeforeEach
    final void createCommand() {

        CommandOutput<String, String, String> output = new StatusOutput<>(codec);
        command = new Command<>(CommandType.INFO, output, null);
    }

    @Test
    void shouldCompleteOnlyOnce() {

        subscription = new RedisPublisher.RedisSubscription<>(mock(StatefulConnection.class), command, false,
                ImmediateEventExecutor.INSTANCE);
        subscription.subscribe(subscriber);
        subscription.changeState(RedisPublisher.State.NO_DEMAND, RedisPublisher.State.DEMAND);
        subscription.request(1);

        RedisPublisher.SubscriptionCommand<String, String, String> wrapper = new RedisPublisher.SubscriptionCommand<>(command,
                subscription, false);
        command.getOutput().setSingle(ByteBuffer.wrap("Hello".getBytes(StandardCharsets.UTF_8)));

        wrapper.onComplete((s, throwable) -> {

            wrapper.completeExceptionally(new IllegalStateException());
        });

        wrapper.complete();

        verify(subscriber).onSubscribe(any());
        verify(subscriber).onNext("Hello");
        verify(subscriber).onComplete();
        verifyNoMoreInteractions(subscriber);
    }

}
