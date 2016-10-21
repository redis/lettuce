package com.lambdaworks.redis.commands.transactional;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.HLLCommandTest;

/**
 * @author Mark Paluch
 */
public class HLLTxCommandTest extends HLLCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return TxSyncInvocationHandler.sync(client.connect());
    }
}
