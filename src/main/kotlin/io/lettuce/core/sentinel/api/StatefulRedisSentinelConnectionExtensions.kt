/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.sentinel.api

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.sentinel.api.coroutines.RedisSentinelSuspendableCommands
import io.lettuce.core.sentinel.api.coroutines.RedisSentinelSuspendableCommandsImpl

/**
 * Extension for [StatefulRedisSentinelConnection] to create [RedisSentinelSuspendableCommands]
 *
 * @author Mikhael Sokolov
 * @author Mark Paluch
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
fun <K, V> StatefulRedisSentinelConnection<K, V>.suspendable(): RedisSentinelSuspendableCommands<K, V> {
    return RedisSentinelSuspendableCommandsImpl(this.reactive())
}
