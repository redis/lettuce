/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.cluster.RedisAdvancedClusterAsyncCommandsImpl;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AbstractRedisAsyncCommands}.
 *
 * @author Aleksandar Todorov
 */
class AbstractRedisAsyncCommandsTest {

    @Mock
    private StatefulRedisClusterConnection<String, String> connection;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void clusterMyId_returnsNodeId_whenCommandSucceeds() {
        AsyncCommand<String, String, String> myIdCmd = completedAsyncCommand("my-node-id");
        doReturn(myIdCmd).when(connection).dispatch(any(RedisCommand.class));

        RedisAdvancedClusterAsyncCommandsImpl<String, String> commands = new RedisAdvancedClusterAsyncCommandsImpl<>(connection,
                StringCodec.UTF8);

        String result = commands.clusterMyId().toCompletableFuture().join();
        assertThat(result).isEqualTo("my-node-id");
    }

    @Test
    void clusterMyId_fallsBackToClusterNodes_onCommandExecutionException() {
        String clusterNodesOutput = "fallback-node 127.0.0.1:6379@16379 myself,master - 0 0 1 connected 0-16383\n"
                + "other-node 127.0.0.1:6380@16380 slave fallback-node 0 0 0 connected";

        AsyncCommand<String, String, String> failedMyId = failedAsyncCommand(
                new RedisCommandExecutionException("ERR unknown subcommand 'MYID'"));
        AsyncCommand<String, String, String> nodesCmd = completedAsyncCommand(clusterNodesOutput);

        doReturn(failedMyId).doReturn(nodesCmd).when(connection).dispatch(any(RedisCommand.class));

        RedisAdvancedClusterAsyncCommandsImpl<String, String> commands = new RedisAdvancedClusterAsyncCommandsImpl<>(connection,
                StringCodec.UTF8);

        String result = commands.clusterMyId().toCompletableFuture().join();
        assertThat(result).isEqualTo("fallback-node");
    }

    @Test
    void clusterMyId_fallsBackToClusterNodes_onEmptyResult() {
        String clusterNodesOutput = "fallback-node 127.0.0.1:6379@16379 myself,master - 0 0 1 connected 0-16383\n";

        AsyncCommand<String, String, String> emptyMyId = completedAsyncCommand("");
        AsyncCommand<String, String, String> nodesCmd = completedAsyncCommand(clusterNodesOutput);

        doReturn(emptyMyId).doReturn(nodesCmd).when(connection).dispatch(any(RedisCommand.class));

        RedisAdvancedClusterAsyncCommandsImpl<String, String> commands = new RedisAdvancedClusterAsyncCommandsImpl<>(connection,
                StringCodec.UTF8);

        String result = commands.clusterMyId().toCompletableFuture().join();
        assertThat(result).isEqualTo("fallback-node");
    }

    @Test
    void clusterMyId_propagatesNonExecutionException() {
        AsyncCommand<String, String, String> timedOut = failedAsyncCommand(
                new RedisCommandTimeoutException("Command timed out"));

        doReturn(timedOut).when(connection).dispatch(any(RedisCommand.class));

        RedisAdvancedClusterAsyncCommandsImpl<String, String> commands = new RedisAdvancedClusterAsyncCommandsImpl<>(connection,
                StringCodec.UTF8);

        CompletableFuture<String> result = commands.clusterMyId().toCompletableFuture();
        assertThatThrownBy(result::join).hasCauseInstanceOf(RedisCommandTimeoutException.class);
    }

    @Test
    void clusterMyId_throwsWhenNoMyselfNodeInFallback() {
        String clusterNodesOutput = "node-123 127.0.0.1:6379@16379 master - 0 0 1 connected 0-16383\n";

        AsyncCommand<String, String, String> failedMyId = failedAsyncCommand(new RedisCommandExecutionException("NOPERM"));
        AsyncCommand<String, String, String> nodesCmd = completedAsyncCommand(clusterNodesOutput);

        doReturn(failedMyId).doReturn(nodesCmd).when(connection).dispatch(any(RedisCommand.class));

        RedisAdvancedClusterAsyncCommandsImpl<String, String> commands = new RedisAdvancedClusterAsyncCommandsImpl<>(connection,
                StringCodec.UTF8);

        CompletableFuture<String> result = commands.clusterMyId().toCompletableFuture();
        assertThatThrownBy(result::join).hasCauseInstanceOf(RedisException.class)
                .hasMessageContaining("Failed to determine cluster node id");
    }

    private static AsyncCommand<String, String, String> completedAsyncCommand(String value) {
        AsyncCommand<String, String, String> cmd = new AsyncCommand<>(
                new Command<>(CommandType.CLUSTER, new StatusOutput<>(StringCodec.UTF8)));
        cmd.complete(value);
        return cmd;
    }

    private static AsyncCommand<String, String, String> failedAsyncCommand(Throwable ex) {
        AsyncCommand<String, String, String> cmd = new AsyncCommand<>(
                new Command<>(CommandType.CLUSTER, new StatusOutput<>(StringCodec.UTF8)));
        cmd.completeExceptionally(ex);
        return cmd;
    }

}
