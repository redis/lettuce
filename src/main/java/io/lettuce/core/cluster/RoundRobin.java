/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Circular element provider. This class allows infinite scrolling over a collection with the possibility to provide an initial
 * offset.
 *
 * @author Mark Paluch
 */
class RoundRobin<V> {

    protected volatile Collection<? extends V> collection = Collections.emptyList();

    protected volatile V offset;

    /**
     * Return whether this {@link RoundRobin} is still consistent and contains all items from the leader {@link Collection} and
     * vice versa.
     *
     * @param leader the leader collection containing source elements for this {@link RoundRobin}.
     * @return {@code true} if this {@link RoundRobin} is consistent with the leader {@link Collection}.
     */
    public boolean isConsistent(Collection<? extends V> leader) {

        Collection<? extends V> collection = this.collection;

        return collection.containsAll(leader) && leader.containsAll(collection);
    }

    /**
     * Rebuild the {@link RoundRobin} from the leader {@link Collection}.
     *
     * @param leader the leader collection containing source elements for this {@link RoundRobin}.
     */
    public void rebuild(Collection<? extends V> leader) {

        this.collection = new ArrayList<>(leader);
        this.offset = null;
    }

    /**
     * Returns the next item.
     *
     * @return the next item
     */
    public V next() {

        Collection<? extends V> collection = this.collection;
        V offset = this.offset;

        if (offset != null) {
            boolean accept = false;
            for (V element : collection) {
                if (element == offset) {
                    accept = true;
                    continue;
                }

                if (accept) {
                    return this.offset = element;
                }
            }
        }

        return this.offset = collection.iterator().next();
    }

}
