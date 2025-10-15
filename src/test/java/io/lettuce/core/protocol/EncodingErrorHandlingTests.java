/*
 * Copyright 2011-2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.ArrayDeque;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.codec.StringCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;

/**
 * Unit tests for encoding error handling functionality.
 * Tests the encoding error flag mechanism to prevent queue corruption.
 *
 * @author Lettuce Contributors
 */
@ExtendWith(MockitoExtension.class)
class EncodingErrorHandlingTests {

    @Mock
    private ChannelHandlerContext ctx;

    private CommandEncoder encoder;
    private ArrayDeque<RedisCommand<?, ?, ?>> commandStack;

    @BeforeEach
    void setUp() {
        encoder = new CommandEncoder();
        commandStack = new ArrayDeque<>();
    }

    @Test
    void shouldMarkCommandWithEncodingError() {
        // Create a command that will fail encoding
        Command<String, String, String> command = new FailingEncodingCommand();
        ByteBuf buffer = Unpooled.buffer();

        // Verify initial state
        assertThat(command.hasEncodingError()).isFalse();
        assertThat(command.isDone()).isFalse();

        // Trigger encoding failure
        try {
            encoder.encode(ctx, command, buffer);
        } catch (Exception e) {
            // Expected - encoding should fail
        }

        // Verify command is marked with encoding error
        assertThat(command.hasEncodingError()).isTrue();
        assertThat(command.isDone()).isTrue();

        buffer.release();
    }

    @Test
    void shouldNotMarkSuccessfulCommandWithEncodingError() {
        Command<String, String, String> command = new Command<>(CommandType.SET, 
            new StatusOutput<>(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8));
        command.getArgs().addKey("test").addValue("value");
        
        ByteBuf buffer = Unpooled.buffer();

        // Verify initial state
        assertThat(command.hasEncodingError()).isFalse();

        // Encode successfully
        try {
            encoder.encode(ctx, command, buffer);
        } catch (Exception e) {
            // Should not happen for successful commands
            throw new RuntimeException(e);
        }

        // Verify command is not marked with encoding error
        assertThat(command.hasEncodingError()).isFalse();
        assertThat(command.isDone()).isFalse(); // Should not be completed yet

        buffer.release();
    }

    @Test
    void shouldCleanupEncodingFailuresFromStack() {
        // Create test commands
        Command<String, String, String> commandA = createSuccessfulCommand("A");
        Command<String, String, String> commandB = createSuccessfulCommand("B");
        Command<String, String, String> commandC = createSuccessfulCommand("C");

        // Mark B as encoding failure
        commandB.markEncodingError();
        commandB.completeExceptionally(new EncoderException("Encoding failed"));

        // Add commands to stack in order
        commandStack.add(commandA);
        commandStack.add(commandB);
        commandStack.add(commandC);

        // Simulate response processing cleanup
        // In real scenario, A would be processed first, then B would be cleaned up
        // Let's process A first (simulate successful response)
        assertThat(commandStack.poll()).isSameAs(commandA);
        
        // Now cleanup should remove B before processing C
        cleanupEncodingFailures(commandStack);

        // Verify stack state - C should be at head after B is removed
        assertThat(commandStack.size()).isEqualTo(1);
        assertThat(commandStack.peek()).isSameAs(commandC);
    }

    @Test
    void shouldCleanupMultipleConsecutiveEncodingFailures() {
        // Create test commands
        Command<String, String, String> commandA = createSuccessfulCommand("A");
        Command<String, String, String> commandB = createSuccessfulCommand("B");
        Command<String, String, String> commandC = createSuccessfulCommand("C");
        Command<String, String, String> commandD = createSuccessfulCommand("D");

        // Mark B and C as encoding failures
        commandB.markEncodingError();
        commandC.markEncodingError();

        // Add commands to stack
        commandStack.add(commandA);
        commandStack.add(commandB);
        commandStack.add(commandC);
        commandStack.add(commandD);

        // Process A normally
        assertThat(commandStack.poll()).isSameAs(commandA);

        // Now cleanup should remove B and C before processing D
        cleanupEncodingFailures(commandStack);

        // Verify only D remains
        assertThat(commandStack.size()).isEqualTo(1);
        assertThat(commandStack.peek()).isSameAs(commandD);
    }

    @Test
    void shouldHandleStackWithOnlyEncodingFailures() {
        // Create commands that all failed encoding
        Command<String, String, String> commandA = createSuccessfulCommand("A");
        Command<String, String, String> commandB = createSuccessfulCommand("B");

        commandA.markEncodingError();
        commandB.markEncodingError();

        commandStack.add(commandA);
        commandStack.add(commandB);

        // Cleanup should remove all commands
        cleanupEncodingFailures(commandStack);

        assertThat(commandStack).isEmpty();
    }

    @Test
    void shouldHandleEmptyStack() {
        // Cleanup on empty stack should not throw
        cleanupEncodingFailures(commandStack);
        assertThat(commandStack).isEmpty();
    }

    @Test
    void shouldNotAffectCommandsWithoutEncodingErrors() {
        Command<String, String, String> commandA = createSuccessfulCommand("A");
        Command<String, String, String> commandB = createSuccessfulCommand("B");
        Command<String, String, String> commandC = createSuccessfulCommand("C");

        commandStack.add(commandA);
        commandStack.add(commandB);
        commandStack.add(commandC);

        // Cleanup should not remove any commands
        cleanupEncodingFailures(commandStack);

        assertThat(commandStack.size()).isEqualTo(3);
        assertThat(commandStack.poll()).isSameAs(commandA);
        assertThat(commandStack.poll()).isSameAs(commandB);
        assertThat(commandStack.poll()).isSameAs(commandC);
    }

    /**
     * Simulates the encoding failure cleanup logic from CommandHandler
     */
    private void cleanupEncodingFailures(ArrayDeque<RedisCommand<?, ?, ?>> stack) {
        while (!stack.isEmpty() && stack.peek().hasEncodingError()) {
            stack.poll();
        }
    }

    private Command<String, String, String> createSuccessfulCommand(String identifier) {
        Command<String, String, String> command = new Command<>(CommandType.SET, 
            new StatusOutput<>(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8));
        command.getArgs().addKey("key" + identifier).addValue("value" + identifier);
        return command;
    }

    /**
     * Test command that always fails during encoding
     */
    private static class FailingEncodingCommand extends Command<String, String, String> {
        
        public FailingEncodingCommand() {
            super(CommandType.SET, new StatusOutput<>(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8));
        }

        @Override
        public void encode(ByteBuf buf) {
            throw new RuntimeException("Simulated encoding failure");
        }
    }
}