package com.lambdaworks.redis.commands.reactive;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.NumericCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

public class NumericReactiveCommandTest extends NumericCommandTest {
    @Override
    protected RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }
}
