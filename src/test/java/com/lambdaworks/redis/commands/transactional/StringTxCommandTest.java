package com.lambdaworks.redis.commands.transactional;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.StringCommandTest;

/**
 * @author Mark Paluch
 */
public class StringTxCommandTest extends StringCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return TxSyncInvocationHandler.sync(client.connect());
    }
}
