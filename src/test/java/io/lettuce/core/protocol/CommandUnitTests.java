package io.lettuce.core.protocol;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisException;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.StatusOutput;

/**
 * Unit test for {@link Command}.
 *
 * @author Will Glozer
 * @author Mark Paluch
 */
public class CommandUnitTests {

    private Command<String, String, String> sut;

    @BeforeEach
    void createCommand() {

        CommandOutput<String, String, String> output = new StatusOutput<>(StringCodec.UTF8);
        sut = new Command<>(CommandType.INFO, output, null);
    }

    @Test
    void isCancelled() {
        assertThat(sut.isCancelled()).isFalse();
        assertThat(sut.isDone()).isFalse();

        sut.cancel();

        assertThat(sut.isCancelled()).isTrue();
        assertThat(sut.isDone()).isTrue();

        sut.cancel();
    }

    @Test
    void isDone() {
        assertThat(sut.isCancelled()).isFalse();
        assertThat(sut.isDone()).isFalse();

        sut.complete();

        assertThat(sut.isCancelled()).isFalse();
        assertThat(sut.isDone()).isTrue();
    }

    @Test
    void isDoneExceptionally() {

        sut.completeExceptionally(new IllegalStateException());

        assertThat(sut.isCancelled()).isFalse();
        assertThat(sut.isDone()).isTrue();
    }

    @Test
    void get() {
        assertThat(sut.get()).isNull();
        sut.getOutput().set(StandardCharsets.US_ASCII.encode("one"));
        assertThat(sut.get()).isEqualTo("one");
    }

    @Test
    void getError() {
        sut.getOutput().setError("error");
        assertThat(sut.getError()).isEqualTo("error");
    }

    @Test
    void setOutputAfterCompleted() {
        sut.complete();
        assertThatThrownBy(() -> sut.setOutput(new StatusOutput<>(StringCodec.UTF8))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testToString() {
        assertThat(sut.toString()).contains("Command");
    }

    @Test
    void customKeyword() {

        sut = new Command<>(MyKeywords.DUMMY, null, null);
        sut.setOutput(new StatusOutput<>(StringCodec.UTF8));

        assertThat(sut.toString()).contains(MyKeywords.DUMMY.name());
    }

    @Test
    void customKeywordWithArgs() {
        sut = new Command<>(MyKeywords.DUMMY, null, new CommandArgs<>(StringCodec.UTF8));
        sut.getArgs().add(MyKeywords.DUMMY);
        assertThat(sut.getArgs().toString()).contains(MyKeywords.DUMMY.name());
    }

    @Test
    void getWithTimeout() {
        sut.getOutput().set(StandardCharsets.US_ASCII.encode("one"));
        sut.complete();

        assertThat(sut.get()).isEqualTo("one");
    }

    @Test
    void outputSubclassOverride1() {
        CommandOutput<String, String, String> output = new CommandOutput<String, String, String>(StringCodec.UTF8, null) {

            @Override
            public String get() throws RedisException {
                return null;
            }

        };
        assertThatThrownBy(() -> output.set(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void outputSubclassOverride2() {
        CommandOutput<String, String, String> output = new CommandOutput<String, String, String>(StringCodec.UTF8, null) {

            @Override
            public String get() throws RedisException {
                return null;
            }

        };
        assertThatThrownBy(() -> output.set(0)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void sillyTestsForEmmaCoverage() {
        assertThat(CommandType.valueOf("APPEND")).isEqualTo(CommandType.APPEND);
        assertThat(CommandKeyword.valueOf("AFTER")).isEqualTo(CommandKeyword.AFTER);
    }

    private enum MyKeywords implements ProtocolKeyword {

        DUMMY;

        @Override
        public byte[] getBytes() {
            return name().getBytes();
        }

    }

}
