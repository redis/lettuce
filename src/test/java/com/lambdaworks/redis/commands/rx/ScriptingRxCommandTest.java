package com.lambdaworks.redis.commands.rx;

import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.ScriptingCommandTest;

public class ScriptingRxCommandTest extends ScriptingCommandTest {

    private RedisReactiveCommands<String, String> reactive;

    @Override
    protected RedisCommands<String, String> connect() {
        reactive = client.connectAsync().getStatefulConnection().reactive();
        return RxSyncInvocationHandler.sync(client.connectAsync().getStatefulConnection());
    }
}
