package com.lambdaworks.redis.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.lambdaworks.redis.GeoCoordinates;
import com.lambdaworks.redis.KeyValue;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.dynamic.segment.CommandSegment;
import com.lambdaworks.redis.dynamic.segment.CommandSegments;
import com.lambdaworks.redis.dynamic.support.ReflectionUtils;
import com.lambdaworks.redis.internal.LettuceLists;
import com.lambdaworks.redis.models.command.CommandDetail;

/**
 * @author Mark Paluch
 */
public class CommandMethodVerifierTest {

    private CommandMethodVerifier sut;

    @Before
    public void before() {

        CommandDetail mget = new CommandDetail("mget", -2, null, 0, 0, 0);
        CommandDetail randomkey = new CommandDetail("randomkey", 1, null, 0, 0, 0);
        CommandDetail rpop = new CommandDetail("rpop", 2, null, 0, 0, 0);
        CommandDetail set = new CommandDetail("set", 3, null, 0, 0, 0);
        CommandDetail geoadd = new CommandDetail("geoadd", -4, null, 0, 0, 0);

        sut = new CommandMethodVerifier(LettuceLists.newList(mget, randomkey, rpop, set, geoadd));
    }

    @Test
    public void misspelledName() {

        try {
            validateMethod("megt");
            fail("Missing CommandMethodSyntaxException");
        } catch (CommandMethodSyntaxException e) {
            assertThat(e).hasMessageContaining("Command megt does not exist. Did you mean: MGET, SET?");
        }
    }

    @Test
    public void tooFewAtLeastParameters() {

        try {
            validateMethod("mget");
            fail("Missing CommandMethodSyntaxException");
        } catch (CommandMethodSyntaxException e) {
            assertThat(e)
                    .hasMessageContaining("Command MGET requires at least 1 parameters but method declares 0 parameter(s)");
        }
    }

    @Test
    public void shouldPassWithCorrectParameterCount() {

        validateMethod("mget", String.class);
        validateMethod("randomkey");
        validateMethod("rpop", String.class);
        validateMethod("set", KeyValue.class);
        validateMethod("geoadd", String.class, String.class, GeoCoordinates.class);
    }

    @Test
    public void tooManyParameters() {

        try {
            validateMethod("rpop", String.class, String.class);
            fail("Missing CommandMethodSyntaxException");
        } catch (CommandMethodSyntaxException e) {
            assertThat(e).hasMessageContaining("Command RPOP accepts 1 parameters but method declares 2 parameter(s)");
        }
    }

    @Test
    public void methodDoesNotAcceptParameters() {

        try {
            validateMethod("randomkey", String.class);
            fail("Missing CommandMethodSyntaxException");
        } catch (CommandMethodSyntaxException e) {
            assertThat(e).hasMessageContaining("Command RANDOMKEY accepts no parameters");
        }
    }

    private void validateMethod(String methodName, Class<?>... parameterTypes) {

        Method method = ReflectionUtils.findMethod(MyInterface.class, methodName, parameterTypes);
        CommandMethod commandMethod = new CommandMethod(method);

        sut.validate(new CommandSegments(Arrays.asList(CommandSegment.constant(methodName))), commandMethod);
    }

    static interface MyInterface {

        void megt();

        void mget();

        void mget(String key);

        void mget(String key1, String key2);

        void set(KeyValue<String, String> keyValue);

        void geoadd(String key, String member, GeoCoordinates geoCoordinates);

        void randomkey();

        void randomkey(String key);

        void rpop(String key);

        void rpop(String key1, String key2);
    }
}
