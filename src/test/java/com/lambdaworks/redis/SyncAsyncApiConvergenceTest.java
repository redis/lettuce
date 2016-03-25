package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Mark Paluch
 * @since 3.0
 */
@RunWith(Parameterized.class)
public class SyncAsyncApiConvergenceTest {

    private Method method;

    @SuppressWarnings("rawtypes")
    private Class<RedisAsyncCommands> asyncClass = RedisAsyncCommands.class;

    @Parameterized.Parameters(name = "Method {0}/{1}")
    public static List<Object[]> parameters() {

        List<Object[]> result = new ArrayList<Object[]>();
        Method[] methods = RedisCommands.class.getMethods();
        for (Method method : methods) {
            result.add(new Object[] { method.getName(), method });
        }

        return result;
    }

    public SyncAsyncApiConvergenceTest(String methodName, Method method) {
        this.method = method;
    }

    @Test
    public void testMethodPresentOnAsyncApi() throws Exception {
        Method method = asyncClass.getMethod(this.method.getName(), this.method.getParameterTypes());
        assertThat(method).isNotNull();
    }

    @Test
    public void testSameResultType() throws Exception {
        Method method = asyncClass.getMethod(this.method.getName(), this.method.getParameterTypes());
        Type returnType = method.getGenericReturnType();

        if (method.getReturnType().equals(RedisFuture.class)) {
            ParameterizedType genericReturnType = (ParameterizedType) method.getGenericReturnType();
            Type[] actualTypeArguments = genericReturnType.getActualTypeArguments();

            if (actualTypeArguments[0] instanceof GenericArrayType) {
                GenericArrayType arrayType = (GenericArrayType) actualTypeArguments[0];
                returnType = Array.newInstance((Class<?>) arrayType.getGenericComponentType(), 0).getClass();
            } else {
                returnType = actualTypeArguments[0];
            }
        }

        assertThat(returnType.toString()).describedAs(this.method.toString()).isEqualTo(
                this.method.getGenericReturnType().toString());
    }
}
