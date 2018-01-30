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
package io.lettuce.core;

import java.util.Collection;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import reactor.core.CoreSubscriber;
import reactor.core.Exceptions;
import reactor.util.context.Context;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.StreamingOutput;
import io.lettuce.core.protocol.CommandWrapper;
import io.lettuce.core.protocol.DemandAware;
import io.lettuce.core.protocol.RedisCommand;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Reactive command {@link Publisher} using ReactiveStreams.
 *
 * This publisher handles command execution and response propagation to a {@link Subscriber}. Collections can be dissolved into
 * individual elements instead of emitting collections. This publisher allows multiple subscriptions if it's backed by a
 * {@link Supplier command supplier}.
 * <p>
 * When using streaming outputs ({@link io.lettuce.core.output.CommandOutput} that implement {@link StreamingOutput}) elements
 * are emitted as they are decoded. Otherwise, results are processed at command completion.
 *
 * @author Mark Paluch
 * @since 5.0
 */
class RedisPublisher<K, V, T> implements Publisher<T> {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(RedisPublisher.class);

    private final boolean traceEnabled = LOG.isTraceEnabled();

    private final Supplier<? extends RedisCommand<K, V, T>> commandSupplier;
    private final AtomicReference<RedisCommand<K, V, T>> ref;
    private final StatefulConnection<K, V> connection;
    private final boolean dissolve;

    /**
     * Creates a new {@link RedisPublisher} for a static command.
     *
     * @param staticCommand static command, must not be {@literal null}
     * @param connection the connection, must not be {@literal null}
     * @param dissolve dissolve collections into particular elements
     */
    public RedisPublisher(RedisCommand<K, V, T> staticCommand, StatefulConnection<K, V> connection, boolean dissolve) {
        this(() -> staticCommand, connection, dissolve);
    }

    /**
     * Creates a new {@link RedisPublisher} for a command supplier.
     *
     * @param commandSupplier command supplier, must not be {@literal null}
     * @param connection the connection, must not be {@literal null}
     * @param dissolve dissolve collections into particular elements
     */
    public RedisPublisher(Supplier<RedisCommand<K, V, T>> commandSupplier, StatefulConnection<K, V> connection, boolean dissolve) {

        LettuceAssert.notNull(commandSupplier, "CommandSupplier must not be null");
        LettuceAssert.notNull(connection, "StatefulConnection must not be null");

        this.commandSupplier = commandSupplier;
        this.connection = connection;
        this.dissolve = dissolve;
        this.ref = new AtomicReference<>(commandSupplier.get());
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {

        if (this.traceEnabled) {
            LOG.trace("subscribe: {}@{}", subscriber.getClass().getName(), Objects.hashCode(subscriber));
        }

        // Reuse the first command but then discard it.
        RedisCommand<K, V, T> command = ref.get();

        if (command != null) {
            if (!ref.compareAndSet(command, null)) {
                command = commandSupplier.get();
            }
        } else {
            command = commandSupplier.get();
        }

        RedisSubscription<T> redisSubscription = new RedisSubscription<>(connection, command, dissolve);
        redisSubscription.subscribe(subscriber);
    }

    /**
     * Implementation of {@link Subscription}. This subscription can receive demand for data signals with {@link #request(long)}
     * . It maintains a {@link State} to react on pull signals like demand for data or push signals as soon as data is
     * available. Subscription behavior and state transitions are kept inside the {@link State}.
     *
     * @param <T> data element type
     */
    static class RedisSubscription<T> extends StreamingOutput.Subscriber<T> implements Subscription {

        static final InternalLogger LOG = InternalLoggerFactory.getInstance(RedisPublisher.class);

        static final int ST_PROGRESS = 0;
        static final int ST_COMPLETED = 1;

        @SuppressWarnings({ "rawtypes", "unchecked" })
        static final AtomicLongFieldUpdater<RedisSubscription> DEMAND = AtomicLongFieldUpdater.newUpdater(
                RedisSubscription.class, "demand");

        static final AtomicReferenceFieldUpdater<RedisSubscription, State> STATE = AtomicReferenceFieldUpdater.newUpdater(
                RedisSubscription.class, State.class, "state");

        static final AtomicReferenceFieldUpdater<RedisSubscription, CommandDispatch> COMMAND_DISPATCH = AtomicReferenceFieldUpdater
                .newUpdater(RedisSubscription.class, CommandDispatch.class, "commandDispatch");

        static final AtomicIntegerFieldUpdater<RedisSubscription> COMPLETION = AtomicIntegerFieldUpdater.newUpdater(
                RedisSubscription.class, "completion");

        private final SubscriptionCommand<?, ?, T> subscriptionCommand;
        private final boolean traceEnabled = LOG.isTraceEnabled();

        final Queue<T> data = Operators.newQueue();
        final StatefulConnection<?, ?> connection;
        final RedisCommand<?, ?, T> command;
        final boolean dissolve;

        // accessed via AtomicLongFieldUpdater
        @SuppressWarnings("unused")
        volatile long demand;
        @SuppressWarnings("unused")
        volatile State state = State.UNSUBSCRIBED;
        @SuppressWarnings("unused")
        volatile int completion = ST_PROGRESS;
        @SuppressWarnings("unused")
        volatile CommandDispatch commandDispatch = CommandDispatch.UNDISPATCHED;

        volatile boolean allDataRead = false;

        volatile Subscriber<? super T> subscriber;

        @SuppressWarnings("unchecked")
        RedisSubscription(StatefulConnection<?, ?> connection, RedisCommand<?, ?, T> command, boolean dissolve) {

            LettuceAssert.notNull(connection, "Connection must not be null");
            LettuceAssert.notNull(command, "RedisCommand must not be null");

            this.connection = connection;
            this.command = command;
            this.dissolve = dissolve;

            if (command.getOutput() instanceof StreamingOutput<?>) {
                StreamingOutput<T> streamingOutput = (StreamingOutput<T>) command.getOutput();

                if (connection instanceof StatefulRedisConnection<?, ?> && ((StatefulRedisConnection) connection).isMulti()) {
                    streamingOutput.setSubscriber(new CompositeSubscriber<>(this, streamingOutput.getSubscriber()));
                } else {
                    streamingOutput.setSubscriber(this);
                }
            }

            this.subscriptionCommand = new SubscriptionCommand<>(command, this, dissolve);
        }

        /**
         * Subscription procedure called by a {@link Publisher}
         *
         * @param subscriber the subscriber, must not be {@literal null}.
         */
        void subscribe(Subscriber<? super T> subscriber) {

            if (subscriber == null) {
                throw new NullPointerException("Subscriber must not be null");
            }

            State state = state();

            if (traceEnabled) {
                LOG.trace("{} subscribe: {}@{}", state, subscriber.getClass().getName(), subscriber.hashCode());
            }

            state.subscribe(this, subscriber);
        }

        /**
         * Signal for data demand.
         *
         * @param n number of requested elements.
         */
        @Override
        public final void request(long n) {

            State state = state();

            if (traceEnabled) {
                LOG.trace("{} request: {}", state, n);
            }

            state.request(this, n);
        }

        /**
         * Cancels a command.
         */
        @Override
        public final void cancel() {

            State state = state();

            if (traceEnabled) {
                LOG.trace("{} cancel", state);
            }

            state.cancel(this);
        }

        /**
         * Called by {@link StreamingOutput} to dispatch data (push).
         *
         * @param t element
         */
        @Override
        public void onNext(T t) {

            LettuceAssert.notNull(t, "Data must not be null");

            if (state() == State.COMPLETED) {
                return;
            }

            if (!data.offer(t)) {

                Subscriber<?> subscriber = this.subscriber;
                Context context = Context.empty();
                if (subscriber instanceof CoreSubscriber) {
                    context = ((CoreSubscriber) subscriber).currentContext();
                }

                Throwable e = Operators.onOperatorError(this, Exceptions.failWithOverflow(), t, context);
                onError(e);
                return;
            }

            onDataAvailable();
        }

        /**
         * Called via a listener interface to indicate that reading is possible.
         */
        final void onDataAvailable() {

            State state = state();

            if (traceEnabled) {
                LOG.trace("{} onDataAvailable()", state);
            }

            state.onDataAvailable(this);
        }

        /**
         * Called via a listener interface to indicate that all data has been read.
         */
        final void onAllDataRead() {

            State state = state();

            if (traceEnabled) {
                LOG.trace("{} onAllDataRead()", state);
            }

            state.onAllDataRead(this);
        }

        /**
         * Called by a listener interface to indicate that as error has occured.
         *
         * @param t the error
         */
        final void onError(Throwable t) {

            State state = state();

            if (LOG.isErrorEnabled()) {
                LOG.trace("{} onError(): {}", state, t.toString(), t);
            }

            state.onError(this, t);
        }

        /**
         * Reads data from the input, if possible.
         *
         * @return the data that was read or {@literal null}
         */
        protected T read() {
            return data.poll();
        }

        boolean hasDemand() {
            return getDemand() > 0;
        }

        private long getDemand() {
            return DEMAND.get(this);
        }

        boolean changeState(State oldState, State newState) {
            return STATE.compareAndSet(this, oldState, newState);
        }

        public boolean complete() {

            if (COMPLETION.compareAndSet(this, ST_PROGRESS, ST_COMPLETED)) {

                STATE.set(this, State.COMPLETED);
                return true;
            }

            return false;
        }

        void checkCommandDispatch() {
            COMMAND_DISPATCH.get(this).dispatch(this);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        void dispatchCommand() {
            connection.dispatch((RedisCommand) subscriptionCommand);
        }

        void checkOnDataAvailable() {

            if (data.isEmpty()) {
                potentiallyReadMore();
            }

            if (!data.isEmpty()) {
                onDataAvailable();
            }
        }

        void potentiallyReadMore() {

            if ((getDemand() + 1) > data.size()) {
                state().readData(this);
            }
        }

        /**
         * Reads and publishes data from the input. Continues until either there is no more demand, or until there is no more
         * data to be read.
         *
         * @return {@literal true} if there is more demand, {@literal false} otherwise.
         */
        boolean readAndPublish() {

            while (hasDemand()) {

                T data = read();

                if (data != null) {

                    this.subscriber.onNext(data);

                    if (Operators.addCap(DEMAND, this, -1) == 0) {
                        return false;
                    }
                } else {
                    return true;
                }
            }

            return false;
        }

        RedisPublisher.State state() {
            return STATE.get(this);
        }
    }

    /**
     * Represents a state for command dispatch of the {@link Subscription}. The following figure indicates the two different
     * states that exist, and the relationships between them.
     *
     * <pre>
     *   UNDISPATCHED
     *        |
     *        v
     *   DISPATCHED
     * </pre>
     *
     * Refer to the individual states for more information.
     */
    private enum CommandDispatch {

        /**
         * Initial state. Will respond to {@link #dispatch(RedisSubscription)} by changing the state to {@link #DISPATCHED} and
         * dispatch the command.
         */
        UNDISPATCHED {

            @Override
            void dispatch(RedisSubscription<?> redisSubscription) {

                if (RedisSubscription.COMMAND_DISPATCH.compareAndSet(redisSubscription, this, DISPATCHED)) {
                    redisSubscription.dispatchCommand();
                }
            }
        },
        DISPATCHED;

        void dispatch(RedisSubscription<?> redisSubscription) {
        }
    }

    /**
     * Represents a state for the {@link Subscription} to be in. The following figure indicates the four different states that
     * exist, and the relationships between them.
     *
     * <pre>
     *       UNSUBSCRIBED
     *        |
     *        v
     * NO_DEMAND -------------------> DEMAND
     *    |    ^                      ^    |
     *    |    |                      |    |
     *    |    --------- READING <-----    |
     *    |                 |              |
     *    |                 v              |
     *    ------------> COMPLETED <---------
     * </pre>
     *
     * Refer to the individual states for more information.
     */
    private enum State {

        /**
         * The initial unsubscribed state. Will respond to {@link #subscribe(RedisSubscription, Subscriber)} by changing state
         * to {@link #NO_DEMAND}.
         */
        UNSUBSCRIBED {
            @SuppressWarnings("unchecked")
            @Override
            void subscribe(RedisSubscription<?> subscription, Subscriber<?> subscriber) {

                LettuceAssert.notNull(subscriber, "Subscriber must not be null");

                if (subscription.changeState(this, NO_DEMAND)) {

                    subscription.subscriber = (Subscriber) subscriber;
                    subscriber.onSubscribe(subscription);
                } else {
                    throw new IllegalStateException(toString());
                }
            }
        },

        /**
         * State that gets entered when there is no demand. Responds to {@link #request(RedisSubscription, long)}
         * (RedisPublisher, long)} by increasing the demand, changing state to {@link #DEMAND} and will check whether there is
         * data available for reading.
         */
        NO_DEMAND {
            @Override
            void request(RedisSubscription<?> subscription, long n) {

                if (Operators.request(RedisSubscription.DEMAND, subscription, n)) {

                    if (subscription.changeState(this, DEMAND)) {

                        try {
                            subscription.checkCommandDispatch();
                        } catch (Exception ex) {
                            subscription.onError(ex);
                        }
                        subscription.checkOnDataAvailable();
                    }

                    subscription.potentiallyReadMore();

                    if (subscription.allDataRead) {
                        onAllDataRead(subscription);
                    }
                } else {
                    onError(subscription, Exceptions.nullOrNegativeRequestException(n));
                }
            }
        },

        /**
         * State that gets entered when there is demand. Responds to {@link #onDataAvailable(RedisSubscription)} by reading the
         * available data. The state will be changed to {@link #NO_DEMAND} if there is no demand.
         */
        DEMAND {
            @Override
            void onDataAvailable(RedisSubscription<?> subscription) {

                do {

                    if (!read(subscription)) {
                        return;
                    }
                } while (subscription.hasDemand() && subscription.changeState(NO_DEMAND, this));
            }

            @Override
            void request(RedisSubscription<?> subscription, long n) {

                if (Operators.request(RedisSubscription.DEMAND, subscription, n)) {

                    if (subscription.changeState(NO_DEMAND, DEMAND)) {
                        read(subscription);
                    }

                    subscription.potentiallyReadMore();
                } else {
                    onError(subscription, Exceptions.nullOrNegativeRequestException(n));
                }
            }

            private boolean read(RedisSubscription<?> subscription) {

                if (subscription.changeState(this, READING)) {

                    boolean hasDemand = subscription.readAndPublish();

                    if (subscription.allDataRead && subscription.data.isEmpty()) {
                        subscription.onAllDataRead();
                        return true;
                    }

                    if (hasDemand) {
                        subscription.changeState(READING, DEMAND);
                        subscription.checkOnDataAvailable();
                    } else {
                        subscription.changeState(READING, NO_DEMAND);
                    }

                    return true;
                }

                return false;
            }
        },

        READING {
            @Override
            void request(RedisSubscription<?> subscription, long n) {
                DEMAND.request(subscription, n);
            }
        },

        /**
         * The terminal completed state. Does not respond to any events.
         */
        COMPLETED {

            @Override
            void request(RedisSubscription<?> subscription, long n) {
                // ignore
            }

            @Override
            void cancel(RedisSubscription<?> subscription) {
                // ignore
            }

            @Override
            void onAllDataRead(RedisSubscription<?> subscription) {
                // ignore
            }

            @Override
            void onError(RedisSubscription<?> subscription, Throwable t) {
                // ignore
            }
        };

        void subscribe(RedisSubscription<?> subscription, Subscriber<?> subscriber) {
            throw new IllegalStateException(toString());
        }

        void request(RedisSubscription<?> subscription, long n) {
            throw new IllegalStateException(toString());
        }

        void cancel(RedisSubscription<?> subscription) {

            subscription.command.cancel();
            if (subscription.changeState(this, COMPLETED)) {
                readData(subscription);
            }
        }

        void readData(RedisSubscription<?> subscription) {

            DemandAware.Source source = subscription.subscriptionCommand.source;

            if (source != null) {
                source.requestMore();
            }
        }

        void onDataAvailable(RedisSubscription<?> subscription) {
            // ignore
        }

        void onAllDataRead(RedisSubscription<?> subscription) {

            subscription.allDataRead = true;

            if (subscription.data.isEmpty() && subscription.complete()) {

                readData(subscription);

                Subscriber<?> subscriber = subscription.subscriber;

                if (subscriber != null) {
                    subscriber.onComplete();
                }
            }
        }

        void onError(RedisSubscription<?> subscription, Throwable t) {

            if (subscription.changeState(this, COMPLETED)) {

                readData(subscription);

                Subscriber<?> subscriber = subscription.subscriber;
                if (subscriber != null) {
                    subscriber.onError(t);
                }
            }
        }
    }

    /**
     * Command that emits it data after completion to a {@link RedisSubscription}.
     *
     * @param <K> key type
     * @param <V> value type
     * @param <T> response type
     */
    private static class SubscriptionCommand<K, V, T> extends CommandWrapper<K, V, T> implements DemandAware.Sink {

        private final boolean dissolve;
        private final RedisSubscription<T> subscription;
        private volatile boolean completed = false;
        private volatile DemandAware.Source source;

        public SubscriptionCommand(RedisCommand<K, V, T> command, RedisSubscription<T> subscription, boolean dissolve) {

            super(command);

            this.subscription = subscription;
            this.dissolve = dissolve;
        }

        @Override
        public boolean hasDemand() {
            return completed || subscription.state() == State.COMPLETED || subscription.data.isEmpty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public void complete() {

            if (completed) {
                return;
            }

            try {
                super.complete();

                if (getOutput() != null) {
                    Object result = getOutput().get();

                    if (getOutput().hasError()) {
                        onError(new RedisCommandExecutionException(getOutput().getError()));
                        completed = true;
                        return;
                    }

                    if (!(getOutput() instanceof StreamingOutput<?>) && result != null) {

                        if (dissolve && result instanceof Collection) {

                            Collection<T> collection = (Collection<T>) result;

                            for (T t : collection) {
                                if (t != null) {
                                    subscription.onNext(t);
                                }
                            }
                        } else {
                            subscription.onNext((T) result);
                        }
                    }
                }

                subscription.onAllDataRead();
            } finally {
                completed = true;
            }
        }

        @Override
        public void setSource(DemandAware.Source source) {
            this.source = source;
        }

        @Override
        public void removeSource() {
            this.source = null;
        }

        @Override
        public void cancel() {

            if (completed) {
                return;
            }

            super.cancel();

            completed = true;
        }

        @Override
        public boolean completeExceptionally(Throwable throwable) {

            if (completed) {
                return false;
            }

            boolean b = super.completeExceptionally(throwable);
            onError(throwable);
            completed = true;
            return b;
        }

        private void onError(Throwable throwable) {
            subscription.onError(throwable);
        }
    }

    /**
     * Composite {@link io.lettuce.core.output.StreamingOutput.Subscriber} that can notify multiple nested subscribers.
     *
     * @param <T> element type
     */
    private static class CompositeSubscriber<T> extends StreamingOutput.Subscriber<T> {

        private final StreamingOutput.Subscriber<T> first;
        private final StreamingOutput.Subscriber<T> second;

        public CompositeSubscriber(StreamingOutput.Subscriber<T> first, StreamingOutput.Subscriber<T> second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public void onNext(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onNext(Collection<T> outputTarget, T t) {

            first.onNext(outputTarget, t);
            second.onNext(outputTarget, t);
        }
    }
}
