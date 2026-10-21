package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.Value;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class MultiOutputUnitTests {

    @Test
    void shouldCompleteCommand() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        Command<String, String, String> command = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8));

        output.add(command);

        output.multi(1);
        output.set(ByteBuffer.wrap("OK".getBytes()));
        output.complete(1);

        assertThat(command.getOutput().get()).isEqualTo("OK");
    }

    @Test
    void shouldForwardBooleanToCommand() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        Command<String, String, Boolean> added = new Command<>(CommandType.EXISTS, new BooleanOutput<>(StringCodec.UTF8));
        Command<String, String, Boolean> exists = new Command<>(CommandType.EXISTS, new BooleanOutput<>(StringCodec.UTF8));

        output.add(added);
        output.add(exists);

        output.multi(2);
        output.set(true);
        output.complete(1);
        output.set(false);
        output.complete(1);

        assertThat(added.getOutput().get()).isTrue();
        assertThat(exists.getOutput().get()).isFalse();
        assertThat(output.get()).containsExactly(true, false);
    }

    @Test
    void shouldReportErrorForCommand() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        Command<String, String, String> command = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8));

        output.add(command);

        output.multi(1);
        output.setError(ByteBuffer.wrap("Fail".getBytes()));
        output.complete(1);

        assertThat(command.getOutput().getError()).isEqualTo("Fail");
        assertThat(output.getError()).isNull();
    }

    @Test
    void shouldFailMulti() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        Command<String, String, String> command = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8));

        output.add(command);

        output.setError(ByteBuffer.wrap("Fail".getBytes()));
        output.complete(0);

        assertThat(command.getOutput().getError()).isNull();
        assertThat(output.getError()).isEqualTo("Fail");
    }

    @Test
    void shouldForwardMultiMapToCommand() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        RecordingOutput inner = new RecordingOutput(StringCodec.UTF8);
        Command<String, String, String> command = new Command<>(CommandType.EVAL, inner);

        output.add(command);

        output.multi(1);
        output.multiMap(1);
        output.set(ByteBuffer.wrap("name".getBytes()));
        output.set(ByteBuffer.wrap("lettuce".getBytes()));
        output.complete(1);

        assertThat(inner.events).contains("multiMap:1");
        assertThat(inner.events).doesNotContain("multi:2");
    }

    @Test
    void shouldForwardMultiArrayMultiPushAndMultiSetToCommand() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        RecordingOutput inner = new RecordingOutput(StringCodec.UTF8);
        Command<String, String, String> command = new Command<>(CommandType.EVAL, inner);

        output.add(command);

        output.multi(1);
        output.multiArray(2);
        output.multiPush(3);
        output.multiSet(4);
        output.complete(1);

        assertThat(inner.events).contains("multiArray:2", "multiPush:3", "multiSet:4");
        assertThat(inner.events).doesNotContain("multi:3", "multi:4");
    }

    @Test
    void shouldKeepClientErrorOnTransactionWhileDecodingCommand() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        RecordingOutput inner = new RecordingOutput(StringCodec.UTF8);
        Command<String, String, String> command = new Command<>(CommandType.EVAL, inner);

        output.add(command);

        output.multi(1);
        output.setError("Command timed out");

        assertThat(output.getError()).isEqualTo("Command timed out");
        assertThat(inner.events).isEmpty();
    }

    @Test
    void shouldKeepStringErrorWhenCommandQueueIsEmpty() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);

        output.multi(1);

        output.setError("EXECABORT");

        assertThat(output.hasError()).isTrue();
        assertThat(output.getError()).isEqualTo("EXECABORT");
    }

    @Test
    void shouldKeepStringErrorWhenTransactionWasDiscarded() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);

        output.set(null);

        output.setError("EXECABORT");

        assertThat(output.hasError()).isTrue();
        assertThat(output.getError()).isEqualTo("EXECABORT");
    }

    @Test
    void shouldKeepTransactionLevelStringError() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);

        output.setError("EXECABORT");

        assertThat(output.hasError()).isTrue();
        assertThat(output.getError()).isEqualTo("EXECABORT");
    }

    @Test
    void shouldForwardByteBufferErrorToErrorTolerantOutput() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        ErrorTolerantBooleanListOutput<String, String> errorTolerantOutput = new ErrorTolerantBooleanListOutput<>(
                StringCodec.UTF8);
        Command<String, String, List<Boolean>> command = new Command<>(CommandType.BF_MADD, errorTolerantOutput);

        output.add(command);

        // Transaction header: 1 command in EXEC reply
        output.multi(1);

        // Command returns an array of 2 elements (e.g. BF.MADD: 1 success, 1 error)
        output.multi(2);
        output.set(1);
        output.setError(ByteBuffer.wrap("ERR item already exists".getBytes()));
        output.complete(1);

        assertThat(command.getOutput().hasError()).isFalse();
        assertThat(command.getOutput().get()).containsExactly(true, null);
        assertThat(output.hasError()).isFalse();
        assertThat(output.get()).hasSize(1);
    }

    @Test
    void shouldForwardByteBufferErrorToErrorTolerantValueOutput() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        ErrorTolerantBooleanValueListOutput<String, String> errorTolerantOutput = new ErrorTolerantBooleanValueListOutput<>(
                StringCodec.UTF8);
        Command<String, String, List<Value<Boolean>>> command = new Command<>(CommandType.BF_MADD, errorTolerantOutput);

        output.add(command);

        // Transaction header: 1 command in EXEC reply
        output.multi(1);

        // Command returns an array of 2 elements (e.g. BF.MADD: 1 success, 1 error)
        output.multi(2);
        output.set(1);
        output.setError(ByteBuffer.wrap("ERR item already exists".getBytes()));
        output.complete(1);

        assertThat(command.getOutput().hasError()).isFalse();
        assertThat(command.getOutput().get()).containsExactly(Value.fromNullable(true), Value.empty());
        assertThat(output.hasError()).isFalse();
        assertThat(output.get()).hasSize(1);
    }

    /**
     * Records the decoder callbacks it receives, so tests can assert on the exact callback instead of only the resulting value.
     */
    private static class RecordingOutput<K, V> extends CommandOutput<K, V, String> {

        final List<String> events = new ArrayList<>();

        RecordingOutput(RedisCodec<K, V> codec) {
            super(codec, null);
        }

        @Override
        public void set(ByteBuffer bytes) {
            events.add("set:" + decodeString(bytes));
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
        public void setError(String error) {
            events.add("setError:" + error);
            super.setError(error);
        }

        @Override
        public String get() {
            return String.join(",", events);
        }

    }

}
