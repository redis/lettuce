package com.lambdaworks.redis.commands.reactive;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.HashCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

public class HashReactiveCommandTest extends HashCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }

}
