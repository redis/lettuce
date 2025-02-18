package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import io.lettuce.core.json.DefaultJsonParser;
import io.lettuce.core.json.JsonArray;
import io.lettuce.core.json.JsonObject;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonValue;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolVersion;
import reactor.core.publisher.Mono;

/**
 * Unit tests for {@link ClientOptions}.
 *
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class ClientOptionsUnitTests {

    @Test
    void testNew() {
        checkAssertions(ClientOptions.create());
    }

    @Test
    void testDefault() {

        ClientOptions options = ClientOptions.builder().build();

        assertThat(options.getReadOnlyCommands().isReadOnly(new Command<>(CommandType.SET, null))).isFalse();
        assertThat(options.getReadOnlyCommands().isReadOnly(new Command<>(CommandType.PUBLISH, null))).isFalse();
        assertThat(options.getReadOnlyCommands().isReadOnly(new Command<>(CommandType.GET, null))).isTrue();
        assertThat(options.getJsonParser().get()).isInstanceOf(DefaultJsonParser.class);
    }

    @Test
    void testBuilder() {
        ClientOptions options = ClientOptions.builder().scriptCharset(StandardCharsets.US_ASCII).build();
        checkAssertions(options);
        assertThat(options.getScriptCharset()).isEqualTo(StandardCharsets.US_ASCII);
    }

    @Test
    void testCopy() {

        ClientOptions original = ClientOptions.builder().scriptCharset(StandardCharsets.US_ASCII).build();
        ClientOptions copy = ClientOptions.copyOf(original);

        checkAssertions(copy);
        assertThat(copy.getScriptCharset()).isEqualTo(StandardCharsets.US_ASCII);
        assertThat(copy.mutate().build().getScriptCharset()).isEqualTo(StandardCharsets.US_ASCII);

        assertThat(original.mutate()).isNotSameAs(copy.mutate());
    }

    @Test
    void jsonParser() {
        JsonParser parser = new CustomJsonParser();
        ClientOptions options = ClientOptions.builder().jsonParser(() -> parser).build();
        assertThat(options.getJsonParser().get()).isInstanceOf(CustomJsonParser.class);
    }

    static class CustomJsonParser implements JsonParser {

        @Override
        public JsonValue loadJsonValue(ByteBuffer buffer) {
            return null;
        }

        @Override
        public JsonValue createJsonValue(ByteBuffer bytes) {
            return null;
        }

        @Override
        public JsonValue createJsonValue(String value) {
            return null;
        }

        @Override
        public JsonObject createJsonObject() {
            return null;
        }

        @Override
        public JsonArray createJsonArray() {
            return null;
        }

        @Override
        public JsonValue fromObject(Object object) {
            return null;
        }

    }

    void checkAssertions(ClientOptions sut) {
        assertThat(sut.isAutoReconnect()).isTrue();
        assertThat(sut.isCancelCommandsOnReconnectFailure()).isFalse();
        assertThat(sut.getProtocolVersion()).isEqualTo(ProtocolVersion.RESP3);
        assertThat(sut.isSuspendReconnectOnProtocolFailure()).isFalse();
        assertThat(sut.getDisconnectedBehavior()).isEqualTo(ClientOptions.DisconnectedBehavior.DEFAULT);
    }

}
