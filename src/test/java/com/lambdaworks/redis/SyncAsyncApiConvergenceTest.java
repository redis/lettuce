package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
@RunWith(Parameterized.class)
public class SyncAsyncApiConvergenceTest {

    private Method method;

    @SuppressWarnings("rawtypes")
    private Class<RedisAsyncConnection> asyncClass = RedisAsyncConnection.class;

    @Parameterized.Parameters(name = "Method {0}/{1}")
    public static List<Object[]> parameters() {

        List<Object[]> result = new ArrayList<Object[]>();
        Method[] methods = RedisConnection.class.getMethods();
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
        Class<?> returnType = method.getReturnType();

        if (returnType.equals(RedisFuture.class)) {
            ParameterizedType genericReturnType = (ParameterizedType) method.getGenericReturnType();
            Type[] actualTypeArguments = genericReturnType.getActualTypeArguments();

            if (actualTypeArguments[0] instanceof TypeVariable) {

                assertThat(Object.class).isEqualTo(this.method.getReturnType());
                return;
            }

            if (actualTypeArguments[0] instanceof ParameterizedType) {

                ParameterizedType parameterizedType = (ParameterizedType) actualTypeArguments[0];
                returnType = (Class<?>) parameterizedType.getRawType();
            } else if (actualTypeArguments[0] instanceof GenericArrayType) {

                GenericArrayType arrayType = (GenericArrayType) actualTypeArguments[0];
                returnType = Array.newInstance((Class<?>) arrayType.getGenericComponentType(), 0).getClass();
            } else {
                returnType = (Class<?>) actualTypeArguments[0];
            }
        }

        assertThat(returnType).isEqualTo(this.method.getReturnType());

    }
}
