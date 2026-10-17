package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class ReplayOutputUnitTests {

    @Test
    void shouldReplaySimpleCompletion() {

        ReplayOutput<String, String> replay = new ReplayOutput<>();
        ValueOutput<String, String> target = new ValueOutput<>(StringCodec.ASCII);

        replay.multi(1);
        replay.set(ByteBuffer.wrap("foo".getBytes()));
        replay.complete(1);

        replay.replay(target);

        assertThat(target.get()).isEqualTo("foo");
    }

    @Test
    void shouldReplayNestedCompletion() {

        ReplayOutput<String, String> replay = new ReplayOutput<>();
        ArrayOutput<String, String> target = new ArrayOutput<>(StringCodec.ASCII);

        replay.multi(1);
        replay.multi(1);
        replay.set(ByteBuffer.wrap("foo".getBytes()));
        replay.complete(2);

        replay.multi(1);
        replay.set(ByteBuffer.wrap("bar".getBytes()));
        replay.complete(2);
        replay.complete(1);

        replay.replay(target);

        assertThat(target.get().get(0)).isEqualTo(Arrays.asList("foo", Collections.singletonList("bar")));
    }

    @Test
    void shouldDecodeErrorResponse() {

        ReplayOutput<String, String> replay = new ReplayOutput<>();
        ValueOutput<String, String> target = new ValueOutput<>(StringCodec.ASCII);

        replay.setError(ByteBuffer.wrap("foo".getBytes()));

        replay.replay(target);

        assertThat(replay.getError()).isEqualTo("foo");
        assertThat(target.getError()).isEqualTo("foo");
    }

    @Test
    void shouldReplayBoolean() {

        ReplayOutput<String, String> replay = new ReplayOutput<>();
        BooleanOutput<String, String> target = new BooleanOutput<>(StringCodec.ASCII);

        replay.set(true);

        replay.replay(target);

        assertThat(target.get()).isTrue();
    }

    @Test
    void shouldReplayMap() {

        ReplayOutput<String, String> replay = new ReplayOutput<>();
        ObjectOutput<String, String> target = new ObjectOutput<>(StringCodec.ASCII);

        replay.multiMap(1);
        replay.set(ByteBuffer.wrap("foo".getBytes()));
        replay.set(ByteBuffer.wrap("bar".getBytes()));
        replay.complete(1);

        replay.replay(target);

        assertThat(target.get()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) target.get();
        assertThat(map).containsEntry("foo", "bar");
    }

    @Test
    void shouldReplaySet() {

        ReplayOutput<String, String> replay = new ReplayOutput<>();
        AtomicInteger count = new AtomicInteger();
        CommandOutput<String, String, String> target = new CommandOutput<String, String, String>(StringCodec.ASCII, null) {

            @Override
            public void multiSet(int c) {
                count.set(c);
            }

        };

        replay.multiSet(5);

        replay.replay(target);

        assertThat(count.get()).isEqualTo(5);
    }

}
