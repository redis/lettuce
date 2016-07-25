package com.lambdaworks.redis.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.api.sync.RedisHLLCommands;

public class HLLCommandTest extends AbstractRedisClientTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private RedisHLLCommands<String, String> commands() {
        return redis;
    }

    @Test
    public void pfadd() throws Exception {

        assertThat(commands().pfadd(key, value, value)).isEqualTo(1);
        assertThat(commands().pfadd(key, value, value)).isEqualTo(0);
        assertThat(commands().pfadd(key, value)).isEqualTo(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pfaddNoValues() throws Exception {
        commands().pfadd(key);
    }

    @Test
    public void pfaddNullValues() throws Exception {
        try {
            commands().pfadd(key, null);
            fail("Missing IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            commands().pfadd(key, value, null);
            fail("Missing IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void pfmerge() throws Exception {
        commands().pfadd(key, value);
        commands().pfadd("key2", "value2");
        commands().pfadd("key3", "value3");

        assertThat(commands().pfmerge(key, "key2", "key3")).isEqualTo("OK");
        assertThat(commands().pfcount(key)).isEqualTo(3);

        commands().pfadd("key2660", "rand", "mat");
        commands().pfadd("key7112", "mat", "perrin");

        commands().pfmerge("key8885", "key2660", "key7112");

        assertThat(commands().pfcount("key8885")).isEqualTo(3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pfmergeNoKeys() throws Exception {
        commands().pfmerge(key);
    }

    @Test
    public void pfcount() throws Exception {
        commands().pfadd(key, value);
        commands().pfadd("key2", "value2");
        assertThat(commands().pfcount(key)).isEqualTo(1);
        assertThat(commands().pfcount(key, "key2")).isEqualTo(2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void pfcountNoKeys() throws Exception {
        commands().pfcount();
    }

}
