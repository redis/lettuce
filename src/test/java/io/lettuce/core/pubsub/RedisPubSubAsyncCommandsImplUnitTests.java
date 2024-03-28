package io.lettuce.core.pubsub;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.KeyListOutput;
import io.lettuce.core.output.MapOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.RedisCommand;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.ExecutionException;

import static io.lettuce.core.protocol.CommandType.*;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;

class RedisPubSubAsyncCommandsImplUnitTests {

    private RedisPubSubAsyncCommandsImpl<String,String> commands;
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
        AsyncCommand dispachedMock = mock (AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.psubscribe(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        Assertions.assertEquals( PSUBSCRIBE, capturedCommand.getValue().getType());
        assertInstanceOf(PubSubOutput.class, capturedCommand.getValue().getOutput());
        Assertions.assertEquals( "key<channelPattern>",  capturedCommand.getValue().getArgs().toCommandString());

        assertNotEquals(capturedCommand.getValue(), dispachedMock);
    }

    @Test
    void punsubscribe() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";
        AsyncCommand dispachedMock = mock (AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.punsubscribe(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        Assertions.assertEquals( PUNSUBSCRIBE, capturedCommand.getValue().getType());
        assertInstanceOf(PubSubOutput.class, capturedCommand.getValue().getOutput());
        Assertions.assertEquals( "key<channelPattern>",  capturedCommand.getValue().getArgs().toCommandString());

        assertNotEquals(capturedCommand.getValue(), dispachedMock);
    }

    @Test
    void subscribe() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";
        AsyncCommand dispachedMock = mock (AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.subscribe(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        Assertions.assertEquals( SUBSCRIBE, capturedCommand.getValue().getType());
        assertInstanceOf(PubSubOutput.class, capturedCommand.getValue().getOutput());
        Assertions.assertEquals( "key<channelPattern>",  capturedCommand.getValue().getArgs().toCommandString());

        assertNotEquals(capturedCommand.getValue(), dispachedMock);
    }

    @Test
    void unsubscribe() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";
        AsyncCommand dispachedMock = mock (AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.unsubscribe(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        Assertions.assertEquals( UNSUBSCRIBE, capturedCommand.getValue().getType());
        assertInstanceOf(PubSubOutput.class, capturedCommand.getValue().getOutput());
        Assertions.assertEquals( "key<channelPattern>",  capturedCommand.getValue().getArgs().toCommandString());

        assertNotEquals(capturedCommand.getValue(), dispachedMock);
    }

    @Test
    void publish() throws ExecutionException, InterruptedException {
        String channel = "acmeChannel";
        String message = "acmeMessage";

        AsyncCommand dispachedMock = mock (AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.publish(channel, message).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        Assertions.assertEquals( PUBLISH, capturedCommand.getValue().getType());
        assertInstanceOf(IntegerOutput.class, capturedCommand.getValue().getOutput());
        Assertions.assertEquals( "key<acmeChannel> value<acmeMessage>",  capturedCommand.getValue().getArgs().toCommandString());

        assertNotEquals(capturedCommand.getValue(), dispachedMock);
    }

    @Test
    void pubsubChannels() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";

        AsyncCommand dispachedMock = mock (AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.pubsubChannels(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        Assertions.assertEquals( PUBSUB, capturedCommand.getValue().getType());
        assertInstanceOf(KeyListOutput.class, capturedCommand.getValue().getOutput());
        Assertions.assertEquals( "CHANNELS key<channelPattern>",  capturedCommand.getValue().getArgs().toCommandString());

        assertNotEquals(capturedCommand.getValue(), dispachedMock);
    }

    @Test
    void pubsubNumsub() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";

        AsyncCommand dispachedMock = mock (AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.pubsubNumsub(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        Assertions.assertEquals( PUBSUB, capturedCommand.getValue().getType());
        assertInstanceOf(MapOutput.class, capturedCommand.getValue().getOutput());
        Assertions.assertEquals( "NUMSUB key<channelPattern>",  capturedCommand.getValue().getArgs().toCommandString());

        assertNotEquals(capturedCommand.getValue(), dispachedMock);
    }

    @Test
    void pubsubShardChannels() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";

        AsyncCommand dispachedMock = mock (AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.pubsubShardChannels(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        Assertions.assertEquals( PUBSUB, capturedCommand.getValue().getType());
        assertInstanceOf(KeyListOutput.class, capturedCommand.getValue().getOutput());
        Assertions.assertEquals( "SHARDCHANNELS key<channelPattern>",  capturedCommand.getValue().getArgs().toCommandString());

        assertNotEquals(capturedCommand.getValue(), dispachedMock);
    }

    @Test
    void pubsubShardNumsub() throws ExecutionException, InterruptedException {
        String pattern = "channelPattern";

        AsyncCommand dispachedMock = mock (AsyncCommand.class);
        when(mockedConnection.dispatch((RedisCommand<String, String, Object>) any())).thenReturn(dispachedMock);

        commands.pubsubShardNumsub(pattern).get();

        ArgumentCaptor<AsyncCommand> capturedCommand = ArgumentCaptor.forClass(AsyncCommand.class);;
        verify(mockedConnection).dispatch(capturedCommand.capture());

        Assertions.assertEquals( PUBSUB, capturedCommand.getValue().getType());
        assertInstanceOf(MapOutput.class, capturedCommand.getValue().getOutput());
        Assertions.assertEquals( "SHARDNUMSUB key<channelPattern>",  capturedCommand.getValue().getArgs().toCommandString());

        assertNotEquals(capturedCommand.getValue(), dispachedMock);
    }
}
