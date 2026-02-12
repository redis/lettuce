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
 * Unit tests for {@link XAddArgs}.
 *
 * @author Aleksandar Todorov
 */
@Tag(UNIT_TEST)
class XAddArgsUnitTests {

    @Test
    void shouldBuildIdmpCommand() {
        byte[] producerId = "producer-1".getBytes(StandardCharsets.UTF_8);
        byte[] idempotentId = "msg-123".getBytes(StandardCharsets.UTF_8);

        XAddArgs args = XAddArgs.Builder.idmp(producerId, idempotentId);

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        ByteBuf buf = Unpooled.buffer();
        commandArgs.encode(buf);
        String encoded = buf.toString(StandardCharsets.UTF_8);

        assertThat(encoded).contains("IDMP");
        assertThat(encoded).contains("producer-1");
        assertThat(encoded).contains("msg-123");
    }

    @Test
    void shouldBuildIdmpAutoCommand() {
        byte[] producerId = "producer-1".getBytes(StandardCharsets.UTF_8);

        XAddArgs args = XAddArgs.Builder.idmpAuto(producerId);

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        ByteBuf buf = Unpooled.buffer();
        commandArgs.encode(buf);
        String encoded = buf.toString(StandardCharsets.UTF_8);

        assertThat(encoded).contains("IDMPAUTO");
        assertThat(encoded).contains("producer-1");
    }

    @Test
    void shouldEnforceCorrectArgumentOrder() {
        byte[] producerId = "producer-1".getBytes(StandardCharsets.UTF_8);
        byte[] idempotentId = "msg-123".getBytes(StandardCharsets.UTF_8);

        XAddArgs args = new XAddArgs().nomkstream().trimmingMode(StreamDeletionPolicy.KEEP_REFERENCES)
                .idmp(producerId, idempotentId).maxlen(100);

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        ByteBuf buf = Unpooled.buffer();
        commandArgs.encode(buf);
        String encoded = buf.toString(StandardCharsets.UTF_8);

        // Verify order: NOMKSTREAM → KEEPREF → IDMP → MAXLEN → *
        int nomkstreamPos = encoded.indexOf("NOMKSTREAM");
        int keeprefPos = encoded.indexOf("KEEPREF");
        int idmpPos = encoded.indexOf("IDMP");
        int maxlenPos = encoded.indexOf("MAXLEN");

        assertThat(nomkstreamPos).isLessThan(keeprefPos);
        assertThat(keeprefPos).isLessThan(idmpPos);
        assertThat(idmpPos).isLessThan(maxlenPos);
    }

    @Test
    void shouldRejectBothIdmpAndIdmpAuto() {
        byte[] producerId = "producer-1".getBytes(StandardCharsets.UTF_8);
        byte[] idempotentId = "msg-123".getBytes(StandardCharsets.UTF_8);

        XAddArgs args = new XAddArgs().idmp(producerId, idempotentId);

        assertThatThrownBy(() -> args.idmpAuto(producerId)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot use both IDMP and IDMPAUTO");
    }

    @Test
    void shouldRejectNullProducerId() {
        byte[] idempotentId = "msg-123".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> XAddArgs.Builder.idmp(null, idempotentId)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Producer ID must not be null");
    }

    @Test
    void shouldRejectNullIdempotentId() {
        byte[] producerId = "producer-1".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> XAddArgs.Builder.idmp(producerId, null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Idempotent ID must not be null");
    }

}
