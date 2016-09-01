package com.lambdaworks.redis.commands.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.lambdaworks.redis.reactive.TestSubscriber;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.KeyValue;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.TransactionResult;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.reactive.RedisReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.TransactionCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TransactionReactiveCommandTest extends TransactionCommandTest {

    private RedisReactiveCommands<String, String> commands;

    @Override
    protected RedisCommands<String, String> connect() {
        return ReactiveSyncInvocationHandler.sync(client.connect());
    }

    @Before
    public void openConnection() throws Exception {
        client.setOptions(ClientOptions.builder().build());
        redis = connect();
        redis.flushall();
        redis.flushdb();

        commands = redis.getStatefulConnection().reactive();
    }

    @After
    public void closeConnection() throws Exception {
        redis.getStatefulConnection().close();
    }

    @Test
    public void discard() throws Exception {
        assertThat(block(commands.multi())).isEqualTo("OK");

        commands.set(key, value);

        assertThat(block(commands.discard())).isEqualTo("OK");
        assertThat(block(commands.get(key))).isNull();
    }

    @Test
    public void watchRollback() throws Exception {

        StatefulRedisConnection<String, String> otherConnection = client.connect();

        otherConnection.sync().set(key, value);

        assertThat(block(commands.watch(key))).isEqualTo("OK");
        assertThat(block(commands.multi())).isEqualTo("OK");

        commands.set(key, value);

        otherConnection.sync().del(key);

        TransactionResult transactionResult = block(commands.exec());
        assertThat(transactionResult).isNotNull();
        assertThat(transactionResult.wasRolledBack()).isTrue();
    }

    @Test
    public void execSingular() throws Exception {

        assertThat(block(commands.multi())).isEqualTo("OK");

        redis.set(key, value);

        assertThat(block(commands.exec())).contains("OK");
        assertThat(block(commands.get(key))).isEqualTo(value);
    }

    @Test
    public void errorInMulti() throws Exception {
        commands.multi().subscribe(TestSubscriber.create());
        commands.set(key, value).subscribe(TestSubscriber.create());
        commands.lpop(key).subscribe(TestSubscriber.create());
        commands.get(key).subscribe(TestSubscriber.create());

        TransactionResult values = block(commands.exec());
        assertThat((String) values.get(0)).isEqualTo("OK");
        assertThat(values.get(1) instanceof RedisException).isTrue();
        assertThat((String) values.get(2)).isEqualTo(value);
    }

    @Test
    public void resultOfMultiIsContainedInCommandFlux() throws Exception {

        TestSubscriber<String> set1 = TestSubscriber.create();
        TestSubscriber<String> set2 = TestSubscriber.create();
        TestSubscriber<KeyValue<String, String>> mget = TestSubscriber.create();
        TestSubscriber<Long> llen = TestSubscriber.create();
        TestSubscriber<Object> exec = TestSubscriber.create();

        commands.multi().subscribe();
        commands.set("key1", "value1").subscribe(set1);
        commands.set("key2", "value2").subscribe(set2);
        commands.mget("key1", "key2").subscribe(mget);
        commands.llen("something").subscribe(llen);
        commands.exec().subscribe(exec);

        exec.await();

        set1.awaitAndAssertNextValues("OK");
        set2.awaitAndAssertNextValues("OK");
        mget.assertValues(KeyValue.just("key1", "value1"), KeyValue.just("key2", "value2"));
        llen.awaitAndAssertNextValues(0L);
    }

    @Test
    public void resultOfMultiIsContainedInExecObservable() throws Exception {

        TestSubscriber<TransactionResult> exec = TestSubscriber.create();

        commands.multi().subscribe();
        commands.set("key1", "value1").subscribe();
        commands.set("key2", "value2").subscribe();
        commands.mget("key1", "key2").subscribe();
        commands.llen("something").subscribe();
        commands.exec().subscribe(exec);

        exec.awaitAndAssertNextValuesWith(object -> {
            assertThat(object).hasSize(4).containsExactly("OK", "OK", list(kv("key1", "value1"), kv("key2", "value2")), 0L);
        }).assertNoError();
    }

    protected <T> T block(Mono<T> mono) {
        return mono.block();
    }

    protected <T> List<T> all(Flux<T> flux) {
        return flux.collectList().block();
    }
}
