package io.lettuce.core.event;

/**
 * What an {@link EventBus} drops for a subscriber that cannot keep up, once the subscriber's limit of events waiting or being
 * delivered is reached.
 *
 * @since 8.0
 * @see EventSubscriptionOptions
 */
public enum OverflowStrategy {

    /**
     * Drop the event that is being published. The subscriber keeps the events it already has.
     */
    DROP_NEWEST,

    /**
     * Drop the oldest event that is waiting for delivery, and keep the event that is being published.
     */
    DROP_OLDEST

}
