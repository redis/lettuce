package io.lettuce.core.tracing;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.CommandWrapper;
import io.micrometer.observation.Observation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@Tag(UNIT_TEST)
class MicrometerTracingUnitTests {

    @Test
    void shouldCompleteSpanAfterObservationStartWhenCommandWasAlreadyCancelled() {

        Observation observation = mock(Observation.class);
        AtomicBoolean observationStartReturned = new AtomicBoolean();

        when(observation.start()).thenAnswer(invocation -> {
            observationStartReturned.set(true);
            return observation;
        });
        doAnswer(invocation -> {
            assertThat(observationStartReturned).isTrue();
            return observation;
        }).when(observation).error(any(Throwable.class));
        doAnswer(invocation -> {
            assertThat(observationStartReturned).isTrue();
            return observation;
        }).when(observation).stop();

        AsyncCommand<String, String, String> command = new AsyncCommand<>(
                new Command<>(CommandType.PING, new StatusOutput<>(StringCodec.UTF8)));
        assertThat(command.cancel(false)).isTrue();

        MicrometerTracing.MicrometerSpan span = new MicrometerTracing.MicrometerSpan("Redis", context -> observation);
        span.start(command);

        InOrder inOrder = inOrder(observation);
        inOrder.verify(observation).start();
        inOrder.verify(observation).error(any(Throwable.class));
        inOrder.verify(observation).stop();
    }

    @Test
    void shouldDeferCancellationCallbackUntilObservationStartReturns() throws InterruptedException {

        CommandWrapper<String, String, String> command = new CommandWrapper<>(
                new Command<>(CommandType.PING, new StatusOutput<>(StringCodec.UTF8)));
        Observation observation = mock(Observation.class);
        AtomicBoolean observationStartReturned = new AtomicBoolean();
        AtomicBoolean cancelReturned = new AtomicBoolean();

        when(observation.start()).thenAnswer(invocation -> {
            Thread cancelThread = new Thread(() -> {
                command.cancel();
                cancelReturned.set(true);
            });
            cancelThread.start();
            cancelThread.join();
            assertThat(cancelReturned).isTrue();
            observationStartReturned.set(true);
            return observation;
        });
        doAnswer(invocation -> {
            assertThat(observationStartReturned).isTrue();
            return observation;
        }).when(observation).error(any(Throwable.class));
        doAnswer(invocation -> {
            assertThat(observationStartReturned).isTrue();
            return observation;
        }).when(observation).stop();

        MicrometerTracing.MicrometerSpan span = new MicrometerTracing.MicrometerSpan("Redis", context -> observation);
        span.start(command);

        InOrder inOrder = inOrder(observation);
        inOrder.verify(observation).start();
        inOrder.verify(observation).error(any(Throwable.class));
        inOrder.verify(observation).stop();
    }

}
