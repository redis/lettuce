package io.lettuce.core.pubsub;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.KeyListOutput;
import io.lettuce.core.output.MapOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.core.protocol.CommandType.*;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(UNIT_TEST)
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

        assertThat(command.getType()).isEqualTo(PUBLISH);
        assertThat(command.getArgs()).isInstanceOf(PubSubCommandArgs.class);
        assertThat(command.getArgs().toCommandString()).isEqualTo("key<channel> value<message payload>");
        assertThat(command.getOutput()).isInstanceOf(IntegerOutput.class);
    }

    @Test
    void pubsubChannels() {
        String pattern = "channelPattern";
        Command<String, String, List<String>> command = this.commandBuilder.pubsubChannels(pattern);

        assertThat(command.getType()).isEqualTo(PUBSUB);
        assertThat(command.getArgs()).isInstanceOf(PubSubCommandArgs.class);
        assertThat(command.getArgs().toCommandString()).isEqualTo("CHANNELS key<channelPattern>");
        assertThat(command.getOutput()).isInstanceOf(KeyListOutput.class);
    }

    @Test
    void pubsubNumsub() {
        String pattern = "channelPattern";
        Command<String, String, Map<String, Long>> command = this.commandBuilder.pubsubNumsub(pattern);

        assertThat(command.getType()).isEqualTo(PUBSUB);
        assertThat(command.getArgs()).isInstanceOf(PubSubCommandArgs.class);
        assertThat(command.getArgs().toCommandString()).isEqualTo("NUMSUB key<channelPattern>");
        assertThat(command.getOutput()).isInstanceOf(MapOutput.class);
    }

    @Test
    void pubsubShardChannels() {
        String pattern = "channelPattern";
        Command<String, String, List<String>> command = this.commandBuilder.pubsubShardChannels(pattern);

        assertThat(command.getType()).isEqualTo(PUBSUB);
        assertThat(command.getArgs()).isInstanceOf(PubSubCommandArgs.class);
        assertThat(command.getArgs().toCommandString()).isEqualTo("SHARDCHANNELS key<channelPattern>");
        assertThat(command.getOutput()).isInstanceOf(KeyListOutput.class);
    }

    @Test
    void pubsubShardNumsub() {
        String pattern = "channelPattern";
        Command<String, String, Map<String, Long>> command = this.commandBuilder.pubsubShardNumsub(pattern);

        assertThat(command.getType()).isEqualTo(PUBSUB);
        assertThat(command.getArgs()).isInstanceOf(PubSubCommandArgs.class);
        assertThat(command.getArgs().toCommandString()).isEqualTo("SHARDNUMSUB key<channelPattern>");
        assertThat(command.getOutput()).isInstanceOf(MapOutput.class);
    }

    @Test
    void psubscribe() {
        String pattern = "channelPattern";
        Command<String, String, String> command = this.commandBuilder.psubscribe(pattern);

        assertThat(command.getType()).isEqualTo(PSUBSCRIBE);
        assertThat(command.getArgs()).isInstanceOf(PubSubCommandArgs.class);
        assertThat(command.getArgs().toCommandString()).isEqualTo("key<channelPattern>");
        assertThat(command.getOutput()).isInstanceOf(PubSubOutput.class);
    }

    @Test
    void punsubscribe() {
        String pattern = "channelPattern";
        Command<String, String, String> command = this.commandBuilder.punsubscribe(pattern);

        assertThat(command.getType()).isEqualTo(PUNSUBSCRIBE);
        assertThat(command.getArgs()).isInstanceOf(PubSubCommandArgs.class);
        assertThat(command.getArgs().toCommandString()).isEqualTo("key<channelPattern>");
        assertThat(command.getOutput()).isInstanceOf(PubSubOutput.class);
    }

    @Test
    void subscribe() {
        String pattern = "channelPattern";
        Command<String, String, String> command = this.commandBuilder.subscribe(pattern);

        assertThat(command.getType()).isEqualTo(SUBSCRIBE);
        assertThat(command.getArgs()).isInstanceOf(PubSubCommandArgs.class);
        assertThat(command.getArgs().toCommandString()).isEqualTo("key<channelPattern>");
        assertThat(command.getOutput()).isInstanceOf(PubSubOutput.class);
    }

    @Test
    void unsubscribe() {
        String pattern = "channelPattern";
        Command<String, String, String> command = this.commandBuilder.unsubscribe(pattern);

        assertThat(command.getType()).isEqualTo(UNSUBSCRIBE);
        assertThat(command.getArgs()).isInstanceOf(PubSubCommandArgs.class);
        assertThat(command.getArgs().toCommandString()).isEqualTo("key<channelPattern>");
        assertThat(command.getOutput()).isInstanceOf(PubSubOutput.class);
    }

    @Test
    void ssubscribe() {
        String channel = "channelPattern";
        Command<String, String, String> command = this.commandBuilder.ssubscribe(channel);

        assertThat(command.getType()).isEqualTo(SSUBSCRIBE);
        assertThat(command.getArgs()).isInstanceOf(CommandArgs.class);
        assertThat(command.getArgs().toCommandString()).isEqualTo("key<channelPattern>");
        assertThat(command.getOutput()).isInstanceOf(PubSubOutput.class);
    }

    @Test
    void sunsubscribe() {
        String channel = "channelPattern";
        Command<String, String, String> command = this.commandBuilder.sunsubscribe(channel);

        assertThat(command.getType()).isEqualTo(SUNSUBSCRIBE);
        assertThat(command.getArgs()).isInstanceOf(CommandArgs.class);
        assertThat(command.getArgs().toCommandString()).isEqualTo("key<channelPattern>");
        assertThat(command.getOutput()).isInstanceOf(PubSubOutput.class);
    }

}
