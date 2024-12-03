/*
 * Copyright 2019-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import io.lettuce.core.event.EventBus;
import io.lettuce.core.protocol.Endpoint;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

class RedisAuthenticationHandler extends BaseRedisAuthenticationHandler<StatefulRedisConnectionImpl<?, ?>> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RedisAuthenticationHandler.class);

    public RedisAuthenticationHandler(StatefulRedisConnectionImpl<?, ?> connection, EventBus eventBus) {
        super(connection, eventBus);
    }

    protected boolean isSupportedConnection() {
        if (connection instanceof StatefulRedisPubSubConnection
                && ProtocolVersion.RESP2 == connection.getConnectionState().getNegotiatedProtocolVersion()) {
            logger.warn("Renewable credentials are not supported with RESP2 protocol on a pub/sub connection.");
            return false;
        }
        return true;
    }

}
