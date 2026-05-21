package io.lettuce.core;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(UNIT_TEST)
class IncrexArgsUnitTests {

    @Test
    void shouldRenderLboundUbound() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        IncrexArgs.Builder.lbound(0).ubound(100).build(args);
        assertThat(args.toCommandString()).isEqualTo("LBOUND 0 UBOUND 100");
    }

    @Test
    void shouldRenderSaturate() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        IncrexArgs.Builder.saturate().build(args);
        assertThat(args.toCommandString()).isEqualTo("SATURATE");
    }

    @Test
    void shouldRenderEx() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        IncrexArgs.Builder.ex(60).build(args);
        assertThat(args.toCommandString()).isEqualTo("EX 60");
    }

    @Test
    void shouldRenderPx() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        IncrexArgs.Builder.px(5000).build(args);
        assertThat(args.toCommandString()).isEqualTo("PX 5000");
    }

    @Test
    void shouldRenderExAt() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        IncrexArgs.Builder.exAt(1700000000).build(args);
        assertThat(args.toCommandString()).isEqualTo("EXAT 1700000000");
    }

    @Test
    void shouldRenderPxAt() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        IncrexArgs.Builder.pxAt(1700000000000L).build(args);
        assertThat(args.toCommandString()).isEqualTo("PXAT 1700000000000");
    }

    @Test
    void shouldRenderPersist() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        IncrexArgs.Builder.persist().build(args);
        assertThat(args.toCommandString()).isEqualTo("PERSIST");
    }

    @Test
    void shouldRenderEnx() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        IncrexArgs.Builder.ex(60).enx().build(args);
        assertThat(args.toCommandString()).isEqualTo("EX 60 ENX");
    }

    @Test
    void shouldRenderFullArgs() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        IncrexArgs.Builder.lbound(0).ubound(100).saturate().ex(60).enx().build(args);
        assertThat(args.toCommandString()).isEqualTo("LBOUND 0 UBOUND 100 SATURATE EX 60 ENX");
    }

    @Test
    void shouldRenderNoArgs() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        new IncrexArgs().build(args);
        assertThat(args.toCommandString()).isEmpty();
    }

    @Test
    void shouldRenderDoubleBounds() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        IncrexArgs.Builder.lbound(-1.5).ubound(9.5).build(args);
        assertThat(args.toCommandString()).isEqualTo("LBOUND -1.5 UBOUND 9.5");
    }

}
