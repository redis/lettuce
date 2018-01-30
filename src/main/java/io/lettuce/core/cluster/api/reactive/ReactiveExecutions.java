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
package io.lettuce.core.cluster.api.reactive;

import java.util.Collection;

import reactor.core.publisher.Flux;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * Execution holder for a reactive command to be executed on multiple nodes.
 *
 * @author Mark Paluch
 * @since 4.4
 */
public interface ReactiveExecutions<T> {

    /**
     * Return a {@link Flux} that contains a combined stream of the multi-node execution.
     *
     * @return
     */
    Flux<T> flux();

    /**
     * @return collection of nodes on which the command was executed.
     */
    Collection<RedisClusterNode> nodes();
}
