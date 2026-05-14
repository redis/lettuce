package io.lettuce.core;

import java.util.Objects;

/**
 * Result of the {@code INCREX} command, containing the new value and the actual increment applied.
 *
 * @param <T> Number type — {@link Long} for integer mode, {@link Double} for float mode.
 * @since 7.6
 */
public class IncrexValue<T extends Number> {

    private final T value;

    private final T increment;

    public IncrexValue(T value, T increment) {
        this.value = value;
        this.increment = increment;
    }

    /**
     * The key's value after the increment operation.
     */
    public T getValue() {
        return value;
    }

    /**
     * The actual increment applied. May differ from the requested increment under {@code OVERFLOW SAT}.
     */
    public T getIncrement() {
        return increment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof IncrexValue))
            return false;
        IncrexValue<?> that = (IncrexValue<?>) o;
        return Objects.equals(value, that.value) && Objects.equals(increment, that.increment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, increment);
    }

    @Override
    public String toString() {
        return "IncrexValue{value=" + value + ", increment=" + increment + "}";
    }

}
