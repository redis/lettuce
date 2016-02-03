package com.lambdaworks.redis.commands.rx;

import static org.assertj.core.api.Assertions.*;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.commands.TransactionCommandTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observables.BlockingObservable;

public class TransactionRxCommandTest extends TransactionCommandTest {

    private RedisReactiveCommands<String, String> commands;

    @Override
    protected RedisCommands<String, String> connect() {
        return RxSyncInvocationHandler.sync(client.connectAsync().getStatefulConnection());
    }

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

    @Test
    public void discard() throws Exception {
        assertThat(first(commands.multi())).isEqualTo("OK");
        commands.set(key, value);
        assertThat(first(commands.discard())).isEqualTo("OK");
        assertThat(first(commands.get(key))).isNull();
    }

    @Test
    public void execSingular() throws Exception {
        assertThat(first(commands.multi())).isEqualTo("OK");
        redis.set(key, value);
        assertThat(first(commands.exec())).isEqualTo("OK");
        assertThat(first(commands.get(key))).isEqualTo(value);
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
