package com.lambdaworks.redis.commands.reactive;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.ListCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

public class ListReactiveCommandTest extends ListCommandTest {
    @Override
    protected RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }
}
