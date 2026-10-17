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
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;

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
    void shouldReplayResp3Callbacks() {

        ReplayOutput<String, String> replay = new ReplayOutput<>();
        RecordingOutput target = new RecordingOutput();

        replay.setSingle(ByteBuffer.wrap("simple".getBytes()));
        replay.setBigNumber(ByteBuffer.wrap("12345678901234567890".getBytes()));
        replay.set(true);
        replay.multiArray(1);
        replay.multiPush(2);
        replay.multiMap(3);
        replay.multiSet(4);

        replay.replay(target);

        assertThat(target.callbacks).containsExactly("single", "bigNumber", "boolean", "array:1", "push:2", "map:3", "set:4");
    }

    @Test
    void shouldForwardResp3CallbacksToTransactionCommand() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.ASCII);
        RecordingOutput target = new RecordingOutput();
        output.add(new Command<>(CommandType.APPEND, target));

        output.multi(1);
        output.setSingle(ByteBuffer.wrap("simple".getBytes()));
        output.setBigNumber(ByteBuffer.wrap("12345678901234567890".getBytes()));
        output.set(true);
        output.multiArray(1);
        output.multiPush(2);
        output.multiMap(3);
        output.multiSet(4);

        assertThat(target.callbacks).containsExactly("single", "bigNumber", "boolean", "array:1", "push:2", "map:3", "set:4");
    }

    private static class RecordingOutput extends CommandOutput<String, String, Object> {

        final List<String> callbacks = new ArrayList<>();

        RecordingOutput() {
            super(StringCodec.ASCII, null);
        }

        @Override
        public void set(ByteBuffer bytes) {
            callbacks.add("bulk");
        }

        @Override
        public void setSingle(ByteBuffer bytes) {
            callbacks.add("single");
        }

        @Override
        public void setBigNumber(ByteBuffer bytes) {
            callbacks.add("bigNumber");
        }

        @Override
        public void set(boolean value) {
            callbacks.add("boolean");
        }

        @Override
        public void multi(int count) {
            callbacks.add("multi:" + count);
        }

        @Override
        public void multiArray(int count) {
            callbacks.add("array:" + count);
        }

        @Override
        public void multiPush(int count) {
            callbacks.add("push:" + count);
        }

        @Override
        public void multiMap(int count) {
            callbacks.add("map:" + count);
        }

        @Override
        public void multiSet(int count) {
            callbacks.add("set:" + count);
        }

    }

}
