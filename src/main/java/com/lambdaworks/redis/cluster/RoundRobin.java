package com.lambdaworks.redis.cluster;

import java.util.Collection;

/**
 * Circular element provider. This class allows infinite scrolling over a collection with the possibility to provide an initial
 * offset.
 * 
 * @author Mark Paluch
 */
class RoundRobin<V> {

    protected final Collection<? extends V> collection;
    protected V offset;

    public RoundRobin(Collection<? extends V> collection) {
        this(collection, null);
    }

    public RoundRobin(Collection<? extends V> collection, V offset) {
        this.collection = collection;
        this.offset = offset;
    }

    /**
     * Returns the next item.
     * 
     * @return the next item
     */
    public V next() {
        if (offset != null) {
            boolean accept = false;
            for (V element : collection) {
                if (element == offset) {
                    accept = true;
                    continue;
                }

                if (accept) {
                    return offset = element;
                }
            }
        }

        return offset = collection.iterator().next();
    }
}