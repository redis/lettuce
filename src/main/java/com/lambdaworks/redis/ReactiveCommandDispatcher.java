package com.lambdaworks.redis;

import java.util.Collection;
import java.util.function.Supplier;

import rx.Observable;
import rx.Subscriber;

import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.protocol.CommandWrapper;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * Reactive command dispatcher.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class ReactiveCommandDispatcher<K, V, T> implements Observable.OnSubscribe<T> {

    private Supplier<? extends RedisCommand<K, V, T>> commandSupplier;
    private RedisCommand<K, V, T> command;
    private StatefulConnection<K, V> connection;
    private boolean dissolve;

    public ReactiveCommandDispatcher(Supplier<RedisCommand<K, V, T>> commandSupplier, StatefulConnection<K, V> connection,
            boolean dissolve) {
        this.commandSupplier = commandSupplier;
        this.connection = connection;
        this.dissolve = dissolve;
        this.command = commandSupplier.get();
    }

    @Override
    public void call(Subscriber<? super T> subscriber) {

        if (subscriber.isUnsubscribed()) {
            return;
        }

        // Reuse the first command but then discard it.
        RedisCommand<K, V, T> command = this.command;
        if (command == null) {
            command = commandSupplier.get();
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
        public void complete() {
            if (completed) {
                return;
            }

            super.complete();

            if (getOutput() != null) {
                Object result = getOutput().get();
                if (result != null) {

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
}
