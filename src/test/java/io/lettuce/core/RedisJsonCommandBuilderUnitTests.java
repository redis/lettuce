package io.lettuce.core;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.json.JsonValue;
import io.lettuce.core.json.JsonParserRegistry;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.protocol.Command;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RedisJsonCommandBuilder}.
 *
 * @author Mark Paluch
 */
class RedisJsonCommandBuilderUnitTests {

    public static final String MY_KEY = "bikes:inventory";

    RedisJsonCommandBuilder<String, String> builder = new RedisJsonCommandBuilder<>(StringCodec.UTF8);

    @Test
    void shouldCorrectlyConstructJsonArrappend() {

        final JsonPath myPath = JsonPath.of("$..commuter_bikes");
        JsonValue<String, String> element = JsonParserRegistry.getJsonParser(StringCodec.UTF8).createJsonValue("{id:bike6}");
        Command<String, String, List<Long>> command = builder.jsonArrappend(MY_KEY, myPath, new JsonValue[] { element });
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$14\r\n" + "JSON.ARRAPPEND\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n" + "$10\r\n" + "{id:bike6}" + "\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonType() {

        final JsonPath myPath = JsonPath.of("$..commuter_bikes");

        Command<String, String, ?> command = builder.jsonType(MY_KEY, myPath);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*3\r\n" + "$9\r\n" + "JSON.TYPE\r\n" + "$15\r\n"
                + "bikes:inventory\r\n" + "$17\r\n" + "$..commuter_bikes\r\n");
    }

    @Test
    void shouldCorrectlyConstructJsonTypeRootPath() {

        final JsonPath myPath = JsonPath.ROOT_PATH;

        Command<String, String, ?> command = builder.jsonType(MY_KEY, myPath);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*2\r\n" + "$9\r\n" + "JSON.TYPE\r\n" + "$15\r\n" + "bikes:inventory\r\n");
    }

}
