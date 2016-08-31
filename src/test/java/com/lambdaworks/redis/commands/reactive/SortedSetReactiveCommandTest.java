package com.lambdaworks.redis.commands.reactive;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.SortedSetCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

public class SortedSetReactiveCommandTest extends SortedSetCommandTest {
    @Override
    protected RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }
}
