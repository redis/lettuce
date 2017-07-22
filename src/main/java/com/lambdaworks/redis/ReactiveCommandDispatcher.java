/*
 * Copyright 2011-2017 the original author or authors.
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
package com.lambdaworks.redis;

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
    public ReactiveCommandDispatcher(RedisCommand<K, V, T> staticCommand, StatefulConnection<K, V> connection, boolean dissolve) {
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
                streamingOutput.setSubscriber(new DelegatingWrapper<>(new ObservableSubscriberWrapper<>(subscriber),
                        streamingOutput.getSubscriber()));
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
            if (completed || subscriber.isUnsubscribed()) {
                return;
            }

            try {
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

                try {
                    subscriber.onCompleted();
                } catch (Exception e) {
                    completeExceptionally(e);
                }
            } finally {
                completed = true;
            }
        }

        @Override
        public void cancel() {

            if (completed || subscriber.isUnsubscribed()) {
                return;
            }

            super.cancel();
            subscriber.onCompleted();
            completed = true;
        }

        @Override
        public boolean completeExceptionally(Throwable throwable) {
            if (completed || subscriber.isUnsubscribed()) {
                return false;
            }

            boolean b = super.completeExceptionally(throwable);
            subscriber.onError(throwable);
            completed = true;
            return b;
        }
    }

    static class ObservableSubscriberWrapper<T> extends StreamingOutput.Subscriber<T> {

        private Subscriber<? super T> subscriber;

        public ObservableSubscriberWrapper(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onNext(T t) {

            if (subscriber.isUnsubscribed()) {
                return;
            }

            subscriber.onNext(t);
        }
    }

    static class DelegatingWrapper<T> extends StreamingOutput.Subscriber<T> {

        private final StreamingOutput.Subscriber<T> first;
        private final StreamingOutput.Subscriber<T> second;

        public DelegatingWrapper(StreamingOutput.Subscriber<T> first, StreamingOutput.Subscriber<T> second) {
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
