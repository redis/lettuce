package com.lambdaworks.redis.commands.rx;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.ScriptingCommandTest;
import com.lambdaworks.util.RxSyncInvocationHandler;

public class ScriptingRxCommandTest extends ScriptingCommandTest {


    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connect());
    }
}
