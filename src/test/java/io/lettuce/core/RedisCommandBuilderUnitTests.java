package io.lettuce.core;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.Command;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RedisCommandBuilder}.
 *
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class RedisCommandBuilderUnitTests {

    public static final String MY_KEY = "hKey";

    public static final String MY_FIELD1 = "hField1";

    public static final String MY_FIELD2 = "hField2";

    public static final String MY_FIELD3 = "hField3";

    RedisCommandBuilder<String, String> sut = new RedisCommandBuilder<>(StringCodec.UTF8);

    @Test()
    void shouldCorrectlyConstructHello() {

        Command<String, String, ?> command = sut.hello(3, "日本語", "日本語".toCharArray(), null);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*5\r\n" + "$5\r\n" + "HELLO\r\n" + "$1\r\n" + "3\r\n"
                + "$4\r\n" + "AUTH\r\n" + "$9\r\n" + "日本語\r\n" + "$9\r\n" + "日本語\r\n");
    }

    @Test
    void shouldCorrectlyConstructXreadgroup() {

        Command<String, String, ?> command = sut.xreadgroup(Consumer.from("a", "b"), new XReadArgs(),
                XReadArgs.StreamOffset.latest("stream"));

        assertThat(Unpooled.wrappedBuffer(command.getArgs().getFirstEncodedKey()).toString(StandardCharsets.UTF_8))
                .isEqualTo("stream");
    }

    @Test
    void shouldCorrectlyConstructHexpire() {

        Command<String, String, ?> command = sut.hexpire(MY_KEY, 1, ExpireArgs.Builder.nx(), MY_FIELD1, MY_FIELD2, MY_FIELD3);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*9\r\n" + "$7\r\n" + "HEXPIRE\r\n" + "$4\r\n" + "hKey\r\n"
                + "$1\r\n" + "1\r\n" + "$2\r\n" + "NX\r\n" + "$6\r\n" + "FIELDS\r\n" + "$1\r\n" + "3\r\n" + "$7\r\n"
                + "hField1\r\n" + "$7\r\n" + "hField2\r\n" + "$7\r\n" + "hField3\r\n");
    }

    @Test
    void shouldCorrectlyConstructHexpireat() {

        Command<String, String, ?> command = sut.hexpireat(MY_KEY, 1, ExpireArgs.Builder.nx(), MY_FIELD1, MY_FIELD2, MY_FIELD3);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*9\r\n" + "$9\r\n" + "HEXPIREAT\r\n" + "$4\r\n" + "hKey\r\n"
                + "$1\r\n" + "1\r\n" + "$2\r\n" + "NX\r\n" + "$6\r\n" + "FIELDS\r\n" + "$1\r\n" + "3\r\n" + "$7\r\n"
                + "hField1\r\n" + "$7\r\n" + "hField2\r\n" + "$7\r\n" + "hField3\r\n");
    }

    @Test
    void shouldCorrectlyConstructHexpiretime() {

        Command<String, String, ?> command = sut.hexpiretime(MY_KEY, MY_FIELD1, MY_FIELD2, MY_FIELD3);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*7\r\n" + "$11\r\n" + "HEXPIRETIME\r\n" + "$4\r\n" + "hKey\r\n" + "$6\r\n" + "FIELDS\r\n" + "$1\r\n"
                        + "3\r\n" + "$7\r\n" + "hField1\r\n" + "$7\r\n" + "hField2\r\n" + "$7\r\n" + "hField3\r\n");
    }

    @Test
    void shouldCorrectlyConstructHpersist() {

        Command<String, String, ?> command = sut.hpersist(MY_KEY, MY_FIELD1, MY_FIELD2, MY_FIELD3);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*7\r\n" + "$8\r\n" + "HPERSIST\r\n" + "$4\r\n" + "hKey\r\n" + "$6\r\n" + "FIELDS\r\n" + "$1\r\n"
                        + "3\r\n" + "$7\r\n" + "hField1\r\n" + "$7\r\n" + "hField2\r\n" + "$7\r\n" + "hField3\r\n");
    }

    @Test
    void shouldCorrectlyConstructHpexpire() {

        Command<String, String, ?> command = sut.hpexpire(MY_KEY, 1, ExpireArgs.Builder.nx(), MY_FIELD1, MY_FIELD2, MY_FIELD3);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*9\r\n" + "$8\r\n" + "HPEXPIRE\r\n" + "$4\r\n" + "hKey\r\n"
                + "$1\r\n" + "1\r\n" + "$2\r\n" + "NX\r\n" + "$6\r\n" + "FIELDS\r\n" + "$1\r\n" + "3\r\n" + "$7\r\n"
                + "hField1\r\n" + "$7\r\n" + "hField2\r\n" + "$7\r\n" + "hField3\r\n");
    }

    @Test
    void shouldCorrectlyConstructHpexpireat() {

        Command<String, String, ?> command = sut.hpexpireat(MY_KEY, 1, ExpireArgs.Builder.nx(), MY_FIELD1, MY_FIELD2,
                MY_FIELD3);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*9\r\n" + "$10\r\n" + "HPEXPIREAT\r\n" + "$4\r\n"
                + "hKey\r\n" + "$1\r\n" + "1\r\n" + "$2\r\n" + "NX\r\n" + "$6\r\n" + "FIELDS\r\n" + "$1\r\n" + "3\r\n"
                + "$7\r\n" + "hField1\r\n" + "$7\r\n" + "hField2\r\n" + "$7\r\n" + "hField3\r\n");
    }

    @Test
    void shouldCorrectlyConstructHpexpiretime() {

        Command<String, String, ?> command = sut.hpexpiretime(MY_KEY, MY_FIELD1, MY_FIELD2, MY_FIELD3);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*7\r\n" + "$12\r\n" + "HPEXPIRETIME\r\n" + "$4\r\n" + "hKey\r\n" + "$6\r\n" + "FIELDS\r\n"
                        + "$1\r\n" + "3\r\n" + "$7\r\n" + "hField1\r\n" + "$7\r\n" + "hField2\r\n" + "$7\r\n" + "hField3\r\n");
    }

    @Test
    void shouldCorrectlyConstructHttl() {

        Command<String, String, ?> command = sut.httl(MY_KEY, MY_FIELD1, MY_FIELD2, MY_FIELD3);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*7\r\n" + "$4\r\n" + "HTTL\r\n" + "$4\r\n" + "hKey\r\n" + "$6\r\n" + "FIELDS\r\n" + "$1\r\n"
                        + "3\r\n" + "$7\r\n" + "hField1\r\n" + "$7\r\n" + "hField2\r\n" + "$7\r\n" + "hField3\r\n");
    }

    @Test
    void shouldCorrectlyConstructHpttl() {

        Command<String, String, ?> command = sut.hpttl(MY_KEY, MY_FIELD1, MY_FIELD2, MY_FIELD3);
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*7\r\n" + "$5\r\n" + "HPTTL\r\n" + "$4\r\n" + "hKey\r\n" + "$6\r\n" + "FIELDS\r\n" + "$1\r\n"
                        + "3\r\n" + "$7\r\n" + "hField1\r\n" + "$7\r\n" + "hField2\r\n" + "$7\r\n" + "hField3\r\n");
    }

    @Test
    void shouldCorrectlyConstructClientTrackinginfo() {

        Command<String, String, ?> command = sut.clientTrackinginfo();
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*2\r\n" + "$6\r\n" + "CLIENT\r\n" + "$12\r\n" + "TRACKINGINFO\r\n");
    }

    @Test
    void shouldCorrectlyConstructClusterMyshardid() {

        Command<String, String, ?> command = sut.clusterMyShardId();
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*2\r\n" + "$7\r\n" + "CLUSTER\r\n" + "$9\r\n" + "MYSHARDID\r\n");
    }

    @Test
    void shouldCorrectlyConstructClusterLinks() {

        Command<String, String, List<Map<String, Object>>> command = sut.clusterLinks();
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*2\r\n$7\r\nCLUSTER\r\n$5\r\nLINKS\r\n");
    }

}
