package io.lettuce.core.commands;

import static io.lettuce.core.ScriptOutputType.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.FlushMode;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for function commands.
 *
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("FUNCTION")
public class FunctionCommandIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final RedisCommands<String, String> redis;

    @Inject
    protected FunctionCommandIntegrationTests(RedisClient client, RedisCommands<String, String> redis) {
        this.client = client;
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        redis.functionFlush(FlushMode.SYNC);
    }

    @AfterEach
    void tearDown() {

        Wait.untilNoException(() -> {
            try {
                redis.functionKill();
            } catch (RedisException e) {
                // ignore
            }
            redis.ping();
        }).waitOrTimeout();
    }

    @Test
    void fcall() {

        redis.functionLoad("#!lua name=mylib \n redis.register_function('myfunc', function(keys, args) return args[1] end)");
        assertThat((String) redis.fcall("myfunc", STATUS, new String[] { key }, "Hello")).isEqualTo("Hello");
    }

    @Test
    void fcallReadOnly() {

        redis.functionLoad("#!lua name=mylib \n " + "local function my_echo(keys, args) \n" + " return args[1] \n" + "end\n"
                + "redis.register_function{function_name='my_echo',callback=my_echo, flags={ 'no-writes' }}");
        assertThat((String) redis.fcallReadOnly("my_echo", STATUS, new String[] { key }, "Hello")).isEqualTo("Hello");
    }

    @Test
    void functionLoad() {

        redis.functionLoad("#!lua name=mylib \n redis.register_function('myfunc', function(keys, args) return args[1] end)");
        assertThatExceptionOfType(RedisCommandExecutionException.class).isThrownBy(() -> redis.functionLoad(
                "#!lua name=mylib \n redis.register_function('myfunc', function(keys, args) return args[1] end)"));

        String result = redis.functionLoad(
                "#!lua name=mylib \n redis.register_function('myfunc', function(keys, args) return args[1] end)", true);
        assertThat(result).isEqualTo("mylib");
    }

    @Test
    void functionDumpAndRestore() {

        redis.functionLoad("#!lua name=mylib \n redis.register_function('myfunc', function(keys, args) return args[1] end)");
        byte[] dump = redis.functionDump();
        redis.functionFlush(FlushMode.SYNC);
        System.out.println(redis.functionRestore(dump));
    }

    @Test
    void functionList() {

        List<Map<String, Object>> maps = redis.functionList();
        assertThat(maps).isEmpty();

        redis.functionLoad("#!lua name=mylib \n redis.register_function('myfunc', function(keys, args) return args[1] end)");

        redis.functionLoad("#!lua name=my_other_lib \n " + "local function my_echo(keys, args) \n" + " return args[1] \n"
                + "end\n" + "redis.register_function{function_name='my_echo',callback=my_echo, flags={ 'no-writes' }}");

        maps = redis.functionList();
        assertThat(maps).hasSize(2);
    }

}
