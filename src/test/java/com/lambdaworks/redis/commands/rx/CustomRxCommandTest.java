/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.commands.rx;

import org.junit.Test;

import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.CustomCommandTest;
import com.lambdaworks.redis.output.ValueListOutput;
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

    @Test
    public void dispatchList() throws Exception {

        redis.rpush(key, "a", "b", "c");
        RedisReactiveCommands<String, String> reactive = redis.getStatefulConnection().reactive();

        Observable<String> observable = reactive.dispatch(CommandType.LRANGE, new ValueListOutput<>(utf8StringCodec),
                new CommandArgs<>(utf8StringCodec).addKey(key).add(0).add(-1));

        TestSubscriber<String> testSubscriber = TestSubscriber.create();
        observable.subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertValues("a", "b", "c");
    }
}
