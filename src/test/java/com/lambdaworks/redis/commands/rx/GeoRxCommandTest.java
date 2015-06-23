package com.lambdaworks.redis.commands.rx;

import com.lambdaworks.redis.GeoCommandTest;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.ScriptingCommandTest;

public class GeoRxCommandTest extends GeoCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connectAsync().getStatefulConnection());
    }
}
