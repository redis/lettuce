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

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.RedisURI;

/**
 * Encapsulates asynchronously executed commands to multiple {@link RedisURI nodes}.
 *
 * @author Mark Paluch
 */
class Requests {

    private final Map<RedisURI, TimedAsyncCommand<String, String, String>> rawViews = new TreeMap<>(
            MasterSlaveUtils.RedisURIComparator.INSTANCE);

    public Requests() {
    }

    protected void addRequest(RedisURI redisURI, TimedAsyncCommand<String, String, String> command) {
        rawViews.put(redisURI, command);
    }

    protected long await(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return RefreshFutures.awaitAll(timeout, timeUnit, rawViews.values());
    }

    protected Set<RedisURI> nodes() {
        return rawViews.keySet();
    }

    protected TimedAsyncCommand<String, String, String> getRequest(RedisURI redisURI) {
        return rawViews.get(redisURI);
    }
}
