package io.lettuce.core.protocol;

import java.util.Collection;

import io.lettuce.core.api.push.PushListener;

/**
 * A handler object that provides access to {@link PushListener}.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public interface PushHandler {

    /**
     * Add a new {@link PushListener listener}.
     *
     * @param listener the listener, must not be {@code null}.
     */
    void addListener(PushListener listener);

    /**
     * Remove an existing {@link PushListener listener}.
     *
     * @param listener the listener, must not be {@code null}.
     */
    void removeListener(PushListener listener);

    /**
     * Returns a collection of {@link PushListener}.
     *
     * @return the collection of listeners.
     */
    Collection<PushListener> getPushListeners();

}
