package com.lambdaworks.redis.commands.transactional;

import org.junit.Ignore;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.KeyCommandTest;

/**
 * @author Mark Paluch
 */
public class KeyTxCommandTest extends KeyCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return TxSyncInvocationHandler.sync(client.connect());
    }

    @Ignore
    @Override
    public void move() throws Exception {
    }
}
