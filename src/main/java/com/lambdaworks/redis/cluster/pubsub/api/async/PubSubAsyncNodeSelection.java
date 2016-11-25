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
package com.lambdaworks.redis.cluster.pubsub.api.async;

import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.api.NodeSelectionSupport;
import com.lambdaworks.redis.cluster.api.async.NodeSelectionAsyncCommands;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;

/**
 * Node selection with access to asynchronous executed commands on the set. This API is subject to incompatible changes in a
 * future release. The API is exempt from any compatibility guarantees made by lettuce. The current state implies nothing about
 * the quality or performance of the API in question, only the fact that it is not "API-frozen."
 *
 * The NodeSelection command API and its result types are a base for discussions.
 * 
 * @author Mark Paluch
 * @since 4.4
 */
public interface PubSubAsyncNodeSelection<K, V> extends
        NodeSelectionSupport<RedisPubSubAsyncCommands<K, V>, NodeSelectionPubSubAsyncCommands<K, V>> {

}
