/*
 * Copyright 2019-2020 the original author or authors.
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
package io.lettuce.core.protocol;

import java.time.Duration;

import io.lettuce.core.resource.ClientResources;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;

/**
 * Handler to initialize a secure Redis Connection using a {@link ConnectionInitializer}. Delays channel activation to after the
 * SSL handshake.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public class SslRedisHandshakeHandler extends RedisHandshakeHandler {

    public SslRedisHandshakeHandler(ConnectionInitializer connectionInitializer, ClientResources clientResources,
            Duration initializeTimeout) {
        super(connectionInitializer, clientResources, initializeTimeout);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof SslHandshakeCompletionEvent) {

            SslHandshakeCompletionEvent event = (SslHandshakeCompletionEvent) evt;
            if (event.isSuccess()) {
                super.channelActive(ctx);
            } else {
                fail(ctx, event.cause());
            }
        }

        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // do not propagate channel active when using SSL.
    }
}
