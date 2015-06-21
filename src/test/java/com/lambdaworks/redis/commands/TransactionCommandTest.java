// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.commands;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.RedisCommandExecutionException;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisException;

public class TransactionCommandTest extends AbstractRedisClientTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void discard() throws Exception {
        Assertions.assertThat(redis.multi()).isEqualTo("OK");
        redis.set(key, value);
        Assertions.assertThat(redis.discard()).isEqualTo("OK");
        Assertions.assertThat(redis.get(key)).isNull();
    }

    @Test
    public void exec() throws Exception {
        Assertions.assertThat(redis.multi()).isEqualTo("OK");
        redis.set(key, value);
        Assertions.assertThat(redis.exec()).isEqualTo(list("OK"));
        Assertions.assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void watch() throws Exception {
        Assertions.assertThat(redis.watch(key)).isEqualTo("OK");

        RedisConnection<String, String> redis2 = client.connect().sync();
        redis2.set(key, value + "X");
        redis2.close();

        redis.multi();
        redis.append(key, "foo");
        Assertions.assertThat(redis.exec()).isEqualTo(list());

    }

    @Test
    public void unwatch() throws Exception {
        Assertions.assertThat(redis.unwatch()).isEqualTo("OK");
    }

    @Test
    public void commandsReturnNullInMulti() throws Exception {
        Assertions.assertThat(redis.multi()).isEqualTo("OK");
        Assertions.assertThat(redis.set(key, value)).isNull();
        Assertions.assertThat(redis.get(key)).isNull();
        Assertions.assertThat(redis.exec()).isEqualTo(list("OK", value));
        Assertions.assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void execmulti() throws Exception {
        redis.multi();
        redis.set("one", "1");
        redis.set("two", "2");
        redis.mget("one", "two");
        redis.llen(key);
        Assertions.assertThat(redis.exec()).isEqualTo(list("OK", "OK", list("1", "2"), 0L));
    }

    @Test
    public void errorInMulti() throws Exception {
        redis.multi();
        redis.set(key, value);
        redis.lpop(key);
        redis.get(key);
        List<Object> values = redis.exec();
        assertThat(values.get(0)).isEqualTo("OK");
        assertThat(values.get(1) instanceof RedisException).isTrue();
        assertThat(values.get(2)).isEqualTo(value);
    }

    @Test
    public void execWithoutMulti() throws Exception {
        exception.expect(RedisCommandExecutionException.class);
        exception.expectMessage("ERR EXEC without MULTI");
        redis.exec();
    }

}
