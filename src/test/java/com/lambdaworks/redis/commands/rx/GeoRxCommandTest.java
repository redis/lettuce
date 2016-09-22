package com.lambdaworks.redis.commands.rx;

import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.commands.GeoCommandTest;
import com.lambdaworks.redis.api.sync.RedisCommands;
import org.junit.Test;

public class GeoRxCommandTest extends GeoCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connectAsync().getStatefulConnection());
    }

    @Override
    @Test(expected = NumberFormatException.class)
    public void geodistMissingElements() throws Exception {
        super.geodistMissingElements();
    }
}
