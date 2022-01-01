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

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * Flight recorder event variant of {@link MasterReplicaTopologyChangedEvent}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
@Category({ "Lettuce", "Master/Replica Events" })
@Label("Topology Changed")
@StackTrace(false)
class JfrMasterReplicaTopologyChangedEvent extends Event {

    private final String source;

    private final String topology;

    public JfrMasterReplicaTopologyChangedEvent(MasterReplicaTopologyChangedEvent event) {

        this.source = event.getSource().toString();
        StringBuilder builder = new StringBuilder();

        for (RedisNodeDescription node : event.getNodes()) {
            builder.append(String.format("%s %s\n", node.getUri(), node.getRole()));
        }

        this.topology = builder.toString();
    }

}
