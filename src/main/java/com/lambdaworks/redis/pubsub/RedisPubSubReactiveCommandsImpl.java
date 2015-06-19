package com.lambdaworks.redis.pubsub;

import static com.lambdaworks.redis.protocol.CommandType.*;

import rx.Observable;
import rx.Subscriber;

import com.lambdaworks.redis.RedisReactiveCommandsImpl;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.pubsub.api.rx.ChannelMessage;
import com.lambdaworks.redis.pubsub.api.rx.PatternMessage;
import com.lambdaworks.redis.pubsub.api.rx.RedisPubSubReactiveCommands;

/**
 * A reactive and thread-safe API for a Redis pub/sub connection.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class RedisPubSubReactiveCommandsImpl<K, V> extends RedisReactiveCommandsImpl<K, V> implements
        RedisPubSubReactiveCommands<K, V> {

    /**
     * Initialize a new connection.
     *
     * @param connection the connection .
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisPubSubReactiveCommandsImpl(StatefulRedisPubSubConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
        this.connection = connection;
    }

    /**
     * Add a new listener.
     * 
     * @param listener Listener.
     */
    @Override
    public void addListener(RedisPubSubListener<K, V> listener) {
        getStatefulConnection().addListener(listener);
    }

    @Override
    public Observable<PatternMessage<K, V>> observePatterns() {

        SubscriptionPubSubListener<K, V, PatternMessage<K, V>> listener = new SubscriptionPubSubListener<K, V, PatternMessage<K, V>>() {
            @Override
            public void message(K pattern, K channel, V message) {
                if (subscriber == null) {
                    return;
                }

                if (subscriber.isUnsubscribed()) {
                    subscriber.onCompleted();
                    removeListener(this);
                    subscriber = null;
                    return;
                }

                subscriber.onNext(new PatternMessage<>(pattern, channel, message));
            }
        };

        return Observable.create(new PubSubObservable<>(listener));
    }

    @Override
    public Observable<ChannelMessage<K, V>> observeChannels() {

        SubscriptionPubSubListener<K, V, ChannelMessage<K, V>> listener = new SubscriptionPubSubListener<K, V, ChannelMessage<K, V>>() {
            @Override
            public void message(K channel, V message) {
                if (subscriber == null) {
                    return;
                }

                if (subscriber.isUnsubscribed()) {
                    subscriber.onCompleted();
                    removeListener(this);
                    subscriber = null;
                    return;
                }

                subscriber.onNext(new ChannelMessage<>(channel, message));
            }
        };

        return Observable.create(new PubSubObservable<>(listener));
    }

    /**
     * Remove an existing listener.
     * 
     * @param listener Listener.
     */
    @Override
    public void removeListener(RedisPubSubListener<K, V> listener) {
        getStatefulConnection().removeListener(listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Observable<Void> psubscribe(K... patterns) {
        return (Observable<Void>) createObservable(PSUBSCRIBE, new PubSubOutput<K, V, K>(codec), args(patterns));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Observable<Void> punsubscribe(K... patterns) {
        return (Observable<Void>) createObservable(PUNSUBSCRIBE, new PubSubOutput<K, V, K>(codec), args(patterns));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Observable<Void> subscribe(K... channels) {
        return (Observable<Void>) createObservable(SUBSCRIBE, new PubSubOutput<K, V, K>(codec), args(channels));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Observable<Void> unsubscribe(K... channels) {
        return (Observable<Void>) createObservable(UNSUBSCRIBE, new PubSubOutput<K, V, K>(codec), args(channels));
    }

    private CommandArgs<K, V> args(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKeys(keys);
        return args;
    }

    @Override
    @SuppressWarnings("unchecked")
    public StatefulRedisPubSubConnection<K, V> getStatefulConnection() {
        return (StatefulRedisPubSubConnection<K, V>) super.getStatefulConnection();
    }

    private class PubSubObservable<T> implements Observable.OnSubscribe<T> {

        private SubscriptionPubSubListener<K, V, T> listener;

        public PubSubObservable(SubscriptionPubSubListener<K, V, T> listener) {
            this.listener = listener;
        }

        @Override
        public void call(Subscriber<? super T> subscriber) {

            listener.activate(subscriber);
            subscriber.onStart();
            addListener(listener);

        }
    }

    private static class SubscriptionPubSubListener<K, V, T> extends RedisPubSubAdapter<K, V> {

        protected Subscriber<? super T> subscriber;

        public void activate(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

    }
}
