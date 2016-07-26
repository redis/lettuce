package com.lambdaworks.redis.commands.rx;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.KeyCommandTest;
import com.lambdaworks.util.RxSyncInvocationHandler;

public class KeyRxCommandTest extends KeyCommandTest {
    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connect());
    }
}
