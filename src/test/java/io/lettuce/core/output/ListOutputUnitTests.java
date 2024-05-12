package io.lettuce.core.output;

import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.lettuce.core.codec.StringCodec;

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
        assertThatThrownBy(() -> fixture.commandOutput.set(123L)).isInstanceOf(UnsupportedOperationException.class);
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
