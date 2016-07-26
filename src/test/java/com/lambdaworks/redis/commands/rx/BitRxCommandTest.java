package com.lambdaworks.redis.commands.rx;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.BitCommandTest;
import com.lambdaworks.util.RxSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
public class BitRxCommandTest extends BitCommandTest {
    @Override
    protected RedisCommands<String, String> connect() {
        bitstring = RxSyncInvocationHandler.sync(client.connect(new BitStringCodec()));
        return RxSyncInvocationHandler.sync(client.connect());
    }

}
