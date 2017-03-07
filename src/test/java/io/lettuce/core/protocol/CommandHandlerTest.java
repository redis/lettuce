/*
 * Copyright 2011-2016 the original author or authors.
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

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

import io.lettuce.core.ConnectionEvents;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.metrics.DefaultCommandLatencyCollector;
import io.lettuce.core.metrics.DefaultCommandLatencyCollectorOptions;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.resource.ClientResources;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.util.concurrent.ImmediateEventExecutor;

@RunWith(MockitoJUnitRunner.class)
public class CommandHandlerTest {

    private Queue<RedisCommand<String, String, ?>> q;

    private CommandHandler sut;

    private final Command<String, String, String> command = new Command<>(CommandType.APPEND, new StatusOutput<>(
            new Utf8StringCodec()), null);

    @Mock
    private ChannelHandlerContext context;

    @Mock
    private Channel channel;

    @Mock
    private ByteBufAllocator byteBufAllocator;

    @Mock
    private ChannelPipeline pipeline;

    @Mock
    private EventLoop eventLoop;

    @Mock
    private ClientResources clientResources;

    @Mock
    private Endpoint endpoint;

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
        when(context.alloc()).thenReturn(byteBufAllocator);
        when(channel.pipeline()).thenReturn(pipeline);
        when(channel.eventLoop()).thenReturn(eventLoop);
        when(eventLoop.submit(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable r = (Runnable) invocation.getArguments()[0];
            r.run();
            return null;
        });

        when(clientResources.commandLatencyCollector()).thenReturn(
                new DefaultCommandLatencyCollector(DefaultCommandLatencyCollectorOptions.create()));

        sut = new CommandHandler(clientResources, endpoint);
        q = (Queue) ReflectionTestUtils.getField(sut, "queue");
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
        q.clear();

        sut.channelActive(context);

        q.add(command);
        sut.exceptionCaught(context, new Exception());

        assertThat(q).isEmpty();
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
        assertThat((Collection) ReflectionTestUtils.getField(sut, "queue")).isEmpty();
    }

    @Test
    public void shouldCancelCommandOnQueueSingleFailure() throws Exception {

        Command<String, String, String> commandMock = mock(Command.class);

        RuntimeException exception = new RuntimeException();
        when(commandMock.getOutput()).thenThrow(exception);

        ChannelPromise channelPromise = new DefaultChannelPromise(null, ImmediateEventExecutor.INSTANCE);
        try {
            sut.write(context, commandMock, channelPromise);
            fail("Missing RuntimeException");
        } catch (RuntimeException e) {
            assertThat(e).isSameAs(exception);
        }

        assertThat((Collection) ReflectionTestUtils.getField(sut, "queue")).isEmpty();
        verify(commandMock).completeExceptionally(exception);
    }

    @Test
    public void shouldCancelCommandOnQueueBatchFailure() throws Exception {

        Command<String, String, String> commandMock = mock(Command.class);

        RuntimeException exception = new RuntimeException();
        when(commandMock.getOutput()).thenThrow(exception);

        ChannelPromise channelPromise = new DefaultChannelPromise(null, ImmediateEventExecutor.INSTANCE);
        try {
            sut.write(context, Arrays.asList(commandMock), channelPromise);
            fail("Missing RuntimeException");
        } catch (RuntimeException e) {
            assertThat(e).isSameAs(exception);
        }

        assertThat((Collection) ReflectionTestUtils.getField(sut, "queue")).isEmpty();
        verify(commandMock).completeExceptionally(exception);
    }

    @Test
    public void shouldWriteActiveCommands() throws Exception {

        sut.write(context, command, null);

        verify(context).write(command, null);
        assertThat((Collection) ReflectionTestUtils.getField(sut, "queue")).containsOnly(command);
    }

    @Test
    public void shouldNotWriteCancelledCommandBatch() throws Exception {

        command.cancel();
        sut.write(context, Arrays.asList(command), null);

        verifyZeroInteractions(context);
        assertThat((Collection) ReflectionTestUtils.getField(sut, "queue")).isEmpty();
    }

    @Test
    public void shouldWriteActiveCommandsInBatch() throws Exception {

        List<Command<String, String, String>> commands = Arrays.asList(command);
        sut.write(context, commands, null);

        verify(context).write(commands, null);
        assertThat((Collection) ReflectionTestUtils.getField(sut, "queue")).containsOnly(command);
    }

    @Test
    public void shouldWriteActiveCommandsInMixedBatch() throws Exception {

        Command<String, String, String> command2 = new Command<>(CommandType.APPEND, new StatusOutput<String, String>(
                new Utf8StringCodec()), null);

        command.cancel();

        sut.write(context, Arrays.asList(command, command2), null);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(context).write(captor.capture(), any());

        assertThat(captor.getValue()).containsOnly(command2);
        assertThat((Collection) ReflectionTestUtils.getField(sut, "queue")).containsOnly(command2);
    }

    @Test
    public void shouldIgnoreNonReadableBuffers() throws Exception {

        ByteBuf byteBufMock = mock(ByteBuf.class);
        when(byteBufMock.isReadable()).thenReturn(false);

        sut.channelRead(context, byteBufMock);

        verify(byteBufMock, never()).release();
    }

    @Test
    public void shouldSetLatency() throws Exception {

        sut.write(context, Arrays.asList(command), null);

        assertThat(command.sentNs).isNotEqualTo(-1);
        assertThat(command.firstResponseNs).isEqualTo(-1);
    }

}
