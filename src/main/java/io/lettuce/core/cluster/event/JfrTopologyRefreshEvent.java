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
package io.lettuce.core.cluster.event;

import java.util.StringJoiner;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;
import io.lettuce.core.RedisURI;

/**
 * Flight recorder event variant of {@link TopologyRefreshEvent}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
@Category({ "Lettuce", "Cluster Events" })
@Label("Topology Refresh")
@StackTrace(value = false)
class JfrTopologyRefreshEvent extends Event {

    private final String sources;

    public JfrTopologyRefreshEvent(TopologyRefreshEvent event) {

        StringJoiner joiner = new StringJoiner(", ");

        for (RedisURI redisURI : event.getTopologyRefreshSource()) {
            joiner.add(redisURI.toString());
        }

        this.sources = joiner.toString();
    }

}
