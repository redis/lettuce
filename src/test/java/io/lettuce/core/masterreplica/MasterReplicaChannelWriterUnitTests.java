/*
 * Copyright 2020-Present, Redis Ltd. and Contributors
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
package io.lettuce.core.masterreplica;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScanCursorAccessor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.output.KeyScanOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ConnectionIntent;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;

/**
 * @author Mark Paluch
 * @author Jim Brunner
 * @author Sanghun Lee
 */
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MasterReplicaChannelWriterUnitTests {

    @Mock
    private MasterReplicaConnectionProvider<String, String> connectionProvider;

    @Mock
    private ClientResources clientResources;

    private ClientOptions clientOptions = ClientOptions.create();

    @Mock
    private StatefulRedisConnection<String, String> connection;

    @Test
    void shouldReturnIntentForWriteCommand() {

        MasterReplicaChannelWriter writer = new MasterReplicaChannelWriter(connectionProvider, clientResources, clientOptions);

        RedisCommand<String, String, String> set = new Command<>(CommandType.SET, null);
        RedisCommand<String, String, String> mset = new Command<>(CommandType.MSET, null);

        assertThat(writer.getIntent(Arrays.asList(set, mset))).isEqualTo(ConnectionIntent.WRITE);

        assertThat(writer.getIntent(Collections.singletonList(set))).isEqualTo(ConnectionIntent.WRITE);
    }

    @Test
    void shouldReturnDefaultIntentForNoCommands() {

        MasterReplicaChannelWriter writer = new MasterReplicaChannelWriter(connectionProvider, clientResources, clientOptions);

        assertThat(writer.getIntent(Collections.emptyList())).isEqualTo(ConnectionIntent.WRITE);
    }

    @Test
    void shouldReturnIntentForReadCommand() {

        MasterReplicaChannelWriter writer = new MasterReplicaChannelWriter(connectionProvider, clientResources, clientOptions);

        RedisCommand<String, String, String> get = new Command<>(CommandType.GET, null);
        RedisCommand<String, String, String> mget = new Command<>(CommandType.MGET, null);

        assertThat(writer.getIntent(Arrays.asList(get, mget))).isEqualTo(ConnectionIntent.READ);

        assertThat(writer.getIntent(Collections.singletonList(get))).isEqualTo(ConnectionIntent.READ);
    }

    @Test
    void shouldReturnIntentForMixedCommands() {

        MasterReplicaChannelWriter writer = new MasterReplicaChannelWriter(connectionProvider, clientResources, clientOptions);

        RedisCommand<String, String, String> set = new Command<>(CommandType.SET, null);
        RedisCommand<String, String, String> mget = new Command<>(CommandType.MGET, null);

        assertThat(writer.getIntent(Arrays.asList(set, mget))).isEqualTo(ConnectionIntent.WRITE);

        assertThat(writer.getIntent(Collections.singletonList(set))).isEqualTo(ConnectionIntent.WRITE);
    }

    @Test
    void shouldBindTransactionsToMaster() {

        MasterReplicaChannelWriter writer = new MasterReplicaChannelWriter(connectionProvider, clientResources, clientOptions);

        when(connectionProvider.getConnectionAsync(any(ConnectionIntent.class)))
                .thenReturn(CompletableFuture.completedFuture(connection));

        writer.write(mockCommand(CommandType.MULTI));
        writer.write(mockCommand(CommandType.GET));
        writer.write(mockCommand(CommandType.EXEC));

        verify(connectionProvider, times(3)).getConnectionAsync(ConnectionIntent.WRITE);
    }

    @Test
    void shouldBindTransactionsToMasterInBatch() {

        MasterReplicaChannelWriter writer = new MasterReplicaChannelWriter(connectionProvider, clientResources, clientOptions);

        when(connectionProvider.getConnectionAsync(any(ConnectionIntent.class)))
                .thenReturn(CompletableFuture.completedFuture(connection));

        List<Command<String, String, String>> commands = Arrays.asList(mockCommand(CommandType.MULTI),
                mockCommand(CommandType.GET), mockCommand(CommandType.EXEC));

        writer.write(commands);

        verify(connectionProvider).getConnectionAsync(ConnectionIntent.WRITE);
    }

    @Test
    void shouldDeriveIntentFromCommandTypeAfterTransaction() {

        MasterReplicaChannelWriter writer = new MasterReplicaChannelWriter(connectionProvider, clientResources, clientOptions);

        when(connectionProvider.getConnectionAsync(any(ConnectionIntent.class)))
                .thenReturn(CompletableFuture.completedFuture(connection));

        writer.write(mockCommand(CommandType.MULTI));
        writer.write(mockCommand(CommandType.EXEC));
        writer.write(mockCommand(CommandType.GET));

        verify(connectionProvider, times(2)).getConnectionAsync(ConnectionIntent.WRITE);
        verify(connectionProvider).getConnectionAsync(ConnectionIntent.READ);
    }

    @Test
    void shouldDeriveIntentFromCommandTypeAfterDiscardedTransaction() {

        MasterReplicaChannelWriter writer = new MasterReplicaChannelWriter(connectionProvider, clientResources, clientOptions);

        when(connectionProvider.getConnectionAsync(any(ConnectionIntent.class)))
                .thenReturn(CompletableFuture.completedFuture(connection));

        writer.write(mockCommand(CommandType.MULTI));
        writer.write(mockCommand(CommandType.DISCARD));
        writer.write(mockCommand(CommandType.GET));

        verify(connectionProvider, times(2)).getConnectionAsync(ConnectionIntent.WRITE);
        verify(connectionProvider).getConnectionAsync(ConnectionIntent.READ);
    }

    @Test
    void shouldDeriveIntentFromCommandBatchTypeAfterDiscardedTransaction() {

        MasterReplicaChannelWriter writer = new MasterReplicaChannelWriter(connectionProvider, clientResources, clientOptions);

        when(connectionProvider.getConnectionAsync(any(ConnectionIntent.class)))
                .thenReturn(CompletableFuture.completedFuture(connection));

        List<Command<String, String, String>> commands = Arrays.asList(mockCommand(CommandType.MULTI),
                mockCommand(CommandType.EXEC));

        writer.write(commands);
        writer.write(Collections.singletonList(mockCommand(CommandType.GET)));

        verify(connectionProvider).getConnectionAsync(ConnectionIntent.WRITE);
        verify(connectionProvider).getConnectionAsync(ConnectionIntent.READ);
    }

    @Test
    void shouldAssociateSourceWithInitialScanCommand() {

        MasterReplicaChannelWriter writer = new MasterReplicaChannelWriter(connectionProvider, clientResources, clientOptions);

        RedisURI uri = RedisURI.create("localhost", 6482);
        RedisNodeDescription node = mock(RedisNodeDescription.class);
        when(node.getUri()).thenReturn(uri);

        when(connectionProvider.getConnectionAsync(eq(ConnectionIntent.READ), any())).thenAnswer(invocation -> {
            Consumer<RedisNodeDescription> listener = invocation.getArgument(1);
            listener.accept(node);
            return CompletableFuture.completedFuture(connection);
        });

        Command<String, String, KeyScanCursor<String>> scan = scanCommand();

        writer.write(scan);

        assertThat(ScanCursorAccessor.getSource(scan.getOutput().get())).isEqualTo(uri);
        verify(connectionProvider).getConnectionAsync(eq(ConnectionIntent.READ), any());
        verify(connectionProvider, never()).getPinnedConnectionAsync(any());
    }

    @Test
    void shouldPinScanContinuationToSourceNode() {

        MasterReplicaChannelWriter writer = new MasterReplicaChannelWriter(connectionProvider, clientResources, clientOptions);

        RedisURI uri = RedisURI.create("localhost", 6482);

        when(connectionProvider.getPinnedConnectionAsync(uri)).thenReturn(CompletableFuture.completedFuture(connection));

        Command<String, String, KeyScanCursor<String>> scan = scanCommand();
        ScanCursorAccessor.setSource(scan.getOutput().get(), uri);

        writer.write(scan);

        verify(connectionProvider).getPinnedConnectionAsync(uri);
        verify(connectionProvider, never()).getConnectionAsync(any(ConnectionIntent.class));
        verify(connectionProvider, never()).getConnectionAsync(any(ConnectionIntent.class), any());
    }

    @Test
    void shouldFailScanContinuationWhenSourceNodeIsGone() {

        MasterReplicaChannelWriter writer = new MasterReplicaChannelWriter(connectionProvider, clientResources, clientOptions);

        RedisURI uri = RedisURI.create("localhost", 6482);

        CompletableFuture<StatefulRedisConnection<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RedisException("Cannot route scan continuation"));
        when(connectionProvider.getPinnedConnectionAsync(uri)).thenReturn(failed);

        Command<String, String, KeyScanCursor<String>> scan = scanCommand();
        ScanCursorAccessor.setSource(scan.getOutput().get(), uri);
        AsyncCommand<String, String, KeyScanCursor<String>> asyncCommand = new AsyncCommand<>(scan);

        writer.write(asyncCommand);

        assertThat(asyncCommand.isCompletedExceptionally()).isTrue();
        assertThatThrownBy(asyncCommand::join).hasCauseInstanceOf(RedisException.class);
    }

    @Test
    void shouldUseDefaultReadRouteForNonScanReadCommands() {

        MasterReplicaChannelWriter writer = new MasterReplicaChannelWriter(connectionProvider, clientResources, clientOptions);

        when(connectionProvider.getConnectionAsync(any(ConnectionIntent.class)))
                .thenReturn(CompletableFuture.completedFuture(connection));

        writer.write(mockCommand(CommandType.GET));

        verify(connectionProvider).getConnectionAsync(ConnectionIntent.READ);
        verify(connectionProvider, never()).getConnectionAsync(any(ConnectionIntent.class), any());
        verify(connectionProvider, never()).getPinnedConnectionAsync(any());
    }

    @Test
    void shouldBindScansInTransactionToMaster() {

        MasterReplicaChannelWriter writer = new MasterReplicaChannelWriter(connectionProvider, clientResources, clientOptions);

        when(connectionProvider.getConnectionAsync(any(ConnectionIntent.class)))
                .thenReturn(CompletableFuture.completedFuture(connection));

        writer.write(mockCommand(CommandType.MULTI));

        Command<String, String, KeyScanCursor<String>> scan = scanCommand();
        writer.write(scan);
        writer.write(mockCommand(CommandType.EXEC));

        verify(connectionProvider, times(3)).getConnectionAsync(ConnectionIntent.WRITE);
        verify(connectionProvider, never()).getConnectionAsync(any(ConnectionIntent.class), any());
        assertThat(ScanCursorAccessor.getSource(scan.getOutput().get())).isNull();
    }

    @Test
    void shouldAssociateSourceWithScanCommandsInBatch() {

        MasterReplicaChannelWriter writer = new MasterReplicaChannelWriter(connectionProvider, clientResources, clientOptions);

        RedisURI uri = RedisURI.create("localhost", 6482);
        RedisNodeDescription node = mock(RedisNodeDescription.class);
        when(node.getUri()).thenReturn(uri);

        when(connectionProvider.getConnectionAsync(eq(ConnectionIntent.READ), any())).thenAnswer(invocation -> {
            Consumer<RedisNodeDescription> listener = invocation.getArgument(1);
            listener.accept(node);
            return CompletableFuture.completedFuture(connection);
        });

        Command<String, String, KeyScanCursor<String>> scan = scanCommand();

        writer.write(Arrays.asList(scan, mockCommand(CommandType.GET)));

        assertThat(ScanCursorAccessor.getSource(scan.getOutput().get())).isEqualTo(uri);
        verify(connectionProvider).getConnectionAsync(eq(ConnectionIntent.READ), any());
    }

    @Test
    void shouldUseDefaultRouteForBatchWithoutScanCommands() {

        MasterReplicaChannelWriter writer = new MasterReplicaChannelWriter(connectionProvider, clientResources, clientOptions);

        when(connectionProvider.getConnectionAsync(any(ConnectionIntent.class)))
                .thenReturn(CompletableFuture.completedFuture(connection));

        writer.write(Collections.singletonList(mockCommand(CommandType.GET)));

        verify(connectionProvider).getConnectionAsync(ConnectionIntent.READ);
        verify(connectionProvider, never()).getConnectionAsync(any(ConnectionIntent.class), any());
    }

    private static Command<String, String, KeyScanCursor<String>> scanCommand() {
        return new Command<>(CommandType.SCAN, new KeyScanOutput<>(StringCodec.UTF8));
    }

    private static Command<String, String, String> mockCommand(CommandType multi) {
        return new Command<>(multi, new StatusOutput<>(StringCodec.UTF8));
    }

}
