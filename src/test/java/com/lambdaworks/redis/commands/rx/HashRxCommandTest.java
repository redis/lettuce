package com.lambdaworks.redis.commands.rx;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.HashCommandTest;
import com.lambdaworks.util.RxSyncInvocationHandler;

public class HashRxCommandTest extends HashCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connect());
    }

}
