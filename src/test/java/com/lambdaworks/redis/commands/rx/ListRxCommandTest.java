package com.lambdaworks.redis.commands.rx;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.ListCommandTest;

public class ListRxCommandTest extends ListCommandTest {
    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connect());
    }
}
