package io.lettuce.core;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.stream.StreamEntryDeletionResult;
import io.lettuce.core.protocol.Command;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    public static final String STREAM_KEY = "test-stream";

    public static final String GROUP_NAME = "test-group";

    public static final String MESSAGE_ID1 = "1234567890-0";

    public static final String MESSAGE_ID2 = "1234567891-0";

    public static final String MESSAGE_ID3 = "1234567892-0";

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
    void shouldCorrectlyConstructHgetdel() {

        Command<String, String, ?> command = sut.hgetdel(MY_KEY, "one", "two");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*6\r\n" + "$7\r\n" + "HGETDEL\r\n" + "$4\r\n" + "hKey\r\n"
                + "$6\r\n" + "FIELDS\r\n" + "$1\r\n" + "2\r\n" + "$3\r\n" + "one\r\n" + "$3\r\n" + "two\r\n");
    }

    @Test
    void shouldCorrectlyConstructHsetex() {
        Command<String, String, ?> command = sut.hsetex(MY_KEY, Collections.singletonMap("one", "1"));
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*6\r\n" + "$6\r\n" + "HSETEX\r\n" + "$4\r\n" + "hKey\r\n"
                + "$6\r\n" + "FIELDS\r\n" + "$1\r\n" + "1\r\n" + "$3\r\n" + "one\r\n" + "$1\r\n" + "1\r\n");
    }

    @Test
    void shouldCorrectlyConstructHsetexWithArgs() {
        Command<String, String, ?> command = sut.hsetex(MY_KEY, HSetExArgs.Builder.ex(Duration.ofSeconds(10)).fnx(),
                Collections.singletonMap("one", "1"));
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        String expected = "*9\r\n" + "$6\r\n" + "HSETEX\r\n" + "$4\r\n" + "hKey\r\n" + "$2\r\n" + "EX\r\n" + "$2\r\n" + "10\r\n"
                + "$3\r\n" + "FNX\r\n" + "$6\r\n" + "FIELDS\r\n" + "$1\r\n" + "1\r\n" + "$3\r\n" + "one\r\n" + "$1\r\n"
                + "1\r\n";
        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*9\r\n" + "$6\r\n" + "HSETEX\r\n" + "$4\r\n" + "hKey\r\n"
                + "$2\r\n" + "EX\r\n" + "$2\r\n" + "10\r\n" + "$3\r\n" + "FNX\r\n" + "$6\r\n" + "FIELDS\r\n" + "$1\r\n"
                + "1\r\n" + "$3\r\n" + "one\r\n" + "$1\r\n" + "1\r\n");
    }

    @Test
    void shouldCorrectlyConstructHgetex() {
        Command<String, String, ?> command = sut.hgetex(MY_KEY, "one");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*5\r\n" + "$6\r\n" + "HGETEX\r\n" + "$4\r\n" + "hKey\r\n"
                + "$6\r\n" + "FIELDS\r\n" + "$1\r\n" + "1\r\n" + "$3\r\n" + "one\r\n");
    }

    @Test
    void shouldCorrectlyConstructHgetexWithArgs() {
        Command<String, String, ?> command = sut.hgetex(MY_KEY, HGetExArgs.Builder.ex(Duration.ofSeconds(10)).persist(), "one");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(
                "*8\r\n" + "$6\r\n" + "HGETEX\r\n" + "$4\r\n" + "hKey\r\n" + "$2\r\n" + "EX\r\n" + "$2\r\n" + "10\r\n"
                        + "$7\r\n" + "PERSIST\r\n" + "$6\r\n" + "FIELDS\r\n" + "$1\r\n" + "1\r\n" + "$3\r\n" + "one\r\n");
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

    @Test
    void shouldCorrectlyConstructBitopDiff() {

        Command<String, String, ?> command = sut.bitopDiff("dest", "key1", "key2", "key3");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*6\r\n$5\r\nBITOP\r\n$4\r\nDIFF\r\n$4\r\ndest\r\n$4\r\nkey1\r\n$4\r\nkey2\r\n$4\r\nkey3\r\n");
    }

    @Test
    void shouldCorrectlyConstructBitopDiff1() {

        Command<String, String, ?> command = sut.bitopDiff1("dest", "key1", "key2", "key3");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*6\r\n$5\r\nBITOP\r\n$5\r\nDIFF1\r\n$4\r\ndest\r\n$4\r\nkey1\r\n$4\r\nkey2\r\n$4\r\nkey3\r\n");
    }

    @Test
    void shouldCorrectlyConstructBitopAndor() {

        Command<String, String, ?> command = sut.bitopAndor("dest", "key1", "key2", "key3");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*6\r\n$5\r\nBITOP\r\n$5\r\nANDOR\r\n$4\r\ndest\r\n$4\r\nkey1\r\n$4\r\nkey2\r\n$4\r\nkey3\r\n");
    }

    @Test
    void shouldCorrectlyConstructBitopOne() {

        Command<String, String, ?> command = sut.bitopOne("dest", "key1", "key2", "key3");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*6\r\n$5\r\nBITOP\r\n$3\r\nONE\r\n$4\r\ndest\r\n$4\r\nkey1\r\n$4\r\nkey2\r\n$4\r\nkey3\r\n");
    }

    @Test
    void shouldCorrectlyConstructXackdel() {
        Command<String, String, List<StreamEntryDeletionResult>> command = sut.xackdel(STREAM_KEY, GROUP_NAME,
                new String[] { MESSAGE_ID1, MESSAGE_ID2 });
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(
                "*7\r\n" + "$7\r\n" + "XACKDEL\r\n" + "$11\r\n" + "test-stream\r\n" + "$10\r\n" + "test-group\r\n" + "$3\r\n"
                        + "IDS\r\n" + "$1\r\n" + "2\r\n" + "$12\r\n" + "1234567890-0\r\n" + "$12\r\n" + "1234567891-0\r\n");
    }

    @Test
    void shouldCorrectlyConstructXackdelWithPolicy() {
        Command<String, String, List<StreamEntryDeletionResult>> command = sut.xackdel(STREAM_KEY, GROUP_NAME,
                StreamDeletionPolicy.KEEP_REFERENCES, new String[] { MESSAGE_ID1, MESSAGE_ID2, MESSAGE_ID3 });
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*9\r\n" + "$7\r\n" + "XACKDEL\r\n" + "$11\r\n"
                + "test-stream\r\n" + "$10\r\n" + "test-group\r\n" + "$7\r\n" + "KEEPREF\r\n" + "$3\r\n" + "IDS\r\n" + "$1\r\n"
                + "3\r\n" + "$12\r\n" + "1234567890-0\r\n" + "$12\r\n" + "1234567891-0\r\n" + "$12\r\n" + "1234567892-0\r\n");
    }

    @Test
    void shouldCorrectlyConstructXackdelWithDeleteReferencesPolicy() {
        Command<String, String, List<StreamEntryDeletionResult>> command = sut.xackdel(STREAM_KEY, GROUP_NAME,
                StreamDeletionPolicy.DELETE_REFERENCES, new String[] { MESSAGE_ID1 });
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*7\r\n" + "$7\r\n" + "XACKDEL\r\n" + "$11\r\n" + "test-stream\r\n" + "$10\r\n" + "test-group\r\n"
                        + "$6\r\n" + "DELREF\r\n" + "$3\r\n" + "IDS\r\n" + "$1\r\n" + "1\r\n" + "$12\r\n" + "1234567890-0\r\n");
    }

    @Test
    void shouldCorrectlyConstructXackdelWithAcknowledgedPolicy() {
        Command<String, String, List<StreamEntryDeletionResult>> command = sut.xackdel(STREAM_KEY, GROUP_NAME,
                StreamDeletionPolicy.ACKNOWLEDGED, new String[] { MESSAGE_ID1, MESSAGE_ID2 });
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*8\r\n" + "$7\r\n" + "XACKDEL\r\n" + "$11\r\n"
                + "test-stream\r\n" + "$10\r\n" + "test-group\r\n" + "$5\r\n" + "ACKED\r\n" + "$3\r\n" + "IDS\r\n" + "$1\r\n"
                + "2\r\n" + "$12\r\n" + "1234567890-0\r\n" + "$12\r\n" + "1234567891-0\r\n");
    }

    @Test
    void shouldCorrectlyConstructXdelex() {
        Command<String, String, List<StreamEntryDeletionResult>> command = sut.xdelex(STREAM_KEY,
                new String[] { MESSAGE_ID1, MESSAGE_ID2 });
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*6\r\n" + "$6\r\n" + "XDELEX\r\n" + "$11\r\n" + "test-stream\r\n" + "$3\r\n" + "IDS\r\n" + "$1\r\n"
                        + "2\r\n" + "$12\r\n" + "1234567890-0\r\n" + "$12\r\n" + "1234567891-0\r\n");
    }

    @Test
    void shouldCorrectlyConstructXdelexWithSingleMessageId() {
        Command<String, String, List<StreamEntryDeletionResult>> command = sut.xdelex(STREAM_KEY, new String[] { MESSAGE_ID1 });
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*5\r\n" + "$6\r\n" + "XDELEX\r\n" + "$11\r\n"
                + "test-stream\r\n" + "$3\r\n" + "IDS\r\n" + "$1\r\n" + "1\r\n" + "$12\r\n" + "1234567890-0\r\n");
    }

    @Test
    void shouldCorrectlyConstructXdelexWithPolicy() {
        Command<String, String, List<StreamEntryDeletionResult>> command = sut.xdelex(STREAM_KEY,
                StreamDeletionPolicy.KEEP_REFERENCES, new String[] { MESSAGE_ID1, MESSAGE_ID2, MESSAGE_ID3 });
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*8\r\n" + "$6\r\n" + "XDELEX\r\n" + "$11\r\n"
                + "test-stream\r\n" + "$7\r\n" + "KEEPREF\r\n" + "$3\r\n" + "IDS\r\n" + "$1\r\n" + "3\r\n" + "$12\r\n"
                + "1234567890-0\r\n" + "$12\r\n" + "1234567891-0\r\n" + "$12\r\n" + "1234567892-0\r\n");
    }

    @Test
    void shouldCorrectlyConstructXdelexWithDeleteReferencesPolicy() {
        Command<String, String, List<StreamEntryDeletionResult>> command = sut.xdelex(STREAM_KEY,
                StreamDeletionPolicy.DELETE_REFERENCES, new String[] { MESSAGE_ID1 });
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*6\r\n" + "$6\r\n" + "XDELEX\r\n" + "$11\r\n" + "test-stream\r\n" + "$6\r\n" + "DELREF\r\n"
                        + "$3\r\n" + "IDS\r\n" + "$1\r\n" + "1\r\n" + "$12\r\n" + "1234567890-0\r\n");
    }

    @Test
    void shouldCorrectlyConstructXdelexWithAcknowledgedPolicy() {
        Command<String, String, List<StreamEntryDeletionResult>> command = sut.xdelex(STREAM_KEY,
                StreamDeletionPolicy.ACKNOWLEDGED, new String[] { MESSAGE_ID1, MESSAGE_ID2 });
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo(
                "*7\r\n" + "$6\r\n" + "XDELEX\r\n" + "$11\r\n" + "test-stream\r\n" + "$5\r\n" + "ACKED\r\n" + "$3\r\n"
                        + "IDS\r\n" + "$1\r\n" + "2\r\n" + "$12\r\n" + "1234567890-0\r\n" + "$12\r\n" + "1234567891-0\r\n");
    }

    @Test
    void xackdelShouldRejectNullKey() {
        assertThatThrownBy(() -> sut.xackdel(null, GROUP_NAME, new String[] { MESSAGE_ID1 }))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Key must not be null");
    }

    @Test
    void xackdelShouldRejectNullGroup() {
        assertThatThrownBy(() -> sut.xackdel(STREAM_KEY, null, new String[] { MESSAGE_ID1 }))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Group must not be null");
    }

    @Test
    void xackdelShouldRejectEmptyMessageIds() {
        assertThatThrownBy(() -> sut.xackdel(STREAM_KEY, GROUP_NAME, new String[] {}))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("MessageIds must not be empty");
    }

    @Test
    void xackdelShouldRejectNullMessageIds() {
        assertThatThrownBy(() -> sut.xackdel(STREAM_KEY, GROUP_NAME, (String[]) null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("MessageIds must not be empty");
    }

    @Test
    void xackdelShouldRejectNullElementsInMessageIds() {
        assertThatThrownBy(() -> sut.xackdel(STREAM_KEY, GROUP_NAME, new String[] { MESSAGE_ID1, null, MESSAGE_ID2 }))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("MessageIds must not contain null elements");
    }

    @Test
    void xackdelWithPolicyShouldRejectNullKey() {
        assertThatThrownBy(
                () -> sut.xackdel(null, GROUP_NAME, StreamDeletionPolicy.KEEP_REFERENCES, new String[] { MESSAGE_ID1 }))
                        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Key must not be null");
    }

    @Test
    void xdelexShouldRejectNullKey() {
        assertThatThrownBy(() -> sut.xdelex(null, new String[] { MESSAGE_ID1 })).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Key must not be null");
    }

    @Test
    void xdelexShouldRejectEmptyMessageIds() {
        assertThatThrownBy(() -> sut.xdelex(STREAM_KEY, new String[] {})).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MessageIds must not be empty");
    }

    @Test
    void xdelexShouldRejectNullMessageIds() {
        assertThatThrownBy(() -> sut.xdelex(STREAM_KEY, (String[]) null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MessageIds must not be empty");
    }

    @Test
    void xdelexShouldRejectNullElementsInMessageIds() {
        assertThatThrownBy(() -> sut.xdelex(STREAM_KEY, new String[] { MESSAGE_ID1, null, MESSAGE_ID2 }))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("MessageIds must not contain null elements");
    }

    @Test
    void xdelexWithPolicyShouldRejectNullKey() {
        assertThatThrownBy(() -> sut.xdelex(null, StreamDeletionPolicy.KEEP_REFERENCES, new String[] { MESSAGE_ID1 }))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Key must not be null");
    }

    @Test
    void shouldCorrectlyConstructSetWithExAndIfeq() {
        Command<String, String, ?> command = sut.set("mykey", "myvalue",
                SetArgs.Builder.ex(100).compareCondition(CompareCondition.valueEq("oldvalue")));
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*7\r\n" + "$3\r\n" + "SET\r\n" + "$5\r\n" + "mykey\r\n" + "$7\r\n" + "myvalue\r\n" + "$2\r\n"
                        + "EX\r\n" + "$3\r\n" + "100\r\n" + "$4\r\n" + "IFEQ\r\n" + "$8\r\n" + "oldvalue\r\n");
    }

    @Test
    void shouldCorrectlyConstructSetWithExAtAndIfne() {
        Command<String, String, ?> command = sut.set("mykey", "myvalue",
                SetArgs.Builder.exAt(1234567890).compareCondition(CompareCondition.valueNe("wrongvalue")));
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8))
                .isEqualTo("*7\r\n" + "$3\r\n" + "SET\r\n" + "$5\r\n" + "mykey\r\n" + "$7\r\n" + "myvalue\r\n" + "$4\r\n"
                        + "EXAT\r\n" + "$10\r\n" + "1234567890\r\n" + "$4\r\n" + "IFNE\r\n" + "$10\r\n" + "wrongvalue\r\n");
    }

    @Test
    void shouldCorrectlyConstructSetGetWithPxAndIfdne() {
        Command<String, String, ?> command = sut.setGet("mykey", "myvalue",
                SetArgs.Builder.px(50000).compareCondition(CompareCondition.digestNe("0123456789abcdef")));
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*8\r\n" + "$3\r\n" + "SET\r\n" + "$5\r\n" + "mykey\r\n"
                + "$7\r\n" + "myvalue\r\n" + "$2\r\n" + "PX\r\n" + "$5\r\n" + "50000\r\n" + "$5\r\n" + "IFDNE\r\n" + "$16\r\n"
                + "0123456789abcdef\r\n" + "$3\r\n" + "GET\r\n");
    }

    @Test
    void shouldCorrectlyConstructSetGetWithPxAtAndIfdeq() {
        Command<String, String, ?> command = sut.setGet("mykey", "myvalue",
                SetArgs.Builder.pxAt(1234567890123L).compareCondition(CompareCondition.digestEq("fedcba9876543210")));
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*8\r\n" + "$3\r\n" + "SET\r\n" + "$5\r\n" + "mykey\r\n"
                + "$7\r\n" + "myvalue\r\n" + "$4\r\n" + "PXAT\r\n" + "$13\r\n" + "1234567890123\r\n" + "$5\r\n" + "IFDEQ\r\n"
                + "$16\r\n" + "fedcba9876543210\r\n" + "$3\r\n" + "GET\r\n");
    }

    @Test
    void shouldCorrectlyConstructDelexWithValueEq() {
        Command<String, String, ?> command = sut.delex("mykey", CompareCondition.valueEq("expectedvalue"));
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$5\r\n" + "DELEX\r\n" + "$5\r\n" + "mykey\r\n"
                + "$4\r\n" + "IFEQ\r\n" + "$13\r\n" + "expectedvalue\r\n");
    }

    @Test
    void shouldCorrectlyConstructDelexWithDigestNe() {
        Command<String, String, ?> command = sut.delex("mykey", CompareCondition.digestNe("0011223344556677"));
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*4\r\n" + "$5\r\n" + "DELEX\r\n" + "$5\r\n" + "mykey\r\n"
                + "$5\r\n" + "IFDNE\r\n" + "$16\r\n" + "0011223344556677\r\n");
    }

    @Test
    void shouldCorrectlyConstructDigestKey() {
        Command<String, String, ?> command = sut.digestKey("mykey");
        ByteBuf buf = Unpooled.directBuffer();
        command.encode(buf);

        assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("*2\r\n" + "$6\r\n" + "DIGEST\r\n" + "$5\r\n" + "mykey\r\n");
    }

    @Test
    void msetex_nxThenEx_seconds_emissionOrder() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("k", "v");
        MSetExArgs a = MSetExArgs.Builder.nx().ex(Duration.ofSeconds(5));

        Command<String, String, Boolean> cmd = sut.msetex(map, a);
        String s = cmd.getArgs().toCommandString();
        assertThat(s).isEqualTo("1 key<k> value<v> EX 5 NX");
    }

    @Test
    void msetex_xxThenKeepTtl_emissionOrder() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("k", "v");
        MSetExArgs a = MSetExArgs.Builder.xx().keepttl();

        Command<String, String, Boolean> cmd = sut.msetex(map, a);
        String s = cmd.getArgs().toCommandString();
        assertThat(s).isEqualTo("1 key<k> value<v> XX KEEPTTL");
    }

    @Test
    void msetex_noConditionThenPx_millis_emissionOrder() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("k", "v");
        MSetExArgs a = MSetExArgs.Builder.px(Duration.ofMillis(500));

        Command<String, String, Boolean> cmd = sut.msetex(map, a);
        String s = cmd.getArgs().toCommandString();
        assertThat(s).isEqualTo("1 key<k> value<v> PX 500");
    }

    @Test
    void msetex_noConditionThenPx_duration_emissionOrder() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("k", "v");
        MSetExArgs a = MSetExArgs.Builder.px(Duration.ofMillis(1234));

        Command<String, String, Boolean> cmd = sut.msetex(map, a);
        String s = cmd.getArgs().toCommandString();
        assertThat(s).isEqualTo("1 key<k> value<v> PX 1234");
    }

    @Test
    void msetex_exAt_withInstant_emissionOrder() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("k", "v");
        Instant t = Instant.ofEpochSecond(1_234_567_890L);
        MSetExArgs a = MSetExArgs.Builder.exAt(t);

        Command<String, String, Boolean> cmd = sut.msetex(map, a);
        String s = cmd.getArgs().toCommandString();
        assertThat(s).isEqualTo("1 key<k> value<v> EXAT 1234567890");
    }

    @Test
    void msetex_pxAt_withInstant_emissionOrder() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("k", "v");
        Instant t = Instant.ofEpochMilli(4_000L);
        MSetExArgs a = MSetExArgs.Builder.pxAt(t);

        Command<String, String, Boolean> cmd = sut.msetex(map, a);
        String s = cmd.getArgs().toCommandString();
        assertThat(s).isEqualTo("1 key<k> value<v> PXAT 4000");
    }

    @Test
    void msetex_exAt_withLong_emissionOrder() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("k", "v");
        long epochSeconds = 1_234_567_890L;
        MSetExArgs a = MSetExArgs.Builder.exAt(Instant.ofEpochSecond(epochSeconds));

        Command<String, String, Boolean> cmd = sut.msetex(map, a);
        String s = cmd.getArgs().toCommandString();
        assertThat(s).isEqualTo("1 key<k> value<v> EXAT 1234567890");
    }

    @Test
    void msetex_pxAt_withLong_emissionOrder() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("k", "v");
        long epochMillis = 4_567L;
        MSetExArgs a = MSetExArgs.Builder.pxAt(Instant.ofEpochMilli(epochMillis));

        Command<String, String, Boolean> cmd = sut.msetex(map, a);
        String s = cmd.getArgs().toCommandString();
        assertThat(s).isEqualTo("1 key<k> value<v> PXAT 4567");
    }

    @Test
    void msetex_nxThenExAt_emissionOrder() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("k", "v");
        Instant t = Instant.ofEpochSecond(42L);
        MSetExArgs a = MSetExArgs.Builder.nx().exAt(t);

        Command<String, String, Boolean> cmd = sut.msetex(map, a);
        String s = cmd.getArgs().toCommandString();
        assertThat(s).isEqualTo("1 key<k> value<v> EXAT 42 NX");
    }

    @Test
    void msetex_xxThenPxAt_emissionOrder() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("k", "v");
        Instant t = Instant.ofEpochMilli(314L);
        MSetExArgs a = MSetExArgs.Builder.xx().pxAt(t);

        Command<String, String, Boolean> cmd = sut.msetex(map, a);
        String s = cmd.getArgs().toCommandString();
        assertThat(s).isEqualTo("1 key<k> value<v> PXAT 314 XX");
    }

    @Test
    void msetex_exAt_withDate_emissionOrder() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("k", "v");
        Date ts = new Date(1_234_567_890L * 1000L);
        MSetExArgs a = MSetExArgs.Builder.exAt(ts.toInstant());

        Command<String, String, Boolean> cmd = sut.msetex(map, a);
        String s = cmd.getArgs().toCommandString();
        assertThat(s).isEqualTo("1 key<k> value<v> EXAT 1234567890");
    }

    @Test
    void msetex_pxAt_withDate_emissionOrder() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("k", "v");
        Date ts = new Date(9_999L);
        MSetExArgs a = MSetExArgs.Builder.pxAt(ts.toInstant());

        Command<String, String, Boolean> cmd = sut.msetex(map, a);
        String s = cmd.getArgs().toCommandString();
        assertThat(s).isEqualTo("1 key<k> value<v> PXAT 9999");
    }

    @Test
    void msetex_noCondition_noExpiration_onlyMapAndCount() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("k", "v");

        Command<String, String, Boolean> cmd = sut.msetex(map, null);
        String s = cmd.getArgs().toCommandString();
        assertThat(s).isEqualTo("1 key<k> value<v>");
    }

    @Test
    void msetex_twoPairs_keepttl_orderAndCount() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("k1", "v1");
        map.put("k2", "v2");
        MSetExArgs a = MSetExArgs.Builder.keepttl();

        Command<String, String, Boolean> cmd = sut.msetex(map, a);
        String s = cmd.getArgs().toCommandString();
        assertThat(s).isEqualTo("2 key<k1> value<v1> key<k2> value<v2> KEEPTTL");
    }

}
