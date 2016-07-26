package com.lambdaworks.redis.commands.rx;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.NumericCommandTest;
import com.lambdaworks.util.RxSyncInvocationHandler;

public class NumericRxCommandTest extends NumericCommandTest {
    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connect());
    }
}
