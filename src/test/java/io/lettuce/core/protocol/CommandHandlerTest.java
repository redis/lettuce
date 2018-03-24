/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionEvents;
import io.lettuce.core.RedisException;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.metrics.CommandLatencyCollector;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.resource.ClientResources;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.concurrent.ImmediateEventExecutor;

@RunWith(MockitoJUnitRunner.class)
public class CommandHandlerTest {

    private Queue<RedisCommand<String, String, ?>> stack;

    private CommandHandler sut;

    private final Command<String, String, String> command = new Command<>(CommandType.APPEND, new StatusOutput<>(
            StringCodec.UTF8), null);

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
    private ChannelPromise promise;

    @Mock
    private CommandLatencyCollector latencyCollector;

    @BeforeClass
    public static void beforeClass() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext();
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(CommandHandler.class.getName());
        loggerConfig.setLevel(Level.ALL);
    }

    @AfterClass
    public static void afterClass() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext();
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(CommandHandler.class.getName());
        loggerConfig.setLevel(null);
    }

    @Before
    public void before() throws Exception {

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
        when(clientResources.commandLatencyCollector()).thenReturn(latencyCollector);

        sut = new CommandHandler(ClientOptions.create(), clientResources, endpoint);
        stack = (Queue) ReflectionTestUtils.getField(sut, "stack");
    }

    @Test
    public void testChannelActive() throws Exception {
        sut.channelRegistered(context);

        sut.channelActive(context);

        verify(pipeline).fireUserEventTriggered(any(ConnectionEvents.Activated.class));
    }

    @Test
    public void testExceptionChannelActive() throws Exception {
        sut.setState(CommandHandler.LifecycleState.ACTIVE);

        sut.channelActive(context);
        sut.exceptionCaught(context, new Exception());
    }

    @Test
    public void testIOExceptionChannelActive() throws Exception {
        sut.setState(CommandHandler.LifecycleState.ACTIVE);

        sut.channelActive(context);
        sut.exceptionCaught(context, new IOException("Connection timed out"));
    }

    @Test
    public void testExceptionChannelInactive() throws Exception {
        sut.setState(CommandHandler.LifecycleState.DISCONNECTED);
        sut.exceptionCaught(context, new Exception());
        verify(context, never()).fireExceptionCaught(any(Exception.class));
    }

    @Test
    public void testExceptionWithQueue() throws Exception {
        sut.setState(CommandHandler.LifecycleState.ACTIVE);
        stack.clear();

        sut.channelActive(context);

        stack.add(command);
        sut.exceptionCaught(context, new Exception());

        assertThat(stack).isEmpty();
        command.get();

        assertThat(ReflectionTestUtils.getField(command, "exception")).isNotNull();
    }

    @Test
    public void testExceptionWhenClosed() throws Exception {

        sut.setState(CommandHandler.LifecycleState.CLOSED);

        sut.exceptionCaught(context, new Exception());
        verifyZeroInteractions(context);
    }

    @Test
    public void isConnectedShouldReportFalseForNOT_CONNECTED() throws Exception {

        sut.setState(CommandHandler.LifecycleState.NOT_CONNECTED);
        assertThat(sut.isConnected()).isFalse();
    }

    @Test
    public void isConnectedShouldReportFalseForREGISTERED() throws Exception {

        sut.setState(CommandHandler.LifecycleState.REGISTERED);
        assertThat(sut.isConnected()).isFalse();
    }

    @Test
    public void isConnectedShouldReportTrueForCONNECTED() throws Exception {

        sut.setState(CommandHandler.LifecycleState.CONNECTED);
        assertThat(sut.isConnected()).isTrue();
    }

    @Test
    public void isConnectedShouldReportTrueForACTIVATING() throws Exception {

        sut.setState(CommandHandler.LifecycleState.ACTIVATING);
        assertThat(sut.isConnected()).isTrue();
    }

    @Test
    public void isConnectedShouldReportTrueForACTIVE() throws Exception {

        sut.setState(CommandHandler.LifecycleState.ACTIVE);
        assertThat(sut.isConnected()).isTrue();
    }

    @Test
    public void isConnectedShouldReportFalseForDISCONNECTED() throws Exception {

        sut.setState(CommandHandler.LifecycleState.DISCONNECTED);
        assertThat(sut.isConnected()).isFalse();
    }

    @Test
    public void isConnectedShouldReportFalseForDEACTIVATING() throws Exception {

        sut.setState(CommandHandler.LifecycleState.DEACTIVATING);
        assertThat(sut.isConnected()).isFalse();
    }

    @Test
    public void isConnectedShouldReportFalseForDEACTIVATED() throws Exception {

        sut.setState(CommandHandler.LifecycleState.DEACTIVATED);
        assertThat(sut.isConnected()).isFalse();
    }

    @Test
    public void isConnectedShouldReportFalseForCLOSED() throws Exception {

        sut.setState(CommandHandler.LifecycleState.CLOSED);
        assertThat(sut.isConnected()).isFalse();
    }

    @Test
    public void shouldNotWriteCancelledCommands() throws Exception {

        command.cancel();
        sut.write(context, command, null);

        verifyZeroInteractions(context);
        assertThat(stack).isEmpty();
    }

    @Test
    public void shouldCancelCommandOnQueueSingleFailure() throws Exception {

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
    public void shouldCancelCommandOnQueueBatchFailure() throws Exception {

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
    public void shouldFailOnDuplicateCommands() throws Exception {

        Command<String, String, String> commandMock = mock(Command.class);

        ChannelPromise channelPromise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        sut.write(context, Arrays.asList(commandMock, commandMock), channelPromise);

        assertThat(stack).isEmpty();
        verify(commandMock).completeExceptionally(any(RedisException.class));
    }

    @Test
    public void shouldWriteActiveCommands() throws Exception {

        when(promise.isVoid()).thenReturn(true);

        sut.write(context, command, promise);

        verify(context).write(command, promise);
        assertThat(stack).hasSize(1).allMatch(o -> o instanceof LatencyMeteredCommand);
    }

    @Test
    public void shouldNotWriteCancelledCommandBatch() throws Exception {

        command.cancel();
        sut.write(context, Arrays.asList(command), promise);

        verifyZeroInteractions(context);
        assertThat((Collection) ReflectionTestUtils.getField(sut, "stack")).isEmpty();
    }

    @Test
    public void shouldWriteSingleActiveCommandsInBatch() throws Exception {

        List<Command<String, String, String>> commands = Arrays.asList(command);
        when(promise.isVoid()).thenReturn(true);
        sut.write(context, commands, promise);

        verify(context).write(command, promise);
        assertThat(stack).hasSize(1);
    }

    @Test
    public void shouldWriteActiveCommandsInBatch() throws Exception {

        Command<String, String, String> anotherCommand = new Command<>(CommandType.APPEND,
                new StatusOutput<>(StringCodec.UTF8), null);

        List<Command<String, String, String>> commands = Arrays.asList(command, anotherCommand);
        when(promise.isVoid()).thenReturn(true);
        sut.write(context, commands, promise);

        verify(context).write(any(Set.class), eq(promise));
        assertThat(stack).hasSize(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldWriteActiveCommandsInMixedBatch() throws Exception {

        Command<String, String, String> command2 = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8), null);
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
    public void shouldRecordCorrectFirstResponseLatency() throws Exception {

        ChannelPromise channelPromise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        channelPromise.setSuccess();

        sut.channelRegistered(context);
        sut.channelActive(context);

        LatencyMeteredCommand<String, String, String> wrapped = new LatencyMeteredCommand<>(command);

        sut.write(context, wrapped, channelPromise);
        Thread.sleep(10);

        sut.channelRead(context, Unpooled.wrappedBuffer("*1\r\n+OK\r\n".getBytes()));

        verify(latencyCollector).recordCommandLatency(any(), any(), eq(CommandType.APPEND), gt(0L), gt(0L));

        sut.channelUnregistered(context);
    }

    @Test
    public void shouldIgnoreNonReadableBuffers() throws Exception {

        ByteBuf byteBufMock = mock(ByteBuf.class);
        when(byteBufMock.isReadable()).thenReturn(false);

        sut.channelRead(context, byteBufMock);

        verify(byteBufMock, never()).release();
    }
}
