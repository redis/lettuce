package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;

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

}
