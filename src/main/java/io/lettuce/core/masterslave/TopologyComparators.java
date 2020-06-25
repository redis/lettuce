/*
 * Copyright 2016-2020 the original author or authors.
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
package io.lettuce.core.masterslave;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
     * Sort action for topology. Defaults to sort by latency. Can be set via {@code io.lettuce.core.topology.sort} system
     * property.
     *
     * @since 4.5
     */
    enum SortAction {

        /**
         * Sort by latency.
         */
        BY_LATENCY {

            @Override
            void sort(List<RedisNodeDescription> nodes, Comparator<? super RedisNodeDescription> latencyComparator) {
                nodes.sort(latencyComparator);
            }

        },

        /**
         * Do not sort.
         */
        NONE {

            @Override
            void sort(List<RedisNodeDescription> nodes, Comparator<? super RedisNodeDescription> latencyComparator) {

            }

        },

        /**
         * Randomize nodes.
         */
        RANDOMIZE {

            @Override
            void sort(List<RedisNodeDescription> nodes, Comparator<? super RedisNodeDescription> latencyComparator) {
                Collections.shuffle(nodes);
            }

        };

        abstract void sort(List<RedisNodeDescription> nodes, Comparator<? super RedisNodeDescription> latencyComparator);

        /**
         * @return determine {@link SortAction} and fall back to {@link SortAction#BY_LATENCY} if sort action cannot be
         *         resolved.
         */
        static SortAction getSortAction() {

            String sortAction = System.getProperty("io.lettuce.core.topology.sort", BY_LATENCY.name());

            for (SortAction action : values()) {
                if (sortAction.equalsIgnoreCase(action.name())) {
                    return action;
                }
            }

            return BY_LATENCY;
        }

    }

}
