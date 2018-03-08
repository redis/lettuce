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

    private final static ByteArrayCodec CODEC = new ByteArrayCodec();
    private final static ClientOptions CLIENT_OPTIONS = ClientOptions.create();
    private final static EmptyContext CHANNEL_HANDLER_CONTEXT = new EmptyContext();
    private final static byte[] KEY = "key".getBytes();
    private static final String VALUE = "value\r\n";
    private final EmptyPromise PROMISE = new EmptyPromise();

    private CommandHandler commandHandler;

    @Setup
    public void setup() throws Exception {
        commandHandler = new CommandHandler(CLIENT_OPTIONS, EmptyClientResources.INSTANCE, new DefaultEndpoint(CLIENT_OPTIONS));
        commandHandler.channelRegistered(CHANNEL_HANDLER_CONTEXT);
        commandHandler.setState(CommandHandler.LifecycleState.CONNECTED);
    }

    @TearDown
    public void tearDown() throws Exception {
        commandHandler.channelUnregistered(CHANNEL_HANDLER_CONTEXT);
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
        ByteBuf reply = strToByteBuf(String.format("+%s", VALUE));
        Command command = makeCommand();

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, command, PROMISE);

        commandHandler.channelRead(CHANNEL_HANDLER_CONTEXT, reply);
    }

    @Benchmark
    public void measureNettyWriteAndReadBatch1() throws Exception {
        ByteBuf reply = strToByteBuf(String.format("+%s", VALUE));
        List<Command> commands = Collections.singletonList(makeCommand());

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, commands, PROMISE);

        commandHandler.channelRead(CHANNEL_HANDLER_CONTEXT, reply);
    }

    @Benchmark
    public void measureNettyWriteAndReadBatch10() throws Exception {
        ByteBuf reply = strToByteBuf(makeBulkReply(10));
        List<Command> commands = IntStream.range(0, 10)
                .mapToObj(i -> makeCommand())
                .collect(Collectors.toList());

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, commands, PROMISE);

        commandHandler.channelRead(CHANNEL_HANDLER_CONTEXT, reply);
    }

    @Benchmark
    public void measureNettyWriteAndReadBatch100() throws Exception {
        ByteBuf reply = strToByteBuf(makeBulkReply(100));
        List<Command> commands = IntStream.range(0, 100)
                .mapToObj(i -> makeCommand())
                .collect(Collectors.toList());

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, commands, PROMISE);

        commandHandler.channelRead(CHANNEL_HANDLER_CONTEXT, reply);
    }

    @Benchmark
    public void measureNettyWriteAndReadBatch1000() throws Exception {
        ByteBuf reply = strToByteBuf(makeBulkReply(1000));
        List<Command> commands = IntStream.range(0, 1000)
                .mapToObj(i -> makeCommand())
                .collect(Collectors.toList());

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, commands, PROMISE);

        commandHandler.channelRead(CHANNEL_HANDLER_CONTEXT, reply);
    }
}
