/*
 * Copyright 2016-2018 the original author or authors.
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
package io.lettuce.core.masterslave;

import java.util.Comparator;
import java.util.Map;

import io.lettuce.core.RedisURI;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * Comparators for {@link RedisNodeDescription} and {@link RedisURI}.
 *
 * @author Mark Paluch
 */
class TopologyComparators {

    /**
     * Compare {@link RedisNodeDescription} based on their latency. Lowest comes first.
     */
    static class LatencyComparator implements Comparator<RedisNodeDescription> {

        private final Map<RedisNodeDescription, Long> latencies;

        public LatencyComparator(Map<RedisNodeDescription, Long> latencies) {
            this.latencies = latencies;
        }

        @Override
        public int compare(RedisNodeDescription o1, RedisNodeDescription o2) {

            Long latency1 = latencies.get(o1);
            Long latency2 = latencies.get(o2);

            if (latency1 != null && latency2 != null) {
                return latency1.compareTo(latency2);
            }

            if (latency1 != null) {
                return -1;
            }

            if (latency2 != null) {
                return 1;
            }

            return 0;
        }
    }

    /**
     * Compare {@link RedisURI} based on their host and port representation.
     */
    enum RedisURIComparator implements Comparator<RedisURI> {

        INSTANCE;

        @Override
        public int compare(RedisURI o1, RedisURI o2) {
            String h1 = "";
            String h2 = "";

            if (o1 != null) {
                h1 = o1.getHost() + ":" + o1.getPort();
            }

            if (o2 != null) {
                h2 = o2.getHost() + ":" + o2.getPort();
            }

            return h1.compareToIgnoreCase(h2);
        }
    }
}
