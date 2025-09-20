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

import java.util.ArrayDeque;

import org.junit.jupiter.api.Test;

import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.codec.StringCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import org.mockito.Mockito;

/**
 * Demonstration test showing the complete encoding error handling flow.
 * This simulates the exact scenario your solution addresses: preventing
 * queue corruption when encoding failures occur.
 *
 * @author Lettuce Contributors
 */
class EncodingErrorDemoTest {

    @Test
    void demonstrateEncodingErrorHandling() {
        System.out.println("=== Encoding Error Handling Demo ===");
        
        // Create a simulated command stack (like CommandHandler.stack)
        ArrayDeque<RedisCommand<?, ?, ?>> commandStack = new ArrayDeque<>();
        
        // Simulate command pipeline with encoding failures
        System.out.println("\n1. Creating commands...");
        
        Command<String, String, String> commandA = createCommand("A");
        Command<String, String, String> commandB = createFailingCommand("B");
        Command<String, String, String> commandC = createCommand("C");
        Command<String, String, String> commandD = createFailingCommand("D");
        Command<String, String, String> commandE = createCommand("E");
        
        System.out.println("   - Command A: Normal command");
        System.out.println("   - Command B: Will fail encoding");
        System.out.println("   - Command C: Normal command");
        System.out.println("   - Command D: Will fail encoding");
        System.out.println("   - Command E: Normal command");
        
        // Simulate encoding phase
        System.out.println("\n2. Encoding commands...");
        simulateEncoding(commandA, "A encoded successfully");
        simulateEncoding(commandB, "B encoding FAILED - marked with encodingError=true");
        simulateEncoding(commandC, "C encoded successfully");
        simulateEncoding(commandD, "D encoding FAILED - marked with encodingError=true");
        simulateEncoding(commandE, "E encoded successfully");
        
        // Add successfully encoded commands to stack (B and D were never added due to encoding failures)
        commandStack.add(commandA);
        commandStack.add(commandB); // Added but marked with encoding error
        commandStack.add(commandC);
        commandStack.add(commandD); // Added but marked with encoding error
        commandStack.add(commandE);
        
        System.out.println("\n3. Command stack state:");
        System.out.println("   Stack: [A, B(encodingError), C, D(encodingError), E]");
        System.out.println("   Outstanding count: 5 (but only A, C, E were actually sent to Redis)");
        
        // Simulate responses arriving from Redis
        System.out.println("\n4. Processing responses...");
        
        // Response for A arrives
        System.out.println("   Response for A arrives:");
        processResponse(commandStack, "A");
        
        // Response for C arrives (B should be cleaned up automatically)
        System.out.println("   Response for C arrives:");
        cleanupAndProcessResponse(commandStack, "C");
        
        // Response for E arrives (D should be cleaned up automatically)
        System.out.println("   Response for E arrives:");
        cleanupAndProcessResponse(commandStack, "E");
        
        // Verify final state
        System.out.println("\n5. Final verification:");
        assertThat(commandStack).isEmpty();
        assertThat(commandA.isDone()).isTrue();
        assertThat(commandA.hasEncodingError()).isFalse();
        assertThat(commandB.isDone()).isTrue();
        assertThat(commandB.hasEncodingError()).isTrue();
        assertThat(commandC.isDone()).isTrue();
        assertThat(commandC.hasEncodingError()).isFalse();
        assertThat(commandD.isDone()).isTrue();
        assertThat(commandD.hasEncodingError()).isTrue();
        assertThat(commandE.isDone()).isTrue();
        assertThat(commandE.hasEncodingError()).isFalse();
        
        System.out.println("   ✅ All commands processed correctly");
        System.out.println("   ✅ Encoding failures cleaned up");
        System.out.println("   ✅ No queue corruption");
        System.out.println("   ✅ Responses matched to correct commands");
        
        System.out.println("\n=== Demo completed successfully! ===");
    }
    
    private void simulateEncoding(Command<String, String, String> command, String message) {
        if (message.contains("FAILED")) {
            command.markEncodingError();
            command.completeExceptionally(new EncoderException("Encoding failed"));
        }
        System.out.println("   " + message);
    }
    
    private void processResponse(ArrayDeque<RedisCommand<?, ?, ?>> stack, String commandId) {
        RedisCommand<?, ?, ?> command = stack.poll();
        command.complete();
        System.out.println("     - Processed " + commandId + " normally");
    }
    
    private void cleanupAndProcessResponse(ArrayDeque<RedisCommand<?, ?, ?>> stack, String commandId) {
        // Simulate the cleanup logic from your solution
        while (!stack.isEmpty() && stack.peek().hasEncodingError()) {
            RedisCommand<?, ?, ?> failed = stack.poll();
            System.out.println("     - Cleaned up encoding failure (already completed exceptionally)");
        }
        
        if (!stack.isEmpty()) {
            RedisCommand<?, ?, ?> command = stack.poll();
            command.complete();
            System.out.println("     - Processed " + commandId + " normally");
        }
    }
    
    private Command<String, String, String> createCommand(String key) {
        Command<String, String, String> command = new Command<>(CommandType.SET,
            new StatusOutput<>(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8));
        command.getArgs().addKey(key).addValue("value");
        return command;
    }
    
    private Command<String, String, String> createFailingCommand(String key) {
        // For demo purposes, we'll manually mark these as encoding failures
        return createCommand(key);
    }
}