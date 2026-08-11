/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.metrics.CommandLatencyCollector;
import io.lettuce.core.output.BooleanOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.CommandWrapper;
import io.lettuce.core.protocol.Endpoint;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;

import io.netty.channel.embedded.EmbeddedChannel;

/**
 * Unit tests for {@link HashImportOutboundHandler} / {@link HashImportContext} driven through an {@link EmbeddedChannel}: lazy
 * per-fieldset injection of {@code HIMPORT PREPARE} ahead of the {@code SET}, single-injection, re-preparation after a failed
 * {@code PREPARE}, and transaction-safe cleanup — a {@code HIMPORT DISCARD} from {@link HashImport#close()} rides out on the
 * next write, held back while a {@code MULTI} is open and flushed once the transaction ends.
 *
 * @author Aleksandar Todorov
 */
@Tag(UNIT_TEST)
class HashImportOutboundHandlerUnitTests {

    private final RedisCommandBuilder<String, String> builder = new RedisCommandBuilder<>(StringCodec.UTF8);

    private EmbeddedChannel channel() {
        return new EmbeddedChannel(new HashImportOutboundHandler());
    }

    private HashImportSetCommand<String, String> set(HashImport<String> fieldset) {
        return builder.himportSet("key", fieldset, "v1", "v2", "v3");
    }

    private static Command<String, String, String> command(CommandType type) {
        return new Command<>(type, new StatusOutput<>(StringCodec.UTF8));
    }

    @Test
    void injectsPrepareAheadOfFirstSet() {

        EmbeddedChannel channel = channel();
        HashImport<String> fieldset = HashImport.of("f1", "f2", "f3");
        HashImportSetCommand<String, String> set = set(fieldset);

        channel.writeOutbound(set);

        List<Object> out = drain(channel);
        assertThat(out).hasSize(2);
        assertThat(isPrepare(out.get(0))).isTrue();
        assertThat(out.get(1)).isSameAs(set);
    }

    @Test
    void injectsPrepareOnlyOncePerFieldset() {

        EmbeddedChannel channel = channel();
        HashImport<String> fieldset = HashImport.of("f1", "f2", "f3");

        channel.writeOutbound(set(fieldset));
        channel.writeOutbound(set(fieldset));

        assertThat(drain(channel)).filteredOn(HashImportOutboundHandlerUnitTests::isPrepare).hasSize(1);
    }

    @Test
    void repreparesAfterFailedPrepare() {

        EmbeddedChannel channel = channel();
        HashImport<String> fieldset = HashImport.of("f1", "f2", "f3");

        channel.writeOutbound(set(fieldset));
        RedisCommand<?, ?, ?> prepare = (RedisCommand<?, ?, ?>) drain(channel).get(0);

        // server rejects the PREPARE (e.g. ACL/OOM): the fieldset must be evicted so the next SET re-prepares
        prepare.completeExceptionally(new RedisException("ERR PREPARE rejected"));

        channel.writeOutbound(set(fieldset));

        assertThat(drain(channel)).filteredOn(HashImportOutboundHandlerUnitTests::isPrepare)
                .as("fieldset should be re-prepared after a failed PREPARE").hasSize(1);
    }

    @Test
    void rejectsSetForFieldsetClosedAfterPrepare() {

        EmbeddedChannel channel = channel();
        HashImport<String> fieldset = HashImport.of("f1", "f2", "f3");

        // first SET prepares the fieldset on this channel
        channel.writeOutbound(set(fieldset));
        drain(channel);

        fieldset.close();

        // a SET racing close() takes the prepared fast path; it must be rejected client-side with the documented
        // IllegalStateException instead of being sent against a fieldset that is being discarded
        AsyncCommand<String, String, String> late = new AsyncCommand<>(set(fieldset));
        channel.writeOutbound(late);

        assertThat(late.isCompletedExceptionally()).isTrue();
        assertThatThrownBy(late::join).hasCauseInstanceOf(IllegalStateException.class).hasMessageContaining("discarded");
    }

    @Test
    void flushesDiscardOnNextWriteOutsideTransaction() {

        EmbeddedChannel channel = channel();
        HashImport<String> fieldset = HashImport.of("f1", "f2", "f3");

        channel.writeOutbound(set(fieldset));
        drain(channel);

        fieldset.close();
        assertThat(drain(channel)).as("close() alone does not write; the DISCARD waits for the next channel write")
                .noneMatch(HashImportOutboundHandlerUnitTests::isDiscard);

        // no transaction open: the DISCARD rides out on the next write to the channel
        channel.writeOutbound(command(CommandType.PING));
        assertThat(drain(channel)).anyMatch(HashImportOutboundHandlerUnitTests::isDiscard);
    }

    @Test
    void defersDiscardUntilTransactionEnds() {

        EmbeddedChannel channel = channel();
        HashImport<String> fieldset = HashImport.of("f1", "f2", "f3");

        channel.writeOutbound(set(fieldset));
        drain(channel);

        // a MULTI opens on this channel; a concurrent close() must not fold the cleanup DISCARD into the transaction
        channel.writeOutbound(command(CommandType.MULTI));
        drain(channel);

        fieldset.close();
        assertThat(drain(channel)).as("DISCARD must be held back while the transaction is open")
                .noneMatch(HashImportOutboundHandlerUnitTests::isDiscard);

        Command<String, String, String> exec = command(CommandType.EXEC);
        channel.writeOutbound(exec);

        List<Object> out = drain(channel);
        assertThat(out).hasSize(2);
        assertThat(out.get(0)).as("EXEC goes out first").isSameAs(exec);
        assertThat(isDiscard(out.get(1))).as("DISCARD flushed right after the transaction ends").isTrue();
    }

    @Test
    void defersDiscardForTransactionDeliveredAsBatch() {

        EmbeddedChannel channel = channel();
        HashImport<String> fieldset = HashImport.of("f1", "f2", "f3");

        channel.writeOutbound(set(fieldset));
        drain(channel);

        // MULTI arrives inside a batched write (auto-flush off); transaction tracking must observe it through the Collection
        channel.writeOutbound(new ArrayList<>(Arrays.asList(command(CommandType.MULTI), command(CommandType.PING))));
        drain(channel);

        fieldset.close();
        assertThat(drain(channel)).as("MULTI delivered via a batch must still defer the DISCARD")
                .noneMatch(HashImportOutboundHandlerUnitTests::isDiscard);

        // EXEC also arrives inside a batch; observing it ends the transaction and flushes the deferred cleanup
        channel.writeOutbound(new ArrayList<>(Arrays.asList(command(CommandType.PING), command(CommandType.EXEC))));
        assertThat(drain(channel)).anyMatch(HashImportOutboundHandlerUnitTests::isDiscard);
    }

    @Test
    void nestedMultiClosesOnSingleExec() {

        EmbeddedChannel channel = channel();
        HashImport<String> fieldset = HashImport.of("f1", "f2", "f3");

        channel.writeOutbound(set(fieldset));
        drain(channel);

        // Redis rejects a nested MULTI but keeps the one transaction open; tracking must stay open (idempotent), not counted —
        // a counter would demand a second EXEC and defer the DISCARD forever.
        channel.writeOutbound(command(CommandType.MULTI));
        channel.writeOutbound(command(CommandType.MULTI));
        drain(channel);

        fieldset.close();
        assertThat(drain(channel)).noneMatch(HashImportOutboundHandlerUnitTests::isDiscard);

        // a single EXEC ends the (single) transaction and flushes the deferred cleanup
        channel.writeOutbound(command(CommandType.EXEC));
        assertThat(drain(channel)).anyMatch(HashImportOutboundHandlerUnitTests::isDiscard);
    }

    @Test
    void flushesDeferredDiscardWhenTransactionAborted() {

        EmbeddedChannel channel = channel();
        HashImport<String> fieldset = HashImport.of("f1", "f2", "f3");

        channel.writeOutbound(set(fieldset));
        drain(channel);
        channel.writeOutbound(command(CommandType.MULTI));
        drain(channel);

        fieldset.close();
        assertThat(drain(channel)).noneMatch(HashImportOutboundHandlerUnitTests::isDiscard);

        // aborting the transaction (transaction-level DISCARD) also ends it and flushes the deferred cleanup
        channel.writeOutbound(command(CommandType.DISCARD));

        assertThat(drain(channel)).anyMatch(HashImportOutboundHandlerUnitTests::isDiscard);
    }

    @Test
    void dropsDeferredDiscardOnChannelInactive() {

        EmbeddedChannel channel = channel();
        HashImport<String> fieldset = HashImport.of("f1", "f2", "f3");

        channel.writeOutbound(set(fieldset));
        drain(channel);
        channel.writeOutbound(command(CommandType.MULTI));
        drain(channel);

        fieldset.close();

        // the connection drops mid-transaction: the deferred cleanup is dropped with it, nothing is written
        channel.pipeline().fireChannelInactive();

        assertThat(drain(channel)).noneMatch(HashImportOutboundHandlerUnitTests::isDiscard);
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
        EmbeddedChannel channel = new EmbeddedChannel(commandHandler, new HashImportOutboundHandler());

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
        assertThat(isPrepare(stack.get(0))).isTrue();
        assertThat(stack.get(1)).isSameAs(ping1);
        assertThat(stack.get(2)).isSameAs(himportSet);
        assertThat(stack.get(3)).isSameAs(ping2);
    }

    // Injected HIMPORT PREPARE: a HIMPORT command that is not a HashImportSetCommand and carries a StatusOutput.
    private static boolean isPrepare(Object msg) {
        if (!(msg instanceof RedisCommand)) {
            return false;
        }
        RedisCommand<?, ?, ?> command = (RedisCommand<?, ?, ?>) msg;
        return command.getType() == CommandType.HIMPORT && CommandWrapper.unwrap(command, HashImportSetCommand.class) == null
                && command.getOutput() instanceof StatusOutput;
    }

    // Injected HIMPORT DISCARD: a HIMPORT command carrying a BooleanOutput (unique to himportDiscard).
    private static boolean isDiscard(Object msg) {
        if (!(msg instanceof RedisCommand)) {
            return false;
        }
        RedisCommand<?, ?, ?> command = (RedisCommand<?, ?, ?>) msg;
        return command.getType() == CommandType.HIMPORT && command.getOutput() instanceof BooleanOutput;
    }

    // Drains outbound, flattening batched writes (deferred DISCARDs are flushed as one Collection) into individual commands.
    private static List<Object> drain(EmbeddedChannel channel) {

        List<Object> out = new ArrayList<>();
        Object msg;
        while ((msg = channel.readOutbound()) != null) {
            if (msg instanceof Collection) {
                out.addAll((Collection<?>) msg);
            } else {
                out.add(msg);
            }
        }
        return out;
    }

}
