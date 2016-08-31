package com.lambdaworks.redis.commands.reactive;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.ScriptingCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

public class ScriptingReactiveCommandTest extends ScriptingCommandTest {


    @Override
    protected RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }
}
