package com.lambdaworks.redis;

import java.util.Set;

import io.netty.util.internal.ConcurrentSet;

/**
 * Close Events Facility. Can register/unregister CloseListener and fire a closed event to all registered listeners.
 * 
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 16.05.14 10:59
 */
public class CloseEvents {
    private Set<CloseListener> listeners = new ConcurrentSet<CloseListener>();

    protected void fireEventClosed(Object resource) {
        for (CloseListener listener : listeners) {
            listener.resourceClosed(resource);
        }
    }

    public void addListener(CloseListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CloseListener listener) {
        listeners.remove(listener);
    }

    public interface CloseListener {
        void resourceClosed(Object resource);
    }
}
