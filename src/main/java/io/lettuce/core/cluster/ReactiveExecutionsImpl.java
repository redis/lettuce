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
package io.lettuce.core.cluster;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.lettuce.core.cluster.api.reactive.ReactiveExecutions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * Default implementation of {@link ReactiveExecutions}.
 *
 * @author Mark Paluch
 * @since 4.4
 */
class ReactiveExecutionsImpl<T> implements ReactiveExecutions<T> {

    private Map<RedisClusterNode, CompletionStage<? extends Publisher<? extends T>>> executions;

    public ReactiveExecutionsImpl(Map<RedisClusterNode, CompletionStage<? extends Publisher<? extends T>>> executions) {
        this.executions = executions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Flux<T> flux() {
        return Flux.fromIterable(executions.values()).flatMap(Mono::fromCompletionStage).flatMap(f -> f);
    }

    @Override
    public Collection<RedisClusterNode> nodes() {
        return executions.keySet();
    }

}
