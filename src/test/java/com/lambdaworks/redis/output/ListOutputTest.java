package com.lambdaworks.redis.output;

import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.lambdaworks.redis.codec.Utf8StringCodec;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@RunWith(Parameterized.class)
public class ListOutputTest {

    @Parameter(0)
    public CommandOutput<?, ?, List<?>> commandOutput;

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