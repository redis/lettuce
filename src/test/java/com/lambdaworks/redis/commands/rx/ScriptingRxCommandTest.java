package com.lambdaworks.redis.commands.rx;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.ScriptingCommandTest;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScriptingRxCommandTest extends ScriptingCommandTest {
    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connectAsync().getStatefulConnection());
    }
}
