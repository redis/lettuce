package io.lettuce.core;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Close Events Facility. Can register/unregister CloseListener and fire a closed event to all registered listeners.
 *
 * @author Mark Paluch
 * @since 3.0
 */
class CloseEvents {

    private final Set<CloseListener> listeners = ConcurrentHashMap.newKeySet();

    public void fireEventClosed(Object resource) {
        for (CloseListener listener : listeners) {
            listener.resourceClosed(resource);
        }
    }

    public void addListener(CloseListener listener) {
        listeners.add(listener);
    }

    interface CloseListener {

        void resourceClosed(Object resource);

    }

}
