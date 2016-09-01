package com.lambdaworks.redis.commands.reactive;

import com.lambdaworks.redis.reactive.TestSubscriber;
import org.junit.Test;

import com.lambdaworks.redis.api.reactive.RedisReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.CustomCommandTest;
import com.lambdaworks.redis.output.ValueListOutput;
import com.lambdaworks.redis.output.ValueOutput;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

import reactor.core.publisher.Flux;

/**
 * @author Mark Paluch
 */
public class CustomReactiveCommandTest extends CustomCommandTest {

    @Override
    protected RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }

    @Test
    public void dispatchGetAndSet() throws Exception {

        redis.set(key, value);
        RedisReactiveCommands<String, String> reactive = redis.getStatefulConnection().reactive();

        Flux<String> flux = reactive.dispatch(CommandType.GET, new ValueOutput<>(utf8StringCodec),
                new CommandArgs<>(utf8StringCodec).addKey(key));

        TestSubscriber<String> testSubscriber = TestSubscriber.subscribe(flux);
        testSubscriber.awaitAndAssertNextValues(value).assertComplete().assertNoError();
    }

    @Test
    public void dispatchList() throws Exception {

        redis.rpush(key, "a", "b", "c");
        RedisReactiveCommands<String, String> reactive = redis.getStatefulConnection().reactive();

        Flux<String> flux = reactive.dispatch(CommandType.LRANGE, new ValueListOutput<>(utf8StringCodec),
                new CommandArgs<>(utf8StringCodec).addKey(key).add(0).add(-1));

        TestSubscriber<String> testSubscriber = TestSubscriber.subscribe(flux);
        testSubscriber.awaitAndAssertNextValues("a", "b", "c").assertComplete().assertNoError();
    }

}
