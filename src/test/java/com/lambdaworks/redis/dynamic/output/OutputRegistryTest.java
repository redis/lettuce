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
package com.lambdaworks.redis.dynamic.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.List;

import org.junit.Test;

import com.lambdaworks.redis.ScoredValue;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.dynamic.support.ClassTypeInformation;
import com.lambdaworks.redis.dynamic.support.ResolvableType;
import com.lambdaworks.redis.output.*;

/**
 * @author Mark Paluch
 */
public class OutputRegistryTest {

    @Test
    public void getKeyOutputType() throws Exception {

        OutputType outputComponentType = OutputRegistry.getOutputComponentType(KeyListOutput.class);

        assertThat(outputComponentType.getTypeInformation().getComponentType().getType()).isEqualTo(Object.class);
    }

    @Test
    public void getStringListOutputType() throws Exception {

        OutputType outputComponentType = OutputRegistry.getOutputComponentType(StringListOutput.class);

        assertThat(outputComponentType.getTypeInformation().getComponentType())
                .isEqualTo(ClassTypeInformation.from(String.class));
    }

    @Test
    public void componentTypeOfKeyOuputWithCodecIsAssignableFromString() throws Exception {

        OutputType outputComponentType = OutputRegistry.getOutputComponentType(KeyOutput.class);

        ResolvableType resolvableType = outputComponentType.withCodec(new StringCodec());

        assertThat(resolvableType.isAssignableFrom(String.class)).isTrue();
    }

    @Test
    public void componentTypeOfKeyListOuputWithCodecIsAssignableFromListOfString() throws Exception {

        OutputType outputComponentType = OutputRegistry.getOutputComponentType(KeyListOutput.class);

        ResolvableType resolvableType = outputComponentType.withCodec(new StringCodec());

        assertThat(resolvableType.isAssignableFrom(ResolvableType.forClassWithGenerics(List.class, String.class))).isTrue();
    }

    @Test
    public void streamingTypeOfKeyOuputWithCodecIsAssignableFromString() throws Exception {

        OutputType outputComponentType = OutputRegistry.getStreamingType(KeyListOutput.class);

        ResolvableType resolvableType = outputComponentType.withCodec(new StringCodec());

        assertThat(resolvableType.isAssignableFrom(ResolvableType.forClass(String.class))).isTrue();
    }

    @Test
    public void streamingTypeOfKeyListOuputWithCodecIsAssignableFromListOfString() throws Exception {

        OutputType outputComponentType = OutputRegistry.getStreamingType(ScoredValueListOutput.class);

        ResolvableType resolvableType = outputComponentType.withCodec(new StringCodec());

        assertThat(resolvableType.isAssignableFrom(ResolvableType.forClassWithGenerics(ScoredValue.class, String.class)))
                .isTrue();
    }

    @Test
    public void customizedValueOutput() throws Exception {

        OutputType outputComponentType = OutputRegistry.getOutputComponentType(KeyTypedOutput.class);

        ResolvableType resolvableType = outputComponentType.withCodec(ByteArrayCodec.INSTANCE);

        assertThat(resolvableType.isAssignableFrom(ResolvableType.forClass(byte[].class))).isTrue();
    }

    private static abstract class IntermediateOutput<K1, V1> extends CommandOutput<K1, V1, V1> {

        public IntermediateOutput(RedisCodec<K1, V1> codec, V1 output) {
            super(codec, null);
        }
    }

    private static class KeyTypedOutput extends IntermediateOutput<ByteBuffer, byte[]> {

        public KeyTypedOutput(RedisCodec<ByteBuffer, byte[]> codec) {
            super(codec, null);
        }
    }
}
