package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Unit tests for {@link SUnionCardArgs}.
 */
@Tag(UNIT_TEST)
class SUnionCardArgsUnitTests {

    @Test
    void shouldRenderApprox() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        SUnionCardArgs.Builder.approx().build(args);
        assertThat(args.toCommandString()).isEqualTo("APPROX");
    }

    @Test
    void shouldRenderLimit() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        SUnionCardArgs.Builder.limit(1000).build(args);
        assertThat(args.toCommandString()).isEqualTo("LIMIT 1000");
    }

    @Test
    void shouldRenderLimitZero() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        SUnionCardArgs.Builder.limit(0).build(args);
        assertThat(args.toCommandString()).isEqualTo("LIMIT 0");
    }

    @Test
    void shouldRenderApproxBeforeLimit() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        SUnionCardArgs.Builder.limit(1000).approx().build(args);
        assertThat(args.toCommandString()).isEqualTo("APPROX LIMIT 1000");
    }

    @Test
    void shouldRejectNegativeLimit() {
        assertThatThrownBy(() -> SUnionCardArgs.Builder.limit(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRenderNoArgs() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        new SUnionCardArgs().build(args);
        assertThat(args.toCommandString()).isEmpty();
    }

}
