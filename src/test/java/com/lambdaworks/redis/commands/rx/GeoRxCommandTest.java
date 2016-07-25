package com.lambdaworks.redis.commands.rx;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.GeoCommandTest;

public class GeoRxCommandTest extends GeoCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connect());
    }
}
