/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.protocol;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.*;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.ValueOutput;
import io.netty.buffer.ByteBuf;

/**
 * Benchmark for {@link CommandHandler}.
 * <p/>
 * Test cases:
 * <ul>
 * <li>user command writes</li>
 * <li>netty (in-eventloop) writes</li>
 * <li>netty (in-eventloop) reads</li>
 * </ul>
 *
 * @author Mark Paluch
 * @author Grzegorz Szpak
 */
@State(Scope.Benchmark)
public class CommandHandlerBenchmark {

    private static final ByteArrayCodec CODEC = new ByteArrayCodec();
    private static final ClientOptions CLIENT_OPTIONS = ClientOptions.create();
    private static final EmptyContext CHANNEL_HANDLER_CONTEXT = new EmptyContext();
    private static final byte[] KEY = "key".getBytes();
    private static final String VALUE = "value\r\n";
    private final EmptyPromise PROMISE = new EmptyPromise();

    private CommandHandler commandHandler;
    private ByteBuf reply1;
    private ByteBuf reply10;
    private ByteBuf reply100;
    private ByteBuf reply1000;
    private List<Command> commands1;
    private List<Command> commands10;
    private List<Command> commands100;
    private List<Command> commands1000;

    @Setup
    public void setup() throws Exception {

        commandHandler = new CommandHandler(CLIENT_OPTIONS, EmptyClientResources.INSTANCE, new DefaultEndpoint(CLIENT_OPTIONS,
                EmptyClientResources.INSTANCE));
        commandHandler.channelRegistered(CHANNEL_HANDLER_CONTEXT);
        commandHandler.setState(CommandHandler.LifecycleState.CONNECTED);

        reply1 = createByteBuf(String.format("+%s", VALUE));
        reply10 = createByteBuf(createBulkReply(10));
        reply100 = createByteBuf(createBulkReply(100));
        reply1000 = createByteBuf(createBulkReply(1000));

        commands1 = createCommands(1);
        commands10 = createCommands(10);
        commands100 = createCommands(100);
        commands1000 = createCommands(1000);
    }

    @TearDown
    public void tearDown() throws Exception {

        commandHandler.channelUnregistered(CHANNEL_HANDLER_CONTEXT);

        Arrays.asList(reply1, reply10, reply100, reply1000).forEach(ByteBuf::release);
    }

    private static List<Command> createCommands(int count) {
        return IntStream.range(0, count).mapToObj(i -> createCommand()).collect(Collectors.toList());
    }

    private static ByteBuf createByteBuf(String str) {

        ByteBuf buf = CHANNEL_HANDLER_CONTEXT.alloc().directBuffer();
        buf.writeBytes(str.getBytes());
        return buf;
    }

    private static String createBulkReply(int numOfReplies) {

        String baseReply = String.format("$%d\r\n%s\r\n", VALUE.length(), VALUE);

        return String.join("", Collections.nCopies(numOfReplies, baseReply));
    }

    @SuppressWarnings("unchecked")
    private static Command createCommand() {
        return new Command(CommandType.GET, new ValueOutput<>(CODEC), new CommandArgs(CODEC).addKey(KEY)) {
            @Override
            public boolean isDone() {
                return false;
            }
        };
    }

    @Benchmark
    public void measureNettyWriteAndRead() throws Exception {

        Command command = createCommand();

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, command, PROMISE);
        int index = reply1.readerIndex();
        reply1.retain();

        commandHandler.channelRead(CHANNEL_HANDLER_CONTEXT, reply1);

        // cleanup
        reply1.readerIndex(index);
    }

    @Benchmark
    public void measureNettyWriteAndReadBatch1() throws Exception {
        doBenchmark(commands1, reply1);
    }

    @Benchmark
    public void measureNettyWriteAndReadBatch10() throws Exception {
        doBenchmark(commands10, reply10);
    }

    @Benchmark
    public void measureNettyWriteAndReadBatch100() throws Exception {
        doBenchmark(commands100, reply100);
    }

    @Benchmark
    public void measureNettyWriteAndReadBatch1000() throws Exception {
        doBenchmark(commands1000, reply1000);
    }

    private void doBenchmark(List<Command> commandStack, ByteBuf response) throws Exception {

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, commandStack, PROMISE);

        int index = response.readerIndex();
        response.retain();

        commandHandler.channelRead(CHANNEL_HANDLER_CONTEXT, response);

        // cleanup
        response.readerIndex(index);
    }
}
