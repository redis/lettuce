package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Unit tests for {@link BlessScanArgs}.
 */
@Tag(UNIT_TEST)
class BlessScanArgsUnitTests {

    @Test
    void shouldEncodeCount() {

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        BlessScanArgs.Builder.count(10).build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("COUNT 10");
    }

    @Test
    void shouldEncodeNothingWhenUnset() {

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        new BlessScanArgs().build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEmpty();
    }

    @Test
    void fluentSetterReturnsSameInstance() {

        BlessScanArgs args = new BlessScanArgs();

        assertThat(args.count(5)).isSameAs(args);
    }

}
