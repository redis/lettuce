package com.lambdaworks.redis.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.ConnectionEvents;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.internal.LettuceLists;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.resource.ClientResources;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

@RunWith(MockitoJUnitRunner.class)
public class CommandHandlerTest {

    private Queue<RedisCommand<String, String, ?>> q = new ArrayDeque<>(10);

    private CommandHandler<String, String> sut;

    public static final Command<String, String, String> command = new Command<>(CommandType.APPEND,
            new StatusOutput<String, String>(new Utf8StringCodec()), null);

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

        when(channel.write(any())).thenAnswer(invocation -> new DefaultChannelPromise(channel));

        when(channel.writeAndFlush(any())).thenAnswer(invocation -> new DefaultChannelPromise(channel));

        sut = new CommandHandler<String, String>(ClientOptions.create(), clientResources, q);
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
    public void testMTCConcurrentWriteThenReset() throws Throwable {
        TestFramework.runOnce(new MTCConcurrentWriteThenReset(clientResources, q));
    }

    @Test
    public void testMTCConcurrentResetThenWrite() throws Throwable {
        TestFramework.runOnce(new MTCConcurrentResetThenWrite(clientResources, q));
    }
    
    @Test
    public void testMTCConcurrentConcurrentWrite() throws Throwable {
        TestFramework.runOnce(new MTCConcurrentConcurrentWrite(clientResources, q));
    }

    /**
     * Test of concurrent access to locks. write call wins over reset call.
     */
    static class MTCConcurrentWriteThenReset extends MultithreadedTestCase {

        private TestableCommandHandler handler;
        private List<Thread> expectedThreadOrder = Collections.synchronizedList(LettuceLists.newList());
        private List<Thread> entryThreadOrder = Collections.synchronizedList(LettuceLists.newList());
        private List<Thread> exitThreadOrder = Collections.synchronizedList(LettuceLists.newList());

        public MTCConcurrentWriteThenReset(ClientResources clientResources, Queue<RedisCommand<String, String, ?>> queue) {
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

        private TestableCommandHandler handler;
        private List<Thread> expectedThreadOrder = Collections.synchronizedList(LettuceLists.newList());
        private List<Thread> entryThreadOrder = Collections.synchronizedList(LettuceLists.newList());
        private List<Thread> exitThreadOrder = Collections.synchronizedList(LettuceLists.newList());

        public MTCConcurrentResetThenWrite(ClientResources clientResources, Queue<RedisCommand<String, String, ?>> queue) {
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

        private TestableCommandHandler handler;

        public MTCConcurrentConcurrentWrite(ClientResources clientResources, Queue<RedisCommand<String, String, ?>> queue) {
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
