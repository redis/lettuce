package com.lambdaworks.redis.commands.transactional;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.SortCommandTest;

/**
 * @author Mark Paluch
 */
public class SortTxCommandTest extends SortCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return TxSyncInvocationHandler.sync(client.connect());
    }
}
