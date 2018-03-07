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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.ValueOutput;
import io.netty.buffer.ByteBuf;

/**
 * Benchmark for {@link CommandHandler}. Test cases:
 * <ul>
 * <li>netty (in-eventloop) writes</li>
 * <li>netty (in-eventloop) batch writes</li>
 * </ul>
 *
 * @author Mark Paluch
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

    @Setup
    public void setup() throws Exception {
        commandHandler = new CommandHandler(CLIENT_OPTIONS, EmptyClientResources.INSTANCE, new DefaultEndpoint(CLIENT_OPTIONS));
        commandHandler.channelRegistered(CHANNEL_HANDLER_CONTEXT);
        commandHandler.setState(CommandHandler.LifecycleState.CONNECTED);

        reply1 = strToByteBuf(String.format("+%s", VALUE));
        reply10 = strToByteBuf(makeBulkReply(10));
        reply100 = strToByteBuf(makeBulkReply(100));
        reply1000 = strToByteBuf(makeBulkReply(1000));
        for (ByteBuf buf: Arrays.asList(reply1, reply10, reply100, reply1000)) {
            buf.retain();
        }
    }

    @TearDown
    public void tearDown() throws Exception {
        commandHandler.channelUnregistered(CHANNEL_HANDLER_CONTEXT);
        for (ByteBuf buf: Arrays.asList(reply1, reply10, reply100, reply1000)) {
            buf.release(2);
        }
    }

    private ByteBuf strToByteBuf(String str) {
        ByteBuf buf = CHANNEL_HANDLER_CONTEXT.alloc().directBuffer();
        buf.writeBytes(str.getBytes());
        return buf;
    }

    private String makeBulkReply(int numOfReplies) {
        String baseReply = String.format("$%d\r\n%s\r\n", VALUE.length(), VALUE);
        return String.join("", Collections.nCopies(numOfReplies, baseReply));
    }

    private Command makeCommand() {
        return new Command(CommandType.GET, new ValueOutput<>(CODEC), new CommandArgs(CODEC).addKey(KEY));
    }

    @Benchmark
    public void measureNettyWriteAndRead() throws Exception {
        Command command = makeCommand();

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, command, PROMISE);

        commandHandler.channelRead(CHANNEL_HANDLER_CONTEXT, reply1);
        reply1.resetReaderIndex();
        reply1.retain();
    }

    @Benchmark
    public void measureNettyWriteAndReadBatch1() throws Exception {
        List<Command> commands = Collections.singletonList(makeCommand());

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, commands, PROMISE);

        commandHandler.channelRead(CHANNEL_HANDLER_CONTEXT, reply1);
        reply1.resetReaderIndex();
        reply1.retain();
    }

    @Benchmark
    public void measureNettyWriteAndReadBatch10() throws Exception {
        List<Command> commands = IntStream.range(0, 10)
                .mapToObj(i -> makeCommand())
                .collect(Collectors.toList());

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, commands, PROMISE);

        commandHandler.channelRead(CHANNEL_HANDLER_CONTEXT, reply10);
        reply10.resetReaderIndex();
        reply10.retain();
    }

    @Benchmark
    public void measureNettyWriteAndReadBatch100() throws Exception {
        List<Command> commands = IntStream.range(0, 100)
                .mapToObj(i -> makeCommand())
                .collect(Collectors.toList());

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, commands, PROMISE);

        commandHandler.channelRead(CHANNEL_HANDLER_CONTEXT, reply100);
        reply100.resetReaderIndex();
        reply100.retain();
    }

    @Benchmark
    public void measureNettyWriteAndReadBatch1000() throws Exception {
        List<Command> commands = IntStream.range(0, 1000)
                .mapToObj(i -> makeCommand())
                .collect(Collectors.toList());

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, commands, PROMISE);

        commandHandler.channelRead(CHANNEL_HANDLER_CONTEXT, reply1000);
        reply1000.resetReaderIndex();
        reply1000.retain();
    }
}
