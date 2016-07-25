package com.lambdaworks.redis.commands.rx;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.TransactionCommandTest;
import com.lambdaworks.redis.internal.LettuceLists;

import rx.Observable;
import rx.Single;
import rx.observables.BlockingObservable;
import rx.observers.TestSubscriber;

public class TransactionRxCommandTest extends TransactionCommandTest {

    private RedisReactiveCommands<String, String> commands;

    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connect());
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
        redis.close();
    }

    @Test
    public void discard() throws Exception {
        assertThat(block(commands.multi())).isEqualTo("OK");

        commands.set(key, value);

        assertThat(block(commands.discard())).isEqualTo("OK");
        assertThat(block(commands.get(key))).isNull();
    }

    @Test
    public void execSingular() throws Exception {

        assertThat(block(commands.multi())).isEqualTo("OK");

        redis.set(key, value);

        assertThat(first(commands.exec())).isEqualTo("OK");
        assertThat(block(commands.get(key))).isEqualTo(value);
    }

    @Test
    public void errorInMulti() throws Exception {
        commands.multi().subscribe(TestSubscriber.create());
        commands.set(key, value).subscribe(TestSubscriber.create());
        commands.lpop(key).subscribe(TestSubscriber.create());
        commands.get(key).subscribe(TestSubscriber.create());

        List<Object> values = all(commands.exec());
        assertThat(values.get(0)).isEqualTo("OK");
        assertThat(values.get(1) instanceof RedisException).isTrue();
        assertThat(values.get(2)).isEqualTo(value);
    }

    @Test
    public void resultOfMultiIsContainedInCommandObservables() throws Exception {

        TestSubscriber<String> set1 = TestSubscriber.create();
        TestSubscriber<String> set2 = TestSubscriber.create();
        TestSubscriber<String> mget = TestSubscriber.create();
        TestSubscriber<Long> llen = TestSubscriber.create();
        TestSubscriber<Object> exec = TestSubscriber.create();

        commands.multi().subscribe();
        commands.set("key1", "value1").subscribe(set1);
        commands.set("key2", "value2").subscribe(set2);
        commands.mget("key1", "key2").subscribe(mget);
        commands.llen("something").subscribe(llen);
        commands.exec().subscribe(exec);

        exec.awaitTerminalEvent();

        set1.assertValue("OK");
        set2.assertValue("OK");
        mget.assertValues("value1", "value2");
        llen.assertValue(0L);
    }

    @Test
    public void resultOfMultiIsContainedInExecObservable() throws Exception {

        TestSubscriber<Object> exec = TestSubscriber.create();

        commands.multi().subscribe();
        commands.set("key1", "value1").subscribe();
        commands.set("key2", "value2").subscribe();
        commands.mget("key1", "key2").subscribe();
        commands.llen("something").subscribe();
        commands.exec().subscribe(exec);

        exec.awaitTerminalEvent();

        assertThat(exec.getOnNextEvents()).hasSize(4).containsExactly("OK", "OK", list("value1", "value2"), 0L);
    }

    protected <T> T first(Observable<T> observable) {
        BlockingObservable<T> blocking = observable.toBlocking();
        Iterator<T> iterator = blocking.getIterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    protected <T> T block(Single<T> single) {
        return single.toBlocking().value();
    }

    protected <T> List<T> all(Observable<T> observable) {
        BlockingObservable<T> blocking = observable.toBlocking();
        Iterator<T> iterator = blocking.getIterator();
        return LettuceLists.newList(iterator);
    }
}
