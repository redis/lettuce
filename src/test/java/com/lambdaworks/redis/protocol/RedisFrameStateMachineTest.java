package com.lambdaworks.redis.protocol;

import com.lambdaworks.redis.GeoCoordinates;
import com.lambdaworks.redis.GeoWithin;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.redis.RedisMessageType;
import io.netty.handler.codec.redis.RedisFrame;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * @author Mark Paluch
 */
public class RedisFrameStateMachineTest {

    public static final Utf8StringCodec CODEC = new Utf8StringCodec();
    private RedisFrameStateMachine sut = new RedisFrameStateMachine();

    @Test
    public void simpleStatus() throws Exception {

        Command<String, String, String> command = newCommand(new StatusOutput<>(CODEC));

        boolean completed = sut.decode(simpleSegment("HELLO"), command.getOutput());

        assertThat(completed).isTrue();
        assertThat(command.getOutput().get()).isEqualTo("HELLO");
    }


    @Test
    public void error() throws Exception {

        Command<String, String, String> command = newCommand(new StatusOutput<>(CODEC));

        boolean completed = sut.decode(errorSegment("HELLO"), command.getOutput());

        assertThat(completed).isTrue();
        assertThat(command.getOutput().getError()).isEqualTo("HELLO");
    }

    @Test
    public void integer() throws Exception {

        Command<String, String, Long> command = newCommand(new IntegerOutput<>(
                CODEC));

        boolean completed = sut.decode(integerSegment(1), command.getOutput());

        assertThat(completed).isTrue();
        assertThat(command.getOutput().get()).isEqualTo(1L);
    }

    @Test
    public void bulkString() throws Exception {

        Command<String, String, String> command = newCommand(new ValueOutput<>(
                CODEC));
        
        boolean completed = sut.decode(bulkStringSegment("foo"), command.getOutput());

        assertThat(completed).isTrue();
        assertThat(command.getOutput().get()).isEqualTo("foo");
    }

    @Test
    public void nullResponse() throws Exception {

        Command<String, String, String> command = newCommand(new ValueOutput<>(
                CODEC));

        boolean completed = sut.decode(nullSegment(), command.getOutput());

        assertThat(completed).isTrue();
        assertThat(command.getOutput().get()).isNull();
    }

    @Test
    public void arrayOfSimpleStrings() throws Exception {

        Command<String, String, List<String>> command = newCommand(new ValueListOutput<>(CODEC));

        assertThat(sut.decode(arrayHeader(2), command.getOutput())).isFalse();
        assertThat(sut.decode(bulkStringSegment("hello"), command.getOutput())).isFalse();
        assertThat(sut.decode(bulkStringSegment("world"), command.getOutput())).isTrue();

        assertThat(command.getOutput().get()).containsExactly("hello", "world");
    }

    @Test
    public void nestedArrays() throws Exception {

        Command<String, String, List<Object>> command = newCommand(new NestedMultiOutput<>(CODEC));

        assertThat(sut.decode(arrayHeader(3), command.getOutput())).isFalse();
        assertThat(sut.decode(arrayHeader(2), command.getOutput())).isFalse();
        assertThat(sut.decode(bulkStringSegment("hello"), command.getOutput())).isFalse(); // nested array 1
        assertThat(sut.decode(bulkStringSegment("world"), command.getOutput())).isFalse(); // nested array 1

        assertThat(sut.decode(bulkStringSegment("foo"), command.getOutput())).isFalse();
        assertThat(sut.decode(bulkStringSegment("bar"), command.getOutput())).isTrue();

        assertThat(command.getOutput().get()).hasSize(3);
        assertThat((List) command.getOutput().get().get(0)).contains("hello", "world");
        assertThat(command.getOutput().get().get(1)).isEqualTo("foo");
        assertThat(command.getOutput().get().get(2)).isEqualTo("bar");

    }

    @Test
    public void nestedArraysWithNulls() throws Exception {

        Command<String, String, List<Object>> command = newCommand(new NestedMultiOutput<>(CODEC));

        assertThat(sut.decode(arrayHeader(3), command.getOutput())).isFalse();
        assertThat(sut.decode(arrayHeader(2), command.getOutput())).isFalse();
        assertThat(sut.decode(bulkStringSegment("hello"), command.getOutput())).isFalse(); // nested array 1
        assertThat(sut.decode(bulkStringSegment("world"), command.getOutput())).isFalse(); // nested array 1

        assertThat(sut.decode(bulkStringSegment(""), command.getOutput())).isFalse();
        assertThat(sut.decode(bulkStringSegment(""), command.getOutput())).isTrue();

        assertThat(command.getOutput().get()).hasSize(3);
        assertThat((List) command.getOutput().get().get(0)).contains("hello", "world");
        assertThat(command.getOutput().get().get(1)).isEqualTo("");
        assertThat(command.getOutput().get().get(2)).isEqualTo("");

    }

    @Test
    public void geocoordinatesWithNulls() throws Exception {

        Command<String, String, List<GeoCoordinates>> command = newCommand(new GeoCoordinatesListOutput<>(CODEC));

        assertThat(sut.decode(arrayHeader(3), command.getOutput())).isFalse();
        assertThat(sut.decode(arrayHeader(2), command.getOutput())).isFalse();
        assertThat(sut.decode(bulkStringSegment("8.6638757586479187"), command.getOutput())).isFalse(); // nested array
                                                                                                                 // 1
        assertThat(sut.decode(bulkStringSegment("49.52825247787210117"), command.getOutput())).isFalse(); // nested
                                                                                                                   // array 1

        assertThat(sut.decode(arrayHeader(-1), command.getOutput())).isFalse();
        assertThat(sut.decode(arrayHeader(-1), command.getOutput())).isTrue();

        assertThat(command.getOutput().get()).hasSize(3);
        assertThat(command.getOutput().get().get(0).x.doubleValue()).isCloseTo(8.6638757586479187, offset(0.001));
        assertThat(command.getOutput().get().get(1)).isNull();
        assertThat(command.getOutput().get().get(2)).isNull();

    }

    @Test
    public void georadiusWithArgs() throws Exception {

        GeoWithinListOutput<String, String> output = new GeoWithinListOutput<>(CODEC, true, true, false);
        Command<String, String, List<GeoWithin<String>>> command = newCommand(output);

        assertThat(sut.decode(arrayHeader(2), command.getOutput())).isFalse();
        assertThat(sut.decode(arrayHeader(3), command.getOutput())).isFalse();
        assertThat(sut.decode(bulkStringSegment("Weinheim"), command.getOutput())).isFalse(); // nested array 1
        assertThat(sut.decode(bulkStringSegment("2.7883"), command.getOutput())).isFalse(); // nested array 1
        assertThat(sut.decode(integerSegment(3666615932941099L), command.getOutput())).isFalse(); // nested array 1

        assertThat(sut.decode(arrayHeader(3), command.getOutput())).isFalse(); // nested array 2
        assertThat(sut.decode(bulkStringSegment("Bahn"), command.getOutput())).isFalse(); // nested array 1
        assertThat(sut.decode(bulkStringSegment("0.0000"), command.getOutput())).isFalse(); // nested array 1
        assertThat(sut.decode(integerSegment(3666616138513624L), command.getOutput())).isTrue(); // nested array 1

        assertThat(command.getOutput().get()).hasSize(2);

    }

    @Test
    public void georadiusWithoutArgs() throws Exception {

        GeoWithinListOutput<String, String> output = new GeoWithinListOutput<>(CODEC, true, true, false);
        Command<String, String, List<GeoWithin<String>>> command = newCommand(output);

        assertThat(sut.decode(arrayHeader(1), command.getOutput())).isFalse();
        assertThat(sut.decode(bulkStringSegment("Bahn"), command.getOutput())).isTrue(); // nested array 1

        assertThat(command.getOutput().get()).hasSize(1);

    }

    private RedisFrame arrayHeader(long l) {
        return new RedisFrame.ArrayHeader(l);
    }

    private RedisFrame nullSegment() {
        return RedisFrame.NullFrame.INSTANCE;
    }

    private RedisFrame simpleSegment(String s) {
        return new RedisFrame.ByteBufFrame(RedisMessageType.SIMPLE_STRING, byteBufOf(s));
    }

    private RedisFrame bulkStringSegment(String s) {
        return new RedisFrame.ByteBufFrame(RedisMessageType.BULK_STRING, byteBufOf(s));
    }

    private RedisFrame errorSegment(String s) {
        return new RedisFrame.ByteBufFrame(RedisMessageType.ERROR, byteBufOf(s));
    }

    private RedisFrame integerSegment(long l) {
        return new RedisFrame.IntegerFrame(l);
    }

    private static ByteBuf byteBufOf(String s) {
        return byteBufOf(s.getBytes());
    }

    private static ByteBuf byteBufOf(byte[] data) {
        return Unpooled.wrappedBuffer(data);
    }
    
    private <T> Command<String, String, T> newCommand(CommandOutput<String, String, T> output) {
        return new Command<>(CommandType.COMMAND,
                    output);
    }
}