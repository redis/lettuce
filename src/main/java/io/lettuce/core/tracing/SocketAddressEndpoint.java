/*
 * Copyright 2022-2024 the original author or authors.
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
package io.lettuce.core.tracing;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * {@link Tracing.Endpoint} based on a {@link SocketAddress}.
 *
 * @author Mark Paluch
 * @since 6.3
 */
class SocketAddressEndpoint implements Tracing.Endpoint {

    private final SocketAddress socketAddress;

    public SocketAddressEndpoint(SocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    @Override
    public String toString() {

        if (socketAddress instanceof InetSocketAddress) {
            InetSocketAddress inet = (InetSocketAddress) socketAddress;
            return inet.getHostString() + ":" + inet.getPort();
        }

        return socketAddress.toString();
    }

}
