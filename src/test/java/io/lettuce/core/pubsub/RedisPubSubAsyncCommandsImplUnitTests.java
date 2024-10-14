package io.lettuce.core.pubsub;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.KeyListOutput;
import io.lettuce.core.output.MapOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.RedisCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.ExecutionException;

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.core.protocol.CommandType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag(UNIT_TEST)
class RedisPubSubAsyncCommandsImplUnitTests {

    private RedisPubSubAsyncCommandsImpl<String, String> commands;

    private StatefulRedisPubSubConnection<String, String> mockedConnection;

    private final RedisCodec<String, String> codec = StringCodec.UTF8;

    @BeforeEach
    void setup() {
        mockedConnection = mock(StatefulRedisPubSubConnection.class);
        commands = new RedisPubSubAsyncCommandsImpl<>(mockedConnection, codec);
    }

    @Test
    void psubscribe() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";
        AsyncCommand dispachedMock = mock(AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.psubscribe(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);
        ;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        assertThat(capturedCommand.getValue().getType()).isEqualTo(PSUBSCRIBE);
        assertInstanceOf(PubSubOutput.class, capturedCommand.getValue().getOutput());
        assertThat(capturedCommand.getValue().getArgs().toCommandString()).isEqualTo("key<channelPattern>");

        assertThat(capturedCommand.getValue()).isNotEqualTo(dispachedMock);
    }

    @Test
    void punsubscribe() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";
        AsyncCommand dispachedMock = mock(AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.punsubscribe(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);
        ;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        assertThat(capturedCommand.getValue().getType()).isEqualTo(PUNSUBSCRIBE);
        assertInstanceOf(PubSubOutput.class, capturedCommand.getValue().getOutput());
        assertThat(capturedCommand.getValue().getArgs().toCommandString()).isEqualTo("key<channelPattern>");

        assertThat(capturedCommand.getValue()).isNotEqualTo(dispachedMock);
    }

    @Test
    void subscribe() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";
        AsyncCommand dispachedMock = mock(AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.subscribe(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);
        ;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        assertThat(capturedCommand.getValue().getType()).isEqualTo(SUBSCRIBE);
        assertInstanceOf(PubSubOutput.class, capturedCommand.getValue().getOutput());
        assertThat(capturedCommand.getValue().getArgs().toCommandString()).isEqualTo("key<channelPattern>");

        assertThat(capturedCommand.getValue()).isNotEqualTo(dispachedMock);
    }

    @Test
    void unsubscribe() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";
        AsyncCommand dispachedMock = mock(AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.unsubscribe(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);
        ;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        assertThat(capturedCommand.getValue().getType()).isEqualTo(UNSUBSCRIBE);
        assertInstanceOf(PubSubOutput.class, capturedCommand.getValue().getOutput());
        assertThat(capturedCommand.getValue().getArgs().toCommandString()).isEqualTo("key<channelPattern>");

        assertThat(capturedCommand.getValue()).isNotEqualTo(dispachedMock);
    }

    @Test
    void publish() throws ExecutionException, InterruptedException {
        String channel = "acmeChannel";
        String message = "acmeMessage";

        AsyncCommand dispachedMock = mock(AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.publish(channel, message).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);
        ;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        assertThat(capturedCommand.getValue().getType()).isEqualTo(PUBLISH);
        assertInstanceOf(IntegerOutput.class, capturedCommand.getValue().getOutput());
        assertThat(capturedCommand.getValue().getArgs().toCommandString()).isEqualTo("key<acmeChannel> value<acmeMessage>");

        assertThat(capturedCommand.getValue()).isNotEqualTo(dispachedMock);
    }

    @Test
    void pubsubChannels() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";

        AsyncCommand dispachedMock = mock(AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.pubsubChannels(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);
        ;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        assertThat(capturedCommand.getValue().getType()).isEqualTo(PUBSUB);
        assertInstanceOf(KeyListOutput.class, capturedCommand.getValue().getOutput());
        assertThat(capturedCommand.getValue().getArgs().toCommandString()).isEqualTo("CHANNELS key<channelPattern>");

        assertThat(capturedCommand.getValue()).isNotEqualTo(dispachedMock);
    }

    @Test
    void pubsubNumsub() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";

        AsyncCommand dispachedMock = mock(AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.pubsubNumsub(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);
        ;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        assertThat(capturedCommand.getValue().getType()).isEqualTo(PUBSUB);
        assertInstanceOf(MapOutput.class, capturedCommand.getValue().getOutput());
        assertThat(capturedCommand.getValue().getArgs().toCommandString()).isEqualTo("NUMSUB key<channelPattern>");

        assertThat(capturedCommand.getValue()).isNotEqualTo(dispachedMock);
    }

    @Test
    void pubsubShardChannels() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";

        AsyncCommand dispachedMock = mock(AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.pubsubShardChannels(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);
        ;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        assertThat(capturedCommand.getValue().getType()).isEqualTo(PUBSUB);
        assertInstanceOf(KeyListOutput.class, capturedCommand.getValue().getOutput());
        assertThat(capturedCommand.getValue().getArgs().toCommandString()).isEqualTo("SHARDCHANNELS key<channelPattern>");

        assertThat(capturedCommand.getValue()).isNotEqualTo(dispachedMock);
    }

    @Test
    void pubsubShardNumsub() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";

        AsyncCommand dispachedMock = mock(AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.pubsubShardNumsub(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);
        ;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        assertThat(capturedCommand.getValue().getType()).isEqualTo(PUBSUB);
        assertInstanceOf(MapOutput.class, capturedCommand.getValue().getOutput());
        assertThat(capturedCommand.getValue().getArgs().toCommandString()).isEqualTo("SHARDNUMSUB key<channelPattern>");

        assertThat(capturedCommand.getValue()).isNotEqualTo(dispachedMock);
    }

    @Test
    void ssubscribe() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";
        AsyncCommand dispachedMock = mock(AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.ssubscribe(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);

        verify(mockedConnection).dispatch(capturedCommand.capture());

        assertThat(capturedCommand.getValue().getType()).isEqualTo(SSUBSCRIBE);
        assertInstanceOf(PubSubOutput.class, capturedCommand.getValue().getOutput());
        assertThat(capturedCommand.getValue().getArgs().toCommandString()).isEqualTo("key<channelPattern>");

        assertThat(capturedCommand.getValue()).isNotEqualTo(dispachedMock);
    }

    @Test
    void sunsubscribe() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";
        AsyncCommand dispachedMock = mock(AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.sunsubscribe(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);

        verify(mockedConnection).dispatch(capturedCommand.capture());

        assertThat(capturedCommand.getValue().getType()).isEqualTo(SUNSUBSCRIBE);
        assertInstanceOf(PubSubOutput.class, capturedCommand.getValue().getOutput());
        assertThat(capturedCommand.getValue().getArgs().toCommandString()).isEqualTo("key<channelPattern>");

        assertThat(capturedCommand.getValue()).isNotEqualTo(dispachedMock);
    }

}
