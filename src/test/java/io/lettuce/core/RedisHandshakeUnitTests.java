/*
 * Copyright 2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.lettuce.core;

import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.ProtocolVersion;
import io.netty.channel.embedded.EmbeddedChannel;

/**
 * Unit tests for {@link RedisHandshake}.
 *
 * @author Mark Paluch
 */
class RedisHandshakeUnitTests {

    @Test
    void handshakeWithResp3ShouldPass() {

        EmbeddedChannel channel = new EmbeddedChannel(true, false);

        ConnectionState state = new ConnectionState();
        state.setCredentialsProvider(new StaticCredentialsProvider("foo", "bar".toCharArray()));
        RedisHandshake handshake = new RedisHandshake(ProtocolVersion.RESP3, false, state);
        handshake.initialize(channel);

        AsyncCommand<String, String, Map<String, String>> hello = channel.readOutbound();
        helloResponse(hello.getOutput());
        hello.complete();

        assertThat(state.getNegotiatedProtocolVersion()).isEqualTo(ProtocolVersion.RESP3);
    }

    @Test
    void handshakeWithDiscoveryShouldPass() {

        EmbeddedChannel channel = new EmbeddedChannel(true, false);

        ConnectionState state = new ConnectionState();
        state.setCredentialsProvider(new StaticCredentialsProvider("foo", "bar".toCharArray()));
        RedisHandshake handshake = new RedisHandshake(null, false, state);
        handshake.initialize(channel);

        AsyncCommand<String, String, Map<String, String>> hello = channel.readOutbound();
        helloResponse(hello.getOutput());
        hello.complete();

        assertThat(state.getNegotiatedProtocolVersion()).isEqualTo(ProtocolVersion.RESP3);
    }

    @Test
    void handshakeWithDiscoveryShouldDowngrade() {

        EmbeddedChannel channel = new EmbeddedChannel(true, false);

        ConnectionState state = new ConnectionState();
        state.setCredentialsProvider(new StaticCredentialsProvider(null, null));
        RedisHandshake handshake = new RedisHandshake(null, false, state);
        handshake.initialize(channel);

        AsyncCommand<String, String, Map<String, String>> hello = channel.readOutbound();
        hello.getOutput().setError("NOPROTO");
        hello.completeExceptionally(new RedisException("NOPROTO"));
        hello.complete();

        assertThat(state.getNegotiatedProtocolVersion()).isEqualTo(ProtocolVersion.RESP2);
    }

    private static void helloResponse(CommandOutput<String, String, Map<String, String>> output) {

        output.multiMap(8);
        output.set(ByteBuffer.wrap("id".getBytes()));
        output.set(1);

        output.set(ByteBuffer.wrap("mode".getBytes()));
        output.set(ByteBuffer.wrap("master".getBytes()));

        output.set(ByteBuffer.wrap("role".getBytes()));
        output.set(ByteBuffer.wrap("master".getBytes()));

        output.set(ByteBuffer.wrap("version".getBytes()));
        output.set(ByteBuffer.wrap("1.2.3".getBytes()));
    }

}
