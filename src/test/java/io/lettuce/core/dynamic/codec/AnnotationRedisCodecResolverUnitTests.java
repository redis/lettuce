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
package io.lettuce.core.dynamic.codec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.lettuce.core.Range;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.dynamic.CommandMethod;
import io.lettuce.core.dynamic.DeclaredCommandMethod;
import io.lettuce.core.dynamic.annotation.Key;
import io.lettuce.core.dynamic.annotation.Value;
import io.lettuce.core.dynamic.support.ReflectionUtils;

/**
 * Unit tests for {@link AnnotationRedisCodecResolver}.
 *
 * @author Mark Paluch
 * @author Manyanda Chitimbo
 */
class AnnotationRedisCodecResolverUnitTests {

    private List<RedisCodec<?, ?>> codecs = Arrays.asList(new StringCodec(), new ByteArrayCodec());

    @Test
    void shouldResolveFullyHinted() {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, "stringOnly", String.class, String.class);
        RedisCodec<?, ?> codec = resolve(method);

        assertThat(codec).isInstanceOf(StringCodec.class);
    }

    @Test
    void shouldResolveHintedKey() {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, "annotatedKey", String.class, String.class);
        RedisCodec<?, ?> codec = resolve(method);

        assertThat(codec).isInstanceOf(StringCodec.class);
    }

    @Test
    void shouldResolveHintedValue() {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, "annotatedValue", String.class, String.class);
        RedisCodec<?, ?> codec = resolve(method);

        assertThat(codec).isInstanceOf(StringCodec.class);
    }

    @Test
    void shouldResolveWithoutHints() {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, "nothingAnnotated", String.class, String.class);
        RedisCodec<?, ?> codec = resolve(method);

        assertThat(codec).isInstanceOf(StringCodec.class);
    }

    @Test
    void shouldResolveHintedByteArrayValue() {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, "annotatedByteArrayValue", String.class, byte[].class);
        RedisCodec<?, ?> codec = resolve(method);

        assertThat(codec).isInstanceOf(ByteArrayCodec.class);
    }

    @Test
    void resolutionOfMethodWithMixedTypesShouldFail() {
        Method method = ReflectionUtils.findMethod(CommandMethods.class, "mixedTypes", String.class, byte[].class);
        assertThatThrownBy(() -> resolve(method)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void resolutionOfMethodWithMixedCodecsShouldFail() {
        Method method = ReflectionUtils.findMethod(CommandMethods.class, "mixedCodecs", String.class, byte[].class,
                String.class);
        assertThatThrownBy(() -> resolve(method)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldDiscoverCodecTypesFromWrappers() {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, "withWrappers", Range.class,
                io.lettuce.core.Value.class);

        Set<Class<?>> types = new AnnotationRedisCodecResolver(codecs).findTypes(DeclaredCommandMethod.create(method),
                Value.class);

        assertThat(types).contains(String.class, Number.class);
    }

    RedisCodec<?, ?> resolve(Method method) {

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

        String mixedCodecs(@Key String key1, @Key byte[] key2, @Value String value);

        String withWrappers(@Value Range<String> range, @Value io.lettuce.core.Value<Number> value);

        String withMap(Map<Integer, String> map);

    }

}
