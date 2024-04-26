package io.lettuce.core.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.TestSupport;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HLLCommandIntegrationTests extends TestSupport {

    private final RedisCommands<String, String> redis;

    @Inject
    protected HLLCommandIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
    }

    @Test
    void pfadd() {

        assertThat(redis.pfadd(key, value, value)).isEqualTo(1);
        assertThat(redis.pfadd(key, value, value)).isEqualTo(0);
        assertThat(redis.pfadd(key, value)).isEqualTo(0);
    }

    @Test
    void pfaddNoValues() {
        assertThatThrownBy(() -> redis.pfadd(key)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pfaddNullValues() {
        try {
            redis.pfadd(key, null);
            fail("Missing IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            redis.pfadd(key, value, null);
            fail("Missing IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    void pfmerge() {
        redis.pfadd(key, value);
        redis.pfadd("key2", "value2");
        redis.pfadd("key3", "value3");

        assertThat(redis.pfmerge(key, "key2", "key3")).isEqualTo("OK");
        assertThat(redis.pfcount(key)).isEqualTo(3);

        redis.pfadd("key2660", "rand", "mat");
        redis.pfadd("key7112", "mat", "perrin");

        redis.pfmerge("key8885", "key2660", "key7112");

        assertThat(redis.pfcount("key8885")).isEqualTo(3);
    }

    @Test
    void pfmergeNoKeys() {
        assertThatThrownBy(() -> redis.pfmerge(key)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pfcount() {
        redis.pfadd(key, value);
        redis.pfadd("key2", "value2");
        assertThat(redis.pfcount(key)).isEqualTo(1);
        assertThat(redis.pfcount(key, "key2")).isEqualTo(2);
    }

    @Test
    void pfcountNoKeys() {
        assertThatThrownBy(() -> redis.pfcount()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pfaddPfmergePfCount() {

        redis.pfadd("key2660", "rand", "mat");
        redis.pfadd("key7112", "mat", "perrin");

        redis.pfmerge("key8885", "key2660", "key7112");

        assertThat(redis.pfcount("key8885")).isEqualTo(3);
    }

}
