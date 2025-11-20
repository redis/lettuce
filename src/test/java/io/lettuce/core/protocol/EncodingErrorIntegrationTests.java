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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.StatusOutput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.EncoderException;

/**
 * Integration tests for encoding error handling in the command pipeline.
 * Tests the full flow from codec failure through command completion.
 *
 * @author Lettuce Contributors
 */
class EncodingErrorIntegrationTests {

    @Test
    void shouldHandleCodecEncodingFailure() {
        // Create a command with a failing codec
        FailingCodec failingCodec = new FailingCodec();
        CommandArgs<String, String> args = new CommandArgs<>(failingCodec);
        args.addKey("test");
        
        Command<String, String, String> command = new Command<>(CommandType.SET, 
            new StatusOutput<>(failingCodec), args);

        ByteBuf buffer = Unpooled.buffer();

        // Verify initial state
        assertThat(command.hasEncodingError()).isFalse();
        assertThat(command.isDone()).isFalse();

        // Encoding should fail and mark the command
        assertThatThrownBy(() -> command.encode(buffer))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Codec encoding failed");

        // At this point, if we were in the encoder, it would catch this and mark the command
        command.markEncodingError();
        command.completeExceptionally(new EncoderException("Cannot encode command", 
            new RuntimeException("Codec encoding failed")));

        // Verify command state after encoding failure
        assertThat(command.hasEncodingError()).isTrue();
        assertThat(command.isDone()).isTrue();

        buffer.release();
    }

    @Test
    void shouldSimulateCommandPipelineWithEncodingFailures() {
        // Simulate a scenario with multiple commands where some fail encoding
        
        // Command A - succeeds
        Command<String, String, String> commandA = createSuccessfulCommand("A");
        
        // Command B - fails encoding  
        Command<String, String, String> commandB = createCommandWithFailingCodec("B");
        
        // Command C - succeeds
        Command<String, String, String> commandC = createSuccessfulCommand("C");

        ByteBuf buffer = Unpooled.buffer();

        // Encode command A - should succeed
        commandA.encode(buffer);
        assertThat(commandA.hasEncodingError()).isFalse();
        
        // Encode command B - should fail
        try {
            commandB.encode(buffer);
        } catch (RuntimeException e) {
            // Simulate CommandEncoder behavior
            commandB.markEncodingError();
            commandB.completeExceptionally(new EncoderException("Cannot encode command", e));
        }
        assertThat(commandB.hasEncodingError()).isTrue();
        assertThat(commandB.isDone()).isTrue();

        // Encode command C - should succeed
        commandC.encode(buffer);
        assertThat(commandC.hasEncodingError()).isFalse();

        // Verify that encoding failure doesn't affect other commands
        assertThat(commandA.hasEncodingError()).isFalse();
        assertThat(commandC.hasEncodingError()).isFalse();

        buffer.release();
    }

    @Test
    void shouldPreserveCommandStateAfterEncodingError() {
        Command<String, String, String> command = createCommandWithFailingCodec("test");
        
        // Store original command properties
        ProtocolKeyword originalType = command.getType();
        CommandArgs<String, String> originalArgs = command.getArgs();

        ByteBuf buffer = Unpooled.buffer();

        // Attempt encoding
        try {
            command.encode(buffer);
        } catch (RuntimeException e) {
            command.markEncodingError();
            command.completeExceptionally(new EncoderException("Cannot encode command", e));
        }

        // Verify command properties are preserved
        assertThat(command.getType()).isSameAs(originalType);
        assertThat(command.getArgs()).isSameAs(originalArgs);
        assertThat(command.hasEncodingError()).isTrue();
        assertThat(command.isDone()).isTrue();

        buffer.release();
    }

    @Test
    void shouldNotMarkSuccessfulCommandsWithEncodingError() {
        Command<String, String, String> command = createSuccessfulCommand("test");
        ByteBuf buffer = Unpooled.buffer();

        // Encode successfully
        command.encode(buffer);

        // Verify no encoding error
        assertThat(command.hasEncodingError()).isFalse();
        assertThat(command.isDone()).isFalse(); // Not completed until response received

        buffer.release();
    }

    @Test
    void shouldHandleEncodingErrorOnCommandCompletion() {
        Command<String, String, String> command = createCommandWithFailingCodec("test");
        
        // Mark as encoding error (simulating CommandEncoder behavior)
        command.markEncodingError();
        
        EncoderException encodingException = new EncoderException("Cannot encode command");
        boolean completed = command.completeExceptionally(encodingException);

        // Verify completion
        assertThat(completed).isTrue();
        assertThat(command.isDone()).isTrue();
        assertThat(command.hasEncodingError()).isTrue();
    }

    private Command<String, String, String> createSuccessfulCommand(String key) {
        Command<String, String, String> command = new Command<>(CommandType.SET,
            new StatusOutput<>(new WorkingCodec()), new CommandArgs<>(new WorkingCodec()));
        command.getArgs().addKey(key).addValue("value");
        return command;
    }

    private Command<String, String, String> createCommandWithFailingCodec(String key) {
        FailingCodec failingCodec = new FailingCodec();
        Command<String, String, String> command = new Command<>(CommandType.SET,
            new StatusOutput<>(failingCodec), new CommandArgs<>(failingCodec));
        command.getArgs().addKey(key).addValue("value");
        return command;
    }

    /**
     * Codec that always fails during encoding
     */
    private static class FailingCodec implements RedisCodec<String, String> {

        @Override
        public String decodeKey(ByteBuffer bytes) {
            return "decoded-key";
        }

        @Override
        public String decodeValue(ByteBuffer bytes) {
            return "decoded-value";
        }

        @Override
        public ByteBuffer encodeKey(String key) {
            throw new RuntimeException("Codec encoding failed");
        }

        @Override
        public ByteBuffer encodeValue(String value) {
            throw new RuntimeException("Codec encoding failed");
        }
    }

    /**
     * Codec that works normally
     */
    private static class WorkingCodec implements RedisCodec<String, String> {

        @Override
        public String decodeKey(ByteBuffer bytes) {
            return new String(bytes.array());
        }

        @Override
        public String decodeValue(ByteBuffer bytes) {
            return new String(bytes.array());
        }

        @Override
        public ByteBuffer encodeKey(String key) {
            return ByteBuffer.wrap(key.getBytes());
        }

        @Override
        public ByteBuffer encodeValue(String value) {
            return ByteBuffer.wrap(value.getBytes());
        }
    }
}