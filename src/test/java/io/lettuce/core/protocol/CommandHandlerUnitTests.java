/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.protocol;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.AdditionalMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.metrics.CommandLatencyCollector;
import io.lettuce.core.output.KeyValueListOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.output.ValueListOutput;
import io.lettuce.core.output.ValueOutput;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;
import io.lettuce.test.Delay;
import io.lettuce.test.ReflectionTestUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * Unit tests for {@link CommandHandler}.
 *
 * @author Mark Paluch
 * @author Jongyeol Choi
 * @author Gavin Cook
 * @author Shaphan
 */
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommandHandlerUnitTests {

    private Queue<RedisCommand<String, String, ?>> stack;

    private CommandHandler sut;

    private final Command<String, String, String> command = new Command<>(CommandType.APPEND,
            new StatusOutput<>(StringCodec.UTF8), null);

    @Mock
    private ChannelHandlerContext context;

    @Mock
    private Channel channel;

    @Mock
    private ChannelConfig config;

    @Mock
    private ChannelPipeline pipeline;

    @Mock
    private EventLoop eventLoop;

    @Mock
    private ClientResources clientResources;

    @Mock
    private Endpoint endpoint;

    @Mock
    private PushListener listener;

    @Mock
    private ChannelPromise promise;

    @Mock
    private CommandLatencyCollector latencyCollector;

    @BeforeAll
    static void beforeClass() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext();
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(CommandHandler.class.getName());
        loggerConfig.setLevel(Level.ALL);
    }

    @AfterAll
    static void afterClass() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext();
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(CommandHandler.class.getName());
        loggerConfig.setLevel(null);
    }

    @BeforeEach
    void before() throws Exception {

        when(context.channel()).thenReturn(channel);
        when(context.alloc()).thenReturn(ByteBufAllocator.DEFAULT);
        when(channel.pipeline()).thenReturn(pipeline);
        when(channel.eventLoop()).thenReturn(eventLoop);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress(Inet4Address.getLocalHost(), 1234));
        when(channel.localAddress()).thenReturn(new InetSocketAddress(Inet4Address.getLocalHost(), 1234));
        when(channel.config()).thenReturn(config);
        when(eventLoop.submit(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable r = (Runnable) invocation.getArguments()[0];
            r.run();
            return null;
        });

        when(latencyCollector.isEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(latencyCollector);
        when(clientResources.tracing()).thenReturn(Tracing.disabled());
        when(endpoint.getPushListeners()).thenReturn(Collections.singleton(listener));

        sut = new CommandHandler(ClientOptions.create(), clientResources, endpoint);
        stack = (Queue) ReflectionTestUtils.getField(sut, "stack");
    }

    @Test
    void testExceptionChannelActive() throws Exception {
        sut.setState(CommandHandler.LifecycleState.ACTIVE);

        sut.channelActive(context);
        sut.exceptionCaught(context, new Exception());
    }

    @Test
    void testIOExceptionChannelActive() throws Exception {
        sut.setState(CommandHandler.LifecycleState.ACTIVE);

        sut.channelActive(context);
        sut.exceptionCaught(context, new IOException("Connection timed out"));
    }

    @Test
    void testExceptionChannelInactive() throws Exception {
        sut.setState(CommandHandler.LifecycleState.DISCONNECTED);
        sut.exceptionCaught(context, new Exception());
        verify(context, never()).fireExceptionCaught(any(Exception.class));
    }

    @Test
    void testExceptionWithQueue() throws Exception {
        sut.setState(CommandHandler.LifecycleState.ACTIVE);
        stack.clear();

        sut.channelActive(context);

        stack.add(command);
        sut.exceptionCaught(context, new Exception());

        assertThat(stack).isEmpty();
        command.get();

        assertThat((Object) ReflectionTestUtils.getField(command, "exception")).isNotNull();
    }

    @Test
    void testExceptionWhenClosed() throws Exception {

        sut.setState(CommandHandler.LifecycleState.CLOSED);

        sut.exceptionCaught(context, new Exception());
        verifyNoInteractions(context);
    }

    @Test
    void isConnectedShouldReportFalseForNOT_CONNECTED() {

        sut.setState(CommandHandler.LifecycleState.NOT_CONNECTED);
        assertThat(sut.isConnected()).isFalse();
    }

    @Test
    void isConnectedShouldReportFalseForREGISTERED() {

        sut.setState(CommandHandler.LifecycleState.REGISTERED);
        assertThat(sut.isConnected()).isFalse();
    }

    @Test
    void isConnectedShouldReportTrueForCONNECTED() {

        sut.setState(CommandHandler.LifecycleState.CONNECTED);
        assertThat(sut.isConnected()).isTrue();
    }

    @Test
    void isConnectedShouldReportTrueForACTIVATING() {

        sut.setState(CommandHandler.LifecycleState.ACTIVATING);
        assertThat(sut.isConnected()).isTrue();
    }

    @Test
    void isConnectedShouldReportTrueForACTIVE() {

        sut.setState(CommandHandler.LifecycleState.ACTIVE);
        assertThat(sut.isConnected()).isTrue();
    }

    @Test
    void isConnectedShouldReportFalseForDISCONNECTED() {

        sut.setState(CommandHandler.LifecycleState.DISCONNECTED);
        assertThat(sut.isConnected()).isFalse();
    }

    @Test
    void isConnectedShouldReportFalseForDEACTIVATING() {

        sut.setState(CommandHandler.LifecycleState.DEACTIVATING);
        assertThat(sut.isConnected()).isFalse();
    }

    @Test
    void isConnectedShouldReportFalseForDEACTIVATED() {

        sut.setState(CommandHandler.LifecycleState.DEACTIVATED);
        assertThat(sut.isConnected()).isFalse();
    }

    @Test
    void isConnectedShouldReportFalseForCLOSED() {

        sut.setState(CommandHandler.LifecycleState.CLOSED);
        assertThat(sut.isConnected()).isFalse();
    }

    @Test
    void shouldNotWriteCancelledCommand() throws Exception {

        command.cancel();
        sut.write(context, command, promise);

        verifyNoInteractions(context);
        assertThat(stack).isEmpty();

        verify(promise).trySuccess();
    }

    @Test
    void shouldNotWriteCancelledCommands() throws Exception {

        command.cancel();
        sut.write(context, Collections.singleton(command), promise);

        verifyNoInteractions(context);
        assertThat(stack).isEmpty();

        verify(promise).trySuccess();
    }

    @Test
    void shouldCancelCommandOnQueueSingleFailure() throws Exception {

        Command<String, String, String> commandMock = mock(Command.class);

        RuntimeException exception = new RuntimeException();
        when(commandMock.getOutput()).thenThrow(exception);

        ChannelPromise channelPromise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        try {
            sut.write(context, commandMock, channelPromise);
            fail("Missing RuntimeException");
        } catch (RuntimeException e) {
            assertThat(e).isSameAs(exception);
        }

        assertThat(stack).isEmpty();
        verify(commandMock).completeExceptionally(exception);
    }

    @Test
    void shouldCancelCommandOnQueueBatchFailure() throws Exception {

        Command<String, String, String> commandMock = mock(Command.class);

        RuntimeException exception = new RuntimeException();
        when(commandMock.getOutput()).thenThrow(exception);

        ChannelPromise channelPromise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        try {
            sut.write(context, Arrays.asList(commandMock), channelPromise);
            fail("Missing RuntimeException");
        } catch (RuntimeException e) {
            assertThat(e).isSameAs(exception);
        }

        assertThat(stack).isEmpty();
        verify(commandMock).completeExceptionally(exception);
    }

    @Test
    void shouldFailOnDuplicateCommands() throws Exception {

        Command<String, String, String> commandMock = mock(Command.class);

        ChannelPromise channelPromise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        sut.write(context, Arrays.asList(commandMock, commandMock), channelPromise);

        assertThat(stack).isEmpty();
        verify(commandMock).completeExceptionally(any(RedisException.class));
    }

    @Test
    void shouldWriteActiveCommands() throws Exception {

        when(promise.isVoid()).thenReturn(true);

        sut.write(context, command, promise);

        verify(context).write(command, promise);
        assertThat(stack).hasSize(1).allMatch(o -> o instanceof LatencyMeteredCommand);
    }

    @Test
    void shouldConsiderQueueLimits() throws Exception {

        sut = new CommandHandler(ClientOptions.builder().requestQueueSize(1).build(), clientResources, endpoint);
        stack = (Queue) ReflectionTestUtils.getField(sut, "stack");

        when(promise.isVoid()).thenReturn(true);

        Command<String, String, String> command1 = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8),
                null);

        Command<String, String, String> command2 = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8),
                null);

        sut.write(context, command1, promise);
        assertThatThrownBy(() -> sut.write(context, command2, promise)).isInstanceOf(RedisException.class);

        verify(context).write(command1, promise);
        verify(context, never()).write(command2, promise);
    }

    @Test
    void shouldConsiderMaxQueueLimits() throws Exception {

        sut = new CommandHandler(ClientOptions.builder().requestQueueSize(Integer.MAX_VALUE - 1).build(), clientResources,
                endpoint);
        stack = (Queue) ReflectionTestUtils.getField(sut, "stack");

        when(promise.isVoid()).thenReturn(true);

        Command<String, String, String> command1 = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8),
                null);

        Command<String, String, String> command2 = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8),
                null);

        sut.write(context, command1, promise);
        sut.write(context, command2, promise);

        verify(context).write(command1, promise);
        verify(context).write(command2, promise);
    }

    @Test
    void shouldNotWriteCancelledCommandBatch() throws Exception {

        command.cancel();
        sut.write(context, Arrays.asList(command), promise);

        verifyNoInteractions(context);
        assertThat((Collection) ReflectionTestUtils.getField(sut, "stack")).isEmpty();
    }

    @Test
    void shouldWriteSingleActiveCommandsInBatch() throws Exception {

        List<Command<String, String, String>> commands = Arrays.asList(command);
        when(promise.isVoid()).thenReturn(true);
        sut.write(context, commands, promise);

        verify(context).write(command, promise);
        assertThat(stack).hasSize(1);
    }

    @Test
    void shouldWriteActiveCommandsInBatch() throws Exception {

        Command<String, String, String> anotherCommand = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8),
                null);

        List<Command<String, String, String>> commands = Arrays.asList(command, anotherCommand);
        when(promise.isVoid()).thenReturn(true);
        sut.write(context, commands, promise);

        verify(context).write(any(Set.class), eq(promise));
        assertThat(stack).hasSize(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldWriteActiveCommandsInMixedBatch() throws Exception {

        Command<String, String, String> command2 = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8),
                null);
        command.cancel();
        when(promise.isVoid()).thenReturn(true);

        sut.write(context, Arrays.asList(command, command2), promise);

        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
        verify(context).write(captor.capture(), any());

        assertThat(captor.getValue()).containsOnly(command2);
        assertThat(stack).hasSize(1).allMatch(o -> o instanceof LatencyMeteredCommand)
                .allMatch(o -> CommandWrapper.unwrap((RedisCommand) o) == command2);
    }

    @Test
    void shouldRecordCorrectFirstResponseLatency() throws Exception {

        ChannelPromise channelPromise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        channelPromise.setSuccess();

        sut.channelRegistered(context);
        sut.channelActive(context);

        sut.write(context, command, channelPromise);
        Delay.delay(Duration.ofMillis(10));

        sut.channelRead(context, Unpooled.wrappedBuffer("*1\r\n+OK\r\n".getBytes()));

        verify(latencyCollector).recordCommandLatency(any(), any(), any(LatencyMeteredCommand.class), gt(0L), gt(0L));

        sut.channelUnregistered(context);
    }

    @Test
    void shouldIgnoreNonReadableBuffers() throws Exception {

        ByteBuf byteBufMock = mock(ByteBuf.class);
        when(byteBufMock.isReadable()).thenReturn(false);

        sut.channelRead(context, byteBufMock);

        verify(byteBufMock, never()).release();
    }

    @Test
    void shouldNotifyPushListener() throws Exception {

        ChannelPromise channelPromise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        channelPromise.setSuccess();

        sut.channelRegistered(context);
        sut.channelActive(context);

        sut.channelRead(context, Unpooled.wrappedBuffer(">2\r\n+caching\r\n+foo\r\n".getBytes()));

        ArgumentCaptor<PushMessage> messageCaptor = ArgumentCaptor.forClass(PushMessage.class);
        verify(listener).onPushMessage(messageCaptor.capture());

        PushMessage message = messageCaptor.getValue();
        assertThat(message.getType()).isEqualTo("caching");
        assertThat(message.getContent()).containsExactly(ByteBuffer.wrap("caching".getBytes()),
                ByteBuffer.wrap("foo".getBytes()));
    }

    @Test
    void shouldNotDiscardReadBytes() throws Exception {

        ChannelPromise channelPromise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        channelPromise.setSuccess();

        sut.channelRegistered(context);
        sut.channelActive(context);

        sut.getStack().add(new Command<>(CommandType.PING, new StatusOutput<>(StringCodec.UTF8)));

        // set the command handler buffer capacity to 30, make it easy to test
        ByteBuf internalBuffer = context.alloc().buffer(30);
        sut.setReadBuffer(internalBuffer);

        // mock a multi reply, which will reach the buffer usage ratio
        ByteBuf msg = context.alloc().buffer(100);

        msg.writeBytes("*1\r\n+OK\r\n".getBytes());

        sut.channelRead(context, msg);

        assertThat(internalBuffer.readerIndex()).isEqualTo(9);
        assertThat(internalBuffer.writerIndex()).isEqualTo(9);
        sut.channelUnregistered(context);
    }

    @Test
    void shouldDiscardReadBytes() throws Exception {

        ChannelPromise channelPromise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        channelPromise.setSuccess();

        sut.channelRegistered(context);
        sut.channelActive(context);

        sut.getStack().add(new Command<>(CommandType.PING, new StatusOutput<>(StringCodec.UTF8)));
        sut.getStack().add(new Command<>(CommandType.PING, new StatusOutput<>(StringCodec.UTF8)));
        sut.getStack().add(new Command<>(CommandType.PING, new StatusOutput<>(StringCodec.UTF8)));

        // set the command handler buffer capacity to 30, make it easy to test
        ByteBuf internalBuffer = context.alloc().buffer(30);
        sut.setReadBuffer(internalBuffer);

        // mock a multi reply, which will reach the buffer usage ratio
        ByteBuf msg = context.alloc().buffer(100);

        msg.writeBytes("*1\r\n+OK\r\n".getBytes());
        msg.writeBytes("*1\r\n+OK\r\n".getBytes());
        msg.writeBytes("*1\r\n+OK\r\n".getBytes());

        sut.channelRead(context, msg);

        assertThat(internalBuffer.readerIndex()).isEqualTo(0);
        assertThat(internalBuffer.writerIndex()).isEqualTo(0);
        sut.channelUnregistered(context);
    }

    @Test
    void shouldCallPolicyToDiscardReadBytes() throws Exception {

        DecodeBufferPolicy policy = mock(DecodeBufferPolicy.class);

        CommandHandler commandHandler = new CommandHandler(ClientOptions.builder().decodeBufferPolicy(policy).build(),
                clientResources, endpoint);

        ChannelPromise channelPromise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        channelPromise.setSuccess();

        commandHandler.channelRegistered(context);
        commandHandler.channelActive(context);

        commandHandler.getStack().add(new Command<>(CommandType.PING, new StatusOutput<>(StringCodec.UTF8)));

        ByteBuf msg = context.alloc().buffer(100);
        msg.writeBytes("*1\r\n+OK\r\n".getBytes());

        commandHandler.channelRead(context, msg);
        commandHandler.channelUnregistered(context);

        verify(policy).afterCommandDecoded(any());
    }

    @Test
    void shouldHandleIncompleteResponses() throws Exception {

        Command<String, String, List<String>> lrangeCommand = new Command<>(CommandType.LRANGE,
                new ValueListOutput<>(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8).addKey("lrangeKey").add(0).add(1));

        Command<String, String, List<KeyValue<String, String>>> hmgetCommand = new Command<>(CommandType.HMGET,
                new KeyValueListOutput<>(StringCodec.UTF8, Arrays.asList("KEY1", "KEY2", "KEY3")),
                new CommandArgs<>(StringCodec.UTF8).addKeys("hmgetKey", "KEY1", "KEY2", "KEY3"));

        ChannelPromise channelPromise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        channelPromise.setSuccess();

        sut.channelRegistered(context);
        sut.channelActive(context);

        sut.write(context, lrangeCommand, channelPromise);
        assertThat(stack).hasSize(1);

        // the LRANGE response comes back across two channelReads
        sut.channelRead(context, Unpooled.wrappedBuffer(("*4\r\n" + "$3\r\nONE\r\n" + "$4\r\n>TW").getBytes()));
        assertThat(stack).hasSize(1);

        sut.channelRead(context, Unpooled.wrappedBuffer(("O\r\n" + "$5\r\nTHREE\r\n" + "$4\r\nFOUR\r\n").getBytes()));
        assertThat(stack).isEmpty();
        assertThat(lrangeCommand.get()).isNotNull();
        assertThat(lrangeCommand.get()).hasSize(4);

        sut.write(context, hmgetCommand, channelPromise);

        // the HMGET response comes back in another read
        sut.channelRead(context, Unpooled.wrappedBuffer("*3\r\n$4\r\nVAL1\r\n$4\r\nVAL2\r\n$4\r\nVAL3\r\n".getBytes()));

        assertThat(stack.isEmpty()).isTrue();
        assertThat(hmgetCommand.get()).isNotNull();
        assertThat(hmgetCommand.get()).hasSize(3);
    }

    /**
     * @see <a href="https://github.com/redis/lettuce/issues/3087">Issue 3087</a>
     */
    @Test
    void shouldHandleNullBuffers() throws Exception {
        sut.setReadBuffer(null);
        sut.channelRead(context, Unpooled.wrappedBuffer(("*4\r\n" + "$3\r\nONE\r\n" + "$4\r\n>TW").getBytes()));
        assertThat(stack).hasSize(0);

        sut.channelUnregistered(context);
    }

    /**
     * if large keys are received ,the large buffer will created and then released
     */
    @Test
    void shouldLargeBufferCreatedAndRelease() throws Exception {
        ChannelPromise channelPromise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        channelPromise.setSuccess();
        ClientOptions clientOptions = ClientOptions.builder().createByteBufWhenRecvLargeKey(true).build();
        sut = new CommandHandler(clientOptions, clientResources, endpoint);
        sut.channelRegistered(context);
        sut.channelActive(context);
        sut.getStack().add(new Command<>(CommandType.GET, new ValueOutput<String, String>(StringCodec.UTF8)));

        ByteBuf internalBuffer = ReflectionTestUtils.getField(sut, "readBuffer");

        // step1 Receive First TCP Packet
        ByteBuf msg = context.alloc().buffer(13);
        // 1+5+2+7 ($+length+\r\n+len(value))
        msg.writeBytes("$65536\r\nval_abc".getBytes(StandardCharsets.UTF_8));
        sut.channelRead(context, msg);

        int markedReaderIndex = ReflectionTestUtils.getField(internalBuffer, "markedReaderIndex");
        assertThat(markedReaderIndex).isEqualTo(8);
        assertThat(internalBuffer.readerIndex()).isEqualTo(8);
        assertThat(internalBuffer.writerIndex()).isEqualTo(15);

        // step2 Receive Second TCP Packet
        ByteBuf msg2 = context.alloc().buffer(64 * 1024);
        StringBuilder sb = new StringBuilder();
        // 65536-7
        for (int i = 0; i < 65529; i++) {
            sb.append((char) ('a' + i % 26));
        }
        sb.append("\r\n");
        msg2.writeBytes(sb.toString().getBytes(StandardCharsets.UTF_8));
        sut.channelRead(context, msg2);

        // step3 Got Result: readBuffer.capacity = 64k and tmpBuffer is null
        ByteBuf readBuffer = ReflectionTestUtils.getField(sut, "readBuffer");
        assertThat(readBuffer.capacity()).isEqualTo(64 * 1024);
        assertThat(readBuffer.readerIndex()).isZero();
        assertThat(readBuffer.writerIndex()).isZero();

        ByteBuf tmpBuffer = ReflectionTestUtils.getField(sut, "tmpReadBuffer");
        assertThat(tmpBuffer).isNull();
    }

    /**
     * readBufferSize = 16 createByteBufWhenRecvLargeKey = true large than readBufferSize will create a new buffer
     *
     */
    @Test
    void shouldLargeThan16ThenCreateAndRelease() throws Exception {
        ChannelPromise channelPromise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        channelPromise.setSuccess();
        int readBufferSize = 16;
        ClientOptions clientOptions = ClientOptions.builder().createByteBufWhenRecvLargeKey(true).readBufferSize(readBufferSize)
                .build();
        sut = new CommandHandler(clientOptions, clientResources, endpoint);
        sut.channelRegistered(context);
        sut.channelActive(context);
        sut.getStack().add(new Command<>(CommandType.GET, new ValueOutput<String, String>(StringCodec.UTF8)));

        ByteBuf internalBuffer = ReflectionTestUtils.getField(sut, "readBuffer");

        // step1 Receive First TCP Packet
        ByteBuf msg = context.alloc().buffer(13);
        // 1+5+2+7 ($+length+\r\n+len(value))
        msg.writeBytes("$16\r\nabc_val".getBytes(StandardCharsets.UTF_8));
        sut.channelRead(context, msg);

        int markedReaderIndex = ReflectionTestUtils.getField(internalBuffer, "markedReaderIndex");
        assertThat(markedReaderIndex).isEqualTo(5);
        assertThat(internalBuffer.readerIndex()).isEqualTo(5);
        assertThat(internalBuffer.writerIndex()).isEqualTo(12);

        // step2 Receive Second TCP Packet
        ByteBuf msg2 = context.alloc().buffer(32);
        msg2.writeBytes("abcd_abcd\r\n".getBytes(StandardCharsets.UTF_8));
        sut.channelRead(context, msg2);

        // step3 Got Result: readBuffer.capacity = 64k and tmpBuffer is null
        ByteBuf readBuffer = ReflectionTestUtils.getField(sut, "readBuffer");
        assertThat(readBuffer.capacity()).isEqualTo(readBufferSize);
        assertThat(readBuffer.readerIndex()).isZero();
        assertThat(readBuffer.writerIndex()).isZero();

        ByteBuf tmpBuffer = ReflectionTestUtils.getField(sut, "tmpReadBuffer");
        assertThat(tmpBuffer).isNull();
    }

}
