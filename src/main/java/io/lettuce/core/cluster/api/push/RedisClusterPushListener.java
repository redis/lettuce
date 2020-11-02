/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.cluster.api.push;

import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * Interface to be implemented by push message listeners that are interested in listening to {@link PushMessage} using Redis
 * Cluster.
 *
 * @author Mark Paluch
 * @since 6.0
 * @see PushMessage
 * @see io.lettuce.core.api.push.PushListener
 */
@FunctionalInterface
public interface RedisClusterPushListener {

    /**
     * Handle a push message.
     *
     * @param node the {@link RedisClusterNode} from which the {@code message} originates.
     * @param message message to respond to.
     */
    void onPushMessage(RedisClusterNode node, PushMessage message);

}
