package io.lettuce.core.event;

import java.util.Optional;
import java.util.OptionalInt;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Delivery settings for one {@link EventBus} subscription. A setting that is not set falls back to the default of the
 * {@link EventBus}. {@link EventBus} implementations that do not support these settings ignore them.
 * <p>
 * Example: keep only the latest few connection events for a listener that may fall behind.
 *
 * <pre class="code">
 * eventBus.subscribe(ConnectionEvent.class, listener,
 *         EventSubscriptionOptions.builder().maxInFlight(16).overflowStrategy(OverflowStrategy.DROP_OLDEST).build());
 * </pre>
 *
 * @since 8.0
 */
public final class EventSubscriptionOptions {

    private static final EventSubscriptionOptions DEFAULTS = new EventSubscriptionOptions(null, null);

    private final Integer maxInFlight;

    private final OverflowStrategy overflowStrategy;

    private EventSubscriptionOptions(Integer maxInFlight, OverflowStrategy overflowStrategy) {
        this.maxInFlight = maxInFlight;
        this.overflowStrategy = overflowStrategy;
    }

    /**
     * Returns options that set nothing, so every setting falls back to the default of the {@link EventBus}.
     *
     * @return the default {@link EventSubscriptionOptions}.
     */
    public static EventSubscriptionOptions defaults() {
        return DEFAULTS;
    }

    /**
     * Returns a new {@link Builder} for {@link EventSubscriptionOptions}.
     *
     * @return a new {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the number of events that may wait or be in delivery for this subscription before events are dropped.
     *
     * @return the limit, or empty to use the default of the {@link EventBus}.
     */
    public OptionalInt maxInFlight() {
        return maxInFlight == null ? OptionalInt.empty() : OptionalInt.of(maxInFlight);
    }

    /**
     * Returns which event to drop once {@link #maxInFlight()} is reached.
     *
     * @return the strategy, or empty to use the default of the {@link EventBus} ({@link OverflowStrategy#DROP_NEWEST} for
     *         {@link DefaultEventBus}).
     */
    public Optional<OverflowStrategy> overflowStrategy() {
        return Optional.ofNullable(overflowStrategy);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [maxInFlight=" + maxInFlight + ", overflowStrategy=" + overflowStrategy + "]";
    }

    /**
     * Builder for {@link EventSubscriptionOptions}.
     */
    public static final class Builder {

        private Integer maxInFlight;

        private OverflowStrategy overflowStrategy;

        private Builder() {
        }

        /**
         * Sets the number of events that may wait or be in delivery for this subscription before events are dropped.
         *
         * @param maxInFlight the limit, must be greater than {@code 0}.
         * @return {@code this} {@link Builder}.
         */
        public Builder maxInFlight(int maxInFlight) {

            LettuceAssert.isTrue(maxInFlight > 0, "maxInFlight must be greater than 0");

            this.maxInFlight = maxInFlight;
            return this;
        }

        /**
         * Sets which event to drop once the limit is reached.
         *
         * @param overflowStrategy the strategy, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         */
        public Builder overflowStrategy(OverflowStrategy overflowStrategy) {

            LettuceAssert.notNull(overflowStrategy, "OverflowStrategy must not be null");

            this.overflowStrategy = overflowStrategy;
            return this;
        }

        /**
         * Creates the {@link EventSubscriptionOptions}.
         *
         * @return the new {@link EventSubscriptionOptions}.
         */
        public EventSubscriptionOptions build() {
            return new EventSubscriptionOptions(maxInFlight, overflowStrategy);
        }

    }

}
