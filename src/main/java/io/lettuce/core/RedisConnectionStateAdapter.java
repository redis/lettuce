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
package io.lettuce.core;

import java.net.SocketAddress;

/**
 * Convenience adapter with an empty implementation of all {@link RedisConnectionStateListener} callback methods.
 *
 * @author Mark Paluch
 * @since 4.4
 */
public class RedisConnectionStateAdapter implements RedisConnectionStateListener {

    @Override
    public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress) {
        // empty adapter method
    }

    @Override
    public void onRedisDisconnected(RedisChannelHandler<?, ?> connection) {
        // empty adapter method
    }

    @Override
    public void onRedisExceptionCaught(RedisChannelHandler<?, ?> connection, Throwable cause) {
        // empty adapter method
    }

}
