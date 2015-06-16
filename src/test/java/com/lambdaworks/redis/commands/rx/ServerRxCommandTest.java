package com.lambdaworks.redis.commands.rx;

import static org.assertj.core.api.Assertions.*;

import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.ServerCommandTest;

public class ServerRxCommandTest extends ServerCommandTest {

    private RedisReactiveCommands<String, String> reactive;

    @Before
    public void openConnection() throws Exception {
        super.openConnection();
        reactive = redis.getStatefulConnection().reactive();
    }

    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connectAsync().getStatefulConnection());
    }

    /**
     * Luckily these commands do not destroy anything in contrast to sync/async.
     */
    @Test
    public void shutdown() throws Exception {
        reactive.shutdown(true);
        assertThat(reactive.isOpen()).isTrue();
    }

    @Test
    public void debugOom() throws Exception {
        reactive.debugOom();
        assertThat(reactive.isOpen()).isTrue();
    }

    @Test
    public void debugSegfault() throws Exception {
        reactive.debugSegfault();
        assertThat(reactive.isOpen()).isTrue();
    }
}
