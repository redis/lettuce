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
package io.lettuce.core.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.codec.Utf8StringCodec;

/**
 * @author Mark Paluch
 */
class ListOutputUnitTests {

    static Collection<Fixture> parameters() {

        KeyListOutput<String, String> keyListOutput = new KeyListOutput<>(StringCodec.UTF8);
        Fixture keyList = new Fixture(keyListOutput, keyListOutput, "hello world".getBytes(), "hello world");

        ValueListOutput<String, String> valueListOutput = new ValueListOutput<>(StringCodec.UTF8);
        Fixture valueList = new Fixture(valueListOutput, valueListOutput, "hello world".getBytes(), "hello world");

        StringListOutput<String, String> stringListOutput = new StringListOutput<>(StringCodec.UTF8);
        Fixture stringList = new Fixture(stringListOutput, stringListOutput, "hello world".getBytes(), "hello world");

        return Arrays.asList(keyList, valueList, stringList);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void settingEmptySubscriberShouldFail(Fixture fixture) {
        assertThatThrownBy(() -> fixture.streamingOutput.setSubscriber(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void defaultSubscriberIsSet(Fixture fixture) {
        fixture.commandOutput.multi(1);
        assertThat(fixture.streamingOutput.getSubscriber()).isNotNull().isInstanceOf(ListSubscriber.class);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void setIntegerShouldFail(Fixture fixture) {
        assertThatThrownBy(() -> fixture.commandOutput.set(123L)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void setValueShouldConvert(Fixture fixture) {

        fixture.commandOutput.multi(1);
        fixture.commandOutput.set(ByteBuffer.wrap(fixture.valueBytes));

        assertThat(fixture.commandOutput.get()).contains(fixture.value);
    }

    static class Fixture {

        final CommandOutput<Object, Object, List<Object>> commandOutput;

        final StreamingOutput<?> streamingOutput;

        final byte[] valueBytes;

        final Object value;

        Fixture(CommandOutput<?, ?, ?> commandOutput, StreamingOutput<?> streamingOutput, byte[] valueBytes, Object value) {

            this.commandOutput = (CommandOutput) commandOutput;
            this.streamingOutput = streamingOutput;
            this.valueBytes = valueBytes;
            this.value = value;
        }

        @Override
        public String toString() {
            return commandOutput.getClass().getSimpleName() + "/" + value;
        }

    }

}
