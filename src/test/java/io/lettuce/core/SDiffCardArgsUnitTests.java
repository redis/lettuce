package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Unit tests for {@link SDiffCardArgs}.
 */
@Tag(UNIT_TEST)
class SDiffCardArgsUnitTests {

    @Test
    void shouldRenderLimit() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        SDiffCardArgs.Builder.limit(100).build(args);
        assertThat(args.toCommandString()).isEqualTo("LIMIT 100");
    }

    @Test
    void shouldRenderLimitZero() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        SDiffCardArgs.Builder.limit(0).build(args);
        assertThat(args.toCommandString()).isEqualTo("LIMIT 0");
    }

    @Test
    void shouldRejectNegativeLimit() {
        assertThatThrownBy(() -> SDiffCardArgs.Builder.limit(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRenderNoArgs() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        new SDiffCardArgs().build(args);
        assertThat(args.toCommandString()).isEmpty();
    }

}
