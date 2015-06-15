package com.lambdaworks.redis.commands.rx;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.lambdaworks.Delay.delay;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import rx.Observable;
import rx.exceptions.OnErrorNotImplementedException;
import rx.observables.BlockingObservable;

import com.google.common.collect.Lists;
import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.api.rx.RedisReactiveCommands;

public class TransactionRxCommandTest extends AbstractRedisClientTest {

    private RedisReactiveCommands<String, String> commands;

    @Before
    public void openConnection() throws Exception {
        client.setOptions(new ClientOptions.Builder().build());
        redis = connect();
        redis.flushall();
        redis.flushdb();

        commands = redis.getStatefulConnection().reactive();
    }

    @After
    public void closeConnection() throws Exception {
        redis.close();
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void discard() throws Exception {
        assertThat(first(commands.multi())).isEqualTo("OK");
        commands.set(key, value);
        assertThat(first(commands.discard())).isEqualTo("OK");
        assertThat(first(commands.get(key))).isNull();
    }

    @Test
    public void exec() throws Exception {
        assertThat(first(commands.multi())).isEqualTo("OK");
        redis.set(key, value);
        assertThat(all(commands.exec())).isEqualTo(list("OK"));
        assertThat(first(commands.get(key))).isEqualTo(value);
    }

    @Test
    public void execSingular() throws Exception {
        assertThat(first(commands.multi())).isEqualTo("OK");
        redis.set(key, value);
        assertThat(first(commands.exec())).isEqualTo("OK");
        assertThat(first(commands.get(key))).isEqualTo(value);
    }

    @Test
    public void watch() throws Exception {
        assertThat(first(commands.watch(key))).isEqualTo("OK");

        RedisConnection<String, String> redis2 = client.connect();
        redis2.set(key, value + "X");
        redis2.close();

        commands.multi().subscribe();
        commands.append(key, "foo").subscribe();
        assertThat(all(commands.exec())).isEqualTo(list());

    }

    @Test
    public void unwatch() throws Exception {
        assertThat(first(commands.unwatch())).isEqualTo("OK");
    }

    @Test
    public void commandsReturnNullInMulti() throws Exception {
        assertThat(first(commands.multi())).isEqualTo("OK");
        AtomicLong counter = new AtomicLong();
        commands.set(key, value).forEach(s -> counter.incrementAndGet());
        Observable<String> get = commands.get(key);
        get.forEach(s -> counter.incrementAndGet());

        delay(seconds(2));
        assertThat(counter.get()).isEqualTo(0);
        assertThat(all(commands.exec())).isEqualTo(list("OK", value));
        assertThat(first(commands.get(key))).isEqualTo(value);
        assertThat(counter.get()).isEqualTo(2);
        assertThat(first(get)).isEqualTo("value");
        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    public void execmulti() throws Exception {
        first(commands.multi());
        commands.set("one", "1").subscribe();
        commands.set("two", "2").subscribe();
        commands.mget("one", "two").subscribe();
        commands.llen(key).subscribe();
        assertThat(all(commands.exec())).isEqualTo(list("OK", "OK", list("1", "2"), 0L));
    }

    @Test
    public void errorInMulti() throws Exception {
        commands.multi().subscribe();
        commands.set(key, value).subscribe();
        commands.lpop(key).onExceptionResumeNext(Observable.<String> empty()).subscribe();
        commands.get(key).subscribe();
        List<Object> values = all(commands.exec());
        assertThat(values.get(0)).isEqualTo("OK");
        assertThat(values.get(1) instanceof RedisException).isTrue();
        assertThat(values.get(2)).isEqualTo(value);
    }

    @Test(expected = OnErrorNotImplementedException.class)
    public void errorInMultiSafeSubscriber() throws Exception {
        commands.multi().subscribe();
        commands.set(key, value).subscribe();
        commands.lpop(key).subscribe();
        all(commands.exec());
    }

    @Test
    public void execWithoutMulti() throws Exception {
        exception.expect(RedisException.class);
        exception.expectMessage("ERR EXEC without MULTI");
        commands.exec().toBlocking().first();
    }

    protected <T> T first(Observable<T> observable) {
        BlockingObservable<T> blocking = observable.toBlocking();
        Iterator<T> iterator = blocking.getIterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    protected <T> List<T> all(Observable<T> observable) {
        BlockingObservable<T> blocking = observable.toBlocking();
        Iterator<T> iterator = blocking.getIterator();
        return Lists.newArrayList(iterator);
    }
}
