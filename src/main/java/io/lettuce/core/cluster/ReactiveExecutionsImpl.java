/*
 * Copyright 2016 the original author or authors.
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
package io.lettuce.core.cluster;

import java.util.Collection;
import java.util.Map;

import io.lettuce.core.cluster.api.reactive.ReactiveExecutions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import rx.Observable;

/**
 * Default implementation of {@link ReactiveExecutions}.
 *
 * @author Mark Paluch
 * @since 4.4
 */
class ReactiveExecutionsImpl<T> implements ReactiveExecutions<T> {

    private Map<RedisClusterNode, ? extends Publisher<T>> executions;

    public ReactiveExecutionsImpl(Map<RedisClusterNode, ? extends Publisher<T>> executions) {
        this.executions = executions;
    }

    @Override
    public Flux<T> flux() {
        return Flux.merge(executions.values());
    }

    @Override
    public Collection<RedisClusterNode> nodes() {
        return executions.keySet();
    }
}
