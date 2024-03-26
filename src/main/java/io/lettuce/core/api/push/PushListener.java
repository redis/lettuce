package io.lettuce.core.api.push;

/**
 * Interface to be implemented by push message listeners that are interested in listening to {@link PushMessage}. Requires Redis
 * 6+ using RESP3.
 *
 * @author Mark Paluch
 * @since 6.0
 * @see PushMessage
 */
@FunctionalInterface
public interface PushListener {

    /**
     * Handle a push message.
     *
     * @param message message to respond to.
     */
    void onPushMessage(PushMessage message);

}
