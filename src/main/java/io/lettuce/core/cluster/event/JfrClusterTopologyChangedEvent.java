/*
 * Copyright 2011-2022 the original author or authors.
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
package io.lettuce.core.cluster.event;

import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.event.Event;

/**
 * Flight recorder event variant of {@link ClusterTopologyChangedEvent}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
@Category({ "Lettuce", "Cluster Events" })
@Label("Topology Changed")
@StackTrace(false)
class JfrClusterTopologyChangedEvent extends jdk.jfr.Event implements Event {

    private final String topology;

    /**
     * Creates a new {@link JfrClusterTopologyChangedEvent}.
     */
    public JfrClusterTopologyChangedEvent(ClusterTopologyChangedEvent event) {

        StringBuilder builder = new StringBuilder();

        for (RedisClusterNode redisClusterNode : event.after()) {
            builder.append(String.format("%s [%s] %s %s\n", redisClusterNode.getNodeId(), redisClusterNode.getUri(),
                    redisClusterNode.getFlags(), redisClusterNode.getRole()));
        }

        this.topology = builder.toString();
    }

}
