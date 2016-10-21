package com.lambdaworks.redis.commands.rx;

import org.junit.Test;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.GeoCommandTest;

/**
 * @author Mark Paluch
 */
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
