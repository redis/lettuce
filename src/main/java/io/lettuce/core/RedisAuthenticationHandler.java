/*
 * Copyright 2019-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ReauthenticateEvent;
import io.lettuce.core.event.connection.ReauthenticateFailedEvent;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Endpoint;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.protocol.RedisCommand;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Redis authentication handler. Internally used to authenticate a Redis connection. This class is part of the internal API.
 *
 * @author Ivo Gaydazhiev
 * @since 6.6.0
 */
public class RedisAuthenticationHandler {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(RedisAuthenticationHandler.class);

    private final StatefulRedisConnectionImpl<?, ?> connection;

    private final RedisCredentialsProvider credentialsProvider;

    private final AtomicReference<Disposable> credentialsSubscription = new AtomicReference<>();

    private final Boolean isPubSubConnection;

    public RedisAuthenticationHandler(StatefulRedisConnectionImpl<?, ?> connection,
            RedisCredentialsProvider credentialsProvider, Boolean isPubSubConnection) {
        this.connection = connection;
        this.credentialsProvider = credentialsProvider;
        this.isPubSubConnection = isPubSubConnection;
    }

    /**
     * This method subscribes to a stream of credentials provided by the `StreamingCredentialsProvider`.
     * <p>
     * Each time new credentials are received, the client is re-authenticated. The previous subscription, if any, is disposed of
     * before setting the new subscription.
     */
    public void subscribe() {
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
        publishReauthFailedEvent(e);
    }

    /**
     * Performs re-authentication with the provided credentials.
     *
     * @param credentials the new credentials
     */
    protected void reauthenticate(RedisCredentials credentials) {
        connection.setCredentials(credentials);
    }

    protected boolean isSupportedConnection() {
        if (isPubSubConnection && ProtocolVersion.RESP2 == connection.getConnectionState().getNegotiatedProtocolVersion()) {
            log.warn("Renewable credentials are not supported with RESP2 protocol on a pub/sub connection.");
            return false;
        }
        return true;
    }

    private void publishReauthFailedEvent(Throwable throwable) {
        connection.getResources().eventBus().publish(new ReauthenticateFailedEvent(throwable));
    }

    public static boolean isSupported(ClientOptions clientOptions) {
        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        switch (clientOptions.getReauthenticateBehaviour()) {
            case ON_NEW_CREDENTIALS:
                return true;

            case DEFAULT:
                return false;

            default:
                return false;
        }
    }

}
