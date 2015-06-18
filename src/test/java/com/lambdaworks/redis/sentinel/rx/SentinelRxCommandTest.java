package com.lambdaworks.redis.sentinel.rx;

import static com.lambdaworks.redis.TestSettings.hostAddr;

import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.commands.rx.RxSyncInvocationHandler;
import com.lambdaworks.redis.sentinel.SentinelCommandTest;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class SentinelRxCommandTest extends SentinelCommandTest {

    @Override
    public void openConnection() throws Exception {

        sentinel = RxSyncInvocationHandler.sync(sentinelClient.connectSentinelAsync().getStatefulConnection());

        try {
            sentinel.master(MASTER_ID);
        } catch (Exception e) {
            sentinelRule.monitor(MASTER_ID, hostAddr(), TestSettings.port(3), 1, true);
        }
    }
}
