package com.lambdaworks.redis.commands.reactive;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.KeyCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

public class KeyReactiveCommandTest extends KeyCommandTest {
    @Override
    protected RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }
}
