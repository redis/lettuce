/*
 * Copyright 2017-2020 the original author or authors.
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.reactivestreams.Subscription;

import reactor.core.Exceptions;
import reactor.core.publisher.Hooks;
import reactor.util.annotation.Nullable;
import reactor.util.concurrent.Queues;
import reactor.util.context.Context;
import io.lettuce.core.internal.LettuceFactories;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Operator utilities to handle noop subscriptions, validate request size and to cap concurrent additive operations to
 * Long.MAX_VALUE, which is generic to {@link Subscription#request(long)} handling.
 * <p>
 * This class duplicates some methods from {@link reactor.core.publisher.Operators} to be independent from Reactor API changes.
 *
 * @author Mark Paluch
 * @since 5.0
 */
class Operators {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(Operators.class);

    /**
     * A key that can be used to store a sequence-specific {@link Hooks#onOperatorError(BiFunction)} hook in a {@link Context},
     * as a {@link BiFunction BiFunction&lt;Throwable, Object, Throwable&gt;}.
     */
    private static final String KEY_ON_OPERATOR_ERROR = "reactor.onOperatorError.local";

    private static final Field onOperatorErrorHook = findOnOperatorErrorHookField();

    private static final Supplier<Queue<Object>> queueSupplier = getQueueSupplier();

    private static Field findOnOperatorErrorHookField() {

        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<Field>) () -> {

                Field field = Hooks.class.getDeclaredField("onOperatorErrorHook");

                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }

                return field;
            });

        } catch (PrivilegedActionException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Supplier<Queue<Object>> getQueueSupplier() {

        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<Supplier<Queue<Object>>>) () -> {
                Method unbounded = Queues.class.getMethod("unbounded");
                return (Supplier) unbounded.invoke(Queues.class);
            });

        } catch (PrivilegedActionException e) {
            return LettuceFactories::newSpScQueue;
        }
    }

    /**
     * Cap an addition to Long.MAX_VALUE
     *
     * @param a left operand.
     * @param b right operand.
     * @return Addition result or Long.MAX_VALUE if overflow.
     */
    static long addCap(long a, long b) {

        long res = a + b;
        if (res < 0L) {
            return Long.MAX_VALUE;
        }
        return res;
    }

    /**
     * Concurrent addition bound to Long.MAX_VALUE. Any concurrent write will "happen before" this operation.
     *
     * @param <T> the parent instance type.
     * @param updater current field updater.
     * @param instance current instance to update.
     * @param toAdd delta to add.
     * @return {@code true} if the operation succeeded.
     * @since 5.0.1
     */
    public static <T> boolean request(AtomicLongFieldUpdater<T> updater, T instance, long toAdd) {

        if (validate(toAdd)) {
            addCap(updater, instance, toAdd);

            return true;
        }

        return false;
    }

    /**
     * Concurrent addition bound to Long.MAX_VALUE. Any concurrent write will "happen before" this operation.
     *
     * @param <T> the parent instance type.
     * @param updater current field updater.
     * @param instance current instance to update.
     * @param toAdd delta to add.
     * @return value before addition or Long.MAX_VALUE.
     */
    static <T> long addCap(AtomicLongFieldUpdater<T> updater, T instance, long toAdd) {

        long r, u;
        for (;;) {
            r = updater.get(instance);
            if (r == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            u = addCap(r, toAdd);
            if (updater.compareAndSet(instance, r, u)) {
                return r;
            }
        }
    }

    /**
     * Evaluate if a request is strictly positive otherwise {@link #reportBadRequest(long)}.
     *
     * @param n the request value.
     * @return {@code true} if valid.
     */
    static boolean validate(long n) {

        if (n <= 0) {
            reportBadRequest(n);
            return false;
        }
        return true;
    }

    /**
     * Log an {@link IllegalArgumentException} if the request is null or negative.
     *
     * @param n the failing demand.
     * @see Exceptions#nullOrNegativeRequestException(long)
     */
    static void reportBadRequest(long n) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Negative request", Exceptions.nullOrNegativeRequestException(n));
        }
    }

    /**
     *
     * @param elements the invalid requested demand.
     * @return a new {@link IllegalArgumentException} with a cause message abiding to reactive stream specification rule 3.9.
     */
    static IllegalArgumentException nullOrNegativeRequestException(long elements) {
        return new IllegalArgumentException("Spec. Rule 3.9 - Cannot request a non strictly positive number: " + elements);
    }

    /**
     * Map an "operator" error given an operator parent {@link Subscription}. The result error will be passed via onError to the
     * operator downstream. {@link Subscription} will be cancelled after checking for fatal error via
     * {@link Exceptions#throwIfFatal(Throwable)}. Takes an additional signal, which can be added as a suppressed exception if
     * it is a {@link Throwable} and the default {@link Hooks#onOperatorError(BiFunction) hook} is in place.
     *
     * @param subscription the linked operator parent {@link Subscription}.
     * @param error the callback or operator error.
     * @param dataSignal the value (onNext or onError) signal processed during failure.
     * @param context a context that might hold a local error consumer.
     * @return mapped {@link Throwable}.
     */
    static Throwable onOperatorError(@Nullable Subscription subscription, Throwable error, @Nullable Object dataSignal,
            Context context) {

        Exceptions.throwIfFatal(error);
        if (subscription != null) {
            subscription.cancel();
        }

        Throwable t = Exceptions.unwrap(error);
        BiFunction<? super Throwable, Object, ? extends Throwable> hook = context.getOrDefault(KEY_ON_OPERATOR_ERROR, null);
        if (hook == null && onOperatorErrorHook != null) {
            hook = getOnOperatorErrorHook();
        }

        if (hook == null) {
            if (dataSignal != null) {
                if (dataSignal != t && dataSignal instanceof Throwable) {
                    t = Exceptions.addSuppressed(t, (Throwable) dataSignal);
                }
                // do not wrap original value to avoid strong references
                /*
                 * else { }
                 */
            }
            return t;
        }
        return hook.apply(error, dataSignal);
    }

    /**
     * Create a new {@link Queue}.
     *
     * @return the new queue.
     */
    @SuppressWarnings("unchecked")
    static <T> Queue<T> newQueue() {
        return (Queue<T>) queueSupplier.get();
    }

    @SuppressWarnings("unchecked")
    private static BiFunction<? super Throwable, Object, ? extends Throwable> getOnOperatorErrorHook() {

        try {
            return (BiFunction<? super Throwable, Object, ? extends Throwable>) onOperatorErrorHook.get(Hooks.class);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

}
