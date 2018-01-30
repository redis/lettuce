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
package io.lettuce.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.internal.LettuceLists;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * Collection of common read setting implementations.
 *
 * @author Mark Paluch
 * @since 4.0
 */
class ReadFromImpl {

    /**
     * Read from master only.
     */
    static final class ReadFromMaster extends ReadFrom {

        @Override
        public List<RedisNodeDescription> select(Nodes nodes) {

            for (RedisNodeDescription node : nodes) {
                if (node.getRole() == RedisInstance.Role.MASTER) {
                    return LettuceLists.newList(node);
                }
            }

            return Collections.emptyList();
        }
    }

    /**
     * Read from master and slaves. Prefer master reads and fall back to slaves if the master is not available.
     */
    static final class ReadFromMasterPreferred extends ReadFrom {

        @Override
        public List<RedisNodeDescription> select(Nodes nodes) {

            List<RedisNodeDescription> result = new ArrayList<>(nodes.getNodes().size());

            for (RedisNodeDescription node : nodes) {
                if (node.getRole() == RedisInstance.Role.MASTER) {
                    result.add(node);
                }
            }

            for (RedisNodeDescription node : nodes) {
                if (node.getRole() == RedisInstance.Role.SLAVE) {
                    result.add(node);
                }
            }

            return result;
        }
    }

    /**
     * Read from slave only.
     */
    static final class ReadFromSlave extends ReadFrom {

        @Override
        public List<RedisNodeDescription> select(Nodes nodes) {

            List<RedisNodeDescription> result = new ArrayList<>(nodes.getNodes().size());

            for (RedisNodeDescription node : nodes) {
                if (node.getRole() == RedisInstance.Role.SLAVE) {
                    result.add(node);
                }
            }

            return result;
        }
    }

    /**
     * Read from master and slaves. Prefer slave reads and fall back to master if the no slave is not available.
     */
    static final class ReadFromSlavePreferred extends ReadFrom {

        @Override
        public List<RedisNodeDescription> select(Nodes nodes) {

            List<RedisNodeDescription> result = new ArrayList<>(nodes.getNodes().size());

            for (RedisNodeDescription node : nodes) {
                if (node.getRole() == RedisInstance.Role.SLAVE) {
                    result.add(node);
                }
            }

            for (RedisNodeDescription node : nodes) {
                if (node.getRole() == RedisInstance.Role.MASTER) {
                    result.add(node);
                }
            }

            return result;
        }
    }

    /**
     * Read from nearest node.
     */
    static final class ReadFromNearest extends ReadFrom {

        @Override
        public List<RedisNodeDescription> select(Nodes nodes) {
            return nodes.getNodes();
        }
    }
}
