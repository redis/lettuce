package com.lambdaworks;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.Sleeper;
import com.google.code.tempusfugit.temporal.ThreadSleep;
import com.google.code.tempusfugit.temporal.Timeout;

/**
 * Wait-Until helper.
 * 
 * @author Mark Paluch
 */
public class Wait {

    /**
     * Initialize a {@link com.lambdaworks.Wait.WaitBuilder} to wait until the {@code supplier} supplies {@literal true}
     * 
     * @param supplier
     * @return
     */
    public static WaitBuilder<Boolean> untilTrue(Supplier<Boolean> supplier) {
        WaitBuilder<Boolean> wb = new WaitBuilder<>();

        wb.supplier = supplier;
        wb.check = o -> o;

        return wb;
    }

    /**
     * Initialize a {@link com.lambdaworks.Wait.WaitBuilder} to wait until the {@code condition} does not throw exceptions
     * 
     * @param condition
     * @return
     */
    public static WaitBuilder<?> untilNoException(VoidWaitCondition condition) {
        WaitBuilder<?> wb = new WaitBuilder<>();
        wb.waitCondition = () -> {
            try {
                condition.test();
                return true;
            } catch (Exception e) {
                return false;
            }
        };

        wb.supplier = () -> {
            condition.test();
            return null;
        };

        return wb;
    }

    /**
     * Initialize a {@link com.lambdaworks.Wait.WaitBuilder} to wait until the {@code actualSupplier} provides an object that is
     * not equal to {@code expectation}
     * 
     * @param expectation
     * @param actualSupplier
     * @param <T>
     * @return
     */
    public static <T> WaitBuilder<T> untilNotEquals(T expectation, Supplier<T> actualSupplier) {
        WaitBuilder<T> wb = new WaitBuilder<>();

        wb.supplier = actualSupplier;
        wb.check = o -> {
            if (o == expectation) {
                return false;
            }

            if ((o == null && expectation != null) || (o != null && expectation == null)) {
                return true;
            }

            if (o instanceof Number && expectation instanceof Number) {
                Number actualNumber = (Number) o;
                Number expectedNumber = (Number) o;

                if (actualNumber.doubleValue() == expectedNumber.doubleValue()) {
                    return false;
                }

                if (actualNumber.longValue() == expectedNumber.longValue()) {
                    return false;
                }
            }

            return !o.equals(expectation);
        };
        wb.messageFunction = o -> "Objects are equal: " + expectation + " and " + o;

        return wb;
    }

    /**
     * Initialize a {@link com.lambdaworks.Wait.WaitBuilder} to wait until the {@code actualSupplier} provides an object that is
     * not equal to {@code expectation}
     *
     * @param expectation
     * @param actualSupplier
     * @param <T>
     * @return
     */
    public static <T> WaitBuilder<T> untilEquals(T expectation, Supplier<T> actualSupplier) {
        WaitBuilder<T> wb = new WaitBuilder<>();

        wb.supplier = actualSupplier;
        wb.check = o -> {
            if (o == expectation) {
                return true;
            }

            if ((o == null && expectation != null) || (o != null && expectation == null)) {
                return false;
            }

            if (o instanceof Number && expectation instanceof Number) {
                Number actualNumber = (Number) o;
                Number expectedNumber = (Number) expectation;

                if (actualNumber.doubleValue() == expectedNumber.doubleValue()) {
                    return true;
                }

                if (actualNumber.longValue() == expectedNumber.longValue()) {
                    return true;
                }
            }

            return o.equals(expectation);
        };
        wb.messageFunction = o -> "Objects are not equal: " + expectation + " and " + o;

        return wb;
    }

    @FunctionalInterface
    public interface WaitCondition {

        boolean isSatisfied() throws Exception;
    }

    @FunctionalInterface
    public interface VoidWaitCondition {

        void test() throws Exception;
    }

    @FunctionalInterface
    public interface Supplier<T> {
        T get() throws Exception;
    }

    public static class WaitBuilder<T> {

        private Duration duration = Duration.seconds(10);
        private Sleeper sleeper = new ThreadSleep(Duration.millis(100));
        private Function<T, String> messageFunction;
        private Supplier<T> supplier;
        private Predicate<T> check;
        private WaitCondition waitCondition;

        public WaitBuilder<T> during(Duration duration) {
            this.duration = duration;
            return this;
        }

        public WaitBuilder<T> message(String message) {
            this.messageFunction = o -> message;
            return this;
        }

        public void waitOrTimeout() {

            Waiter waiter = new Waiter();
            waiter.duration = duration;
            waiter.sleeper = sleeper;
            waiter.messageFunction = (Function<Object, String>) messageFunction;

            if (waitCondition != null) {
                waiter.waitOrTimeout(waitCondition, supplier);
            } else {
                waiter.waitOrTimeout(supplier, check);
            }
        }

    }

    private static class Waiter {
        private Duration duration;
        private Sleeper sleeper;
        private Function<Object, String> messageFunction;

        private <T> void waitOrTimeout(Supplier<T> supplier, Predicate<T> check) {

            try {
                if (!success(() -> check.test(supplier.get()), Timeout.timeout(duration))) {
                    if (messageFunction != null) {
                        throw new TimeoutException(messageFunction.apply(supplier.get()));
                    }
                    throw new TimeoutException("Condition not satisfied for: " + supplier.get());
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private <T> void waitOrTimeout(WaitCondition waitCondition, Supplier<T> supplier) {

            try {
                if (!success(waitCondition, Timeout.timeout(duration))) {
                    try {
                        if (messageFunction != null) {
                            throw new TimeoutException(messageFunction.apply(supplier.get()));
                        }
                        throw new TimeoutException("Condition not satisfied for: " + supplier.get());
                    } catch (TimeoutException e) {
                        throw e;
                    } catch (Exception e) {
                        if (messageFunction != null) {
                            throw new ExecutionException(messageFunction.apply(null), e);
                        }
                        throw new ExecutionException("Condition not satisfied", e);
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private boolean success(WaitCondition condition, Timeout timeout) throws Exception {
            while (!timeout.hasExpired()) {
                if (condition.isSatisfied()) {
                    return true;
                }
                sleeper.sleep();
            }
            return false;
        }
    }
}
