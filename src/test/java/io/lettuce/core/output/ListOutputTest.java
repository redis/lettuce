/*
 * Copyright 2011-2017 the original author or authors.
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
package io.lettuce.core.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import io.lettuce.core.codec.Utf8StringCodec;

/**
 * @author Mark Paluch
 */
@RunWith(Parameterized.class)
public class ListOutputTest {

    @Parameter(0)
    public CommandOutput<Object, Object, List<Object>> commandOutput;

    @Parameter(1)
    public StreamingOutput<?> streamingOutput;

    @Parameter(2)
    public byte[] valueBytes;

    @Parameter(3)
    public Object value;

    @Parameters
    public static Collection<Object[]> parameters() {

        Utf8StringCodec codec = new Utf8StringCodec();

        KeyListOutput<String, String> keyListOutput = new KeyListOutput<>(codec);
        Object[] keyList = new Object[] { keyListOutput, keyListOutput, "hello world".getBytes(), "hello world" };

        ValueListOutput<String, String> valueListOutput = new ValueListOutput<>(codec);
        Object[] valueList = new Object[] { valueListOutput, valueListOutput, "hello world".getBytes(), "hello world" };

        StringListOutput<String, String> stringListOutput = new StringListOutput<>(codec);
        Object[] stringList = new Object[] { stringListOutput, stringListOutput, "hello world".getBytes(), "hello world" };

        return Arrays.asList(keyList, valueList, stringList);

    }

    @Test(expected = IllegalArgumentException.class)
    public void settingEmptySubscriberShouldFail() throws Exception {
        streamingOutput.setSubscriber(null);
    }

    @Test
    public void defaultSubscriberIsSet() throws Exception {
        assertThat(streamingOutput.getSubscriber()).isNotNull().isInstanceOf(ListSubscriber.class);
    }

    @Test(expected = IllegalStateException.class)
    public void setIntegerShouldFail() throws Exception {
        commandOutput.set(123L);
    }

    @Test
    public void setValueShouldConvert() throws Exception {
        commandOutput.set(ByteBuffer.wrap(valueBytes));

        assertThat(commandOutput.get()).contains(value);
    }
}
