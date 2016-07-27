// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.lambdaworks.redis.TransactionResult;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.RedisCommandExecutionException;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.api.sync.RedisCommands;

public class TransactionCommandTest extends AbstractRedisClientTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void discard() throws Exception {
        assertThat(redis.multi()).isEqualTo("OK");
        redis.set(key, value);
        assertThat(redis.discard()).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
    }

    @Test
    public void exec() throws Exception {
        assertThat(redis.multi()).isEqualTo("OK");
        redis.set(key, value);
        assertThat(redis.exec()).contains("OK");
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void watch() throws Exception {
        assertThat(redis.watch(key)).isEqualTo("OK");

        RedisCommands<String, String> redis2 = client.connect().sync();
        redis2.set(key, value + "X");
        redis2.getStatefulConnection().close();

        redis.multi();
        redis.append(key, "foo");

        TransactionResult transactionResult = redis.exec();

        assertThat(transactionResult.wasRolledBack()).isTrue();
        assertThat(transactionResult).isEmpty();

    }

    @Test
    public void unwatch() throws Exception {
        assertThat(redis.unwatch()).isEqualTo("OK");
    }

    @Test
    public void commandsReturnNullInMulti() throws Exception {

        assertThat(redis.multi()).isEqualTo("OK");
        assertThat(redis.set(key, value)).isNull();
        assertThat(redis.get(key)).isNull();

        TransactionResult exec = redis.exec();
        assertThat(exec.wasRolledBack()).isFalse();
        assertThat(exec).contains("OK", value);

        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void execmulti() throws Exception {
        redis.multi();
        redis.set("one", "1");
        redis.set("two", "2");
        redis.mget("one", "two");
        redis.llen(key);
        assertThat(redis.exec()).contains("OK", "OK", list(kv("one", "1"), kv("two", "2")), 0L);
    }

    @Test
    public void emptyMulti() throws Exception {
        redis.multi();
        TransactionResult exec = redis.exec();
        assertThat(exec.wasRolledBack()).isFalse();
        assertThat(exec).isEmpty();
    }

    @Test
    public void errorInMulti() throws Exception {
        redis.multi();
        redis.set(key, value);
        redis.lpop(key);
        redis.get(key);
        TransactionResult values = redis.exec();
        assertThat(values.wasRolledBack()).isFalse();
        assertThat((String) values.get(0)).isEqualTo("OK");
        assertThat(values.get(1) instanceof RedisException).isTrue();
        assertThat((String) values.get(2)).isEqualTo(value);
    }

    @Test
    public void execWithoutMulti() throws Exception {
        exception.expect(RedisCommandExecutionException.class);
        exception.expectMessage("ERR EXEC without MULTI");
        redis.exec();
    }
}
