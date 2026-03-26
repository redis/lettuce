/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.cluster.RedisAdvancedClusterReactiveCommandsImpl;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AbstractRedisReactiveCommands}.
 *
 * @author Aleksandar Todorov
 */
class AbstractRedisReactiveCommandsTest {

    @Mock
    private StatefulRedisClusterConnection<String, String> connection;

    @Mock
    private ClientResources clientResources;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(clientResources.tracing()).thenReturn(Tracing.disabled());
        when(connection.getResources()).thenReturn(clientResources);
        when(connection.getOptions()).thenReturn(ClientOptions.builder().build());
    }

    @Test
    void clusterMyId_returnsNodeId_whenCommandSucceeds() {
        mockDispatch(completeWith("my-node-id"));

        RedisAdvancedClusterReactiveCommandsImpl<String, String> commands = new RedisAdvancedClusterReactiveCommandsImpl<>(
                connection, StringCodec.UTF8);

        StepVerifier.create(commands.clusterMyId()).expectNext("my-node-id").verifyComplete();
    }

    @Test
    void clusterMyId_fallsBackToClusterNodes_onCommandExecutionException() {
        String clusterNodesOutput = "fallback-node 127.0.0.1:6379@16379 myself,master - 0 0 1 connected 0-16383\n"
                + "other-node 127.0.0.1:6380@16380 slave fallback-node 0 0 0 connected";

        mockDispatch(failWith(new RedisCommandExecutionException("ERR unknown subcommand 'MYID'")),
                completeWith(clusterNodesOutput));

        RedisAdvancedClusterReactiveCommandsImpl<String, String> commands = new RedisAdvancedClusterReactiveCommandsImpl<>(
                connection, StringCodec.UTF8);

        StepVerifier.create(commands.clusterMyId()).expectNext("fallback-node").verifyComplete();
    }

    @Test
    void clusterMyId_fallsBackToClusterNodes_onEmptyResult() {
        String clusterNodesOutput = "fallback-node 127.0.0.1:6379@16379 myself,master - 0 0 1 connected 0-16383\n";

        mockDispatch(completeWith(""), completeWith(clusterNodesOutput));

        RedisAdvancedClusterReactiveCommandsImpl<String, String> commands = new RedisAdvancedClusterReactiveCommandsImpl<>(
                connection, StringCodec.UTF8);

        StepVerifier.create(commands.clusterMyId()).expectNext("fallback-node").verifyComplete();
    }

    @Test
    void clusterMyId_propagatesNonExecutionException() {
        mockDispatch(failWith(new RedisCommandTimeoutException("Command timed out")));

        RedisAdvancedClusterReactiveCommandsImpl<String, String> commands = new RedisAdvancedClusterReactiveCommandsImpl<>(
                connection, StringCodec.UTF8);

        StepVerifier.create(commands.clusterMyId()).expectError(RedisCommandTimeoutException.class).verify();
    }

    @Test
    void clusterMyId_throwsWhenNoMyselfNodeInFallback() {
        String clusterNodesOutput = "node-123 127.0.0.1:6379@16379 master - 0 0 1 connected 0-16383\n";

        mockDispatch(failWith(new RedisCommandExecutionException("NOPERM")), completeWith(clusterNodesOutput));

        RedisAdvancedClusterReactiveCommandsImpl<String, String> commands = new RedisAdvancedClusterReactiveCommandsImpl<>(
                connection, StringCodec.UTF8);

        StepVerifier.create(commands.clusterMyId()).expectErrorSatisfies(ex -> {
            assertThat(ex).isInstanceOf(RedisException.class).hasMessageContaining("Failed to determine cluster node id");
        }).verify();
    }

    /**
     * Mocks {@code connection.dispatch(RedisCommand)} to complete the passed-in command with the given results in sequence.
     * <p>
     * The reactive pipeline dispatches a {@code SubscriptionCommand} wrapper and waits for it to complete. Unlike the async
     * path, returning a pre-completed command does not work — we must complete the actual command that was dispatched.
     */
    @SuppressWarnings("unchecked")
    private void mockDispatch(DispatchAction... actions) {
        AtomicInteger callIndex = new AtomicInteger(0);
        doAnswer(invocation -> {
            RedisCommand<String, String, ?> cmd = invocation.getArgument(0);
            int index = Math.min(callIndex.getAndIncrement(), actions.length - 1);
            actions[index].apply(cmd);
            return cmd;
        }).when(connection).dispatch(any(RedisCommand.class));
    }

    private static DispatchAction completeWith(String value) {
        return cmd -> {
            cmd.getOutput().set(java.nio.ByteBuffer.wrap(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            cmd.complete();
        };
    }

    private static DispatchAction failWith(Throwable ex) {
        return cmd -> cmd.completeExceptionally(ex);
    }

    @FunctionalInterface
    private interface DispatchAction {

        void apply(RedisCommand<String, String, ?> cmd);

    }

}
