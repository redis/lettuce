/*
 * Copyright 2017-2018 the original author or authors.
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
package io.lettuce.core.output;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import io.lettuce.core.codec.RedisCodec;

/**
 * Output capturing a hostname and port (both string elements) into a {@link SocketAddress}.
 *
 * @author Mark Paluch
 * @since 5.0.1
 */
public class SocketAddressOutput<K, V> extends CommandOutput<K, V, SocketAddress> {

    private String hostname;

    public SocketAddressOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (hostname == null) {
            hostname = decodeAscii(bytes);
            return;
        }

        int port = Integer.parseInt(decodeAscii(bytes));
        output = InetSocketAddress.createUnresolved(hostname, port);
    }
}
