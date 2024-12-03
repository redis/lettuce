package io.lettuce.core;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ReauthEvent;
import io.lettuce.core.event.connection.ReauthFailedEvent;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Endpoint;
import io.lettuce.core.protocol.RedisCommand;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicReference;

public abstract class BaseRedisAuthenticationHandler<T extends RedisChannelHandler<?, ?>> {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(BaseRedisAuthenticationHandler.class);

    protected final T connection;

    private final RedisCommandBuilder<String, String> commandBuilder = new RedisCommandBuilder<>(StringCodec.UTF8);

    private final AtomicReference<Disposable> credentialsSubscription = new AtomicReference<>();

    protected final EventBus eventBus;

    public BaseRedisAuthenticationHandler(T connection, EventBus eventBus) {
        this.connection = connection;
        this.eventBus = eventBus;
    }

    /**
     * Subscribes to the provided `Flux` of credentials if the given `RedisCredentialsProvider` supports streaming credentials.
     * <p>
     * This method subscribes to a stream of credentials provided by the `StreamingCredentialsProvider`. Each time new
     * credentials are received, the client is reauthenticated. If the connection is not supported, the method returns without
     * subscribing.
     * <p>
     * The previous subscription, if any, is disposed of before setting the new subscription.
     *
     * @param credentialsProvider the credentials provider to subscribe to
     */
    public void subscribe(RedisCredentialsProvider credentialsProvider) {
        if (credentialsProvider == null) {
            return;
        }

        if (credentialsProvider instanceof StreamingCredentialsProvider) {
            if (!isSupportedConnection()) {
                return;
            }

            Flux<RedisCredentials> credentialsFlux = ((StreamingCredentialsProvider) credentialsProvider).credentials();

            Disposable subscription = credentialsFlux.subscribe(this::onNext, this::onError, this::complete);

            Disposable oldSubscription = credentialsSubscription.getAndSet(subscription);
            if (oldSubscription != null && !oldSubscription.isDisposed()) {
                oldSubscription.dispose();
            }
        }
    }

    /**
     * Unsubscribes from the current credentials stream.
     */
    public void unsubscribe() {
        Disposable subscription = credentialsSubscription.getAndSet(null);
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

    protected void complete() {
        log.debug("Credentials stream completed");
    }

    protected void onNext(RedisCredentials credentials) {
        reauthenticate(credentials);
    }

    protected void onError(Throwable e) {
        log.error("Credentials renew failed.", e);
    }

    /**
     * Performs re-authentication with the provided credentials.
     *
     * @param credentials the new credentials
     */
    private void reauthenticate(RedisCredentials credentials) {
        CharSequence password = CharBuffer.wrap(credentials.getPassword());

        AsyncCommand<String, String, String> authCmd;
        if (credentials.hasUsername()) {
            authCmd = new AsyncCommand<>(commandBuilder.auth(credentials.getUsername(), password));
        } else {
            authCmd = new AsyncCommand<>(commandBuilder.auth(password));
        }

        dispatchAuth(authCmd).thenRun(() -> {
            publishReauthEvent();
            log.info("Re-authentication succeeded for endpoint {}.", getEpid());
        }).exceptionally(throwable -> {
            publishReauthFailedEvent(throwable);
            log.error("Re-authentication failed for endpoint {}.", getEpid(), throwable);
            return null;
        });
        ;
    }

    private void publishReauthEvent() {
        eventBus.publish(new ReauthEvent(getEpid()));
    }

    private void publishReauthFailedEvent(Throwable throwable) {
        eventBus.publish(new ReauthFailedEvent(getEpid(), throwable));
    }

    protected boolean isSupportedConnection() {
        return true;
    }

    private AsyncCommand<String, String, String> dispatchAuth(RedisCommand<String, String, String> authCommand) {
        AsyncCommand<String, String, String> asyncCommand = new AsyncCommand<>(authCommand);
        RedisCommand<String, String, String> dispatched = connection.getChannelWriter().write(asyncCommand);
        if (dispatched instanceof AsyncCommand) {
            return (AsyncCommand<String, String, String>) dispatched;
        }
        return asyncCommand;
    }

    private String getEpid() {
        if (connection.getChannelWriter() instanceof Endpoint) {
            return ((Endpoint) connection.getChannelWriter()).getId();
        }
        return "unknown";
    }

}
