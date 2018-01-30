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
package io.lettuce.core.masterslave;

import java.util.List;

import io.lettuce.core.RedisException;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * Topology provider for Master-Slave topology discovery during runtime. Implementors of this interface return an unordered list
 * of {@link RedisNodeDescription} instances.
 *
 * @author Mark Paluch
 * @since 4.1
 */
@FunctionalInterface
public interface TopologyProvider {

    /**
     * Lookup nodes within the topology.
     *
     * @return list of {@link RedisNodeDescription} instances
     * @throws RedisException on errors that occured during the lookup
     */
    List<RedisNodeDescription> getNodes();
}
