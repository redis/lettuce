package com.lambdaworks.redis.commands.rx;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.KeyCommandTest;

/**
 * @author Mark Paluch
 */
public class KeyRxCommandTest extends KeyCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connectAsync().getStatefulConnection());
    }
}
