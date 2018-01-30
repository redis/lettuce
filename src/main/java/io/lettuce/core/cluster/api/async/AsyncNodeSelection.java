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
package io.lettuce.core.cluster.api.async;

import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.api.NodeSelectionSupport;

/**
 * Node selection with access to asynchronous executed commands on the set.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public interface AsyncNodeSelection<K, V>
        extends NodeSelectionSupport<RedisAsyncCommands<K, V>, NodeSelectionAsyncCommands<K, V>> {

}
