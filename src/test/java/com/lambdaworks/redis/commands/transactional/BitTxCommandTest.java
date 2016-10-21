package com.lambdaworks.redis.commands.transactional;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.BitCommandTest;

/**
 * @author Mark Paluch
 */
public class BitTxCommandTest extends BitCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        bitstring = TxSyncInvocationHandler.sync(client.connect(new BitStringCodec()));
        return TxSyncInvocationHandler.sync(client.connect());
    }
}
