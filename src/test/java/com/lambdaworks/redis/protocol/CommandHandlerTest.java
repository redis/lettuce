/*
 * Copyright 2011-2017 the original author or authors.
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
package com.lambdaworks.redis.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.ConnectionEvents;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.metrics.DefaultCommandLatencyCollector;
import com.lambdaworks.redis.metrics.DefaultCommandLatencyCollectorOptions;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.resource.ClientResources;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.concurrent.ImmediateEventExecutor;

@RunWith(MockitoJUnitRunner.class)
public class CommandHandlerTest {

    private Queue<RedisCommand<String, String, ?>> q = new ArrayDeque<>(10);

    private CommandHandler<String, String> sut;

    private final Command<String, String, String> command = new Command<>(CommandType.APPEND, new StatusOutput<>(
            new Utf8StringCodec()), null);

    @Mock
    private ChannelHandlerContext context;

    @Mock
    private Channel channel;

    @Mock
    private ChannelPipeline pipeline;

    @Mock
    private EventLoop eventLoop;

    @Mock
    private ClientResources clientResources;

    @Mock
    private RedisChannelHandler channelHandler;

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
        when(channel.pipeline()).thenReturn(pipeline);
        when(channel.eventLoop()).thenReturn(eventLoop);
        when(eventLoop.submit(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable r = (Runnable) invocation.getArguments()[0];
            r.run();
            return null;
        });

        when(clientResources.commandLatencyCollector()).thenReturn(
                new DefaultCommandLatencyCollector(DefaultCommandLatencyCollectorOptions.create()));

        when(channel.newPromise()).then(invocationOnMock -> new DefaultChannelPromise(channel));

        when(channel.writeAndFlush(any(), any(ChannelPromise.class))).thenAnswer(invocation -> {

            if (invocation.getArguments()[0] instanceof RedisCommand) {
                q.add((RedisCommand) invocation.getArguments()[0]);
            }

            if (invocation.getArguments()[0] instanceof Collection) {
                q.addAll((Collection) invocation.getArguments()[0]);
            }

            return null;
        });

        sut = new CommandHandler<>(ClientOptions.create(), clientResources, q);
        sut.setRedisChannelHandler(channelHandler);
    }

    @Test
    public void testChannelActive() throws Exception {
        sut.channelRegistered(context);

        sut.channelActive(context);

        verify(pipeline).fireUserEventTriggered(any(ConnectionEvents.Activated.class));

    }

    @Test
    public void testChannelActiveFailureShouldCancelCommands() throws Exception {

        ClientOptions clientOptions = ClientOptions.builder().cancelCommandsOnReconnectFailure(true).build();

        sut = new CommandHandler<>(clientOptions, clientResources, q);
        sut.setRedisChannelHandler(channelHandler);

        sut.channelRegistered(context);
        sut.write(command);

        reset(context);
        when(context.channel()).thenThrow(new RuntimeException());
        try {
            sut.channelActive(context);
            fail("Missing RuntimeException");
        } catch (RuntimeException e) {
        }

        assertThat(command.isCancelled()).isTrue();
    }

    @Test
    public void testChannelActiveWithBufferedAndQueuedCommands() throws Exception {

        Command<String, String, String> bufferedCommand = new Command<>(CommandType.GET, new StatusOutput<>(
                new Utf8StringCodec()), null);

        Command<String, String, String> pingCommand = new Command<>(CommandType.PING,
                new StatusOutput<>(new Utf8StringCodec()), null);
        q.add(bufferedCommand);

        AtomicLong atomicLong = (AtomicLong) ReflectionTestUtils.getField(sut, "writers");
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                assertThat(atomicLong.get()).isEqualTo(-1);
                assertThat(ReflectionTestUtils.getField(sut, "exclusiveLockOwner")).isNotNull();

                sut.write(pingCommand);

                return null;
            }
        }).when(channelHandler).activated();
        when(channel.isActive()).thenReturn(true);

        sut.channelRegistered(context);
        sut.channelActive(context);

        assertThat(atomicLong.get()).isEqualTo(0);
        assertThat(ReflectionTestUtils.getField(sut, "exclusiveLockOwner")).isNull();
        assertThat(q).containsSequence(pingCommand, bufferedCommand);

        verify(pipeline).fireUserEventTriggered(any(ConnectionEvents.Activated.class));
    }

    @Test
    public void testChannelActiveWithBufferedAndQueuedCommandsRetainsOrder() throws Exception {

        Command<String, String, String> bufferedCommand1 = new Command<>(CommandType.SET, new StatusOutput<>(
                new Utf8StringCodec()), null);

        Command<String, String, String> bufferedCommand2 = new Command<>(CommandType.GET, new StatusOutput<>(
                new Utf8StringCodec()), null);

        Command<String, String, String> queuedCommand1 = new Command<>(CommandType.PING, new StatusOutput<>(
                new Utf8StringCodec()), null);

        Command<String, String, String> queuedCommand2 = new Command<>(CommandType.AUTH, new StatusOutput<>(
                new Utf8StringCodec()), null);

        q.add(queuedCommand1);
        q.add(queuedCommand2);

        Collection buffer = (Collection) ReflectionTestUtils.getField(sut, "commandBuffer");
        buffer.add(bufferedCommand1);
        buffer.add(bufferedCommand2);

        reset(channel);
        when(channel.newPromise()).thenAnswer(invocation -> new DefaultChannelPromise(channel));
        when(channel.eventLoop()).thenReturn(eventLoop);
        when(channel.pipeline()).thenReturn(pipeline);

        sut.channelRegistered(context);
        sut.channelActive(context);

        assertThat(q).isEmpty();
        assertThat(buffer).isEmpty();

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(channel).writeAndFlush(objectArgumentCaptor.capture(), any(ChannelPromise.class));

        assertThat((Collection) objectArgumentCaptor.getValue()).containsSequence(queuedCommand1, queuedCommand2,
                bufferedCommand1, bufferedCommand2);
    }

    @Test
    public void testChannelActiveReplayBufferedCommands() throws Exception {

        Command<String, String, String> bufferedCommand1 = new Command<>(CommandType.SET, new StatusOutput<>(
                new Utf8StringCodec()), null);

        Command<String, String, String> bufferedCommand2 = new Command<>(CommandType.GET, new StatusOutput<>(
                new Utf8StringCodec()), null);

        Command<String, String, String> queuedCommand1 = new Command<>(CommandType.PING, new StatusOutput<>(
                new Utf8StringCodec()), null);

        Command<String, String, String> queuedCommand2 = new Command<>(CommandType.AUTH, new StatusOutput<>(
                new Utf8StringCodec()), null);

        q.add(queuedCommand1);
        q.add(queuedCommand2);

        Collection buffer = (Collection) ReflectionTestUtils.getField(sut, "commandBuffer");
        buffer.add(bufferedCommand1);
        buffer.add(bufferedCommand2);

        sut.channelRegistered(context);
        sut.channelActive(context);

        assertThat(q).containsSequence(queuedCommand1, queuedCommand2, bufferedCommand1, bufferedCommand2);
        assertThat(buffer).isEmpty();
    }

    @Test
    public void testExceptionChannelActive() throws Exception {
        sut.setState(CommandHandler.LifecycleState.ACTIVE);

        when(channel.isActive()).thenReturn(true);

        sut.channelActive(context);
        sut.exceptionCaught(context, new Exception());
    }

    @Test
    public void testIOExceptionChannelActive() throws Exception {
        sut.setState(CommandHandler.LifecycleState.ACTIVE);

        when(channel.isActive()).thenReturn(true);

        sut.channelActive(context);
        sut.exceptionCaught(context, new IOException("Connection timed out"));
    }

    @Test
    public void testWriteChannelDisconnected() throws Exception {

        when(channel.isActive()).thenReturn(true);
        sut.channelRegistered(context);
        sut.channelActive(context);

        sut.setState(CommandHandler.LifecycleState.DISCONNECTED);

        sut.write(command);

        Collection buffer = (Collection) ReflectionTestUtils.getField(sut, "commandBuffer");
        assertThat(buffer).containsOnly(command);
    }

    @Test(expected = RedisException.class)
    public void testWriteChannelDisconnectedWithoutReconnect() throws Exception {

        sut = new CommandHandler<>(ClientOptions.builder().autoReconnect(false).build(), clientResources, q);
        sut.setRedisChannelHandler(channelHandler);

        when(channel.isActive()).thenReturn(true);
        sut.channelRegistered(context);
        sut.channelActive(context);

        sut.setState(CommandHandler.LifecycleState.DISCONNECTED);

        sut.write(command);
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
        when(channel.isActive()).thenReturn(true);

        q.add(command);
        sut.exceptionCaught(context, new Exception());

        assertThat(q).isEmpty();
        command.get();

        assertThat(ReflectionTestUtils.getField(command, "exception")).isNotNull();
    }

    @Test(expected = RedisException.class)
    public void testWriteWhenClosed() throws Exception {

        sut.setState(CommandHandler.LifecycleState.CLOSED);

        sut.write(command);
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
        assertThat((Collection) ReflectionTestUtils.getField(sut, "queue")).hasSize(1).allMatch(
                o -> o instanceof LatencyMeteredCommand);
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
        assertThat((Collection) ReflectionTestUtils.getField(sut, "queue")).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldWriteActiveCommandsInMixedBatch() throws Exception {

        Command<String, String, String> command2 = new Command<>(CommandType.APPEND, new StatusOutput<>(new Utf8StringCodec()),
                null);

        command.cancel();

        sut.write(context, Arrays.asList(command, command2), null);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(context).write(captor.capture(), any());

        assertThat(captor.getValue()).containsOnly(command2);
        assertThat((Collection) ReflectionTestUtils.getField(sut, "queue")).hasSize(1)
                .allMatch(o -> o instanceof LatencyMeteredCommand)
                .allMatch(o -> CommandWrapper.unwrap((RedisCommand) o) == command2);
    }

    @Test
    public void shouldIgnoreNonReadableBuffers() throws Exception {

        ByteBuf byteBufMock = mock(ByteBuf.class);
        when(byteBufMock.isReadable()).thenReturn(false);

        sut.channelRead(context, byteBufMock);

        verify(byteBufMock, never()).release();
    }

    @Test
    public void testMTCConcurrentWriteThenReset() throws Throwable {
        TestFramework.runOnce(new MTCConcurrentWriteThenReset(clientResources, q, command));
    }

    @Test
    public void testMTCConcurrentResetThenWrite() throws Throwable {
        TestFramework.runOnce(new MTCConcurrentResetThenWrite(clientResources, q, command));
    }

    @Test
    public void testMTCConcurrentConcurrentWrite() throws Throwable {
        TestFramework.runOnce(new MTCConcurrentConcurrentWrite(clientResources, q, command));
    }

    /**
     * Test of concurrent access to locks. write call wins over reset call.
     */
    static class MTCConcurrentWriteThenReset extends MultithreadedTestCase {

        private final Command<String, String, String> command;
        private TestableCommandHandler handler;
        private List<Thread> expectedThreadOrder = Collections.synchronizedList(new ArrayList<>());
        private List<Thread> entryThreadOrder = Collections.synchronizedList(new ArrayList<>());
        private List<Thread> exitThreadOrder = Collections.synchronizedList(new ArrayList<>());

        public MTCConcurrentWriteThenReset(ClientResources clientResources, Queue<RedisCommand<String, String, ?>> queue,
                Command<String, String, String> command) {
            this.command = command;
            handler = new TestableCommandHandler(ClientOptions.create(), clientResources, queue) {

                @Override
                protected void incrementWriters() {

                    waitForTick(2);
                    super.incrementWriters();
                    waitForTick(4);
                }

                @Override
                protected void lockWritersExclusive() {

                    waitForTick(4);
                    super.lockWritersExclusive();
                }

                @Override
                protected <C extends RedisCommand<String, String, T>, T> void writeToBuffer(C command) {

                    entryThreadOrder.add(Thread.currentThread());
                    super.writeToBuffer(command);
                }

                @Override
                protected List<RedisCommand<String, String, ?>> prepareReset() {

                    entryThreadOrder.add(Thread.currentThread());
                    return super.prepareReset();
                }

                @Override
                protected void unlockWritersExclusive() {

                    exitThreadOrder.add(Thread.currentThread());
                    super.unlockWritersExclusive();
                }

                @Override
                protected void decrementWriters() {

                    exitThreadOrder.add(Thread.currentThread());
                    super.decrementWriters();
                }
            };
        }

        public void thread1() throws InterruptedException {

            waitForTick(1);
            expectedThreadOrder.add(Thread.currentThread());
            handler.write(command);

        }

        public void thread2() throws InterruptedException {

            waitForTick(3);
            expectedThreadOrder.add(Thread.currentThread());
            handler.reset();
        }

        @Override
        public void finish() {

            assertThat(entryThreadOrder).containsExactlyElementsOf(expectedThreadOrder);
            assertThat(exitThreadOrder).containsExactlyElementsOf(expectedThreadOrder);
        }
    }

    /**
     * Test of concurrent access to locks. write call wins over flush call.
     */
    static class MTCConcurrentResetThenWrite extends MultithreadedTestCase {

        private final Command<String, String, String> command;
        private TestableCommandHandler handler;
        private List<Thread> expectedThreadOrder = Collections.synchronizedList(new ArrayList<>());
        private List<Thread> entryThreadOrder = Collections.synchronizedList(new ArrayList<>());
        private List<Thread> exitThreadOrder = Collections.synchronizedList(new ArrayList<>());

        public MTCConcurrentResetThenWrite(ClientResources clientResources, Queue<RedisCommand<String, String, ?>> queue,
                Command<String, String, String> command) {
            this.command = command;
            handler = new TestableCommandHandler(ClientOptions.create(), clientResources, queue) {

                @Override
                protected void incrementWriters() {

                    waitForTick(4);
                    super.incrementWriters();
                }

                @Override
                protected void lockWritersExclusive() {

                    waitForTick(2);
                    super.lockWritersExclusive();
                    waitForTick(4);
                }

                @Override
                protected <C extends RedisCommand<String, String, T>, T> void writeToBuffer(C command) {

                    entryThreadOrder.add(Thread.currentThread());
                    super.writeToBuffer(command);
                }

                @Override
                protected List<RedisCommand<String, String, ?>> prepareReset() {

                    entryThreadOrder.add(Thread.currentThread());
                    return super.prepareReset();
                }

                @Override
                protected void unlockWritersExclusive() {

                    exitThreadOrder.add(Thread.currentThread());
                    super.unlockWritersExclusive();
                }

                @Override
                protected void decrementWriters() {

                    exitThreadOrder.add(Thread.currentThread());
                    super.decrementWriters();
                }
            };
        }

        public void thread1() throws InterruptedException {

            waitForTick(1);
            expectedThreadOrder.add(Thread.currentThread());
            handler.reset();
        }

        public void thread2() throws InterruptedException {

            waitForTick(3);
            expectedThreadOrder.add(Thread.currentThread());
            handler.write(command);
        }

        @Override
        public void finish() {

            assertThat(entryThreadOrder).containsExactlyElementsOf(expectedThreadOrder);
            assertThat(exitThreadOrder).containsExactlyElementsOf(expectedThreadOrder);
        }
    }

    /**
     * Test of concurrent access to locks. Two concurrent writes.
     */
    static class MTCConcurrentConcurrentWrite extends MultithreadedTestCase {

        private final Command<String, String, String> command;
        private TestableCommandHandler handler;

        public MTCConcurrentConcurrentWrite(ClientResources clientResources, Queue<RedisCommand<String, String, ?>> queue,
                Command<String, String, String> command) {
            this.command = command;
            handler = new TestableCommandHandler(ClientOptions.create(), clientResources, queue) {

                @Override
                protected <C extends RedisCommand<String, String, T>, T> void writeToBuffer(C command) {

                    waitForTick(2);
                    assertThat(writers.get()).isEqualTo(2);
                    waitForTick(3);
                    super.writeToBuffer(command);
                }

            };
        }

        public void thread1() throws InterruptedException {

            waitForTick(1);
            handler.write(command);
        }

        public void thread2() throws InterruptedException {

            waitForTick(1);
            handler.write(command);
        }

    }

    static class TestableCommandHandler extends CommandHandler<String, String> {
        public TestableCommandHandler(ClientOptions clientOptions, ClientResources clientResources,
                Queue<RedisCommand<String, String, ?>> queue) {
            super(clientOptions, clientResources, queue);
        }
    }

}
