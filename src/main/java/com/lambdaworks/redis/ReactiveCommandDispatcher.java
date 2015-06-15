package com.lambdaworks.redis;

import java.util.Collection;
import java.util.function.Supplier;

import rx.Observable;
import rx.Subscriber;

import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.protocol.AsyncCommand;
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

        connection.dispatch(new AsyncCommand<K, V, T>(command) {

            @Override
            public void complete() {
                super.complete();
                if (getOutput() != null && getOutput().get() != null) {
                    Object result = getOutput().get();
                    if (dissolve && result instanceof Collection) {
                        Collection<T> collection = (Collection<T>) result;
                        for (T t : collection) {
                            subscriber.onNext(t);
                        }
                    } else {
                        subscriber.onNext((T) result);
                    }
                }
                subscriber.onCompleted();
            }

            @Override
            public void cancel() {
                super.cancel();
                subscriber.onCompleted();
            }

            @Override
            public boolean completeExceptionally(Throwable throwable) {
                boolean b = super.completeExceptionally(throwable);
                subscriber.onError(throwable);
                return b;
            }
        });

        this.command = null;

    }

}
