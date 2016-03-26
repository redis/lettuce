package io.netty.handler.codec.redis;

import static io.netty.handler.codec.redis.RedisCodecTestUtil.byteBufOf;
import static io.netty.handler.codec.redis.RedisCodecTestUtil.bytesOf;
import static io.netty.handler.codec.redis.RedisCodecTestUtil.stringOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.IllegalReferenceCountException;

/**
 * @author Jongyeol Choi
 * @author Mark Paluch
 */
public class RedisDecoderTest {

    private EmbeddedChannel channel;

    @Before
    public void setup() throws Exception {
        channel = new EmbeddedChannel(new RedisDecoder());
    }

    @After
    public void teardown() throws Exception {
        channel.finish();
    }

    @Test
    public void shouldDecodeSimpleString() {
        byte[] content = bytesOf("OK");
        channel.writeInbound(byteBufOf("+"));
        channel.writeInbound(byteBufOf(content));
        channel.writeInbound(byteBufOf("\r\n"));

        RedisFrame.ByteBufFrame segment = channel.readInbound();

        assertThat(segment.type(), is(RedisMessageType.SIMPLE_STRING));
        assertThat(bytesOf(segment.content()), is(content));

        segment.release();
    }

    @Test
    public void shouldDecodeError() {
        byte[] content = bytesOf("ERROR sample message");
        channel.writeInbound(byteBufOf("-"));
        channel.writeInbound(byteBufOf(content));
        channel.writeInbound(byteBufOf("\r\n"));

        RedisFrame.ByteBufFrame segment = channel.readInbound();

        assertThat(segment.type(), is(RedisMessageType.ERROR));
        assertThat(bytesOf(segment.content()), is(content));

        segment.release();
    }

    @Test
    public void shouldDecodeInteger() {
        byte[] content = bytesOf("1234");
        channel.writeInbound(byteBufOf(":"));
        channel.writeInbound(byteBufOf(content));
        channel.writeInbound(byteBufOf("\r\n"));

        RedisFrame.IntegerFrame segment = channel.readInbound();

        assertThat(segment.type(), is(RedisMessageType.INTEGER));
        assertThat(segment.content(), is(1234L));
    }

    @Test
    public void shouldDecodeBulkString() {
        byte[] content = bytesOf("bulk\nstring\ntest\n1234");
        channel.writeInbound(byteBufOf("$"));
        channel.writeInbound(byteBufOf(Integer.toString(content.length)));
        channel.writeInbound(byteBufOf("\r\n"));
        channel.writeInbound(byteBufOf(content));
        channel.writeInbound(byteBufOf("\r\n"));

        RedisFrame.ByteBufFrame segment = channel.readInbound();

        assertThat(segment.type(), is(RedisMessageType.BULK_STRING));
        assertThat(bytesOf(segment.content()), is(content));

        segment.release();
    }

    @Test
    public void shouldDecodeArrayHeader() {
        channel.writeInbound(byteBufOf("*"));
        channel.writeInbound(byteBufOf("1"));
        channel.writeInbound(byteBufOf("\r\n"));

        RedisFrame.ArrayHeader segment = channel.readInbound();

        assertThat(segment.type(), is(RedisMessageType.ARRAY));
        assertThat(segment.content(), is(1L));
    }

    @Test
    public void shouldDecodeEmptyBulkString() {
        byte[] content = bytesOf("");
        channel.writeInbound(byteBufOf("$"));
        channel.writeInbound(byteBufOf(Integer.toString(content.length)));
        channel.writeInbound(byteBufOf("\r"));
        channel.writeInbound(byteBufOf("\n"));
        channel.writeInbound(byteBufOf(content));
        channel.writeInbound(byteBufOf("\r"));
        channel.writeInbound(byteBufOf("\n"));

        RedisFrame.ByteBufFrame segment = channel.readInbound();

        assertThat(segment.type(), is(RedisMessageType.BULK_STRING));
        assertThat(bytesOf(segment.content()), is(content));

        segment.release();
    }

    @Test
    public void shouldDecodeNullBulkString() {
        channel.writeInbound(byteBufOf("$"));
        channel.writeInbound(byteBufOf(Integer.toString(-1)));
        channel.writeInbound(byteBufOf("\r\n"));

        RedisFrame segment = channel.readInbound();

        assertThat(segment.type(), is(RedisMessageType.BULK_STRING));
        assertThat(segment, is(RedisFrame.NullFrame.INSTANCE));
    }

    @Test
    public void shouldDecodeSimpleArray() throws Exception {
        channel.writeInbound(byteBufOf("*3\r\n"));
        channel.writeInbound(byteBufOf(":1234\r\n"));
        channel.writeInbound(byteBufOf("+simple\r\n"));
        channel.writeInbound(byteBufOf("-error\r\n"));

        RedisFrame.ArrayHeader array = channel.readInbound();

        assertThat(array.type(), is(RedisMessageType.ARRAY));
        assertThat(array.content(), is(equalTo(3L)));

        RedisFrame.IntegerFrame integerSegment = channel.readInbound();
        assertThat(integerSegment.type(), is(RedisMessageType.INTEGER));
        assertThat(integerSegment.content(), is(1234L));

        RedisFrame.ByteBufFrame simpleSegment = channel.readInbound();
        assertThat(simpleSegment.type(), is(RedisMessageType.SIMPLE_STRING));
        assertThat(stringOf(simpleSegment.content()), is("simple"));

        RedisFrame.ByteBufFrame errSegment = channel.readInbound();
        assertThat(errSegment.type(), is(RedisMessageType.ERROR));
        assertThat(stringOf(errSegment.content()), is("error"));

        assertThat(errSegment.refCnt(), is(equalTo(1)));
        assertThat(simpleSegment.content().refCnt(), is(equalTo(1)));

        simpleSegment.release();
        errSegment.release();
    }

    @Test
    public void shouldDecodeArrayWithEmptySubarrays() throws Exception {
        channel.writeInbound(byteBufOf("*2\r\n"));
        channel.writeInbound(byteBufOf("*0\r\n"));
        channel.writeInbound(byteBufOf("*-1\r\n"));

        RedisFrame.ArrayHeader array = channel.readInbound();

        assertThat(array.type(), is(RedisMessageType.ARRAY));
        assertThat(array.content(), is(equalTo(2L)));

        RedisFrame.ArrayHeader subarray1 = channel.readInbound();

        assertThat(subarray1.type(), is(RedisMessageType.ARRAY));
        assertThat(subarray1.content(), is(equalTo(0L)));

        RedisFrame.ArrayHeader subarray2 = channel.readInbound();

        assertThat(subarray2.type(), is(RedisMessageType.ARRAY));
        assertThat(subarray2.content(), is(equalTo(-1L)));

    }

    @Test
    public void shouldDecodeNestedArray() throws Exception {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(byteBufOf("*2\r\n"));
        buf.writeBytes(byteBufOf("*3\r\n:1\r\n:2\r\n:3\r\n"));
        buf.writeBytes(byteBufOf("*2\r\n+Foo\r\n-Bar\r\n"));
        channel.writeInbound(buf);

        RedisFrame.ArrayHeader outer = channel.readInbound();

        assertThat(outer.type(), is(RedisMessageType.ARRAY));
        assertThat(outer.content(), is(2L));

        RedisFrame.ArrayHeader nestedArray1 = channel.readInbound();
        assertThat(nestedArray1.type(), is(RedisMessageType.ARRAY));
        assertThat(nestedArray1.content(), is(3L));

        RedisFrame.IntegerFrame int1 = channel.readInbound();
        RedisFrame.IntegerFrame int2 = channel.readInbound();
        RedisFrame.IntegerFrame int3 = channel.readInbound();

        assertThat(int1.type(), is(RedisMessageType.INTEGER));
        assertThat(int1.content(), is(1L));
        assertThat(int2.type(), is(RedisMessageType.INTEGER));
        assertThat(int2.content(), is(2L));
        assertThat(int3.type(), is(RedisMessageType.INTEGER));
        assertThat(int3.content(), is(3L));

        RedisFrame.ArrayHeader nestedArray2 = channel.readInbound();
        assertThat(nestedArray2.type(), is(RedisMessageType.ARRAY));
        assertThat(nestedArray2.content(), is(2L));

        RedisFrame.ByteBufFrame simple = channel.readInbound();
        RedisFrame.ByteBufFrame err = channel.readInbound();

        assertThat(simple.type(), is(RedisMessageType.SIMPLE_STRING));
        assertThat(stringOf(simple.content()), is("Foo"));

        assertThat(err.type(), is(RedisMessageType.ERROR));
        assertThat(stringOf(err.content()), is("Bar"));

        simple.release();
        err.release();
    }

    @Test(expected = IllegalReferenceCountException.class)
    public void shouldErrorOnDoubleReleaseArrayReferenceCounted() throws Exception {
        byte[] content = bytesOf("bulk\nstring\ntest\n1234");
        channel.writeInbound(byteBufOf("$"));
        channel.writeInbound(byteBufOf(Integer.toString(content.length)));
        channel.writeInbound(byteBufOf("\r\n"));
        channel.writeInbound(byteBufOf(content));
        channel.writeInbound(byteBufOf("\r\n"));

        RedisFrame.ByteBufFrame segment = channel.readInbound();

        segment.release();
        segment.release();
    }

}