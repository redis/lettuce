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
package io.lettuce.core.cluster;

import java.util.Collection;

import io.lettuce.core.cluster.api.push.RedisClusterPushListener;

/**
 * A handler object that provides access to {@link RedisClusterPushListener}.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public interface ClusterPushHandler {

    /**
     * Add a new {@link RedisClusterPushListener listener}.
     *
     * @param listener the listener, must not be {@code null}.
     */
    void addListener(RedisClusterPushListener listener);

    /**
     * Remove an existing {@link RedisClusterPushListener listener}.
     *
     * @param listener the listener, must not be {@code null}.
     */
    void removeListener(RedisClusterPushListener listener);

    /**
     * Returns a collection of {@link RedisClusterPushListener}.
     *
     * @return the collection of listeners.
     */
    Collection<RedisClusterPushListener> getPushListeners();

}
