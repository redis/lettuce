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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.ValueOutput;

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
    private final EmptyPromise PROMISE = new EmptyPromise();

    private CommandHandler commandHandler;
    private Command command;
    private Command batchCommand;
    private Collection<Command> commands1;
    private List<Command> commands10;
    private List<Command> commands100;
    private List<Command> commands1000;

    @Setup
    public void setup() {

        commandHandler = new CommandHandler(CLIENT_OPTIONS, EmptyClientResources.INSTANCE, new DefaultEndpoint(CLIENT_OPTIONS));
        command = new Command(CommandType.GET, new ValueOutput<>(CODEC), new CommandArgs(CODEC).addKey(KEY));


        commandHandler.setState(CommandHandler.LifecycleState.CONNECTED);

        commands1 = Arrays.asList(command);
        commands10 = IntStream.range(0, 10)
                .mapToObj(i -> new Command(CommandType.GET, new ValueOutput<>(CODEC), new CommandArgs(CODEC).addKey(KEY)))
                .collect(Collectors.toList());

        commands100 = IntStream.range(0, 100)
                .mapToObj(i -> new Command(CommandType.GET, new ValueOutput<>(CODEC), new CommandArgs(CODEC).addKey(KEY)))
                .collect(Collectors.toList());

        commands1000 = IntStream.range(0, 1000)
                .mapToObj(i -> new Command(CommandType.GET, new ValueOutput<>(CODEC), new CommandArgs(CODEC).addKey(KEY)))
                .collect(Collectors.toList());
    }

    @Benchmark
    public void measureNettyWrite() throws Exception {

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, command, PROMISE);

        // Prevent OOME
        commandHandler.getStack().clear();
    }

    @Benchmark
    public void measureNettyWriteBatch1() throws Exception {

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, commands1, PROMISE);

        // Prevent OOME
        commandHandler.getStack().clear();
    }

    @Benchmark
    public void measureNettyWriteBatch10() throws Exception {

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, commands10, PROMISE);

        // Prevent OOME
        commandHandler.getStack().clear();
    }

    @Benchmark
    public void measureNettyWriteBatch100() throws Exception {

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, commands100, PROMISE);

        // Prevent OOME
        commandHandler.getStack().clear();
    }

    @Benchmark
    public void measureNettyWriteBatch1000() throws Exception {

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, commands1000, PROMISE);

        // Prevent OOME
        commandHandler.getStack().clear();
    }
}
