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
import java.util.Collections;
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

}
