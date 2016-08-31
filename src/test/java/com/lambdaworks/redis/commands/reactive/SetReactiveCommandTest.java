package com.lambdaworks.redis.commands.reactive;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.SetCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

public class SetReactiveCommandTest extends SetCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }

}
