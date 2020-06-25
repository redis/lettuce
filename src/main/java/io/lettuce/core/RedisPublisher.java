/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core;

import java.util.Collection;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Executor;
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
import io.netty.util.Recycler;
import io.netty.util.concurrent.ImmediateEventExecutor;
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

    private final Executor executor;

    /**
     * Creates a new {@link RedisPublisher} for a static command.
     *
     * @param staticCommand static command, must not be {@code null}.
     * @param connection the connection, must not be {@code null}.
     * @param dissolve dissolve collections into particular elements.
     * @param publishOn executor to use for publishOn signals.
     */
    public RedisPublisher(RedisCommand<K, V, T> staticCommand, StatefulConnection<K, V> connection, boolean dissolve,
            Executor publishOn) {
        this(() -> staticCommand, connection, dissolve, publishOn);
    }

    /**
     * Creates a new {@link RedisPublisher} for a command supplier.
     *
     * @param commandSupplier command supplier, must not be {@code null}.
     * @param connection the connection, must not be {@code null}.
     * @param dissolve dissolve collections into particular elements.
     * @param publishOn executor to use for publishOn signals.
     */
    public RedisPublisher(Supplier<RedisCommand<K, V, T>> commandSupplier, StatefulConnection<K, V> connection,
            boolean dissolve, Executor publishOn) {

        LettuceAssert.notNull(commandSupplier, "CommandSupplier must not be null");
        LettuceAssert.notNull(connection, "StatefulConnection must not be null");
        LettuceAssert.notNull(publishOn, "Executor must not be null");

        this.commandSupplier = commandSupplier;
        this.connection = connection;
        this.dissolve = dissolve;
        this.executor = publishOn;
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

        RedisSubscription<T> redisSubscription = new RedisSubscription<>(connection, command, dissolve, executor);
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
        static final AtomicLongFieldUpdater<RedisSubscription> DEMAND = AtomicLongFieldUpdater
                .newUpdater(RedisSubscription.class, "demand");

        @SuppressWarnings({ "rawtypes", "unchecked" })
        static final AtomicReferenceFieldUpdater<RedisSubscription, State> STATE = AtomicReferenceFieldUpdater
                .newUpdater(RedisSubscription.class, State.class, "state");

        @SuppressWarnings({ "rawtypes", "unchecked" })
        static final AtomicReferenceFieldUpdater<RedisSubscription, CommandDispatch> COMMAND_DISPATCH = AtomicReferenceFieldUpdater
                .newUpdater(RedisSubscription.class, CommandDispatch.class, "commandDispatch");

        private final SubscriptionCommand<?, ?, T> subscriptionCommand;

        private final boolean traceEnabled = LOG.isTraceEnabled();

        final Queue<T> data = Operators.newQueue();

        final StatefulConnection<?, ?> connection;

        final RedisCommand<?, ?, T> command;

        final boolean dissolve;

        private final Executor executor;

        // accessed via AtomicLongFieldUpdater
        @SuppressWarnings("unused")
        volatile long demand;

        @SuppressWarnings("unused")
        volatile State state = State.UNSUBSCRIBED;

        @SuppressWarnings("unused")
        volatile CommandDispatch commandDispatch = CommandDispatch.UNDISPATCHED;

        volatile boolean allDataRead = false;

        volatile RedisSubscriber<? super T> subscriber;

        @SuppressWarnings("unchecked")
        RedisSubscription(StatefulConnection<?, ?> connection, RedisCommand<?, ?, T> command, boolean dissolve,
                Executor executor) {

            LettuceAssert.notNull(connection, "Connection must not be null");
            LettuceAssert.notNull(command, "RedisCommand must not be null");
            LettuceAssert.notNull(executor, "Executor must not be null");

            this.connection = connection;
            this.command = command;
            this.dissolve = dissolve;
            this.executor = executor;

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
         * @param subscriber the subscriber, must not be {@code null}.
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
         * @param t element.
         */
        @Override
        public void onNext(T t) {

            LettuceAssert.notNull(t, "Data must not be null");

            State state = state();

            if (state == State.COMPLETED) {
                return;
            }

            // Fast-path publishing, preserve ordering
            if (data.isEmpty() && state() == State.DEMAND) {

                long initial = getDemand();

                if (initial > 0) {

                    try {
                        DEMAND.decrementAndGet(this);
                        this.subscriber.onNext(t);
                    } catch (Exception e) {
                        onError(e);
                    }
                    return;
                }
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

            allDataRead = true;
            onDataAvailable();
        }

        /**
         * Called by a listener interface to indicate that as error has occurred.
         *
         * @param t the error.
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
         * @return the data that was read or {@code null}
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

        boolean afterRead() {
            return changeState(State.READING, getDemand() > 0 ? State.DEMAND : State.NO_DEMAND);
        }

        public boolean complete() {
            return changeState(State.READING, State.COMPLETED);
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
         */
        void readAndPublish() {

            while (hasDemand()) {

                T data = read();

                if (data == null) {
                    return;
                }

                DEMAND.decrementAndGet(this);
                this.subscriber.onNext(data);
            }
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

                    subscription.subscriber = RedisSubscriber.create(subscriber, subscription.executor);
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
                    onDataAvailable(subscription);
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

                try {
                    do {

                        if (!read(subscription)) {
                            return;
                        }
                    } while (subscription.hasDemand());
                } catch (Exception e) {
                    subscription.onError(e);
                }
            }

            @Override
            void request(RedisSubscription<?> subscription, long n) {

                if (Operators.request(RedisSubscription.DEMAND, subscription, n)) {

                    onDataAvailable(subscription);

                    subscription.potentiallyReadMore();
                } else {
                    onError(subscription, Exceptions.nullOrNegativeRequestException(n));
                }
            }

            /**
             * @param subscription
             * @return {@code true} if the {@code read()} call was able to perform a read and whether this method should be
             *         called again to emit remaining data.
             */
            private boolean read(RedisSubscription<?> subscription) {

                State state = subscription.state();

                // concurrency/entry guard
                if (state == NO_DEMAND || state == DEMAND) {
                    if (!subscription.changeState(state, READING)) {
                        return false;
                    }
                } else {
                    return false;
                }

                subscription.readAndPublish();

                if (subscription.allDataRead && subscription.data.isEmpty()) {
                    state.onAllDataRead(subscription);
                    return false;
                }

                // concurrency/leave guard
                subscription.afterRead();

                if (subscription.allDataRead || !subscription.data.isEmpty()) {
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

            if (subscription.data.isEmpty() && subscription.complete()) {

                readData(subscription);

                Subscriber<?> subscriber = subscription.subscriber;

                if (subscriber != null) {
                    subscriber.onComplete();
                }
            }
        }

        void onError(RedisSubscription<?> subscription, Throwable t) {

            State state;
            while ((state = subscription.state()) != COMPLETED && subscription.changeState(state, COMPLETED)) {

                readData(subscription);

                Subscriber<?> subscriber = subscription.subscriber;
                if (subscriber != null) {
                    subscriber.onError(t);
                    return;
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
                        onError(ExceptionFactory.createExecutionException(getOutput().getError()));
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

    /**
     * Lettuce-specific interface.
     *
     * @param <T>
     */
    interface RedisSubscriber<T> extends CoreSubscriber<T> {

        /**
         * Create a new {@link RedisSubscriber}. Optimizes for immediate executor usage.
         *
         * @param delegate
         * @param executor
         * @param <T>
         * @return
         * @see ImmediateSubscriber
         * @see PublishOnSubscriber
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        static <T> RedisSubscriber<T> create(Subscriber<?> delegate, Executor executor) {

            if (executor == ImmediateEventExecutor.INSTANCE) {
                return new ImmediateSubscriber(delegate);
            }

            return new PublishOnSubscriber(delegate, executor);
        }

    }

    /**
     * {@link RedisSubscriber} using immediate signal dispatch by calling directly {@link Subscriber} method.
     *
     * @param <T>
     */
    static class ImmediateSubscriber<T> implements RedisSubscriber<T> {

        private final CoreSubscriber<T> delegate;

        public ImmediateSubscriber(Subscriber<T> delegate) {
            this.delegate = (CoreSubscriber) reactor.core.publisher.Operators.toCoreSubscriber(delegate);
        }

        @Override
        public Context currentContext() {
            return delegate.currentContext();
        }

        @Override
        public void onSubscribe(Subscription s) {
            delegate.onSubscribe(s);
        }

        @Override
        public void onNext(T t) {
            delegate.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            delegate.onError(t);
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
        }

    }

    /**
     * {@link RedisSubscriber} dispatching subscriber signals on a {@link Executor}.
     *
     * @param <T>
     */
    static class PublishOnSubscriber<T> implements RedisSubscriber<T> {

        private final CoreSubscriber<T> delegate;

        private final Executor executor;

        public PublishOnSubscriber(Subscriber<T> delegate, Executor executor) {
            this.delegate = (CoreSubscriber) reactor.core.publisher.Operators.toCoreSubscriber(delegate);
            this.executor = executor;
        }

        @Override
        public Context currentContext() {
            return delegate.currentContext();
        }

        @Override
        public void onSubscribe(Subscription s) {
            delegate.onSubscribe(s);
        }

        @Override
        public void onNext(T t) {
            executor.execute(OnNext.newInstance(t, delegate));
        }

        @Override
        public void onError(Throwable t) {
            executor.execute(OnComplete.newInstance(t, delegate));
        }

        @Override
        public void onComplete() {
            executor.execute(OnComplete.newInstance(delegate));
        }

    }

    /**
     * OnNext {@link Runnable}. This listener is pooled and must be {@link #recycle() recycled after usage}.
     */
    static class OnNext implements Runnable {

        private static final Recycler<OnNext> RECYCLER = new Recycler<OnNext>() {

            @Override
            protected OnNext newObject(Handle<OnNext> handle) {
                return new OnNext(handle);
            }

        };

        private final Recycler.Handle<OnNext> handle;

        private Object signal;

        private Subscriber<Object> subscriber;

        OnNext(Recycler.Handle<OnNext> handle) {
            this.handle = handle;
        }

        /**
         * Allocate a new instance.
         *
         * @return
         * @see Subscriber#onNext(Object)
         */
        static OnNext newInstance(Object signal, Subscriber<?> subscriber) {

            OnNext entry = RECYCLER.get();

            entry.signal = signal;
            entry.subscriber = (Subscriber) subscriber;

            return entry;
        }

        @Override
        public void run() {
            try {
                subscriber.onNext(signal);
            } finally {
                recycle();
            }
        }

        private void recycle() {

            this.signal = null;
            this.subscriber = null;

            handle.recycle(this);
        }

    }

    /**
     * OnComplete {@link Runnable}. This listener is pooled and must be {@link #recycle() recycled after usage}.
     */
    static class OnComplete implements Runnable {

        private static final Recycler<OnComplete> RECYCLER = new Recycler<OnComplete>() {

            @Override
            protected OnComplete newObject(Handle<OnComplete> handle) {
                return new OnComplete(handle);
            }

        };

        private final Recycler.Handle<OnComplete> handle;

        private Throwable signal;

        private Subscriber<?> subscriber;

        OnComplete(Recycler.Handle<OnComplete> handle) {
            this.handle = handle;
        }

        /**
         * Allocate a new instance.
         *
         * @return
         * @see Subscriber#onError(Throwable)
         */
        static OnComplete newInstance(Throwable signal, Subscriber<?> subscriber) {

            OnComplete entry = RECYCLER.get();

            entry.signal = signal;
            entry.subscriber = subscriber;

            return entry;
        }

        /**
         * Allocate a new instance.
         *
         * @return
         * @see Subscriber#onComplete()
         */
        static OnComplete newInstance(Subscriber<?> subscriber) {

            OnComplete entry = RECYCLER.get();

            entry.signal = null;
            entry.subscriber = subscriber;

            return entry;
        }

        @Override
        public void run() {
            try {
                if (signal != null) {
                    subscriber.onError(signal);
                } else {
                    subscriber.onComplete();
                }
            } finally {
                recycle();
            }
        }

        private void recycle() {

            this.signal = null;
            this.subscriber = null;

            handle.recycle(this);
        }

    }

}
