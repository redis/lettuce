package io.lettuce.core;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link CopyArgs}.
 *
 * @author Mark Paluch
 */
class CopyArgsUnitTests {

    @Test
    void shouldRenderFullArgs() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        CopyArgs.Builder.destinationDb(1).replace(true).build(args);

        assertThat(args.toCommandString()).isEqualTo("DB 1 REPLACE");
    }

    @Test
    void shouldRenderNoArgs() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        CopyArgs.Builder.replace(false).build(args);

        assertThat(args.toCommandString()).isEmpty();
    }

}
