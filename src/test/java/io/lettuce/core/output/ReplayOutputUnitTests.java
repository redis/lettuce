package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        RecordingOutput target = new RecordingOutput();

        replay.set(true);
        replay.replay(target);

        assertThat(target.events).containsExactly("set:true");
    }

    @Test
    void shouldReplaySingleAndBigNumber() {

        ReplayOutput<String, String> replay = new ReplayOutput<>();
        RecordingOutput target = new RecordingOutput();

        replay.setSingle(ByteBuffer.wrap("1.23".getBytes()));
        replay.setBigNumber(ByteBuffer.wrap("12345678901234567890".getBytes()));
        replay.replay(target);

        assertThat(target.events).containsExactly("setSingle:1.23", "setBigNumber:12345678901234567890");
    }

    @Test
    void shouldReplayMultiMapAsMultiMap() {

        ReplayOutput<String, String> replay = new ReplayOutput<>();
        RecordingOutput target = new RecordingOutput();

        replay.multiMap(1);
        replay.set(ByteBuffer.wrap("name".getBytes()));
        replay.set(ByteBuffer.wrap("lettuce".getBytes()));
        replay.replay(target);

        assertThat(target.events).contains("multiMap:1");
        assertThat(target.events).doesNotContain("multi:2");
    }

    @Test
    void shouldReplayMultiArrayMultiPushAndMultiSet() {

        ReplayOutput<String, String> replay = new ReplayOutput<>();
        RecordingOutput target = new RecordingOutput();

        replay.multiArray(2);
        replay.multiPush(3);
        replay.multiSet(4);
        replay.replay(target);

        assertThat(target.events).containsExactly("multiArray:2", "multiPush:3", "multiSet:4");
    }

    /**
     * Records the decoder callbacks it receives, so tests can assert on the exact callback instead of only the resulting value.
     */
    private static class RecordingOutput extends CommandOutput<String, String, String> {

        final List<String> events = new ArrayList<>();

        RecordingOutput() {
            super(StringCodec.ASCII, null);
        }

        @Override
        public void set(ByteBuffer bytes) {
            events.add("set:" + decodeString(bytes));
        }

        @Override
        public void setSingle(ByteBuffer bytes) {
            events.add("setSingle:" + decodeString(bytes));
        }

        @Override
        public void setBigNumber(ByteBuffer bytes) {
            events.add("setBigNumber:" + decodeString(bytes));
        }

        @Override
        public void set(boolean value) {
            events.add("set:" + value);
        }

        @Override
        public void multi(int count) {
            events.add("multi:" + count);
        }

        @Override
        public void multiArray(int count) {
            events.add("multiArray:" + count);
        }

        @Override
        public void multiPush(int count) {
            events.add("multiPush:" + count);
        }

        @Override
        public void multiMap(int count) {
            events.add("multiMap:" + count);
        }

        @Override
        public void multiSet(int count) {
            events.add("multiSet:" + count);
        }

        @Override
        public String get() {
            return String.join(",", events);
        }

    }

}
