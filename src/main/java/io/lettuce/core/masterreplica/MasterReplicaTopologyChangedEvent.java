/*
 * Copyright 2021-2022 the original author or authors.
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
package io.lettuce.core.masterreplica;

import java.util.List;

import io.lettuce.core.RedisURI;
import io.lettuce.core.event.Event;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * Event triggered after obtaining the Master/Replica topology.
 *
 * @author Mark Paluch
 * @since 6.1
 */
class MasterReplicaTopologyChangedEvent implements Event {

    private final RedisURI source;

    private final List<RedisNodeDescription> nodes;

    public MasterReplicaTopologyChangedEvent(RedisURI source, List<RedisNodeDescription> nodes) {
        this.source = source;
        this.nodes = nodes;
    }

    public RedisURI getSource() {
        return source;
    }

    public List<RedisNodeDescription> getNodes() {
        return nodes;
    }

}
