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
package io.lettuce.core.dynamic.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.dynamic.CommandMethod;
import io.lettuce.core.dynamic.DeclaredCommandMethod;
import io.lettuce.core.dynamic.support.ReflectionUtils;
import io.lettuce.core.output.*;

/**
 * @author Mark Paluch
 */
class CodecAwareOutputResolverUnitTests {

    private CodecAwareOutputFactoryResolver resolver = new CodecAwareOutputFactoryResolver(
            new OutputRegistryCommandOutputFactoryResolver(new OutputRegistry()), new ByteBufferAndStringCodec());

    @Test
    void shouldResolveValueOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("string");

        assertThat(commandOutput).isInstanceOf(ValueOutput.class);
    }

    @Test
    void shouldResolveValueListOutput() {

        assertThat(getCommandOutput("stringList")).isOfAnyClassIn(ValueListOutput.class, StringListOutput.class);
        assertThat(getCommandOutput("charSequenceList")).isOfAnyClassIn(ValueListOutput.class, StringListOutput.class);
    }

    @Test
    void shouldResolveKeyOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("byteBuffer");

        assertThat(commandOutput).isInstanceOf(KeyOutput.class);
    }

    @Test
    void shouldResolveKeyListOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("byteBufferList");

        assertThat(commandOutput).isInstanceOf(KeyListOutput.class);
    }

    @Test
    void shouldResolveListOfMapsOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("listOfMapsOutput");

        assertThat(commandOutput).isInstanceOf(ListOfMapsOutput.class);
    }

    @Test
    void shouldResolveMapsOutput() {

        CommandOutput<?, ?, ?> commandOutput = getCommandOutput("mapOutput");

        assertThat(commandOutput).isInstanceOf(MapOutput.class);
    }

    CommandOutput<?, ?, ?> getCommandOutput(String methodName) {

        Method method = ReflectionUtils.findMethod(CommandMethods.class, methodName);
        CommandMethod commandMethod = DeclaredCommandMethod.create(method);

        CommandOutputFactory factory = resolver
                .resolveCommandOutput(new OutputSelector(commandMethod.getReturnType(), new ByteBufferAndStringCodec()));

        return factory.create(new ByteBufferAndStringCodec());
    }

    private static interface CommandMethods {

        List<String> stringList();

        List<? extends CharSequence> charSequenceList();

        List<ByteBuffer> byteBufferList();

        List<Map<ByteBuffer, String>> listOfMapsOutput();

        Map<ByteBuffer, String> mapOutput();

        String string();

        ByteBuffer byteBuffer();

    }

    private static class ByteBufferAndStringCodec implements RedisCodec<ByteBuffer, String> {

        @Override
        public ByteBuffer decodeKey(ByteBuffer bytes) {
            return null;
        }

        @Override
        public String decodeValue(ByteBuffer bytes) {
            return null;
        }

        @Override
        public ByteBuffer encodeKey(ByteBuffer key) {
            return null;
        }

        @Override
        public ByteBuffer encodeValue(String value) {
            return null;
        }

    }

}
