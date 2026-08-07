/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.metrics.CommandLatencyCollector;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.Endpoint;
import io.lettuce.core.protocol.HashImportPrepareCommand;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;

import io.netty.channel.embedded.EmbeddedChannel;

/**
 * Unit tests for {@link HashImportOutboundHandler} driving it through an {@link EmbeddedChannel}: lazy per-fieldset injection
 * of {@code HIMPORT PREPARE} ahead of the {@code SET}, single-injection, and re-preparation after a failed {@code PREPARE}.
 *
 * @author Aleksandar Todorov
 */
@Tag(UNIT_TEST)
class HashImportOutboundHandlerUnitTests {

    @SuppressWarnings("unchecked")
    private final StatefulRedisConnectionImpl<String, String> connection = mock(StatefulRedisConnectionImpl.class);

    private final RedisCommandBuilder<String, String> builder = new RedisCommandBuilder<>(StringCodec.UTF8);

    private EmbeddedChannel channel() {
        return new EmbeddedChannel(new HashImportOutboundHandler(connection));
    }

    private HashImportSetCommand<String, String> set(HashImport<String> fieldset) {
        return builder.himportSet("key", fieldset, "v1", "v2", "v3");
    }

    @Test
    void injectsPrepareAheadOfFirstSet() {

        EmbeddedChannel channel = channel();
        HashImport<String> fieldset = HashImport.of("f1", "f2", "f3");
        HashImportSetCommand<String, String> set = set(fieldset);

        channel.writeOutbound(set);

        List<Object> out = drain(channel);
        assertThat(out).hasSize(2);
        assertThat(out.get(0)).isInstanceOf(HashImportPrepareCommand.class);
        assertThat(out.get(1)).isSameAs(set);
    }

    @Test
    void injectsPrepareOnlyOncePerFieldset() {

        EmbeddedChannel channel = channel();
        HashImport<String> fieldset = HashImport.of("f1", "f2", "f3");

        channel.writeOutbound(set(fieldset));
        channel.writeOutbound(set(fieldset));

        assertThat(drain(channel)).filteredOn(HashImportPrepareCommand.class::isInstance).hasSize(1);
    }

    @Test
    void repreparesAfterFailedPrepare() {

        EmbeddedChannel channel = channel();
        HashImport<String> fieldset = HashImport.of("f1", "f2", "f3");

        channel.writeOutbound(set(fieldset));
        HashImportPrepareCommand<?, ?, ?> prepare = (HashImportPrepareCommand<?, ?, ?>) drain(channel).get(0);

        // server rejects the PREPARE (e.g. ACL/OOM): the fieldset must be evicted so the next SET re-prepares
        prepare.completeExceptionally(new RedisException("ERR PREPARE rejected"));

        channel.writeOutbound(set(fieldset));

        assertThat(drain(channel)).filteredOn(HashImportPrepareCommand.class::isInstance)
                .as("fieldset should be re-prepared after a failed PREPARE").hasSize(1);
    }

    @Test
    void batchStacksPrepareInWireOrder() {

        ClientResources clientResources = mock(ClientResources.class);
        CommandLatencyCollector latency = mock(CommandLatencyCollector.class);
        when(latency.isEnabled()).thenReturn(false);
        when(clientResources.commandLatencyRecorder()).thenReturn(latency);
        when(clientResources.tracing()).thenReturn(Tracing.disabled());

        CommandHandler commandHandler = new CommandHandler(ClientOptions.create(), clientResources, mock(Endpoint.class));
        // production order: CommandHandler head-side, HashImportOutboundHandler tail-side (matches ConnectionBuilder addLast)
        EmbeddedChannel channel = new EmbeddedChannel(commandHandler, new HashImportOutboundHandler(connection));

        HashImport<String> fieldset = HashImport.of("f1", "f2", "f3");
        Command<String, String, String> ping1 = new Command<>(CommandType.PING, new StatusOutput<>(StringCodec.UTF8));
        HashImportSetCommand<String, String> himportSet = set(fieldset);
        Command<String, String, String> ping2 = new Command<>(CommandType.PING, new StatusOutput<>(StringCodec.UTF8));

        channel.writeOutbound(new ArrayList<RedisCommand<?, ?, ?>>(Arrays.asList(ping1, himportSet, ping2)));

        // The reply stack must carry PREPARE ahead of the SET, in the same order the bytes hit the wire; otherwise Redis
        // replies would be matched to the wrong commands. The injected PREPARE is stacked before the batch because the handler
        // (tail-side) injects via ctx.write, reaching CommandHandler before it forwards super.write(batch).
        List<RedisCommand<?, ?, ?>> stack = new ArrayList<>(commandHandler.getStack());
        assertThat(stack).hasSize(4);
        assertThat(stack.get(0)).isInstanceOf(HashImportPrepareCommand.class);
        assertThat(stack.get(1)).isSameAs(ping1);
        assertThat(stack.get(2)).isSameAs(himportSet);
        assertThat(stack.get(3)).isSameAs(ping2);
    }

    private static List<Object> drain(EmbeddedChannel channel) {

        List<Object> out = new ArrayList<>();
        Object msg;
        while ((msg = channel.readOutbound()) != null) {
            out.add(msg);
        }
        return out;
    }

}
