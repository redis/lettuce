package com.lambdaworks.redis;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import reactor.core.Exceptions;

enum BackpressureUtils {
    ;

    /**
     * Check Subscription current state and cancel new Subscription if different null, returning true if ready to subscribe.
     *
     * @param current current Subscription, expected to be null
     * @param next new Subscription
     * @return true if Subscription can be used
     */
    public static boolean validate(Subscription current, Subscription next) {
        Objects.requireNonNull(next, "Subscription cannot be null");
        if (current != null) {
            next.cancel();
            // reportSubscriptionSet();
            return false;
        }

        return true;
    }

    /**
     * Evaluate if a request is strictly positive otherwise {@link #reportBadRequest(long)}
     * 
     * @param n the request value
     * @return true if valid
     */
    public static boolean validate(long n) {
        if (n < 0) {
            reportBadRequest(n);
            return false;
        }
        return true;
    }

    /**
     * Throws an exception if request is 0 or negative as specified in rule 3.09 of Reactive Streams
     *
     * @param n demand to check
     * @param subscriber Subscriber to onError if non strict positive n
     *
     * @return true if valid or false if specification exception occured
     *
     * @throws IllegalArgumentException if subscriber is null and demand is negative or 0.
     */
    public static boolean checkRequest(long n, Subscriber<?> subscriber) {
        if (n <= 0L) {
            if (null != subscriber) {
                subscriber.onError(Exceptions.nullOrNegativeRequestException(n));
            } else {
                throw Exceptions.nullOrNegativeRequestException(n);
            }
            return false;
        }
        return true;
    }

    /**
     * Cap an addition to Long.MAX_VALUE
     *
     * @param a left operand
     * @param b right operand
     * @return Addition result or Long.MAX_VALUE if overflow
     */
    public static long addCap(long a, long b) {
        long res = a + b;
        if (res < 0L) {
            return Long.MAX_VALUE;
        }
        return res;
    }

    /**
     * Cap a substraction to 0
     *
     * @param a left operand
     * @param b right operand
     * @return Subscription result or 0 if overflow
     */
    public static long subOrZero(long a, long b) {
        long res = a - b;
        if (res < 0L) {
            return 0;
        }
        return res;
    }

    /**
     * Concurrent addition bound to Long.MAX_VALUE. Any concurrent write will "happen" before this operation.
     *
     * @param current current atomic to update
     * @param toAdd delta to add
     * @return Addition result or Long.MAX_VALUE
     */
    public static long addAndGet(AtomicLong current, long toAdd) {
        long u, r;
        do {
            r = current.get();
            if (r == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            u = addCap(r, toAdd);
        } while (!current.compareAndSet(r, u));

        return u;
    }

    /**
     * Concurrent substraction bound to 0 and Long.MAX_VALUE. Any concurrent write will "happen" before this operation.
     *
     * @param sequence current atomic to update
     * @param toSub delta to sub
     * @return value before subscription, 0 or Long.MAX_VALUE
     */
    public static long getAndSub(AtomicLong sequence, long toSub) {
        long r, u;
        do {
            r = sequence.get();
            if (r == 0 || r == Long.MAX_VALUE) {
                return r;
            }
            u = subOrZero(r, toSub);
        } while (!sequence.compareAndSet(r, u));

        return r;
    }

    /**
     * Throw {@link IllegalArgumentException}
     * 
     * @param n the demand to evaluate
     */
    public static void reportBadRequest(long n) {
        throw Exceptions.nullOrNegativeRequestException(n);
    }
}