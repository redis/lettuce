package com.lambdaworks.redis.commands.reactive;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.SortCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

public class SortReactiveCommandTest extends SortCommandTest {
    @Override
    protected RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }
}
