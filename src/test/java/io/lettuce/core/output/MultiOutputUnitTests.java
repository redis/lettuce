package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;

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
    void shouldForwardMapToCommand() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        Command<String, String, Object> command = new Command<>(CommandType.EVAL, new ObjectOutput<>(StringCodec.UTF8));

        output.add(command);

        output.multi(1);
        output.multiMap(1);
        output.set(ByteBuffer.wrap("foo".getBytes()));
        output.set(ByteBuffer.wrap("bar".getBytes()));
        output.complete(1);

        assertThat(command.getOutput().get()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) command.getOutput().get();
        assertThat(map).containsEntry("foo", "bar");
        assertThat((Object) output.get().get(0)).isEqualTo(map);
    }

    @Test
    void shouldForwardSetToCommand() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        AtomicInteger multiSetCount = new AtomicInteger();
        CommandOutput<String, String, String> customOutput = new CommandOutput<String, String, String>(StringCodec.UTF8, null) {

            @Override
            public void multiSet(int count) {
                multiSetCount.set(count);
            }

        };
        Command<String, String, String> command = new Command<>(CommandType.SMEMBERS, customOutput);

        output.add(command);

        output.multi(1);
        output.multiSet(3);
        output.complete(1);

        assertThat(multiSetCount.get()).isEqualTo(3);
    }

    @Test
    void shouldForwardPushToCommand() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        AtomicInteger multiPushCount = new AtomicInteger();
        CommandOutput<String, String, String> customOutput = new CommandOutput<String, String, String>(StringCodec.UTF8, null) {

            @Override
            public void multiPush(int count) {
                multiPushCount.set(count);
            }

        };
        Command<String, String, String> command = new Command<>(CommandType.SUBSCRIBE, customOutput);

        output.add(command);

        output.multi(1);
        output.multiPush(2);
        output.complete(1);

        assertThat(multiPushCount.get()).isEqualTo(2);
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
    void shouldNotRecurseOnErrorWhenQueueIsEmpty() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        output.multi(0);

        output.setError("Error string");
        assertThat(output.getError()).isEqualTo("Error string");

        output.setError(ByteBuffer.wrap("Error buffer".getBytes()));
        assertThat(output.getError()).isEqualTo("Error buffer");
    }

}
