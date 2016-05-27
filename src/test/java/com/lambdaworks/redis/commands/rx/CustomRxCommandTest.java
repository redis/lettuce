package com.lambdaworks.redis.commands.rx;

import org.junit.Test;

import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.CustomCommandTest;
import com.lambdaworks.redis.output.ValueOutput;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandType;

import rx.Observable;
import rx.observers.TestSubscriber;

/**
 * @author Mark Paluch
 */
public class CustomRxCommandTest extends CustomCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connectAsync().getStatefulConnection());
    }

    @Test
    public void dispatchGetAndSet() throws Exception {

        redis.set(key, value);
        RedisReactiveCommands<String, String> reactive = redis.getStatefulConnection().reactive();

        Observable<String> observable = reactive.dispatch(CommandType.GET, new ValueOutput<>(utf8StringCodec),
                new CommandArgs<>(utf8StringCodec).addKey(key));

        TestSubscriber<String> testSubscriber = TestSubscriber.create();
        observable.subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertValue(value);
    }

}
