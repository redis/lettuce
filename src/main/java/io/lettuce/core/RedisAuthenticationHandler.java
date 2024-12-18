/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.event.connection.ReauthenticationEvent;
import io.lettuce.core.event.connection.ReauthenticationFailedEvent;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandExpiryWriter;
import io.lettuce.core.protocol.CompleteableCommand;
import io.lettuce.core.protocol.Endpoint;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.protocol.RedisCommand;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static io.lettuce.core.protocol.CommandType.AUTH;
import static io.lettuce.core.protocol.CommandType.DISCARD;
import static io.lettuce.core.protocol.CommandType.EXEC;
import static io.lettuce.core.protocol.CommandType.MULTI;

/**
 * Redis authentication handler. Internally used to authenticate a Redis connection. This class is part of the internal API.
 *
 * @author Ivo Gaydazhiev
 * @since 6.6.0
 */
public class RedisAuthenticationHandler<K, V> {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(RedisAuthenticationHandler.class);

    private final StatefulRedisConnectionImpl<K, V> connection;

    private final RedisCredentialsProvider credentialsProvider;

    private final AtomicReference<Disposable> credentialsSubscription = new AtomicReference<>();

    private final Boolean isPubSubConnection;

    private final AtomicReference<RedisCredentials> credentialsRef = new AtomicReference<>();

    private final ReentrantLock reAuthSafety = new ReentrantLock();

    private final AtomicBoolean inTransaction = new AtomicBoolean(false);

    /**
     * Creates a new {@link RedisAuthenticationHandler}.
     *
     * @param connection the connection to authenticate
     * @param credentialsProvider the implementation of {@link RedisCredentialsProvider} to use
     * @param isPubSubConnection {@code true} if the connection is a pub/sub connection
     */
    public RedisAuthenticationHandler(StatefulRedisConnectionImpl<K, V> connection,
            RedisCredentialsProvider credentialsProvider, Boolean isPubSubConnection) {
        this.connection = connection;
        this.credentialsProvider = credentialsProvider;
        this.isPubSubConnection = isPubSubConnection;
    }

    /**
     * Creates a new {@link RedisAuthenticationHandler} if the connection supports re-authentication.
     *
     * @param connection the connection to authenticate
     * @param credentialsProvider the implementation of {@link RedisCredentialsProvider} to use
     * @param isPubSubConnection {@code true} if the connection is a pub/sub connection
     * @param options the {@link ClientOptions} to use
     * @return a new {@link RedisAuthenticationHandler} if the connection supports re-authentication, otherwise an
     *         implementation of the {@link RedisAuthenticationHandler} that does nothing
     * @since 6.6.0
     * @see RedisCredentialsProvider
     */
    public static <K, V> RedisAuthenticationHandler<K, V> createHandler(StatefulRedisConnectionImpl<K, V> connection,
            RedisCredentialsProvider credentialsProvider, Boolean isPubSubConnection, ClientOptions options) {

        if (isSupported(options)) {

            if (isPubSubConnection && options.getConfiguredProtocolVersion() == ProtocolVersion.RESP2) {
                throw new RedisConnectionException(
                        "Renewable credentials are not supported with RESP2 protocol on a pub/sub connection.");
            }

            return new RedisAuthenticationHandler<>(connection, credentialsProvider, isPubSubConnection);
        }

        return null;
    }

    /**
     * Creates a new default {@link RedisAuthenticationHandler}.
     * <p/>
     * The default {@link RedisAuthenticationHandler} is used when re-authentication is not supported.
     *
     * @return a new {@link RedisAuthenticationHandler}
     * @since 6.6.0
     * @see RedisCredentialsProvider
     */
    public static <K, V> RedisAuthenticationHandler<K, V> createDefaultAuthenticationHandler() {
        return new DisabledAuthenticationHandler<>();
    }

    /**
     * This method subscribes to a stream of credentials provided by the `StreamingCredentialsProvider`.
     * <p>
     * Each time new credentials are received, the client is re-authenticated. The previous subscription, if any, is disposed of
     * before setting the new subscription.
     */
    public void subscribe() {
        if (credentialsProvider == null || !credentialsProvider.supportsStreaming()) {
            return;
        }

        if (!isSupportedConnection()) {
            return;
        }

        Flux<RedisCredentials> credentialsFlux = credentialsProvider.credentials();

        Disposable subscription = credentialsFlux.subscribe(this::onNext, this::onError, this::complete);

        Disposable oldSubscription = credentialsSubscription.getAndSet(subscription);
        if (oldSubscription != null && !oldSubscription.isDisposed()) {
            oldSubscription.dispose();
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
        publishReauthFailedEvent(e);
    }

    /**
     * Performs re-authentication with the provided credentials.
     *
     * @param credentials the new credentials
     */
    protected void reauthenticate(RedisCredentials credentials) {
        setCredentials(credentials);
    }

    boolean isSupportedConnection() {
        if (isPubSubConnection && ProtocolVersion.RESP2 == connection.getConnectionState().getNegotiatedProtocolVersion()) {
            log.warn("Renewable credentials are not supported with RESP2 protocol on a pub/sub connection.");
            return false;
        }
        return true;
    }

    private static boolean isSupported(ClientOptions clientOptions) {
        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        switch (clientOptions.getReauthenticateBehaviour()) {
            case ON_NEW_CREDENTIALS:
                return true;
            case DEFAULT:
            default:
                return false;
        }
    }

    public void postProcess(RedisCommand<K, V, ?> toSend) {
        if (toSend.getType() == EXEC || toSend.getType() == DISCARD) {
            inTransaction.set(false);
            setCredentials(credentialsRef.getAndSet(null));
        }
    }

    public void postProcess(Collection<? extends RedisCommand<K, V, ?>> dispatched) {
        Boolean transactionComplete = null;
        for (RedisCommand<K, V, ?> command : dispatched) {
            if (command.getType() == EXEC || command.getType() == DISCARD) {
                transactionComplete = true;
            }
            if (command.getType() == MULTI) {
                transactionComplete = false;
            }
        }

        if (transactionComplete != null) {
            if (transactionComplete) {
                inTransaction.set(false);
                setCredentials(credentialsRef.getAndSet(null));
            }
        }
    }

    /**
     * Marks that the current connection has started a transaction.
     * <p>
     * During transactions, any re-authentication attempts are deferred until the transaction ends.
     */
    public void startTransaction() {
        reAuthSafety.lock();
        try {
            inTransaction.set(true);
        } finally {
            reAuthSafety.unlock();
        }
    }

    /**
     * Marks that the current connection has ended the transaction.
     * <p>
     * After a transaction is completed, any deferred re-authentication attempts are dispatched.
     */
    public void endTransaction() {
        inTransaction.set(false);
        setCredentials(credentialsRef.getAndSet(null));
    }

    /**
     * Authenticates the current connection using the provided credentials.
     * <p>
     * Unlike using dispatch of {@link RedisAsyncCommands#auth}, this method defers the {@code AUTH} command if the connection
     * is within an active transaction. The authentication command will only be dispatched after the enclosing {@code DISCARD}
     * or {@code EXEC} command is executed, ensuring that authentication does not interfere with ongoing transactions.
     * </p>
     *
     * @param credentials the {@link RedisCredentials} to authenticate the connection. If {@code null}, no action is performed.
     *
     *        <p>
     *        <b>Behavior:</b>
     *        <ul>
     *        <li>If the provided credentials are {@code null}, the method exits immediately.</li>
     *        <li>If a transaction is active (as indicated by {@code inTransaction}), the {@code AUTH} command is not dispatched
     *        immediately but deferred until the transaction ends.</li>
     *        <li>If no transaction is active, the {@code AUTH} command is dispatched immediately using the provided
     *        credentials.</li>
     *        </ul>
     *        </p>
     *
     * @see RedisAsyncCommands#auth
     */
    public void setCredentials(RedisCredentials credentials) {
        if (credentials == null) {
            return;
        }
        reAuthSafety.lock();
        try {
            credentialsRef.set(credentials);
            if (!inTransaction.get()) {
                dispatchAuth(credentialsRef.getAndSet(null));
            }
        } finally {
            reAuthSafety.unlock();
        }
    }

    protected void dispatchAuth(RedisCredentials credentials) {
        if (credentials == null) {
            return;
        }

        // dispatch directly to avoid AUTH preprocessing overrides credentials provider
        RedisCommand<K, V, ?> auth = connection.getChannelWriter().write(authCommand(credentials));
        if (auth instanceof CompleteableCommand) {
            ((CompleteableCommand<?>) auth).onComplete((status, throwable) -> {
                if (throwable != null) {
                    log.error("Re-authentication failed {}.", getEpid(), throwable);
                    publishReauthFailedEvent(throwable);
                } else {
                    log.info("Re-authentication succeeded {}.", getEpid());
                    publishReauthEvent();
                }
            });
        }
    }

    private AsyncCommand<K, V, String> authCommand(RedisCredentials credentials) {
        RedisCodec<K, V> codec = connection.getCodec();
        CommandArgs<K, V> args = new CommandArgs<>(codec);
        if (credentials.getUsername() != null) {
            args.add(credentials.getUsername()).add(credentials.getPassword());
        } else {
            args.add(credentials.getPassword());
        }
        return new AsyncCommand<>(new Command<>(AUTH, new StatusOutput<>(codec), args));
    }

    private void publishReauthEvent() {
        connection.getResources().eventBus().publish(new ReauthenticationEvent(getEpid()));
    }

    private void publishReauthFailedEvent(Throwable throwable) {
        connection.getResources().eventBus().publish(new ReauthenticationFailedEvent(getEpid(), throwable));
    }

    private String getEpid() {
        RedisChannelWriter writer = connection.getChannelWriter();
        while (!(writer instanceof Endpoint)) {

            if (writer instanceof CommandListenerWriter) {
                writer = ((CommandListenerWriter) writer).getDelegate();
                continue;
            }

            if (writer instanceof CommandExpiryWriter) {
                writer = ((CommandExpiryWriter) writer).getDelegate();
                continue;
            }
            return null;
        }

        return ((Endpoint) writer).getId();
    }

    private static final class DisabledAuthenticationHandler<K, V> extends RedisAuthenticationHandler<K, V> {

        public DisabledAuthenticationHandler(StatefulRedisConnectionImpl<K, V> connection,
                RedisCredentialsProvider credentialsProvider, Boolean isPubSubConnection) {
            super(null, null, null);
        }

        public DisabledAuthenticationHandler() {
            super(null, null, null);
        }

        @Override
        public void postProcess(RedisCommand<K, V, ?> toSend) {
            // No-op
        }

        @Override
        public void startTransaction() {
            // No-op
        }

        @Override
        public void endTransaction() {
            // No-op
        }

        @Override
        public void setCredentials(RedisCredentials credentials) {
            // No-op
        }

        @Override
        public void unsubscribe() {
            // No-op
        }

        @Override
        public void subscribe() {
            // No-op
        }

    }

}
