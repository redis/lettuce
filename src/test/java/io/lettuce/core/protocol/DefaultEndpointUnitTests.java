package io.lettuce.core.protocol;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.channels.ClosedChannelException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

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
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceFactories;
import io.lettuce.core.metrics.CommandLatencyRecorder;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.ConnectionTestUtil;
import io.lettuce.test.ReflectionTestUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.EncoderException;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * @author Mark Paluch
 * @author Vaibhav Vashisht
 */
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultEndpointUnitTests {

    private Queue<RedisCommand<String, String, ?>> queue = LettuceFactories.newConcurrentQueue(1000);

    private DefaultEndpoint sut;

    private final Command<String, String, String> command = new Command<>(CommandType.APPEND,
            new StatusOutput<>(StringCodec.UTF8), null);

    @Mock
    private Channel channel;

    @Mock
    private ConnectionFacade connectionFacade;

    @Mock
    private ConnectionWatchdog connectionWatchdog;

    @Mock
    private ClientResources clientResources;

    private ChannelPromise promise;

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
    void before() {

        promise = new DefaultChannelPromise(channel);
        when(channel.writeAndFlush(any())).thenAnswer(invocation -> {
            if (invocation.getArguments()[0] instanceof RedisCommand) {
                queue.add((RedisCommand) invocation.getArguments()[0]);
            }

            if (invocation.getArguments()[0] instanceof Collection) {
                queue.addAll((Collection) invocation.getArguments()[0]);
            }
            return promise;
        });

        when(channel.write(any())).thenAnswer(invocation -> {
            if (invocation.getArguments()[0] instanceof RedisCommand) {
                queue.add((RedisCommand) invocation.getArguments()[0]);
            }

            if (invocation.getArguments()[0] instanceof Collection) {
                queue.addAll((Collection) invocation.getArguments()[0]);
            }
            return promise;
        });

        sut = new DefaultEndpoint(ClientOptions.create(), clientResources);
        sut.setConnectionFacade(connectionFacade);
    }

    @Test
    void queueingTimestampIsConsumedOnceAndResetOnNewAcceptance() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        RedisCommand<String, String, String> queued = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));

        endpoint.write(queued);
        assertThat(endpoint.takeQueueLatencyStart(queued)).isNotNull();
        assertThat(endpoint.takeQueueLatencyStart(queued)).isNull();

        // A redirected attempt passes through write() again and gets a new start time.
        endpoint.write(queued);
        assertThat(endpoint.takeQueueLatencyStart(queued)).isNotNull();
    }

    @Test
    void failedBatchAdmissionIterationReleasesWriterCount() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        Command<String, String, String> first = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        Command<String, String, String> second = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        AtomicInteger iterations = new AtomicInteger();
        Collection<RedisCommand<String, String, ?>> batch = new AbstractCollection<RedisCommand<String, String, ?>>() {

            @Override
            public Iterator<RedisCommand<String, String, ?>> iterator() {
                // The first pass checks duplicate command identities. Fail during the
                // later admission-lock lookup, before any command can be written.
                if (iterations.incrementAndGet() == 2) {
                    throw new IllegalStateException("Could not iterate batch");
                }
                return Arrays.<RedisCommand<String, String, ?>>asList(first, second).iterator();
            }

            @Override
            public int size() {
                return 2;
            }
        };

        assertThatThrownBy(() -> endpoint.write(batch)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not iterate batch");
        SharedLock lock = ReflectionTestUtils.getField(endpoint, "sharedLock");
        Long writers = ReflectionTestUtils.getField(lock, "writers");
        assertThat(writers).isZero();
        assertThat(ConnectionTestUtil.getDisconnectedBuffer(endpoint)).isEmpty();
    }

    @Test
    void simultaneousWritersOfSameCommandCannotInvertEnqueueOrder() throws Exception {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        CountDownLatch firstWriterPaused = new CountDownLatch(1);
        CountDownLatch releaseFirstWriter = new CountDownLatch(1);
        CountDownLatch secondWriterStarted = new CountDownLatch(1);
        AtomicInteger filterCalls = new AtomicInteger();
        AtomicBoolean secondWriterOvertookFirst = new AtomicBoolean();
        ClientOptions options = ClientOptions.builder().replayFilter(cmd -> {
            int visit = filterCalls.incrementAndGet();
            if (visit == 1) {
                firstWriterPaused.countDown();
                try {
                    if (!releaseFirstWriter.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for the first writer");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            } else if (releaseFirstWriter.getCount() != 0) {
                secondWriterOvertookFirst.set(true);
            }
            return false;
        }).build();
        DefaultEndpoint endpoint = new DefaultEndpoint(options, clientResources);
        Command<String, String, String> reused = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2));

        try {
            CompletableFuture<Void> first = CompletableFuture.runAsync(() -> endpoint.write(reused), executor);
            assertThat(firstWriterPaused.await(5, TimeUnit.SECONDS)).isTrue();

            CompletableFuture<Void> second = CompletableFuture.runAsync(() -> {
                secondWriterStarted.countDown();
                endpoint.write(reused);
            }, executor);
            assertThat(secondWriterStarted.await(5, TimeUnit.SECONDS)).isTrue();

            // The second submission cannot pass the first in its acceptance/enqueue phase.
            Thread.sleep(100);
            assertThat(filterCalls.get()).isEqualTo(1);
            releaseFirstWriter.countDown();
            CompletableFuture.allOf(first, second).get(5, TimeUnit.SECONDS);

            assertThat(secondWriterOvertookFirst.get()).isFalse();
            assertThat(ConnectionTestUtil.getDisconnectedBuffer(endpoint)).containsExactly(reused, reused);
            assertThat(endpoint.takeQueueLatencyStart(reused)).isNotNull();
            assertThat(endpoint.takeQueueLatencyStart(reused)).isNotNull();
            assertThat(endpoint.takeQueueLatencyStart(reused)).isNull();
        } finally {
            releaseFirstWriter.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void waitingForAdmissionCannotBlockReentrantExclusiveOperation() throws Exception {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        CountDownLatch firstInFilter = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        AtomicInteger filterCalls = new AtomicInteger();
        AtomicReference<DefaultEndpoint> endpointRef = new AtomicReference<>();
        ClientOptions options = ClientOptions.builder().replayFilter(command -> {
            if (filterCalls.incrementAndGet() == 1) {
                firstInFilter.countDown();
                try {
                    if (!secondStarted.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("The second writer did not start");
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                // The contending writer must suspend its writer count before waiting
                // on this command's admission lock, or doExclusive cannot progress.
                assertThat(endpointRef.get().doExclusive(() -> "exclusive")).isEqualTo("exclusive");
            }
            return false;
        }).build();
        DefaultEndpoint endpoint = new DefaultEndpoint(options, clientResources);
        endpointRef.set(endpoint);
        Command<String, String, String> reused = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2));

        try {
            CompletableFuture<Void> first = CompletableFuture.runAsync(() -> endpoint.write(reused), executor);
            assertThat(firstInFilter.await(5, TimeUnit.SECONDS)).isTrue();
            CompletableFuture<Void> second = CompletableFuture.runAsync(() -> {
                secondStarted.countDown();
                endpoint.write(reused);
            }, executor);
            CompletableFuture.allOf(first, second).get(5, TimeUnit.SECONDS);
            assertThat(ConnectionTestUtil.getDisconnectedBuffer(endpoint)).containsExactly(reused, reused);
            assertThat(endpoint.takeQueueLatencyStart(reused)).isNotNull();
            assertThat(endpoint.takeQueueLatencyStart(reused)).isNotNull();
            assertThat(endpoint.takeQueueLatencyStart(reused)).isNull();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void separateWritesOfSameCommandKeepBothQueueTimestampsInOrder() throws Exception {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        Command<String, String, String> reused = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));

        // Both attempts are buffered before either reaches the CommandHandler.
        endpoint.write(reused);
        Thread.sleep(2);
        endpoint.write(reused);

        assertThat(ConnectionTestUtil.getDisconnectedBuffer(endpoint)).containsExactly(reused, reused);
        Long first = endpoint.takeQueueLatencyStart(reused);
        Long second = endpoint.takeQueueLatencyStart(reused);
        assertThat(first).isNotNull();
        assertThat(second).isNotNull().isGreaterThan(first);
        assertThat(endpoint.takeQueueLatencyStart(reused)).isNull();
    }

    @Test
    void rejectingLaterAttemptPreservesEarlierAcceptedTimestamp() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        AtomicLong attempts = new AtomicLong();
        ClientOptions options = ClientOptions.builder()
                .replayFilter(command -> attempts.incrementAndGet() == 2)
                .build();
        DefaultEndpoint endpoint = new DefaultEndpoint(options, clientResources);
        Command<String, String, String> reused = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));

        endpoint.write(reused);
        endpoint.write(reused); // The replay filter rejects this later attempt.

        assertThat(ConnectionTestUtil.getDisconnectedBuffer(endpoint)).containsExactly(reused);
        assertThat(endpoint.takeQueueLatencyStart(reused)).isNotNull();
        assertThat(endpoint.takeQueueLatencyStart(reused)).isNull();
    }

    @Test
    void separateManualFlushWritesKeepBothQueueTimestamps() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        endpoint.setAutoFlushCommands(false);
        Command<String, String, String> reused = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));

        endpoint.write(reused);
        endpoint.write(reused);

        assertThat(endpoint.takeQueueLatencyStart(reused)).isNotNull();
        assertThat(endpoint.takeQueueLatencyStart(reused)).isNotNull();
        assertThat(endpoint.takeQueueLatencyStart(reused)).isNull();
    }

    @Test
    void separateConnectedWritesKeepBothQueueTimestamps() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        endpoint.setConnectionFacade(connectionFacade);
        when(channel.isActive()).thenReturn(true);
        endpoint.notifyChannelActive(channel);
        Command<String, String, String> reused = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));

        endpoint.write(reused);
        endpoint.write(reused);

        verify(channel, times(2)).writeAndFlush(reused);
        assertThat(endpoint.takeQueueLatencyStart(reused)).isNotNull();
        assertThat(endpoint.takeQueueLatencyStart(reused)).isNotNull();
        assertThat(endpoint.takeQueueLatencyStart(reused)).isNull();
    }

    @Test
    void olderAttemptCompletionCannotRemoveNewAcceptanceTimestamp() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        DelayedCompletionCommand queued = new DelayedCompletionCommand();

        endpoint.write(queued);
        endpoint.write(queued);

        queued.done = true;
        queued.completeListener(0);

        // The first acceptance's completion callback must not clear the second timestamp.
        assertThat(endpoint.takeQueueLatencyStart(queued)).isNotNull();
        assertThat(endpoint.takeQueueLatencyStart(queued)).isNull();
    }

    @Test
    void innerCompletionDoesNotRemovePendingRedirectTimestamp() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        DelayedCompletionCommand queued = new DelayedCompletionCommand();

        endpoint.write(queued);
        queued.completeListener(0);

        // Like ClusterCommand, the outer command remains writable during an inner completion.
        assertThat(endpoint.takeQueueLatencyStart(queued)).isNotNull();

        endpoint.write(queued);
        queued.done = true;
        queued.completeListener(1);
        assertThat(endpoint.takeQueueLatencyStart(queued)).isNull();
    }

    private static final class DelayedCompletionCommand extends CommandWrapper<String, String, String> {

        private final List<BiConsumer<? super String, Throwable>> listeners = new ArrayList<>();

        private boolean done;

        DelayedCompletionCommand() {
            super(new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8)));
        }

        @Override
        public void onComplete(BiConsumer<? super String, Throwable> action) {
            listeners.add(action);
        }

        @Override
        public boolean isDone() {
            return done;
        }

        void completeListener(int index) {
            listeners.get(index).accept(null, null);
        }

    }

    @Test
    void queueingBatchUsesSameAcceptanceTimeAndDisabledHasNoStamp() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint enabled = new DefaultEndpoint(ClientOptions.create(), clientResources);
        RedisCommand<String, String, String> first = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        RedisCommand<String, String, String> second = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        enabled.write(Arrays.asList(first, second));
        Long firstStamp = enabled.takeQueueLatencyStart(first);
        assertThat(firstStamp).isNotNull().isEqualTo(enabled.takeQueueLatencyStart(second));

        when(recorder.isQueueLatencyEnabled()).thenReturn(false);
        DefaultEndpoint disabled = new DefaultEndpoint(ClientOptions.create(), clientResources);
        disabled.write(new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8)));
        assertThat(disabled.takeQueueLatencyStart(first)).isNull();
    }

    @Test
    void queueingTimestampsUseCommandIdentityRatherThanWrapperEquality() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        RedisCommand<String, String, String> original = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        RedisCommand<String, String, String> wrapper = new LatencyMeteredCommand<>(original);

        endpoint.write(original);
        assertThat(endpoint.takeQueueLatencyStart(wrapper)).isNull();
        assertThat(endpoint.takeQueueLatencyStart(original)).isNotNull();

        // A later acceptance of the original must not be consumed by an earlier wrapper.
        endpoint.write(original);
        assertThat(endpoint.takeQueueLatencyStart(wrapper)).isNull();
        assertThat(endpoint.takeQueueLatencyStart(original)).isNotNull();

        endpoint.write(wrapper);
        assertThat(endpoint.takeQueueLatencyStart(original)).isNull();
        assertThat(endpoint.takeQueueLatencyStart(wrapper)).isNotNull();
    }

    @Test
    void closingConnectionPreservesTimestampsForPendingEventLoopWrites() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        endpoint.setConnectionFacade(connectionFacade);

        when(channel.isActive()).thenReturn(true);
        DefaultChannelPromise closePromise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        when(channel.close()).thenReturn(closePromise);
        endpoint.notifyChannelActive(channel);

        RedisCommand<String, String, String> first = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        RedisCommand<String, String, String> second = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        endpoint.write(first);
        endpoint.write(second);

        endpoint.closeAsync();

        // The channel has not closed yet; an outbound event-loop task can still dispatch.
        assertThat(endpoint.takeQueueLatencyStart(first)).isNotNull();

        closePromise.setSuccess();
        assertThat(endpoint.takeQueueLatencyStart(second)).isNull();
    }

    @Test
    void cancelledBufferedAsyncCommandReleasesTimestampWithoutDispatch() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        AsyncCommand<String, String, String> future = new AsyncCommand<>(
                new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8)));

        endpoint.write(future);
        future.cancel();

        assertThat(endpoint.takeQueueLatencyStart(future)).isNull();
    }

    @Test
    void failedBufferedAsyncCommandReleasesTimestampWithoutDispatch() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        AsyncCommand<String, String, String> future = new AsyncCommand<>(
                new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8)));

        endpoint.write(future);
        future.completeExceptionally(new IllegalStateException("timed out before send"));

        assertThat(endpoint.takeQueueLatencyStart(future)).isNull();
    }

    @Test
    void cancelledInnerClusterStyleCommandIsRemovedFromBuffer() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        AsyncCommand<String, String, String> inner = new AsyncCommand<>(
                new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8)));
        // A ClusterCommand reports outer completion separately from its inner future.
        CommandWrapper<String, String, String> outer = new CommandWrapper<String, String, String>(inner) {

            @Override
            public boolean isDone() {
                return false;
            }

        };

        endpoint.write(outer);
        inner.cancel();

        assertThat(outer.isDone()).isFalse();
        assertThat(outer.isCancelled()).isTrue();
        assertThat(endpoint.takeQueueLatencyStart(outer)).isNull();
        assertThat(endpoint.drainCommands()).doesNotContain(outer);
    }

    @Test
    void cancelledBufferedDecoratedCommandReleasesTimestampWithoutDispatch() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        CommandWrapper<String, String, String> decorated = new CommandWrapper<>(
                new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8)));

        endpoint.write(decorated);
        decorated.cancel();

        assertThat(endpoint.takeQueueLatencyStart(decorated)).isNull();
    }

    @Test
    void protectedModeFailureReleasesBufferedPlainCommandTimestamp() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        endpoint.setConnectionFacade(connectionFacade);

        Command<String, String, String> buffered = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        endpoint.write(buffered);
        RedisConnectionException error = new RedisConnectionException("DENIED Redis is running in protected mode");

        endpoint.notifyException(error);

        assertThat(buffered.exception).isSameAs(error);
        assertThat(endpoint.takeQueueLatencyStart(buffered)).isNull();
        assertThat(endpoint.drainCommands()).isEmpty();
    }

    @Test
    void disconnectedBufferOvercommitReleasesOnlyRejectedCommandTimestamps() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.builder().requestQueueSize(1).build(),
                clientResources);
        endpoint.setConnectionFacade(connectionFacade);

        // Retain a tracked command that the requeue will accept.
        Command<String, String, String> retained = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        endpoint.setAutoFlushCommands(false);
        endpoint.write(retained);
        Queue<RedisCommand<?, ?, ?>> queued = LettuceFactories.newConcurrentQueue(1);
        queued.addAll(endpoint.drainCommands());

        // Put a second tracked command in the disconnected buffer. Moving both into a
        // capacity-one buffer accepts the queued command but rejects this second command.
        endpoint.setAutoFlushCommands(true);
        Command<String, String, String> rejected = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        endpoint.write(rejected);

        endpoint.notifyDrainQueuedCommands(() -> queued);

        assertThat(ConnectionTestUtil.getDisconnectedBuffer(endpoint)).contains(retained).doesNotContain(rejected);
        assertThat(retained.exception).isNull();
        assertThat(rejected.exception).isInstanceOf(IllegalStateException.class);
        assertThat(endpoint.takeQueueLatencyStart(rejected)).isNull();
        assertThat(endpoint.takeQueueLatencyStart(retained)).isNotNull();
    }

    @Test
    void concurrentDisconnectedEnqueueFailureDropsOnlyRejectedStamp() throws Exception {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.builder().requestQueueSize(1).build(),
                clientResources);
        Command<String, String, String> first = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        Command<String, String, String> second = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2));

        try {
            CompletableFuture<Void> a = CompletableFuture.runAsync(() -> endpoint.write(first), executor);
            CompletableFuture<Void> b = CompletableFuture.runAsync(() -> endpoint.write(second), executor);
            CompletableFuture.allOf(a.handle((result, failure) -> null), b.handle((result, failure) -> null))
                    .get(10, TimeUnit.SECONDS);

            Queue<RedisCommand<?, ?, ?>> buffer = ConnectionTestUtil.getDisconnectedBuffer(endpoint);
            assertThat(buffer).hasSize(1);
            RedisCommand<?, ?, ?> accepted = buffer.peek();
            RedisCommand<?, ?, ?> rejected = accepted == first ? second : first;
            assertThat(endpoint.takeQueueLatencyStart(accepted)).isNotNull();
            assertThat(endpoint.takeQueueLatencyStart(rejected)).isNull();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentManualFlushEnqueueFailureDropsOnlyRejectedStamp() throws Exception {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.builder().requestQueueSize(1).build(),
                clientResources);
        endpoint.setAutoFlushCommands(false);
        Command<String, String, String> first = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        Command<String, String, String> second = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2));

        try {
            CompletableFuture<Void> a = CompletableFuture.runAsync(() -> endpoint.write(first), executor);
            CompletableFuture<Void> b = CompletableFuture.runAsync(() -> endpoint.write(second), executor);
            CompletableFuture.allOf(a.handle((result, failure) -> null), b.handle((result, failure) -> null))
                    .get(10, TimeUnit.SECONDS);

            Queue<RedisCommand<?, ?, ?>> buffer = ReflectionTestUtils.getField(endpoint, "commandBuffer");
            assertThat(buffer).hasSize(1);
            RedisCommand<?, ?, ?> accepted = buffer.peek();
            RedisCommand<?, ?, ?> rejected = accepted == first ? second : first;
            assertThat(endpoint.takeQueueLatencyStart(accepted)).isNotNull();
            assertThat(endpoint.takeQueueLatencyStart(rejected)).isNull();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentManualFlushWritesOfOneCommandKeepStampOrder() throws Exception {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        CountDownLatch firstEnqueue = new CountDownLatch(1);
        CountDownLatch secondEnqueue = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger enqueues = new AtomicInteger();
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources) {
            @Override
            protected <C extends RedisCommand<?, ?, T>, T> void writeToBuffer(C command) {
                if (enqueues.incrementAndGet() == 1) {
                    firstEnqueue.countDown();
                    awaitConcurrentQueueLatencyGate(releaseFirst);
                } else {
                    secondEnqueue.countDown();
                }
                super.writeToBuffer(command);
            }
        };
        endpoint.setAutoFlushCommands(false);
        Command<String, String, String> reused = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2));

        try {
            CompletableFuture<Void> a = CompletableFuture.runAsync(() -> endpoint.write(reused), executor);
            assertThat(firstEnqueue.await(5, TimeUnit.SECONDS)).isTrue();
            CountDownLatch secondStarted = new CountDownLatch(1);
            CompletableFuture<Void> b = CompletableFuture.runAsync(() -> {
                secondStarted.countDown();
                endpoint.write(reused);
            }, executor);
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(secondEnqueue.await(200, TimeUnit.MILLISECONDS)).isFalse();

            releaseFirst.countDown();
            CompletableFuture.allOf(a, b).get(10, TimeUnit.SECONDS);
            assertThat(ConnectionTestUtil.getDisconnectedBuffer(endpoint)).isEmpty();
            Queue<RedisCommand<?, ?, ?>> buffer = ReflectionTestUtils.getField(endpoint, "commandBuffer");
            assertThat(buffer).containsExactly(reused, reused);
            Long first = endpoint.takeQueueLatencyStart(reused);
            Long second = endpoint.takeQueueLatencyStart(reused);
            assertThat(first).isNotNull();
            assertThat(second).isGreaterThan(first);
            assertThat(endpoint.takeQueueLatencyStart(reused)).isNull();
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentConnectedWritesOfOneCommandKeepStampOrder() throws Exception {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);
        CountDownLatch firstEnqueue = new CountDownLatch(1);
        CountDownLatch secondEnqueue = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger enqueues = new AtomicInteger();
        when(channel.writeAndFlush(any())).thenAnswer(invocation -> {
            if (enqueues.incrementAndGet() == 1) {
                firstEnqueue.countDown();
                awaitConcurrentQueueLatencyGate(releaseFirst);
            } else {
                secondEnqueue.countDown();
            }
            queue.add(invocation.getArgument(0));
            return promise;
        });
        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        endpoint.setConnectionFacade(connectionFacade);
        when(channel.isActive()).thenReturn(true);
        endpoint.notifyChannelActive(channel);
        Command<String, String, String> reused = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2));

        try {
            CompletableFuture<Void> a = CompletableFuture.runAsync(() -> endpoint.write(reused), executor);
            assertThat(firstEnqueue.await(5, TimeUnit.SECONDS)).isTrue();
            CountDownLatch secondStarted = new CountDownLatch(1);
            CompletableFuture<Void> b = CompletableFuture.runAsync(() -> {
                secondStarted.countDown();
                endpoint.write(reused);
            }, executor);
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(secondEnqueue.await(200, TimeUnit.MILLISECONDS)).isFalse();

            releaseFirst.countDown();
            CompletableFuture.allOf(a, b).get(10, TimeUnit.SECONDS);
            verify(channel, times(2)).writeAndFlush(reused);
            Long first = endpoint.takeQueueLatencyStart(reused);
            Long second = endpoint.takeQueueLatencyStart(reused);
            assertThat(first).isNotNull();
            assertThat(second).isGreaterThan(first);
            assertThat(endpoint.takeQueueLatencyStart(reused)).isNull();
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    private static void awaitConcurrentQueueLatencyGate(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for queue latency regression gate");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Test
    void unvisitedDisconnectedBatchCommandsHaveNoQueueTimestampsAfterPartialEnqueueFailure() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);

        Command<String, String, String> first = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        Command<String, String, String> failing = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        Command<String, String, String> unvisited = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        Command<String, String, String> competingOne = new Command<>(CommandType.GET,
                new StatusOutput<>(StringCodec.UTF8));
        Command<String, String, String> competingTwo = new Command<>(CommandType.GET,
                new StatusOutput<>(StringCodec.UTF8));

        AtomicReference<Queue<RedisCommand<?, ?, ?>>> bufferRef = new AtomicReference<>();
        ClientOptions options = ClientOptions.builder().requestQueueSize(3).replayFilter(cmd -> {
            if (cmd == failing) {
                // Simulate two concurrent enqueues after the batch's validation.
                bufferRef.get().add(competingOne);
                bufferRef.get().add(competingTwo);
            }
            return false;
        }).build();
        DefaultEndpoint endpoint = new DefaultEndpoint(options, clientResources);
        Queue<RedisCommand<?, ?, ?>> buffer = ConnectionTestUtil.getDisconnectedBuffer(endpoint);
        bufferRef.set(buffer);

        assertThatThrownBy(() -> endpoint.write(Arrays.asList(first, failing, unvisited)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(buffer).contains(first, competingOne, competingTwo).doesNotContain(failing, unvisited);
        assertThat(endpoint.takeQueueLatencyStart(first)).isNotNull();
        assertThat(endpoint.takeQueueLatencyStart(failing)).isNull();
        assertThat(endpoint.takeQueueLatencyStart(unvisited)).isNull();
    }

    @Test
    void unvisitedManualFlushBatchCommandsHaveNoQueueTimestampsAfterPartialEnqueueFailure() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);

        Command<String, String, String> first = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        Command<String, String, String> failing = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        Command<String, String, String> unvisited = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8));
        Command<String, String, String> competingOne = new Command<>(CommandType.GET,
                new StatusOutput<>(StringCodec.UTF8));
        Command<String, String, String> competingTwo = new Command<>(CommandType.GET,
                new StatusOutput<>(StringCodec.UTF8));

        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.builder().requestQueueSize(3).build(),
                clientResources) {

            @Override
            protected <C extends RedisCommand<?, ?, T>, T> void writeToBuffer(C command) {
                if (command == failing) {
                    Queue<RedisCommand<?, ?, ?>> buffer = ReflectionTestUtils.getField(this, "commandBuffer");
                    buffer.add(competingOne);
                    buffer.add(competingTwo);
                }
                super.writeToBuffer(command);
            }

        };
        endpoint.setAutoFlushCommands(false);

        assertThatThrownBy(() -> endpoint.write(Arrays.asList(first, failing, unvisited)))
                .isInstanceOf(IllegalStateException.class);

        Queue<RedisCommand<?, ?, ?>> buffer = ReflectionTestUtils.getField(endpoint, "commandBuffer");
        assertThat(buffer).contains(first, competingOne, competingTwo).doesNotContain(failing, unvisited);
        assertThat(endpoint.takeQueueLatencyStart(first)).isNotNull();
        assertThat(endpoint.takeQueueLatencyStart(failing)).isNull();
        assertThat(endpoint.takeQueueLatencyStart(unvisited)).isNull();
    }

    @Test
    void directConnectedBatchRejectsDuplicateInstanceButDispatchesOtherCommands() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);

        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        endpoint.setConnectionFacade(connectionFacade);
        when(channel.isActive()).thenReturn(true);
        endpoint.notifyChannelActive(channel);

        Command<String, String, String> duplicate = new Command<>(CommandType.GET,
                new StatusOutput<>(StringCodec.UTF8));
        Command<String, String, String> other = new Command<>(CommandType.SET,
                new StatusOutput<>(StringCodec.UTF8));

        Collection<RedisCommand<String, String, ?>> dispatched = endpoint
                .write(Arrays.asList(duplicate, other, duplicate));

        assertThat(dispatched).containsExactly(other);
        assertThat(duplicate.exception).isInstanceOf(RedisException.class);
        assertThat(endpoint.takeQueueLatencyStart(duplicate)).isNull();
        assertThat(endpoint.takeQueueLatencyStart(other)).isNotNull();
        verify(channel, never()).write(duplicate);
        verify(channel).write(other);
    }

    @Test
    void duplicateInstancesAreRejectedBeforeDisconnectedBatchEnqueue() {
        CommandLatencyRecorder recorder = mock(CommandLatencyRecorder.class);
        when(recorder.isQueueLatencyEnabled()).thenReturn(true);
        when(clientResources.commandLatencyRecorder()).thenReturn(recorder);

        DefaultEndpoint endpoint = new DefaultEndpoint(ClientOptions.create(), clientResources);
        Command<String, String, String> duplicate = new Command<>(CommandType.GET,
                new StatusOutput<>(StringCodec.UTF8));
        Command<String, String, String> other = new Command<>(CommandType.SET,
                new StatusOutput<>(StringCodec.UTF8));

        Collection<RedisCommand<String, String, ?>> dispatched = endpoint
                .write(Arrays.asList(duplicate, other, duplicate));

        assertThat(dispatched).containsExactly(other);
        assertThat(ConnectionTestUtil.getDisconnectedBuffer(endpoint)).containsExactly(other);
        assertThat(duplicate.exception).isInstanceOf(RedisException.class);
        assertThat(endpoint.takeQueueLatencyStart(duplicate)).isNull();
        assertThat(endpoint.takeQueueLatencyStart(other)).isNotNull();
    }

    @Test
    void writeShouldGuaranteeFIFOOrder() {
        sut.write(Collections.singletonList(new Command<>(CommandType.SELECT, new StatusOutput<>(StringCodec.UTF8))));

        sut.registerConnectionWatchdog(connectionWatchdog);
        doAnswer(i -> sut.write(new Command<>(CommandType.AUTH, new StatusOutput<>(StringCodec.UTF8)))).when(connectionWatchdog)
                .arm();
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

        assertThat(queue).hasSize(2).first().hasFieldOrPropertyWithValue("type", CommandType.SELECT);
        assertThat(queue).hasSize(2).last().hasFieldOrPropertyWithValue("type", CommandType.AUTH);
    }

    @Test
    void writeConnectedShouldWriteCommandToChannel() {

        when(channel.isActive()).thenReturn(true);

        sut.notifyChannelActive(channel);
        sut.write(command);

        assertThat(ConnectionTestUtil.getQueueSize(sut)).isEqualTo(1);
        verify(channel).writeAndFlush(command);
    }

    @Test
    void writeDisconnectedShouldBufferCommands() {

        sut.write(command);

        assertThat(ConnectionTestUtil.getDisconnectedBuffer(sut)).contains(command);

        verify(channel, never()).writeAndFlush(any());
    }

    @Test
    void notifyChannelActiveActivatesFacade() {

        sut.notifyChannelActive(channel);

        verify(connectionFacade).activated();
    }

    @Test
    void notifyChannelActiveArmsConnectionWatchdog() {

        sut.registerConnectionWatchdog(connectionWatchdog);

        sut.notifyChannelActive(channel);

        verify(connectionWatchdog).arm();
    }

    @Test
    void notifyChannelInactiveDeactivatesFacade() {

        sut.notifyChannelInactive(channel);

        verify(connectionFacade).deactivated();
    }

    @Test
    void notifyExceptionShouldStoreException() {

        sut.notifyException(new IllegalStateException());
        sut.write(command);

        assertThat(command.exception).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void notifyChannelActiveClearsStoredException() {

        sut.notifyException(new IllegalStateException());
        sut.notifyChannelActive(channel);
        sut.write(command);

        assertThat(command.exception).isNull();
    }

    @Test
    void notifyDrainQueuedCommandsShouldBufferCommands() {

        Queue<RedisCommand<?, ?, ?>> q = LettuceFactories.newConcurrentQueue(100);
        q.add(command);

        sut.notifyDrainQueuedCommands(() -> q);

        assertThat(ConnectionTestUtil.getDisconnectedBuffer(sut)).contains(command);
        verify(channel, never()).write(any());
    }

    @Test
    void notifyDrainQueuedCommandsShouldWriteCommands() {

        when(channel.isActive()).thenReturn(true);

        Queue<RedisCommand<?, ?, ?>> q = LettuceFactories.newConcurrentQueue(100);
        q.add(command);

        sut.notifyChannelActive(channel);
        sut.notifyDrainQueuedCommands(() -> q);

        verify(channel).write(command);
        verify(channel).flush();
    }

    @Test
    void shouldCancelCommandsOnEncoderException() {

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
    void writeShouldRejectCommandsInDisconnectedState() {

        sut = new DefaultEndpoint(ClientOptions.builder() //
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS) //
                .build(), clientResources);

        sut.write(command);
        assertThat(command.exception).hasMessageContaining("Commands are rejected");
    }

    @Test
    void writeShouldRejectCommandsInClosedState() {

        sut.close();

        sut.write(command);
        assertThat(command.exception).hasMessageContaining("Connection is closed");
    }

    @Test
    void writeWithoutAutoReconnectShouldRejectCommandsInDisconnectedState() {

        sut = new DefaultEndpoint(ClientOptions.builder() //
                .autoReconnect(false) //
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.DEFAULT) //
                .build(), clientResources);

        sut.write(command);
        assertThat(command.exception).hasMessageContaining("Commands are rejected");
    }

    @Test
    void closeCleansUpResources() {

        ChannelFuture future = mock(ChannelFuture.class);
        when(future.isSuccess()).thenReturn(true);
        when(channel.close()).thenReturn(future);

        sut.notifyChannelActive(channel);
        sut.registerConnectionWatchdog(connectionWatchdog);

        sut.close();

        verify(channel).close();
        verify(connectionWatchdog).prepareClose();
    }

    @Test
    void closeAllowsOnlyOneCall() {

        ChannelFuture future = mock(ChannelFuture.class);
        when(future.isSuccess()).thenReturn(true);
        when(channel.close()).thenReturn(future);

        sut.notifyChannelActive(channel);
        sut.registerConnectionWatchdog(connectionWatchdog);

        sut.close();
        sut.close();

        verify(channel).close();
        verify(connectionWatchdog).prepareClose();
    }

    @Test
    void retryListenerCompletesSuccessfullyAfterDeferredRequeue() {

        DefaultEndpoint.RetryListener listener = DefaultEndpoint.RetryListener.newInstance(sut, command);

        ChannelFuture future = mock(ChannelFuture.class);
        EventLoop eventLoopGroup = mock(EventLoop.class);

        when(future.isSuccess()).thenReturn(false);
        when(future.cause()).thenReturn(new ClosedChannelException());
        when(channel.eventLoop()).thenReturn(eventLoopGroup);
        when(channel.close()).thenReturn(mock(ChannelFuture.class));

        sut.notifyChannelActive(channel);
        sut.closeAsync();

        listener.operationComplete(future);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(eventLoopGroup).submit(runnableCaptor.capture());

        runnableCaptor.getValue().run();

        assertThat(command.exception).isInstanceOf(RedisException.class);
    }

    @Test
    void retryListenerFailsCommandFastWhenRejectCommandsWhileDisconnected() {

        sut = new DefaultEndpoint(ClientOptions.builder() //
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS) //
                .build(), clientResources);
        sut.setConnectionFacade(connectionFacade);

        DefaultEndpoint.RetryListener listener = DefaultEndpoint.RetryListener.newInstance(sut, command);

        ChannelFuture future = mock(ChannelFuture.class);
        when(future.isSuccess()).thenReturn(false);
        when(future.cause()).thenReturn(new ClosedChannelException());

        listener.operationComplete(future);

        assertThat(command.exception).isInstanceOf(RedisException.class)
                .hasMessageContaining("Currently not connected. Commands are rejected.");
        verify(channel, never()).eventLoop();
    }

    @Test
    void retryListenerRequeuesUnderAcceptCommands() {

        sut = new DefaultEndpoint(ClientOptions.builder() //
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS) //
                .build(), clientResources);
        sut.setConnectionFacade(connectionFacade);

        DefaultEndpoint.RetryListener listener = DefaultEndpoint.RetryListener.newInstance(sut, command);

        ChannelFuture future = mock(ChannelFuture.class);
        EventLoop eventLoop = mock(EventLoop.class);
        when(future.isSuccess()).thenReturn(false);
        when(future.cause()).thenReturn(new ClosedChannelException());
        when(channel.eventLoop()).thenReturn(eventLoop);

        sut.notifyChannelActive(channel);
        listener.operationComplete(future);

        verify(eventLoop).submit(any(Runnable.class));
        assertThat(command.exception).isNull();
    }

    @Test
    void retryListenerDoesNotRetryCompletedCommands() {

        DefaultEndpoint.RetryListener listener = DefaultEndpoint.RetryListener.newInstance(sut, command);

        when(channel.eventLoop()).thenReturn(mock(EventLoop.class));

        command.complete();
        promise.tryFailure(new Exception());

        listener.operationComplete(promise);

        verify(channel, never()).writeAndFlush(command);
    }

    @Test
    void shouldWrapActivationCommands() {

        when(channel.isActive()).thenReturn(true);
        doAnswer(i -> {

            sut.write(new Command<>(CommandType.AUTH, new StatusOutput<>(StringCodec.UTF8)));
            sut.write(Collections.singletonList(new Command<>(CommandType.SELECT, new StatusOutput<>(StringCodec.UTF8))));
            return null;
        }).when(connectionFacade).activated();

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

        assertThat(queue).hasSize(2).hasOnlyElementsOfTypes(ActivationCommand.class);
    }

    @Test
    void shouldNotReplayActivationCommands() {

        when(channel.isActive()).thenReturn(true);
        ConnectionTestUtil.getDisconnectedBuffer(sut)
                .add(new ActivationCommand<>(new Command<>(CommandType.SELECT, new StatusOutput<>(StringCodec.UTF8))));
        ConnectionTestUtil.getDisconnectedBuffer(sut).add(new LatencyMeteredCommand<>(
                new ActivationCommand<>(new Command<>(CommandType.SUBSCRIBE, new StatusOutput<>(StringCodec.UTF8)))));

        doAnswer(i -> {

            sut.write(new Command<>(CommandType.AUTH, new StatusOutput<>(StringCodec.UTF8)));
            return null;
        }).when(connectionFacade).activated();

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

        assertThat(queue).hasSize(1).extracting(RedisCommand::getType).containsOnly(CommandType.AUTH);
    }

    @Test
    void testMTCConcurrentConcurrentWrite() throws Throwable {

        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(4));

        try {
            MTCConcurrentConcurrentWrite instance = new MTCConcurrentConcurrentWrite(command, clientResources);

            CompletableFuture<Void> part1 = CompletableFuture.runAsync(instance::thread1, executor);
            CompletableFuture<Void> part2 = CompletableFuture.runAsync(instance::thread2, executor);

            CompletableFuture<Void> sync = CompletableFuture.allOf(part1, part2);
            sync.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Test of concurrent access to locks. Two concurrent writes.
     */
    static class MTCConcurrentConcurrentWrite {

        private final Command<String, String, String> command;

        private final TestableEndpoint handler;

        private final CountDownLatch phase1 = new CountDownLatch(2);

        private final CountDownLatch phase2 = new CountDownLatch(2);

        private final CountDownLatch phase3 = new CountDownLatch(2);

        MTCConcurrentConcurrentWrite(Command<String, String, String> command, ClientResources clientResources) {

            this.command = command;

            handler = new TestableEndpoint(ClientOptions.create(), clientResources) {

                @Override
                protected <C extends RedisCommand<?, ?, T>, T> void writeToBuffer(C command) {

                    waitAndProgress(phase2);

                    Object sharedLock = ReflectionTestUtils.getField(this, "sharedLock");
                    AtomicLong writers = ReflectionTestUtils.getField(sharedLock, "writers");
                    assertThat(writers.get()).isEqualTo(2);
                    waitAndProgress(phase3);
                    super.writeToBuffer(command);
                }

            };
        }

        public void thread1() {

            waitAndProgress(phase1);
            handler.write(command);
        }

        public void thread2() {

            waitAndProgress(phase1);
            handler.write(command);
        }

        private void waitAndProgress(CountDownLatch latch) {
            latch.countDown();
            try {
                latch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

    }

    static class TestableEndpoint extends DefaultEndpoint {

        /**
         * Create a new {@link DefaultEndpoint}.
         *
         * @param clientOptions client options for this connection, must not be {@code null}.
         * @param clientResources client resources for this connection, must not be {@code null}.
         */
        TestableEndpoint(ClientOptions clientOptions, ClientResources clientResources) {
            super(clientOptions, clientResources);
        }

    }

}
