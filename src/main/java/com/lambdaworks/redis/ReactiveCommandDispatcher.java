package com.lambdaworks.redis;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import rx.Observable;
import rx.Subscriber;

import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.output.StreamingOutput;
import com.lambdaworks.redis.protocol.CommandWrapper;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * Reactive command dispatcher.
 *
 * @author Mark Paluch
 */
public class ReactiveCommandDispatcher<K, V, T> implements Observable.OnSubscribe<T> {

    private Supplier<? extends RedisCommand<K, V, T>> commandSupplier;
    private volatile RedisCommand<K, V, T> command;
    private StatefulConnection<K, V> connection;
    private boolean dissolve;

    /**
     *
     * @param staticCommand static command, must not be {@literal null}
     * @param connection the connection, must not be {@literal null}
     * @param dissolve dissolve collections into particular elements
     */
    public ReactiveCommandDispatcher(RedisCommand<K, V, T> staticCommand, StatefulConnection<K, V> connection,
            boolean dissolve) {
        this(() -> staticCommand, connection, dissolve);
    }

    /**
     *
     * @param commandSupplier command supplier, must not be {@literal null}
     * @param connection the connection, must not be {@literal null}
     * @param dissolve dissolve collections into particular elements
     */
    public ReactiveCommandDispatcher(Supplier<RedisCommand<K, V, T>> commandSupplier, StatefulConnection<K, V> connection,
            boolean dissolve) {

        LettuceAssert.notNull(commandSupplier, "CommandSupplier must not be null");
        LettuceAssert.notNull(connection, "StatefulConnection must not be null");

        this.commandSupplier = commandSupplier;
        this.connection = connection;
        this.dissolve = dissolve;
        this.command = commandSupplier.get();
    }

    @Override
    public void call(Subscriber<? super T> subscriber) {

        // Reuse the first command but then discard it.
        RedisCommand<K, V, T> command = this.command;
        if (command == null) {
            command = commandSupplier.get();
        }

        if (command.getOutput() instanceof StreamingOutput<?>) {
            StreamingOutput<T> streamingOutput = (StreamingOutput<T>) command.getOutput();

            if (connection instanceof StatefulRedisConnection<?, ?> && ((StatefulRedisConnection) connection).isMulti()) {
                streamingOutput.setSubscriber(new DelegatingWrapper<T>(
                        Arrays.asList(new ObservableSubscriberWrapper<>(subscriber), streamingOutput.getSubscriber())));
            } else {
                streamingOutput.setSubscriber(new ObservableSubscriberWrapper<>(subscriber));
            }
        }

        connection.dispatch(new ObservableCommand<>(command, subscriber, dissolve));

        this.command = null;

    }

    private static class ObservableCommand<K, V, T> extends CommandWrapper<K, V, T> {

        private final Subscriber<? super T> subscriber;
        private final boolean dissolve;
        private boolean completed = false;

        public ObservableCommand(RedisCommand<K, V, T> command, Subscriber<? super T> subscriber, boolean dissolve) {
            super(command);
            this.subscriber = subscriber;
            this.dissolve = dissolve;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void complete() {
            if (completed) {
                return;
            }

            super.complete();

            if (getOutput() != null) {
                Object result = getOutput().get();
                if (!(getOutput() instanceof StreamingOutput<?>) && result != null) {

                    if (dissolve && result instanceof Collection) {
                        Collection<T> collection = (Collection<T>) result;
                        for (T t : collection) {
                            subscriber.onNext(t);
                        }
                    } else {
                        subscriber.onNext((T) result);
                    }
                }

                if (getOutput().hasError()) {
                    subscriber.onError(new RedisCommandExecutionException(getOutput().getError()));
                    completed = true;
                    return;
                }
            }

            completed = true;
            subscriber.onCompleted();
        }

        @Override
        public void cancel() {

            if (completed) {
                return;
            }

            super.cancel();
            subscriber.onCompleted();
            completed = true;
        }

        @Override
        public boolean completeExceptionally(Throwable throwable) {
            if (completed) {
                return false;
            }

            boolean b = super.completeExceptionally(throwable);
            subscriber.onError(throwable);
            completed = true;
            return b;
        }
    }

    static class ObservableSubscriberWrapper<T> implements StreamingOutput.Subscriber<T> {

        private Subscriber<? super T> subscriber;

        public ObservableSubscriberWrapper(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onNext(T t) {
            subscriber.onNext(t);
        }
    }

    static class DelegatingWrapper<T> implements StreamingOutput.Subscriber<T> {

        private Collection<StreamingOutput.Subscriber<T>> subscribers;

        public DelegatingWrapper(Collection<StreamingOutput.Subscriber<T>> subscribers) {
            this.subscribers = subscribers;
        }

        @Override
        public void onNext(T t) {

            for (StreamingOutput.Subscriber<T> subscriber : subscribers) {
                subscriber.onNext(t);
            }
        }
    }
}
