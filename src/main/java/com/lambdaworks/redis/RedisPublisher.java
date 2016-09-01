package com.lambdaworks.redis;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.output.StreamingOutput;
import com.lambdaworks.redis.protocol.CommandWrapper;
import com.lambdaworks.redis.protocol.RedisCommand;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import reactor.core.publisher.Operators;

/**
 * Reactive command {@link Publisher} using ReactiveStreams.
 *
 * This publisher handles command execution and response propagation to a {@link Subscriber}. Collections can be dissolved into
 * individual elements instead of emitting collections. This publisher allows multiple subscriptions if it's backed by a
 * {@link Supplier command supplier}.
 * <p>
 * When using streaming outputs ({@link com.lambdaworks.redis.output.CommandOutput} that implement {@link StreamingOutput})
 * elements are emitted as they are decoded. Otherwise, results are processed at command completion.
 *
 * @author Mark Paluch
 * @since 5.0
 */
class RedisPublisher<K, V, T> implements Publisher<T> {

    private final static InternalLogger LOG = InternalLoggerFactory.getInstance(RedisPublisher.class);

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
    public RedisPublisher(Supplier<RedisCommand<K, V, T>> commandSupplier, StatefulConnection<K, V> connection,
            boolean dissolve) {

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
    private static class RedisSubscription<T> implements Subscription, StreamingOutput.Subscriber<T> {

        private final static InternalLogger LOG = InternalLoggerFactory.getInstance(RedisPublisher.class);
        private final boolean traceEnabled = LOG.isTraceEnabled();

        private final AtomicLong demand = new AtomicLong();
        private final Queue<T> data = new ConcurrentLinkedQueue<T>();
        private final AtomicBoolean dispatched = new AtomicBoolean();
        private volatile boolean allDataRead = false;

        private final StatefulConnection<?, ?> connection;
        private final RedisCommand<?, ?, T> command;
        private final boolean dissolve;

        private Subscriber<? super T> subscriber;

        private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

        RedisSubscription(StatefulConnection<?, ?> connection, RedisCommand<?, ?, T> command, boolean dissolve) {

            LettuceAssert.notNull(connection, "Connection must not be null");
            LettuceAssert.notNull(command, "RedisCommand must not be null");

            this.connection = connection;
            this.command = command;
            this.dissolve = dissolve;
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

            if (traceEnabled) {
                LOG.trace("{} subscribe: {}@{}", state(), subscriber.getClass().getName(), Objects.hashCode(subscriber));
            }

            state().subscribe(this, subscriber);
        }

        /**
         * Signal for data demand.
         *
         * @param n number of requested elements
         */
        @Override
        public final void request(long n) {

            if (traceEnabled) {
                LOG.trace("{} request: {}", state(), n);
            }

            state().request(this, n);
        }

        /**
         * Cancels a command.
         */
        @Override
        public final void cancel() {

            if (traceEnabled) {
                LOG.trace("{} cancel", state());
            }

            state().cancel(this);
        }

        private RedisPublisher.State state() {
            return this.state.get();
        }

        /**
         * Called by {@link StreamingOutput} to dispatch data (push).
         *
         * @param t element
         */
        @Override
        public void onNext(T t) {

            LettuceAssert.notNull(t, "Data must not be null");

            data.add(t);
            onDataAvailable();
        }

        /**
         * Called via a listener interface to indicate that reading is possible.
         *
         */
        final void onDataAvailable() {

            if (traceEnabled) {
                LOG.trace("{} onDataAvailable()", state());
            }

            this.state.get().onDataAvailable(this);
        }

        /**
         * Called via a listener interface to indicate that all data has been read.
         *
         */
        final void onAllDataRead() {

            if (traceEnabled) {
                LOG.trace("{} onAllDataRead()", state());
            }

            allDataRead = true;
            this.state.get().onAllDataRead(this);
        }

        /**
         * Called by a listener interface to indicate that as error has occured.
         *
         * @param t the error
         */
        final void onError(Throwable t) {

            if (LOG.isErrorEnabled()) {
                LOG.trace("{} onError(): {}", state(), t.toString(), t);
            }
            this.state.get().onError(this, t);
        }

        /**
         * Reads and publishes data from the input. Continues until either there is no more demand, or until there is no more
         * data to be read.
         *
         * @return {@literal true} if there is more demand, {@literal false} otherwise
         */
        private boolean readAndPublish() throws IOException {

            while (hasDemand()) {
                T data = read();

                if (data != null) {
                    BackpressureUtils.getAndSub(this.demand, 1L);
                    this.subscriber.onNext(data);
                } else {
                    return true;
                }
            }
            return false;
        }

        /**
         * Reads data from the input, if possible.
         * 
         * @return the data that was read or {@literal null}
         */
        protected T read() {
            return data.poll();
        }

        private boolean hasDemand() {
            return this.demand.get() > 0;
        }

        private boolean changeState(State oldState, State newState) {
            return this.state.compareAndSet(oldState, newState);
        }

        void checkCommandDispatch() {

            if (!dispatched.get() && dispatched.compareAndSet(false, true)) {
                dispatchCommand();
            }
        }

        @SuppressWarnings("unchecked")
        private void dispatchCommand() {
            if (command.getOutput() instanceof StreamingOutput<?>) {
                StreamingOutput<T> streamingOutput = (StreamingOutput<T>) command.getOutput();

                if (connection instanceof StatefulRedisConnection<?, ?> && ((StatefulRedisConnection) connection).isMulti()) {
                    streamingOutput
                            .setSubscriber(new CompositeSubscriber<T>(Arrays.asList(this, streamingOutput.getSubscriber())));
                } else {
                    streamingOutput.setSubscriber(this);
                }
            }

            connection.dispatch(new SubscriptionCommand(command, this, dissolve));
        }

        void checkOnDataAvailable() {

            if (!data.isEmpty()) {
                onDataAvailable();
            }
        }
    }

    /**
     * Represents a state for the {@link Subscription} to be in. The following figure indicate the four different states that
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

                if (Operators.checkRequest(n, subscription.subscriber)) {
                    Operators.addAndGet(subscription.demand, n);

                    if (subscription.changeState(this, DEMAND)) {
                        subscription.checkCommandDispatch();
                        subscription.checkOnDataAvailable();
                    }
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

                if (subscription.changeState(this, READING)) {

                    try {
                        boolean demandAvailable = subscription.readAndPublish();
                        if (demandAvailable) {
                            subscription.changeState(READING, DEMAND);
                            subscription.checkOnDataAvailable();
                        } else {

                            if (subscription.allDataRead && subscription.data.isEmpty()) {
                                subscription.onAllDataRead();
                            } else {
                                subscription.changeState(READING, NO_DEMAND);
                            }
                        }
                    } catch (IOException ex) {
                        subscription.onError(ex);
                    }
                }
            }

            @Override
            void request(RedisSubscription<?> subscription, long n) {

                if (Operators.checkRequest(n, subscription.subscriber)) {
                    Operators.addAndGet(subscription.demand, n);
                }
            }

        },

        READING {
            @Override
            void request(RedisSubscription<?> subscription, long n) {

                if (Operators.checkRequest(n, subscription.subscriber)) {
                    Operators.addAndGet(subscription.demand, n);
                }
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
            subscription.changeState(this, COMPLETED);
        }

        void onDataAvailable(RedisSubscription<?> subscription) {
            // ignore
        }

        void onAllDataRead(RedisSubscription<?> subscription) {

            subscription.allDataRead = true;

            if (subscription.data.isEmpty() && subscription.changeState(this, COMPLETED)) {
                if (subscription.subscriber != null) {
                    subscription.subscriber.onComplete();
                }
            }
        }

        void onError(RedisSubscription<?> subscription, Throwable t) {

            if (subscription.changeState(this, COMPLETED)) {
                if (subscription.subscriber != null) {
                    subscription.subscriber.onError(t);
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
    private static class SubscriptionCommand<K, V, T> extends CommandWrapper<K, V, T> {

        private final boolean dissolve;
        private final RedisSubscription<T> subscription;
        private boolean completed = false;

        public SubscriptionCommand(RedisCommand<K, V, T> command, RedisSubscription<T> subscription, boolean dissolve) {

            super(command);

            this.subscription = subscription;
            this.dissolve = dissolve;
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
     * Composite {@link com.lambdaworks.redis.output.StreamingOutput.Subscriber} that can notify multiple nested subscribers.
     *
     * @param <T> element type
     */
    private static class CompositeSubscriber<T> implements StreamingOutput.Subscriber<T> {

        private final Collection<StreamingOutput.Subscriber<T>> subscribers;

        CompositeSubscriber(Collection<StreamingOutput.Subscriber<T>> subscribers) {
            this.subscribers = subscribers;
        }

        @Override
        public void onNext(T t) {
            subscribers.forEach(subscriber -> subscriber.onNext(t));
        }
    }
}
