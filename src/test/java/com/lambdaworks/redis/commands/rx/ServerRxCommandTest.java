package com.lambdaworks.redis.commands.rx;

import static org.assertj.core.api.Assertions.assertThat;

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
        return RxSyncInvocationHandler.sync(client.connect());
    }

    /**
     * Luckily these commands do not destroy anything in contrast to sync/async.
     */
    @Test
    public void shutdown() throws Exception {
        reactive.shutdown(true);
        assertThat(reactive.getStatefulConnection().isOpen()).isTrue();
    }

    @Test
    public void debugOom() throws Exception {
        reactive.debugOom();
        assertThat(reactive.getStatefulConnection().isOpen()).isTrue();
    }

    @Test
    public void debugSegfault() throws Exception {
        reactive.debugSegfault();
        assertThat(reactive.getStatefulConnection().isOpen()).isTrue();
    }

    @Test
    public void debugRestart() throws Exception {
        reactive.debugRestart(1L);
        assertThat(reactive.getStatefulConnection().isOpen()).isTrue();
    }

    @Test
    public void migrate() throws Exception {
        reactive.migrate("host", 1234, "key", 1, 10);
        assertThat(reactive.getStatefulConnection().isOpen()).isTrue();
    }
}
