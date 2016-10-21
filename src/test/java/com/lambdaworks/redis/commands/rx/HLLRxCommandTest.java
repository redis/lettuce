package com.lambdaworks.redis.commands.rx;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.HLLCommandTest;

/**
 * @author Mark Paluch
 */
public class HLLRxCommandTest extends HLLCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connectAsync().getStatefulConnection());
    }

    @Override
    public void pfaddDeprecated() throws Exception {
        // Not available on reactive connection
    }

    @Override
    public void pfmergeDeprecated() throws Exception {
        // Not available on reactive connection
    }

    @Override
    public void pfcountDeprecated() throws Exception {
        // Not available on reactive connection
    }
}
