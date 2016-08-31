package com.lambdaworks.redis.commands.reactive;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.BitCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
public class BitReactiveCommandTest extends BitCommandTest {
    @Override
    protected RedisCommands<String, String> connect() {
        bitstring = ReactiveSyncInvocationHandler.sync(client.connect(new BitStringCodec()));
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }

}
