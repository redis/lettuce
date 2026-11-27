package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link XCfgSetArgs}.
 *
 * @author Aleksandar Todorov
 */
@Tag(UNIT_TEST)
class XCfgSetArgsUnitTests {

    @Test
    void shouldBuildIdmpDuration() {
        XCfgSetArgs args = XCfgSetArgs.Builder.idmpDuration(1000);

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        ByteBuf buf = Unpooled.buffer();
        commandArgs.encode(buf);
        String encoded = buf.toString(StandardCharsets.UTF_8);

        assertThat(encoded).contains("IDMP-DURATION");
        assertThat(encoded).contains("1000");
    }

    @Test
    void shouldBuildIdmpMaxsize() {
        XCfgSetArgs args = XCfgSetArgs.Builder.idmpMaxsize(500);

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        ByteBuf buf = Unpooled.buffer();
        commandArgs.encode(buf);
        String encoded = buf.toString(StandardCharsets.UTF_8);

        assertThat(encoded).contains("IDMP-MAXSIZE");
        assertThat(encoded).contains("500");
    }

    @Test
    void shouldBuildBothParameters() {
        XCfgSetArgs args = new XCfgSetArgs().idmpDuration(1000).idmpMaxsize(500);

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        ByteBuf buf = Unpooled.buffer();
        commandArgs.encode(buf);
        String encoded = buf.toString(StandardCharsets.UTF_8);

        assertThat(encoded).contains("IDMP-DURATION");
        assertThat(encoded).contains("1000");
        assertThat(encoded).contains("IDMP-MAXSIZE");
        assertThat(encoded).contains("500");
    }

    @Test
    void shouldRejectDurationTooLow() {
        assertThatThrownBy(() -> new XCfgSetArgs().idmpDuration(0)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IDMP-DURATION must be between 1 and 86400");
    }

    @Test
    void shouldRejectDurationTooHigh() {
        assertThatThrownBy(() -> new XCfgSetArgs().idmpDuration(86401)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IDMP-DURATION must be between 1 and 86400");
    }

    @Test
    void shouldRejectMaxsizeTooLow() {
        assertThatThrownBy(() -> new XCfgSetArgs().idmpMaxsize(0)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IDMP-MAXSIZE must be between 1 and 10000");
    }

    @Test
    void shouldRejectMaxsizeTooHigh() {
        assertThatThrownBy(() -> new XCfgSetArgs().idmpMaxsize(10001)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IDMP-MAXSIZE must be between 1 and 10000");
    }

    @Test
    void shouldAcceptMinimumValues() {
        XCfgSetArgs args = new XCfgSetArgs().idmpDuration(1).idmpMaxsize(1);

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        ByteBuf buf = Unpooled.buffer();
        commandArgs.encode(buf);
        String encoded = buf.toString(StandardCharsets.UTF_8);

        assertThat(encoded).contains("IDMP-DURATION");
        assertThat(encoded).contains("IDMP-MAXSIZE");
    }

    @Test
    void shouldAcceptMaximumValues() {
        XCfgSetArgs args = new XCfgSetArgs().idmpDuration(86400).idmpMaxsize(10000);

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        ByteBuf buf = Unpooled.buffer();
        commandArgs.encode(buf);
        String encoded = buf.toString(StandardCharsets.UTF_8);

        assertThat(encoded).contains("IDMP-DURATION");
        assertThat(encoded).contains("IDMP-MAXSIZE");
    }

    @Test
    void checkHashCodeIdmpDuration() {
        XCfgSetArgs firstParam = new XCfgSetArgs().idmpDuration(300);
        XCfgSetArgs secondParam = new XCfgSetArgs().idmpDuration(300);
        assertThat(firstParam.hashCode()).isEqualTo(secondParam.hashCode());
        assertThat(firstParam).isEqualTo(secondParam);
    }

    @Test
    void checkHashCodeIdmpMaxsize() {
        XCfgSetArgs firstParam = new XCfgSetArgs().idmpMaxsize(500);
        XCfgSetArgs secondParam = new XCfgSetArgs().idmpMaxsize(500);
        assertThat(firstParam.hashCode()).isEqualTo(secondParam.hashCode());
        assertThat(firstParam).isEqualTo(secondParam);
    }

    @Test
    void checkHashCodeBothParams() {
        XCfgSetArgs firstParam = new XCfgSetArgs().idmpDuration(300).idmpMaxsize(500);
        XCfgSetArgs secondParam = new XCfgSetArgs().idmpDuration(300).idmpMaxsize(500);
        assertThat(firstParam.hashCode()).isEqualTo(secondParam.hashCode());
        assertThat(firstParam).isEqualTo(secondParam);
    }

    @Test
    void checkHashCodeDifferentDuration() {
        XCfgSetArgs firstParam = new XCfgSetArgs().idmpDuration(300);
        XCfgSetArgs secondParam = new XCfgSetArgs().idmpDuration(400);
        assertThat(firstParam.hashCode()).isNotEqualTo(secondParam.hashCode());
        assertThat(firstParam).isNotEqualTo(secondParam);
    }

    @Test
    void checkHashCodeDifferentMaxsize() {
        XCfgSetArgs firstParam = new XCfgSetArgs().idmpMaxsize(500);
        XCfgSetArgs secondParam = new XCfgSetArgs().idmpMaxsize(600);
        assertThat(firstParam.hashCode()).isNotEqualTo(secondParam.hashCode());
        assertThat(firstParam).isNotEqualTo(secondParam);
    }

}
