package com.lambdaworks.redis.sentinel.rx;

import static com.lambdaworks.redis.TestSettings.hostAddr;
import static org.assertj.core.api.Assertions.assertThat;

import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.commands.rx.RxSyncInvocationHandler;
import com.lambdaworks.redis.sentinel.SentinelCommandTest;
import com.lambdaworks.redis.sentinel.api.async.RedisSentinelAsyncCommands;
import com.lambdaworks.redis.sentinel.api.rx.RedisSentinelReactiveCommands;

/**
 * @author Mark Paluch
 */
public class SentinelRxCommandTest extends SentinelCommandTest {

    @Override
    public void openConnection() throws Exception {

        RedisSentinelAsyncCommands<String, String> async = sentinelClient.connectSentinel().async();
        RedisSentinelReactiveCommands<String, String> reactive = async.getStatefulConnection().reactive();
        sentinel = RxSyncInvocationHandler.sync(async.getStatefulConnection());

        try {
            sentinel.master(MASTER_ID);
        } catch (Exception e) {
            sentinelRule.monitor(MASTER_ID, hostAddr(), TestSettings.port(3), 1, true);
        }

        assertThat(reactive.isOpen()).isTrue();
        assertThat(reactive.getStatefulConnection()).isSameAs(async.getStatefulConnection());
    }
}
