package com.lambdaworks.redis.commands.reactive;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.HLLCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

public class HLLReactiveCommandTest extends HLLCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }
}
