/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * @author Mark Paluch
 * @since 3.0
 */
class SyncAsyncApiConvergenceUnitTests {

    @SuppressWarnings("rawtypes")
    private Class<RedisAsyncCommands> asyncClass = RedisAsyncCommands.class;

    static Stream<Method> parameters() {
        return Arrays.stream(RedisCommands.class.getMethods());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void testMethodPresentOnAsyncApi(Method syncMethod) throws Exception {

        Method method = RedisAsyncCommands.class.getMethod(syncMethod.getName(), syncMethod.getParameterTypes());
        assertThat(method).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void testMethodPresentOnReactiveApi(Method syncMethod) throws Exception {

        Method method = RedisReactiveCommands.class.getMethod(syncMethod.getName(), syncMethod.getParameterTypes());
        assertThat(method).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void testSameResultType(Method syncMethod) throws Exception {

        Method method = asyncClass.getMethod(syncMethod.getName(), syncMethod.getParameterTypes());
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

        assertThat(returnType.toString()).describedAs(syncMethod.toString())
                .isEqualTo(syncMethod.getGenericReturnType().toString());
    }

}
