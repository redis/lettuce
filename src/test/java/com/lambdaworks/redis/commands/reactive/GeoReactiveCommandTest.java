package com.lambdaworks.redis.commands.reactive;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.GeoCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

public class GeoReactiveCommandTest extends GeoCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }
}
