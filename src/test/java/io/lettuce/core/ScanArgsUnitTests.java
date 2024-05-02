package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 * @author Mark Paluch
 */
class ScanArgsUnitTests {

    @Test
    void shouldEncodeMatchUsingUtf8() {

        ScanArgs args = ScanArgs.Builder.matches("ö");

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("MATCH w7Y=");
    }

}
