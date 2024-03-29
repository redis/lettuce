package io.lettuce.core.pubsub;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.KeyListOutput;
import io.lettuce.core.output.MapOutput;
import io.lettuce.core.protocol.Command;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.lettuce.core.protocol.CommandType.*;
import static org.junit.jupiter.api.Assertions.*;

class PubSubCommandBuilderUnitTests {

    private PubSubCommandBuilder<String, String> commandBuilder;
    private final RedisCodec<String, String> codec = StringCodec.UTF8;

    @BeforeEach
    void setup() {
        this.commandBuilder = new PubSubCommandBuilder<>(codec);
    }

    @Test
    void publish() {
        String channel = "channel";
        String message = "message payload";
        Command<String, String, Long> command = this.commandBuilder.publish(channel, message);

        assertEquals( PUBLISH, command.getType());
        assertInstanceOf(PubSubCommandArgs.class, command.getArgs());
        assertEquals( "key<channel> value<message payload>",  command.getArgs().toCommandString());
        assertInstanceOf(IntegerOutput.class, command.getOutput());
    }

    @Test
    void pubsubChannels() {
        String pattern = "channelPattern";
        Command<String, String, List<String>> command = this.commandBuilder.pubsubChannels(pattern);

        assertEquals( PUBSUB, command.getType());
        assertInstanceOf(PubSubCommandArgs.class, command.getArgs());
        assertEquals( "CHANNELS key<channelPattern>",  command.getArgs().toCommandString());
        assertInstanceOf(KeyListOutput.class, command.getOutput());
    }

    @Test
    void pubsubNumsub() {
        String pattern = "channelPattern";
        Command<String, String, Map<String, Long>> command = this.commandBuilder.pubsubNumsub(pattern);

        assertEquals( PUBSUB, command.getType());
        assertInstanceOf(PubSubCommandArgs.class, command.getArgs());
        assertEquals( "NUMSUB key<channelPattern>",  command.getArgs().toCommandString());
        assertInstanceOf(MapOutput.class, command.getOutput());
    }

    @Test
    void pubsubShardChannels() {
        String pattern = "channelPattern";
        Command<String, String, List<String>> command = this.commandBuilder.pubsubShardChannels(pattern);

        assertEquals( PUBSUB, command.getType());
        assertInstanceOf(PubSubCommandArgs.class, command.getArgs());
        assertEquals( "SHARDCHANNELS key<channelPattern>",  command.getArgs().toCommandString());
        assertInstanceOf(KeyListOutput.class, command.getOutput());
    }

    @Test
    void pubsubShardNumsub() {
        String pattern = "channelPattern";
        Command<String, String, Map<String, Long>> command = this.commandBuilder.pubsubShardNumsub(pattern);

        assertEquals( PUBSUB, command.getType());
        assertInstanceOf(PubSubCommandArgs.class, command.getArgs());
        assertEquals( "SHARDNUMSUB key<channelPattern>",  command.getArgs().toCommandString());
        assertInstanceOf(MapOutput.class, command.getOutput());
    }

    @Test
    void psubscribe() {
        String pattern = "channelPattern";
        Command<String, String, String> command = this.commandBuilder.psubscribe(pattern);

        assertEquals( PSUBSCRIBE, command.getType());
        assertInstanceOf(PubSubCommandArgs.class, command.getArgs());
        assertEquals( "key<channelPattern>",  command.getArgs().toCommandString());
        assertInstanceOf(PubSubOutput.class, command.getOutput());
    }

    @Test
    void punsubscribe() {
        String pattern = "channelPattern";
        Command<String, String, String> command = this.commandBuilder.punsubscribe(pattern);

        assertEquals( PUNSUBSCRIBE, command.getType());
        assertInstanceOf(PubSubCommandArgs.class, command.getArgs());
        assertEquals( "key<channelPattern>",  command.getArgs().toCommandString());
        assertInstanceOf(PubSubOutput.class, command.getOutput());
    }

    @Test
    void subscribe() {
        String pattern = "channelPattern";
        Command<String, String, String> command = this.commandBuilder.subscribe(pattern);

        assertEquals( SUBSCRIBE, command.getType());
        assertInstanceOf(PubSubCommandArgs.class, command.getArgs());
        assertEquals( "key<channelPattern>",  command.getArgs().toCommandString());
        assertInstanceOf(PubSubOutput.class, command.getOutput());
    }

    @Test
    void unsubscribe() {
        String pattern = "channelPattern";
        Command<String, String, String> command = this.commandBuilder.unsubscribe(pattern);

        assertEquals( UNSUBSCRIBE, command.getType());
        assertInstanceOf(PubSubCommandArgs.class, command.getArgs());
        assertEquals( "key<channelPattern>",  command.getArgs().toCommandString());
        assertInstanceOf(PubSubOutput.class, command.getOutput());
    }
}
