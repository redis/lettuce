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

import static io.lettuce.core.protocol.CommandHandler.*;
import static io.lettuce.core.protocol.MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionEvents;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceFactories;
import io.lettuce.core.metrics.CommandLatencyRecorder;
import io.lettuce.core.resource.ClientResources;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.EncoderException;
import io.netty.util.Recycler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Default {@link Endpoint} implementation.
 *
 * @author Mark Paluch
 * @author Vaibhav Vashisht
 */
public class DefaultEndpoint implements RedisChannelWriter, Endpoint, PushHandler {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultEndpoint.class);

    private static final AtomicLong ENDPOINT_COUNTER = new AtomicLong();

    private static final AtomicIntegerFieldUpdater<DefaultEndpoint> QUEUE_SIZE = AtomicIntegerFieldUpdater
            .newUpdater(DefaultEndpoint.class, "queueSize");

    private static final AtomicIntegerFieldUpdater<DefaultEndpoint> STATUS = AtomicIntegerFieldUpdater
            .newUpdater(DefaultEndpoint.class, "status");

    private static final int ST_OPEN = 0;

    private static final int ST_CLOSED = 1;

    protected volatile Channel channel;

    private final Reliability reliability;

    private final Predicate<RedisCommand<?, ?, ?>> replayFilter;

    private final ClientOptions clientOptions;

    private final ClientResources clientResources;

    private final Queue<RedisCommand<?, ?, ?>> disconnectedBuffer;

    private final Queue<RedisCommand<?, ?, ?>> commandBuffer;

    // Connection-local, weak identity keys keep wrappers from consuming each other's timestamps.
    private final Map<IdentityWeakReference, ArrayDeque<QueueLatencyStamp>> queueingTimestamps;

    private final ReferenceQueue<RedisCommand<?, ?, ?>> queueingTimestampReferences = new ReferenceQueue<>();

    private final boolean boundedQueues;

    private final boolean rejectCommandsWhileDisconnected;

    private final long endpointId = ENDPOINT_COUNTER.incrementAndGet();

    private final List<PushListener> pushListeners = new CopyOnWriteArrayList<>();

    private final SharedLock sharedLock = new SharedLock();

    // With queue metrics enabled, a command's timing stamp and its enqueue/write
    // must be serialized with other submissions of that same instance.
    private final Map<IdentityWeakReference, AdmissionLock> queueingAdmissionLocks;

    private final ReferenceQueue<RedisCommand<?, ?, ?>> admissionLockReferences = new ReferenceQueue<>();

    private long nextAdmissionLockId;

    private final boolean debugEnabled = logger.isDebugEnabled();

    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    private String logPrefix;

    private boolean autoFlushCommands = true;

    private boolean inActivation = false;

    private ConnectionWatchdog connectionWatchdog;

    private ConnectionFacade connectionFacade;

    private volatile Throwable connectionError;

    // access via QUEUE_SIZE
    @SuppressWarnings("unused")
    private volatile int queueSize = 0;

    // access via STATUS
    @SuppressWarnings("unused")
    private volatile int status = ST_OPEN;

    private final String cachedEndpointId;

    /**
     * Create a new {@link DefaultEndpoint}.
     *
     * @param clientOptions client options for this connection, must not be {@code null}.
     * @param clientResources client resources for this connection, must not be {@code null}.
     */
    public DefaultEndpoint(ClientOptions clientOptions, ClientResources clientResources) {

        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        LettuceAssert.notNull(clientOptions, "ClientResources must not be null");

        this.clientOptions = clientOptions;
        this.clientResources = clientResources;
        this.reliability = clientOptions.isAutoReconnect() ? Reliability.AT_LEAST_ONCE : Reliability.AT_MOST_ONCE;
        this.replayFilter = clientOptions.getReplayFilter();
        this.disconnectedBuffer = LettuceFactories.newConcurrentQueue(clientOptions.getRequestQueueSize());
        this.commandBuffer = LettuceFactories.newConcurrentQueue(clientOptions.getRequestQueueSize());
        CommandLatencyRecorder recorder = clientResources.commandLatencyRecorder();
        this.queueingTimestamps = recorder != null && recorder.isQueueLatencyEnabled()
                ? Collections.synchronizedMap(new HashMap<>())
                : null;
        this.queueingAdmissionLocks = queueingTimestamps == null ? null : new HashMap<>();
        this.boundedQueues = clientOptions.getRequestQueueSize() != Integer.MAX_VALUE;
        this.rejectCommandsWhileDisconnected = clientOptions.isRejectCommandsWhileDisconnected();
        this.cachedEndpointId = "0x" + Long.toHexString(endpointId);
    }

    @Override
    public void setConnectionFacade(ConnectionFacade connectionFacade) {
        this.connectionFacade = connectionFacade;
    }

    @Override
    public ClientResources getClientResources() {
        return clientResources;
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        this.autoFlushCommands = autoFlush;
    }

    @Override
    public void addListener(PushListener listener) {
        pushListeners.add(listener);
    }

    @Override
    public void removeListener(PushListener listener) {
        pushListeners.remove(listener);
    }

    @Override
    public List<PushListener> getPushListeners() {
        return pushListeners;
    }

    @Override
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {

        LettuceAssert.notNull(command, "Command must not be null");

        RedisException validation = validateWrite(1);
        if (validation != null) {
            command.completeExceptionally(validation);
            return command;
        }

        // Include time waiting for the endpoint lock in the queueing interval.
        long acceptedAt = queueingTimestamps == null ? 0 : System.nanoTime();
        List<AdmissionLock> acquiredAdmissionLocks = Collections.emptyList();

        try {
            acquiredAdmissionLocks = acquireAdmissionLocks(command);

            // Waiting for another attempt can temporarily release the SharedLock writer
            // reservation. Close can finish during that gap, so recheck after admission.
            if (isClosed()) {
                command.completeExceptionally(new RedisException("Connection is closed"));
                return command;
            }

            if (inActivation) {
                command = processActivationCommand(command);
            }

            if (queueingTimestamps != null) {
                trackQueueLatency(command, acceptedAt);
            }

            if (autoFlushCommands) {
                Channel channel = this.channel;
                if (isConnected(channel)) {
                    writeToChannelAndFlush(channel, command);
                } else {
                    writeToDisconnectedBuffer(command);
                }

            } else {
                writeToBuffer(command);
            }
        } finally {
            releaseAdmissionLocks(acquiredAdmissionLocks);
            sharedLock.decrementWriters();
            if (debugEnabled) {
                logger.debug("{} write() done", logPrefix());
            }
        }

        return command;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> commands) {

        LettuceAssert.notNull(commands, "Commands must not be null");

        // Multiple occurrences of one command instance cannot each own a distinct
        // acceptance timestamp. Reject those commands before the endpoint can
        // write each occurrence as a separate Netty message.
        if (queueingTimestamps != null && commands.size() > 1) {
            Set<RedisCommand<?, ?, ?>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            Set<RedisCommand<?, ?, ?>> duplicates = Collections.newSetFromMap(new IdentityHashMap<>());

            for (RedisCommand<?, ?, ?> command : commands) {
                if (!seen.add(command)) {
                    duplicates.add(command);
                }
            }

            if (!duplicates.isEmpty()) {
                Collection<RedisCommand<K, V, ?>> filtered = new ArrayList<>(commands.size());
                for (RedisCommand<K, V, ?> command : commands) {
                    if (!duplicates.contains(command)) {
                        filtered.add(command);
                    }
                }
                for (RedisCommand<?, ?, ?> duplicate : duplicates) {
                    duplicate.completeExceptionally(new RedisException(
                            "Attempting to write duplicate command that is already enqueued: " + duplicate));
                }
                commands = filtered;
            }
        }

        RedisException validation = validateWrite(commands.size());

        if (validation != null) {
            commands.forEach(it -> it.completeExceptionally(validation));
            return (Collection<RedisCommand<K, V, ?>>) commands;
        }

        long acceptedAt = queueingTimestamps == null ? 0 : System.nanoTime();
        List<AdmissionLock> acquiredAdmissionLocks = Collections.emptyList();

        try {
            acquiredAdmissionLocks = acquireAdmissionLocks(commands);

            // A close may run while this batch waits for a command admission lock.
            // Never enqueue the resumed batch after the endpoint was closed.
            if (isClosed()) {
                RedisException closed = new RedisException("Connection is closed");
                commands.forEach(it -> it.completeExceptionally(closed));
                return (Collection<RedisCommand<K, V, ?>>) commands;
            }

            if (inActivation) {
                commands = processActivationCommands(commands);
            }

            if (autoFlushCommands) {
                Channel channel = this.channel;
                if (isConnected(channel)) {
                    // A channel write can be dispatched asynchronously, so all
                    // acceptance stamps must exist before the first write.
                    if (queueingTimestamps != null) {
                        for (RedisCommand<?, ?, ?> command : commands) {
                            trackQueueLatency(command, acceptedAt);
                        }
                    }
                    writeToChannelAndFlush(channel, commands);
                } else {
                    writeToDisconnectedBuffer(commands, acceptedAt);
                }
            } else {
                writeToBuffer(commands, acceptedAt);
            }
        } finally {
            releaseAdmissionLocks(acquiredAdmissionLocks);
            sharedLock.decrementWriters();
            if (debugEnabled) {
                logger.debug("{} write() done", logPrefix());
            }
        }

        return (Collection<RedisCommand<K, V, ?>>) commands;
    }

    Long takeQueueLatencyStart(RedisCommand<?, ?, ?> command) {
        if (queueingTimestamps == null) {
            return null;
        }

        synchronized (queueingTimestamps) {
            expungeStaleQueueingTimestamps();
            IdentityWeakReference key = new IdentityWeakReference(command, null);
            ArrayDeque<QueueLatencyStamp> attempts = queueingTimestamps.get(key);
            if (attempts == null) {
                return null;
            }
            QueueLatencyStamp stamp = attempts.removeFirst();
            if (attempts.isEmpty()) {
                queueingTimestamps.remove(key);
            }
            return stamp.acceptedAt;
        }
    }

    private void discardLatestQueueLatencyStart(RedisCommand<?, ?, ?> command) {
        if (queueingTimestamps == null) {
            return;
        }
        synchronized (queueingTimestamps) {
            IdentityWeakReference key = new IdentityWeakReference(command, null);
            ArrayDeque<QueueLatencyStamp> attempts = queueingTimestamps.get(key);
            if (attempts != null) {
                attempts.removeLast();
                if (attempts.isEmpty()) {
                    queueingTimestamps.remove(key);
                }
            }
        }
    }

    private void trackQueueLatency(RedisCommand<?, ?, ?> command, long acceptedAt) {
        if (queueingTimestamps == null || command.isDone() || command.isCancelled()) {
            return;
        }

        IdentityWeakReference key = new IdentityWeakReference(command, queueingTimestampReferences);
        QueueLatencyStamp stamp = new QueueLatencyStamp(acceptedAt);
        synchronized (queueingTimestamps) {
            expungeStaleQueueingTimestamps();
            // Separate writes of the same command instance can queue before either reaches
            // the event loop. Consume their acceptance times in dispatch (FIFO) order.
            queueingTimestamps.computeIfAbsent(key, ignored -> new ArrayDeque<>()).addLast(stamp);
        }

        // Completion of an earlier attempt must not remove later acceptance timestamps.
        if (command instanceof CompleteableCommand) {
            ((CompleteableCommand<?>) command).onComplete((result, failure) -> {
                if (command.isDone() || command.isCancelled()) {
                    synchronized (queueingTimestamps) {
                        ArrayDeque<QueueLatencyStamp> attempts = queueingTimestamps.get(key);
                        if (attempts != null && attempts.contains(stamp)) {
                            // A terminal attempt also invalidates any earlier unclaimed stamps.
                            // Later acceptances must remain untouched.
                            while (attempts.removeFirst() != stamp) {
                                // Discard older unclaimed attempts.
                            }
                            if (attempts.isEmpty()) {
                                queueingTimestamps.remove(key);
                            }
                        }
                    }
                }
            });
        }
    }

    private List<AdmissionLock> acquireAdmissionLocks(RedisCommand<?, ?, ?> command) {
        return acquireAdmissionLocks(Collections.singleton(command));
    }

    private List<AdmissionLock> acquireAdmissionLocks(Iterable<? extends RedisCommand<?, ?, ?>> commands) {
        // Register as a writer before walking a caller-provided collection.
        // If its iterator fails, write() still has a matching writer count to release.
        sharedLock.incrementWriters();
        if (queueingAdmissionLocks == null) {
            return Collections.emptyList();
        }

        List<AdmissionLock> locks = new ArrayList<>();
        Set<AdmissionLock> unique = Collections.newSetFromMap(new IdentityHashMap<>());
        synchronized (queueingAdmissionLocks) {
            IdentityWeakReference collected;
            while ((collected = (IdentityWeakReference) admissionLockReferences.poll()) != null) {
                queueingAdmissionLocks.remove(collected);
            }
            for (RedisCommand<?, ?, ?> command : commands) {
                IdentityWeakReference lookup = new IdentityWeakReference(command, null);
                AdmissionLock admission = queueingAdmissionLocks.get(lookup);
                if (admission == null) {
                    admission = new AdmissionLock(++nextAdmissionLockId);
                    queueingAdmissionLocks.put(new IdentityWeakReference(command, admissionLockReferences), admission);
                }
                if (unique.add(admission)) {
                    locks.add(admission);
                }
            }
        }
        locks.sort(Comparator.comparingLong(admission -> admission.id));

        for (;;) {
            int acquired = 0;
            while (acquired < locks.size() && locks.get(acquired).lock.tryLock()) {
                acquired++;
            }
            if (acquired == locks.size()) {
                return locks;
            }

            // A writer waiting for admission must not prevent the current lock holder
            // from acquiring SharedLock exclusively (for example, in a write callback).
            for (int i = acquired - 1; i >= 0; i--) {
                locks.get(i).lock.unlock();
            }
            sharedLock.decrementWriters();

            // Wait without being counted as a writer or holding another admission lock.
            // Release before re-entering SharedLock to avoid the opposite lock inversion.
            AdmissionLock contended = locks.get(acquired);
            contended.lock.lock();
            contended.lock.unlock();
            sharedLock.incrementWriters();
        }
    }

    private void releaseAdmissionLocks(List<AdmissionLock> locks) {
        for (int i = locks.size() - 1; i >= 0; i--) {
            locks.get(i).lock.unlock();
        }
    }

    private static final class AdmissionLock {

        final long id;

        final ReentrantLock lock = new ReentrantLock();

        AdmissionLock(long id) {
            this.id = id;
        }
    }

    private void expungeStaleQueueingTimestamps() {
        IdentityWeakReference reference;
        while ((reference = (IdentityWeakReference) queueingTimestampReferences.poll()) != null) {
            queueingTimestamps.remove(reference);
        }
    }

    private static final class QueueLatencyStamp {

        final long acceptedAt;

        QueueLatencyStamp(long acceptedAt) {
            this.acceptedAt = acceptedAt;
        }

    }

    private static final class IdentityWeakReference extends WeakReference<RedisCommand<?, ?, ?>> {

        private final int hash;

        IdentityWeakReference(RedisCommand<?, ?, ?> command, ReferenceQueue<RedisCommand<?, ?, ?>> queue) {
            super(command, queue);
            this.hash = System.identityHashCode(command);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof IdentityWeakReference)) {
                return false;
            }
            RedisCommand<?, ?, ?> command = get();
            return command != null && command == ((IdentityWeakReference) other).get();
        }

    }

    private <K, V, T> RedisCommand<K, V, T> processActivationCommand(RedisCommand<K, V, T> command) {

        if (!ActivationCommand.isActivationCommand(command)) {
            return new ActivationCommand<>(command);
        }

        return command;
    }

    private <K, V> Collection<RedisCommand<K, V, ?>> processActivationCommands(
            Collection<? extends RedisCommand<K, V, ?>> commands) {

        Collection<RedisCommand<K, V, ?>> commandsToReturn = new ArrayList<>(commands.size());

        for (RedisCommand<K, V, ?> command : commands) {

            if (!ActivationCommand.isActivationCommand(command)) {
                command = new ActivationCommand<>(command);
            }

            commandsToReturn.add(command);
        }

        return commandsToReturn;
    }

    private RedisException validateWrite(int commands) {

        if (isClosed()) {
            return new RedisException("Connection is closed");
        }

        final boolean connected = isConnected(this.channel);
        if (usesBoundedQueues()) {

            if (QUEUE_SIZE.get(this) + commands > clientOptions.getRequestQueueSize()) {
                return new RedisException("Request queue size exceeded: " + clientOptions.getRequestQueueSize()
                        + ". Commands are not accepted until the queue size drops.");
            }

            if (!connected && disconnectedBuffer.size() + commands > clientOptions.getRequestQueueSize()) {
                return new RedisException("Request queue size exceeded: " + clientOptions.getRequestQueueSize()
                        + ". Commands are not accepted until the queue size drops.");
            }

            if (connected && commandBuffer.size() + commands > clientOptions.getRequestQueueSize()) {
                return new RedisException("Command buffer size exceeded: " + clientOptions.getRequestQueueSize()
                        + ". Commands are not accepted until the queue size drops.");
            }
        }

        if (!connected && rejectCommandsWhileDisconnected) {
            return new RedisException("Currently not connected. Commands are rejected.");
        }

        return null;
    }

    private boolean usesBoundedQueues() {
        return boundedQueues;
    }

    private void writeToBuffer(Iterable<? extends RedisCommand<?, ?, ?>> commands, long acceptedAt) {

        for (RedisCommand<?, ?, ?> command : commands) {
            // Track only commands actually visited: a failed bounded enqueue
            // must not leave stamps for later batch entries that were not attempted.
            if (queueingTimestamps != null) {
                trackQueueLatency(command, acceptedAt);
            }
            writeToBuffer(command);
        }
    }

    private void writeToDisconnectedBuffer(Collection<? extends RedisCommand<?, ?, ?>> commands, long acceptedAt) {
        for (RedisCommand<?, ?, ?> command : commands) {
            if (queueingTimestamps != null) {
                trackQueueLatency(command, acceptedAt);
            }
            writeToDisconnectedBuffer(command);
        }
    }

    private void writeToDisconnectedBuffer(RedisCommand<?, ?, ?> command) {

        if (connectionError != null) {
            if (debugEnabled) {
                logger.debug("{} writeToDisconnectedBuffer() Completing command {} due to connection error", logPrefix(),
                        command);
            }
            discardLatestQueueLatencyStart(command);
            command.completeExceptionally(connectionError);

            return;
        }

        if (replayFilter.test(command)) {
            discardLatestQueueLatencyStart(command);
            if (debugEnabled) {
                logger.debug("{} writeToDisconnectedBuffer() Filtering out command {}", logPrefix(), command);
            }
            return;
        }

        if (debugEnabled) {
            logger.debug("{} writeToDisconnectedBuffer() buffering (disconnected) command {}", logPrefix(), command);
        }

        try {
            disconnectedBuffer.add(command);
        } catch (RuntimeException e) {
            // A concurrent writer can fill the bounded buffer after validateWrite().
            discardLatestQueueLatencyStart(command);
            throw e;
        }
    }

    protected <C extends RedisCommand<?, ?, T>, T> void writeToBuffer(C command) {

        if (debugEnabled) {
            logger.debug("{} writeToBuffer() buffering command {}", logPrefix(), command);
        }

        if (connectionError != null) {

            if (debugEnabled) {
                logger.debug("{} writeToBuffer() Completing command {} due to connection error", logPrefix(), command);
            }
            discardLatestQueueLatencyStart(command);
            command.completeExceptionally(connectionError);

            return;
        }

        try {
            commandBuffer.add(command);
        } catch (RuntimeException e) {
            // Do not retain the most recent acceptance stamp for a failed bounded enqueue.
            discardLatestQueueLatencyStart(command);
            throw e;
        }
    }

    private void writeToChannelAndFlush(Channel channel, RedisCommand<?, ?, ?> command) {

        QUEUE_SIZE.incrementAndGet(this);

        ChannelFuture channelFuture = channelWriteAndFlush(channel, command);

        if (reliability == Reliability.AT_MOST_ONCE) {
            // cancel on exceptions and remove from queue, because there is no housekeeping
            channelFuture.addListener(AtMostOnceWriteListener.newInstance(this, command));
        }

        if (reliability == Reliability.AT_LEAST_ONCE) {
            // commands are ok to stay within the queue, reconnect will retrigger them
            channelFuture.addListener(RetryListener.newInstance(this, command));
        }
    }

    private void writeToChannelAndFlush(Channel channel, Collection<? extends RedisCommand<?, ?, ?>> commands) {

        QUEUE_SIZE.addAndGet(this, commands.size());

        if (reliability == Reliability.AT_MOST_ONCE) {

            // cancel on exceptions and remove from queue, because there is no housekeeping
            for (RedisCommand<?, ?, ?> command : commands) {
                channelWrite(channel, command).addListener(AtMostOnceWriteListener.newInstance(this, command));
            }
        }

        if (reliability == Reliability.AT_LEAST_ONCE) {

            // commands are ok to stay within the queue, reconnect will retrigger them
            for (RedisCommand<?, ?, ?> command : commands) {
                channelWrite(channel, command).addListener(RetryListener.newInstance(this, command));
            }
        }

        channelFlush(channel);
    }

    private void channelFlush(Channel channel) {

        if (debugEnabled) {
            logger.debug("{} write() channelFlush", logPrefix());
        }

        channel.flush();
    }

    private ChannelFuture channelWrite(Channel channel, RedisCommand<?, ?, ?> command) {

        if (debugEnabled) {
            logger.debug("{} write() channelWrite command {}", logPrefix(), command);
        }

        return channel.write(command);
    }

    private ChannelFuture channelWriteAndFlush(Channel channel, RedisCommand<?, ?, ?> command) {

        if (debugEnabled) {
            logger.debug("{} write() writeAndFlush command {}", logPrefix(), command);
        }

        return channel.writeAndFlush(command);
    }

    @Override
    public void notifyChannelActive(Channel channel) {

        this.logPrefix = null;
        this.connectionError = null;

        if (isClosed()) {

            logger.info("{} Closing channel because endpoint is already closed", logPrefix());
            channel.close();
            return;
        }

        if (connectionWatchdog != null) {
            connectionWatchdog.arm();
        }

        sharedLock.doExclusive(() -> {
            this.channel = channel;

            try {
                // Move queued commands to buffer before issuing any commands because of connection activation.
                // That's necessary to prepend queued commands first as some commands might get into the queue
                // after the connection was disconnected. They need to be prepended to the command buffer

                if (debugEnabled) {
                    logger.debug("{} activateEndpointAndExecuteBufferedCommands {} command(s) buffered", logPrefix(),
                            disconnectedBuffer.size());
                }

                if (debugEnabled) {
                    logger.debug("{} activating endpoint", logPrefix());
                }

                try {
                    inActivation = true;
                    connectionFacade.activated();
                } finally {
                    inActivation = false;
                }

                flushCommands(channel, disconnectedBuffer);
            } catch (Exception e) {

                if (debugEnabled) {
                    logger.debug("{} channelActive() ran into an exception", logPrefix());
                }

                throw e;
            }
        });
    }

    @Override
    public void notifyChannelInactive(Channel channel) {

        if (isClosed()) {
            Lazy<RedisException> lazy = Lazy.of(() -> new RedisException("Connection closed"));
            cancelCommands("Connection closed", drainCommands(), it -> it.completeExceptionally(lazy.get()));
        }

        sharedLock.doExclusive(() -> {

            if (debugEnabled) {
                logger.debug("{} deactivating endpoint handler", logPrefix());
            }

            connectionFacade.deactivated();
        });

        if (this.channel == channel) {
            this.channel = null;
        }
    }

    @Override
    public void notifyException(Throwable t) {

        if (t instanceof RedisConnectionException && RedisConnectionException.isProtectedMode(t.getMessage())) {

            connectionError = t;
            doExclusive(this::drainCommands).forEach(cmd -> {
                takeQueueLatencyStart(cmd);
                cmd.completeExceptionally(t);
            });
        }

        if (!isConnected(this.channel)) {
            connectionError = t;
        }
    }

    @Override
    public void registerConnectionWatchdog(ConnectionWatchdog connectionWatchdog) {
        this.connectionWatchdog = connectionWatchdog;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void flushCommands() {
        flushCommands(this.channel, commandBuffer);
    }

    private void flushCommands(Channel channel, Queue<RedisCommand<?, ?, ?>> queue) {

        if (debugEnabled) {
            logger.debug("{} flushCommands()", logPrefix());
        }

        if (isConnected(channel)) {

            List<RedisCommand<?, ?, ?>> commands = sharedLock.doExclusive(() -> {

                if (queue.isEmpty()) {
                    return Collections.emptyList();
                }

                return drainCommands(queue);
            });

            if (debugEnabled) {
                logger.debug("{} flushCommands() Flushing {} commands", logPrefix(), commands.size());
            }

            if (!commands.isEmpty()) {
                writeToChannelAndFlush(channel, commands);
            }
        }
    }

    /**
     * Close the connection.
     */
    @Override
    public void close() {

        if (debugEnabled) {
            logger.debug("{} close()", logPrefix());
        }

        closeAsync().join();
    }

    @Override
    public CompletableFuture<Void> closeAsync() {

        if (debugEnabled) {
            logger.debug("{} closeAsync()", logPrefix());
        }

        if (isClosed()) {
            return closeFuture;
        }

        if (STATUS.compareAndSet(this, ST_OPEN, ST_CLOSED)) {

            if (connectionWatchdog != null) {
                connectionWatchdog.prepareClose();
            }

            cancelBufferedCommands("Close");
            if (queueingTimestamps != null) {
                // Writes queued on the event loop must consume their timestamps before cleanup.
                closeFuture.whenComplete((result, failure) -> queueingTimestamps.clear());
            }

            Channel channel = getOpenChannel();

            if (channel != null) {
                Futures.adapt(channel.close(), closeFuture);
            } else {
                closeFuture.complete(null);
            }
        }

        return closeFuture;
    }

    /**
     * Disconnect the channel.
     */
    public void disconnect() {

        Channel channel = this.channel;

        if (channel != null && channel.isOpen()) {
            channel.disconnect();
        }
    }

    private Channel getOpenChannel() {

        Channel channel = this.channel;

        if (channel != null /* && channel.isOpen() is this deliberately omitted? */) {
            return channel;
        }

        return null;
    }

    /**
     * Reset the command-handler to the initial not-connected state.
     */
    public void initialState() {

        commandBuffer.clear();

        Channel currentChannel = this.channel;
        if (currentChannel != null) {

            ChannelFuture close = currentChannel.close();
            if (currentChannel.isOpen()) {
                close.syncUninterruptibly();
            }
        }
    }

    @Override
    public void notifyDrainQueuedCommands(HasQueuedCommands queuedCommands) {

        if (isClosed()) {

            Lazy<RedisException> lazy = Lazy.of(() -> new RedisException("Connection closed"));
            cancelCommands("Connection closed", queuedCommands.drainQueue(), it -> it.completeExceptionally(lazy.get()));
            cancelCommands("Connection closed", drainCommands(), it -> it.completeExceptionally(lazy.get()));
            return;
        } else if (rejectCommandsWhileDisconnected) {

            Lazy<RedisException> lazy = Lazy.of(() -> new RedisException("Connection disconnected"));
            cancelCommands("Connection disconnected", queuedCommands.drainQueue(), it -> it.completeExceptionally(lazy.get()));
            cancelCommands("Connection disconnected", drainCommands(), it -> it.completeExceptionally(lazy.get()));
            return;
        }

        sharedLock.doExclusive(() -> {

            Collection<RedisCommand<?, ?, ?>> commands = queuedCommands.drainQueue();

            if (debugEnabled) {
                logger.debug("{} notifyQueuedCommands adding {} command(s) to buffer", logPrefix(), commands.size());
            }

            drainCommands(disconnectedBuffer, commands);

            for (RedisCommand<?, ?, ?> command : commands) {

                if (command instanceof DemandAware.Sink) {
                    ((DemandAware.Sink) command).removeSource();
                }
            }

            try {
                disconnectedBuffer.addAll(commands);
            } catch (RuntimeException e) {

                if (debugEnabled) {
                    logger.debug("{} notifyQueuedCommands Queue overcommit. Cannot add all commands to buffer (disconnected).",
                            logPrefix(), commands.size());
                }
                commands.removeAll(disconnectedBuffer);

                for (RedisCommand<?, ?, ?> command : commands) {
                    takeQueueLatencyStart(command);
                    command.completeExceptionally(e);
                }
            }

            flushCommands(this.channel, disconnectedBuffer);
        });
    }

    public boolean isClosed() {
        return STATUS.get(this) == ST_CLOSED;
    }

    /**
     * Execute a {@link Supplier} callback guarded by an exclusive lock.
     *
     * @param supplier
     * @param <T>
     * @return
     */
    protected <T> T doExclusive(Supplier<T> supplier) {
        return sharedLock.doExclusive(supplier);
    }

    protected List<RedisCommand<?, ?, ?>> drainCommands() {

        List<RedisCommand<?, ?, ?>> target = new ArrayList<>(disconnectedBuffer.size() + commandBuffer.size());

        drainCommands(disconnectedBuffer, target);
        drainCommands(commandBuffer, target);

        return target;
    }

    /**
     * Drain commands from a queue and return only active commands.
     *
     * @param source the source queue.
     * @return List of commands.
     */
    private List<RedisCommand<?, ?, ?>> drainCommands(Queue<? extends RedisCommand<?, ?, ?>> source) {

        List<RedisCommand<?, ?, ?>> target = new ArrayList<>(source.size());

        RedisCommand<?, ?, ?> cmd;
        while ((cmd = source.poll()) != null) {

            if (!cmd.isDone() && !cmd.isCancelled() && !ActivationCommand.isActivationCommand(cmd)) {
                target.add(cmd);
            } else {
                takeQueueLatencyStart(cmd);
            }
        }

        drainCommands(source, target);
        return target;
    }

    /**
     * Drain commands from a queue and return only active commands.
     *
     * @param source the source queue.
     */
    private void drainCommands(Queue<? extends RedisCommand<?, ?, ?>> source, Collection<RedisCommand<?, ?, ?>> target) {

        RedisCommand<?, ?, ?> cmd;
        while ((cmd = source.poll()) != null) {

            if (!cmd.isDone() && !cmd.isCancelled() && !ActivationCommand.isActivationCommand(cmd)) {
                target.add(cmd);
            } else {
                takeQueueLatencyStart(cmd);
            }
        }
    }

    private void cancelBufferedCommands(String message) {
        cancelCommands(message, doExclusive(this::drainCommands), RedisCommand::cancel);
    }

    private void cancelCommands(String message, Iterable<? extends RedisCommand<?, ?, ?>> toCancel,
            Consumer<RedisCommand<?, ?, ?>> commandConsumer) {

        for (RedisCommand<?, ?, ?> cmd : toCancel) {
            if (cmd.getOutput() != null) {
                cmd.getOutput().setError(message);
            }
            takeQueueLatencyStart(cmd);
            commandConsumer.accept(cmd);
        }
    }

    private boolean isConnected(Channel channel) {

        if (channel == null || !channel.isActive()) {
            return false;
        }

        if (channel.hasAttr(REBIND_ATTRIBUTE)) {
            return channel.attr(REBIND_ATTRIBUTE).get() != RebindState.STARTED;
        }

        return true;
    }

    protected String logPrefix() {

        if (logPrefix != null) {
            return logPrefix;
        }

        String buffer = "[" + ChannelLogDescriptor.logDescriptor(channel) + ", " + "epid=" + getId() + ']';
        return logPrefix = buffer;
    }

    protected ProtocolVersion getProtocolVersion() {
        return clientOptions.getProtocolVersion();
    }

    @Override
    public String getId() {
        return cachedEndpointId;
    }

    static class ListenerSupport {

        Collection<? extends RedisCommand<?, ?, ?>> sentCommands;

        RedisCommand<?, ?, ?> sentCommand;

        DefaultEndpoint endpoint;

        void dequeue() {

            if (sentCommand != null) {
                QUEUE_SIZE.decrementAndGet(endpoint);
            } else {
                QUEUE_SIZE.addAndGet(endpoint, -sentCommands.size());
            }
        }

        protected void complete(Throwable t) {

            if (sentCommand != null) {
                sentCommand.completeExceptionally(t);
            } else {
                for (RedisCommand<?, ?, ?> sentCommand : sentCommands) {
                    sentCommand.completeExceptionally(t);
                }
            }
        }

    }

    static class AtMostOnceWriteListener extends ListenerSupport implements ChannelFutureListener {

        private static final Recycler<AtMostOnceWriteListener> RECYCLER = new Recycler<AtMostOnceWriteListener>() {

            @Override
            protected AtMostOnceWriteListener newObject(Handle<AtMostOnceWriteListener> handle) {
                return new AtMostOnceWriteListener(handle);
            }

        };

        private final Recycler.Handle<AtMostOnceWriteListener> handle;

        AtMostOnceWriteListener(Recycler.Handle<AtMostOnceWriteListener> handle) {
            this.handle = handle;
        }

        static AtMostOnceWriteListener newInstance(DefaultEndpoint endpoint, RedisCommand<?, ?, ?> command) {

            AtMostOnceWriteListener entry = RECYCLER.get();

            entry.endpoint = endpoint;
            entry.sentCommand = command;

            return entry;
        }

        static AtMostOnceWriteListener newInstance(DefaultEndpoint endpoint,
                Collection<? extends RedisCommand<?, ?, ?>> commands) {

            AtMostOnceWriteListener entry = RECYCLER.get();

            entry.endpoint = endpoint;
            entry.sentCommands = commands;

            return entry;
        }

        @Override
        public void operationComplete(ChannelFuture future) {

            try {

                dequeue();

                if (!future.isSuccess() && future.cause() != null) {
                    complete(future.cause());
                }
            } finally {
                recycle();
            }
        }

        private void recycle() {

            this.endpoint = null;
            this.sentCommand = null;
            this.sentCommands = null;

            handle.recycle(this);
        }

    }

    /**
     * A generic future listener which retries unsuccessful writes.
     */
    static class RetryListener extends ListenerSupport implements GenericFutureListener<Future<Void>> {

        private static final Recycler<RetryListener> RECYCLER = new Recycler<RetryListener>() {

            @Override
            protected RetryListener newObject(Handle<RetryListener> handle) {
                return new RetryListener(handle);
            }

        };

        private final Recycler.Handle<RetryListener> handle;

        RetryListener(Recycler.Handle<RetryListener> handle) {
            this.handle = handle;
        }

        static RetryListener newInstance(DefaultEndpoint endpoint, RedisCommand<?, ?, ?> command) {

            RetryListener entry = RECYCLER.get();

            entry.endpoint = endpoint;
            entry.sentCommand = command;

            return entry;
        }

        static RetryListener newInstance(DefaultEndpoint endpoint, Collection<? extends RedisCommand<?, ?, ?>> commands) {

            RetryListener entry = RECYCLER.get();

            entry.endpoint = endpoint;
            entry.sentCommands = commands;

            return entry;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void operationComplete(Future<Void> future) {

            try {
                doComplete(future);
            } finally {
                recycle();
            }
        }

        private void doComplete(Future<Void> future) {

            Throwable cause = future.cause();

            boolean success = future.isSuccess();
            dequeue();

            if (success) {
                return;
            }

            if (cause instanceof EncoderException || cause instanceof Error || cause.getCause() instanceof Error) {
                complete(cause);
                return;
            }

            Channel channel = endpoint.channel;

            // Capture values before recycler clears these.
            RedisCommand<?, ?, ?> sentCommand = this.sentCommand;
            Collection<? extends RedisCommand<?, ?, ?>> sentCommands = this.sentCommands;

            if (endpoint.rejectCommandsWhileDisconnected) {
                complete(new RedisException("Currently not connected. Commands are rejected."));
            } else {
                potentiallyRequeueCommands(channel, sentCommand, sentCommands);
            }

            if (!(cause instanceof ClosedChannelException)) {

                String message = "Unexpected exception during request: {}";
                InternalLogLevel logLevel = InternalLogLevel.WARN;

                if (cause instanceof IOException && SUPPRESS_IO_EXCEPTION_MESSAGES.contains(cause.getMessage())) {
                    logLevel = InternalLogLevel.DEBUG;
                }

                logger.log(logLevel, message, cause.toString(), cause);
            }
        }

        /**
         * Requeue command/commands
         *
         * @param channel
         * @param sentCommand
         * @param sentCommands
         */
        private void potentiallyRequeueCommands(Channel channel, RedisCommand<?, ?, ?> sentCommand,
                Collection<? extends RedisCommand<?, ?, ?>> sentCommands) {

            // do not requeue commands that are done
            if (sentCommand != null && sentCommand.isDone()) {
                return;
            }

            // do not requeue commands that are to be filtered out
            if (this.endpoint.replayFilter.test(sentCommand)) {
                return;
            }

            if (sentCommands != null && sentCommands.stream().allMatch(RedisCommand::isDone)) {
                return;
            }

            if (channel != null) {
                DefaultEndpoint endpoint = this.endpoint;
                channel.eventLoop().submit(() -> {
                    requeueCommands(sentCommand, sentCommands, endpoint);
                });
            } else {
                requeueCommands(sentCommand, sentCommands, endpoint);
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private void requeueCommands(RedisCommand<?, ?, ?> sentCommand,
                Collection<? extends RedisCommand<?, ?, ?>> sentCommands, DefaultEndpoint endpoint) {

            if (sentCommand != null) {
                try {
                    endpoint.write(sentCommand);
                } catch (Exception e) {
                    sentCommand.completeExceptionally(e);
                }
            } else {
                try {
                    endpoint.write((Collection) sentCommands);
                } catch (Exception e) {
                    for (RedisCommand<?, ?, ?> command : sentCommands) {
                        command.completeExceptionally(e);
                    }
                }
            }
        }

        private void recycle() {

            this.endpoint = null;
            this.sentCommand = null;
            this.sentCommands = null;

            handle.recycle(this);
        }

    }

    private enum Reliability {
        AT_MOST_ONCE, AT_LEAST_ONCE
    }

    static class Lazy<T> implements Supplier<T> {

        private static final Lazy<?> EMPTY = new Lazy<>(() -> null, null, true);

        static final String UNRESOLVED = "[Unresolved]";

        private final Supplier<? extends T> supplier;

        private T value;

        private volatile boolean resolved;

        /**
         * Creates a new {@link Lazy} instance for the given supplier.
         *
         * @param supplier
         */
        private Lazy(Supplier<? extends T> supplier) {
            this(supplier, null, false);
        }

        /**
         * Creates a new {@link Lazy} for the given {@link Supplier}, value and whether it has been resolved or not.
         *
         * @param supplier must not be {@literal null}.
         * @param value can be {@literal null}.
         * @param resolved whether the value handed into the constructor represents a resolved value.
         */
        private Lazy(Supplier<? extends T> supplier, T value, boolean resolved) {

            this.supplier = supplier;
            this.value = value;
            this.resolved = resolved;
        }

        /**
         * Creates a new {@link Lazy} to produce an object lazily.
         *
         * @param <T> the type of which to produce an object of eventually.
         * @param supplier the {@link Supplier} to create the object lazily.
         * @return
         */
        public static <T> Lazy<T> of(Supplier<? extends T> supplier) {
            return new Lazy<>(supplier);
        }

        /**
         * Creates a pre-resolved empty {@link Lazy}.
         *
         * @return
         * @since 2.1
         */
        @SuppressWarnings("unchecked")
        public static <T> Lazy<T> empty() {
            return (Lazy<T>) EMPTY;
        }

        /**
         * Returns the value created by the configured {@link Supplier}. Will return the calculated instance for subsequent
         * lookups.
         *
         * @return
         */
        public T get() {

            T value = getNullable();

            if (value == null) {
                throw new IllegalStateException("Expected lazy evaluation to yield a non-null value but got null");
            }

            return value;
        }

        public T getNullable() {

            if (resolved) {
                return value;
            }

            this.value = supplier.get();
            this.resolved = true;

            return value;
        }

    }

}
