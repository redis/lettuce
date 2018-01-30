/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster.event;

import java.util.Collections;
import java.util.List;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.event.Event;

/**
 * Signals a discovered cluster topology change. The event carries the view {@link #before()} and {@link #after} the change.
 *
 * @author Mark Paluch
 * @since 3.4
 */
public class ClusterTopologyChangedEvent implements Event {

    private final List<RedisClusterNode> before;
    private final List<RedisClusterNode> after;

    /**
     * Creates a new {@link ClusterTopologyChangedEvent}.
     *
     * @param before the cluster topology view before the topology changed, must not be {@literal null}
     * @param after the cluster topology view after the topology changed, must not be {@literal null}
     */
    public ClusterTopologyChangedEvent(List<RedisClusterNode> before, List<RedisClusterNode> after) {
        this.before = Collections.unmodifiableList(before);
        this.after = Collections.unmodifiableList(after);
    }

    /**
     * Returns the cluster topology view before the topology changed.
     *
     * @return the cluster topology view before the topology changed.
     */
    public List<RedisClusterNode> before() {
        return before;
    }

    /**
     * Returns the cluster topology view after the topology changed.
     *
     * @return the cluster topology view after the topology changed.
     */
    public List<RedisClusterNode> after() {
        return after;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [before=").append(before.size());
        sb.append(", after=").append(after.size());
        sb.append(']');
        return sb.toString();
    }
}
