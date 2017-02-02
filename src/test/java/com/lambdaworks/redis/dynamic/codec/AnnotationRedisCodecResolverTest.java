/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.dynamic.codec;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.lambdaworks.redis.Range;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.dynamic.CommandMethod;
import com.lambdaworks.redis.dynamic.DeclaredCommandMethod;
import com.lambdaworks.redis.dynamic.annotation.Key;
import com.lambdaworks.redis.dynamic.annotation.Value;
import com.lambdaworks.redis.dynamic.support.ReflectionUtils;

/**
 * @author Mark Paluch
 */
public class AnnotationRedisCodecResolverTest {

    private List<RedisCodec<?, ?>> codecs = Arrays.asList(new StringCodec(), new ByteArrayCodec());

    @Test
    public void shouldResolveFullyHinted() {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, "stringOnly", String.class, String.class);
        RedisCodec<?, ?> codec = resolve(method);

        assertThat(codec).isInstanceOf(StringCodec.class);
    }

    @Test
    public void shouldResolveHintedKey() {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, "annotatedKey", String.class, String.class);
        RedisCodec<?, ?> codec = resolve(method);

        assertThat(codec).isInstanceOf(StringCodec.class);
    }

    @Test
    public void shouldResolveHintedValue() {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, "annotatedValue", String.class, String.class);
        RedisCodec<?, ?> codec = resolve(method);

        assertThat(codec).isInstanceOf(StringCodec.class);
    }

    @Test
    public void shouldResolveWithoutHints() {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, "nothingAnnotated", String.class, String.class);
        RedisCodec<?, ?> codec = resolve(method);

        assertThat(codec).isInstanceOf(StringCodec.class);
    }

    @Test
    public void shouldResolveHintedByteArrayValue() {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, "annotatedByteArrayValue", String.class, byte[].class);
        RedisCodec<?, ?> codec = resolve(method);

        assertThat(codec).isInstanceOf(ByteArrayCodec.class);
    }

    @Test(expected = IllegalStateException.class)
    public void resolutionShouldFail() {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, "mixedTypes", String.class, byte[].class);
        resolve(method);
    }

    @Test
    public void shouldDiscoverCodecTypesFromWrappers() {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, "withWrappers", Range.class,
                com.lambdaworks.redis.Value.class);

        Set<Class<?>> types = new AnnotationRedisCodecResolver(codecs).findTypes(DeclaredCommandMethod.create(method),
                Value.class);

        assertThat(types).contains(String.class, Number.class);
    }

    protected RedisCodec<?, ?> resolve(Method method) {

        CommandMethod commandMethod = DeclaredCommandMethod.create(method);
        AnnotationRedisCodecResolver resolver = new AnnotationRedisCodecResolver(codecs);

        return resolver.resolve(commandMethod);
    }

    private static interface CommandMethods {

        String stringOnly(@Key String key, @Value String value);

        String annotatedKey(@Key String key, String value);

        String annotatedValue(String key, @Value String value);

        String annotatedByteArrayValue(String key, @Value byte[] value);

        String nothingAnnotated(String key, String value);

        String mixedTypes(@Key String key, @Value byte[] value);

        String withWrappers(@Value Range<String> range, @Value com.lambdaworks.redis.Value<Number> value);

        String withMap(Map<Integer, String> map);
    }

}
