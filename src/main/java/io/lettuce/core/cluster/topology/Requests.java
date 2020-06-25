/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.cluster.topology;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.RedisURI;

/**
 * Encapsulates asynchronously executed commands to multiple {@link RedisURI nodes}.
 *
 * @author Mark Paluch
 */
class Requests {

    private final Map<RedisURI, TimedAsyncCommand<String, String, String>> rawViews;

    protected Requests() {
        rawViews = new TreeMap<>(TopologyComparators.RedisURIComparator.INSTANCE);
    }

    private Requests(Map<RedisURI, TimedAsyncCommand<String, String, String>> rawViews) {
        this.rawViews = rawViews;
    }

    protected void addRequest(RedisURI redisURI, TimedAsyncCommand<String, String, String> command) {
        rawViews.put(redisURI, command);
    }

    /**
     * Returns a marker future that completes when all of the futures in this {@link Requests} complete. The marker never fails
     * exceptionally but signals completion only.
     *
     * @return
     */
    public CompletableFuture<Void> allCompleted() {
        return CompletableFuture.allOf(rawViews.values().stream().map(it -> it.exceptionally(throwable -> "ignore"))
                .toArray(CompletableFuture[]::new));
    }

    protected Set<RedisURI> nodes() {
        return rawViews.keySet();
    }

    protected TimedAsyncCommand<String, String, String> getRequest(RedisURI redisURI) {
        return rawViews.get(redisURI);
    }

    protected Requests mergeWith(Requests requests) {

        Map<RedisURI, TimedAsyncCommand<String, String, String>> result = new TreeMap<>(
                TopologyComparators.RedisURIComparator.INSTANCE);
        result.putAll(this.rawViews);
        result.putAll(requests.rawViews);

        return new Requests(result);
    }

}
