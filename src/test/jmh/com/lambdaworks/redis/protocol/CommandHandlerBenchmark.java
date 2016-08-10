package com.lambdaworks.redis.protocol;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.output.ValueOutput;

/**
 * Benchmark for {@link CommandHandler}. Test cases:
 * <ul>
 * <li>netty (in-eventloop) writes</li>
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

    private CommandHandler commandHandler;
    private Command command;

    @Setup
    public void setup() {

        commandHandler = new CommandHandler(EmptyClientResources.INSTANCE, new DefaultEndpoint(CLIENT_OPTIONS));
        command = new Command(CommandType.GET, new ValueOutput<>(CODEC), new CommandArgs(CODEC).addKey(KEY));

        commandHandler.setState(CommandHandler.LifecycleState.CONNECTED);
    }

    @Benchmark
    public void measureNettyWrite() throws Exception {

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, command, null);

        // Prevent OOME
        commandHandler.getQueue().clear();
    }
}
