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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Queue;
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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;
import io.lettuce.ConnectionTestUtil;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisException;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.internal.LettuceFactories;
import io.lettuce.core.output.StatusOutput;
import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelPromise;
import io.netty.handler.codec.EncoderException;
import io.netty.util.concurrent.ImmediateEventExecutor;

@RunWith(MockitoJUnitRunner.class)
public class DefaultEndpointTest {

    private Queue<RedisCommand<String, String, ?>> queue = LettuceFactories.newConcurrentQueue(1000);

    private DefaultEndpoint sut;

    private final Command<String, String, String> command = new Command<>(CommandType.APPEND, new StatusOutput<>(
            new Utf8StringCodec()), null);

    @Mock
    private Channel channel;

    @Mock
    private ConnectionFacade connectionFacade;

    @Mock
    private ConnectionWatchdog connectionWatchdog;

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
    public void before() {

        when(channel.writeAndFlush(any())).thenAnswer(invocation -> {
            if (invocation.getArguments()[0] instanceof RedisCommand) {
                queue.add((RedisCommand) invocation.getArguments()[0]);
            }

            if (invocation.getArguments()[0] instanceof Collection) {
                queue.addAll((Collection) invocation.getArguments()[0]);
            }
            return new DefaultChannelPromise(channel);
        });

        when(channel.write(any())).thenAnswer(invocation -> {
            if (invocation.getArguments()[0] instanceof RedisCommand) {
                queue.add((RedisCommand) invocation.getArguments()[0]);
            }

            if (invocation.getArguments()[0] instanceof Collection) {
                queue.addAll((Collection) invocation.getArguments()[0]);
            }
            return new DefaultChannelPromise(channel);
        });

        sut = new DefaultEndpoint(ClientOptions.create());
        sut.setConnectionFacade(connectionFacade);
    }

    @Test
    public void writeConnectedShouldWriteCommandToChannel() {

        when(channel.isActive()).thenReturn(true);

        sut.notifyChannelActive(channel);
        sut.write(command);

        assertThat(ConnectionTestUtil.getQueueSize(sut)).isEqualTo(1);
        verify(channel).writeAndFlush(command);
    }

    @Test
    public void writeDisconnectedShouldBufferCommands() {

        sut.write(command);

        assertThat(ConnectionTestUtil.getDisconnectedBuffer(sut)).contains(command);

        verify(channel, never()).writeAndFlush(any());
    }

    @Test
    public void notifyChannelActiveActivatesFacade() {

        sut.notifyChannelActive(channel);

        verify(connectionFacade).activated();
    }

    @Test
    public void notifyChannelActiveArmsConnectionWatchdog() {

        sut.registerConnectionWatchdog(connectionWatchdog);

        sut.notifyChannelActive(channel);

        verify(connectionWatchdog).arm();
    }

    @Test
    public void notifyChannelInactiveDeactivatesFacade() {

        sut.notifyChannelInactive(channel);

        verify(connectionFacade).deactivated();
    }

    @Test
    public void notifyExceptionShouldStoreException() {

        sut.notifyException(new IllegalStateException());
        sut.write(command);

        assertThat(command.exception).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void notifyChannelActiveClearsStoredException() {

        sut.notifyException(new IllegalStateException());
        sut.notifyChannelActive(channel);
        sut.write(command);

        assertThat(command.exception).isNull();
    }

    @Test
    public void notifyDrainQueuedCommandsShouldBufferCommands() {

        Queue<RedisCommand<?, ?, ?>> q = LettuceFactories.newConcurrentQueue(100);
        q.add(command);

        sut.notifyDrainQueuedCommands(() -> q);

        assertThat(ConnectionTestUtil.getDisconnectedBuffer(sut)).contains(command);
        verify(channel, never()).write(any());
    }

    @Test
    public void notifyDrainQueuedCommandsShouldWriteCommands() {

        when(channel.isActive()).thenReturn(true);

        Queue<RedisCommand<?, ?, ?>> q = LettuceFactories.newConcurrentQueue(100);
        q.add(command);

        sut.notifyChannelActive(channel);
        sut.notifyDrainQueuedCommands(() -> q);

        verify(channel).write(command);
        verify(channel).flush();

    }

    @Test
    public void shouldCancelCommandsOnEncoderException() {

        when(channel.isActive()).thenReturn(true);
        sut.notifyChannelActive(channel);

        DefaultChannelPromise promise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);

        when(channel.writeAndFlush(any())).thenAnswer(invocation -> {
            if (invocation.getArguments()[0] instanceof RedisCommand) {
                queue.add((RedisCommand) invocation.getArguments()[0]);
            }

            if (invocation.getArguments()[0] instanceof Collection) {
                queue.addAll((Collection) invocation.getArguments()[0]);
            }
            return promise;
        });

        promise.setFailure(new EncoderException("foo"));

        sut.write(command);

        assertThat(command.exception).isInstanceOf(EncoderException.class);
    }

    @Test
    public void writeShouldRejectCommandsInDisconnectedState() {

        sut = new DefaultEndpoint(ClientOptions.builder() //
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS) //
                .build());

        try {
            sut.write(command);
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("Commands are rejected");
        }
    }

    @Test
    public void writeShouldRejectCommandsInClosedState() {

        sut.close();

        try {
            sut.write(command);
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("Connection is closed");
        }
    }

    @Test
    public void writeWithoutAutoReconnectShouldRejectCommandsInDisconnectedState() {

        sut = new DefaultEndpoint(ClientOptions.builder() //
                .autoReconnect(false) //
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.DEFAULT) //
                .build());

        try {
            sut.write(command);
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("Commands are rejected");
        }
    }

    @Test
    public void closeCleansUpResources() {

        sut.notifyChannelActive(channel);
        sut.registerConnectionWatchdog(connectionWatchdog);

        sut.close();

        verify(channel).close();
        verify(connectionWatchdog).prepareClose();
    }

    @Test
    public void closeAllowsOnlyOneCall() {

        sut.notifyChannelActive(channel);
        sut.registerConnectionWatchdog(connectionWatchdog);

        sut.close();
        sut.close();

        verify(channel).close();
        verify(connectionWatchdog).prepareClose();
    }

    @Test
    public void testMTCConcurrentConcurrentWrite() throws Throwable {
        TestFramework.runOnce(new MTCConcurrentConcurrentWrite(command));
    }

    /**
     * Test of concurrent access to locks. Two concurrent writes.
     */
    static class MTCConcurrentConcurrentWrite extends MultithreadedTestCase {

        private final Command<String, String, String> command;
        private TestableEndpoint handler;

        public MTCConcurrentConcurrentWrite(Command<String, String, String> command) {

            this.command = command;

            handler = new TestableEndpoint(ClientOptions.create()) {

                @Override
                protected <C extends RedisCommand<?, ?, T>, T> void writeToBuffer(C command) {

                    waitForTick(2);

                    Object sharedLock = ReflectionTestUtils.getField(this, "sharedLock");
                    AtomicLong writers = (AtomicLong) ReflectionTestUtils.getField(sharedLock, "writers");
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

    static class TestableEndpoint extends DefaultEndpoint {

        /**
         * Create a new {@link DefaultEndpoint}.
         *
         * @param clientOptions client options for this connection, must not be {@literal null}
         */
        public TestableEndpoint(ClientOptions clientOptions) {
            super(clientOptions);
        }
    }
}
