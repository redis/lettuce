package io.lettuce.core.protocol;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.StatusOutput;

/**
 * Unit tests for {@link CommandWrapper}.
 *
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class CommandWrapperUnitTests {

    private RedisCodec<String, String> codec = StringCodec.UTF8;

    private Command<String, String, String> sut;

    @BeforeEach
    final void createCommand() {

        CommandOutput<String, String, String> output = new StatusOutput<>(codec);
        sut = new Command<>(CommandType.INFO, output, null);
    }

    @Test
    void shouldAppendOnComplete() {

        AtomicReference<Boolean> v1 = new AtomicReference<>();
        AtomicReference<Boolean> v2 = new AtomicReference<>();

        CommandWrapper<String, String, String> commandWrapper = new CommandWrapper<>(sut);

        commandWrapper.onComplete(s -> v1.set(true));
        commandWrapper.onComplete(s -> v2.set(true));

        commandWrapper.complete();

        assertThat(v1.get()).isTrue();
        assertThat(v2.get()).isTrue();
    }

    @Test
    void shouldGuardAgainstMultipleCompleteCalls() {

        CommandWrapper<String, String, String> commandWrapper = new CommandWrapper<>(sut);

        AtomicInteger counter = new AtomicInteger();

        commandWrapper.onComplete((s, throwable) -> commandWrapper.completeExceptionally(new IllegalStateException()));
        commandWrapper.onComplete((s, throwable) -> {
            counter.incrementAndGet();
        });

        commandWrapper.complete();

        assertThat(counter).hasValue(1);
    }

    @Test
    void shouldGuardAgainstMultipleCancelCalls() {

        CommandWrapper<String, String, String> commandWrapper = new CommandWrapper<>(sut);

        AtomicInteger counter = new AtomicInteger();

        commandWrapper.onComplete(s -> commandWrapper.cancel());
        commandWrapper.onComplete((s, throwable) -> {
            counter.incrementAndGet();
        });

        commandWrapper.cancel();

        assertThat(counter).hasValue(1);
    }

    @Test
    void shouldGuardAgainstMultipleCompleteExceptionallyCalls() {

        CommandWrapper<String, String, String> commandWrapper = new CommandWrapper<>(sut);

        AtomicInteger counter = new AtomicInteger();

        commandWrapper.onComplete(s -> commandWrapper.complete());
        commandWrapper.onComplete((s, throwable) -> {
            counter.incrementAndGet();
        });

        commandWrapper.completeExceptionally(new IllegalStateException());

        assertThat(counter).hasValue(1);
    }

    @Test
    void shouldPropagateCallbacksToDelegate() {

        AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(sut);
        CommandWrapper<String, String, String> commandWrapper = new CommandWrapper<>(asyncCommand);

        AtomicInteger counter = new AtomicInteger();

        commandWrapper.onComplete((s, throwable) -> {
            counter.incrementAndGet();
        });

        asyncCommand.cancel();

        assertThat(counter).hasValue(1);
    }

}
