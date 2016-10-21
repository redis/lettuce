package com.lambdaworks.redis.commands.transactional;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.SortedSetCommandTest;

/**
 * @author Mark Paluch
 */
public class SortedSetTxCommandTest extends SortedSetCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return TxSyncInvocationHandler.sync(client.connect());
    }
}
