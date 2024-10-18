package io.lettuce.core.output;

import io.lettuce.core.codec.StringCodec;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class IntegerListOutputUnitTests {

    static Collection<Fixture> parameters() {

        IntegerListOutput<String, String> integerListOutput = new IntegerListOutput<>(StringCodec.UTF8);
        Fixture integerList = new Fixture(integerListOutput, integerListOutput,
                ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(1L).array(), 1L);

        IntegerListOutput<String, String> nullListOutput = new IntegerListOutput<>(StringCodec.UTF8);
        Fixture nullList = new Fixture(nullListOutput, nullListOutput, null, null);

        ValueListOutput<String, String> valueListOutput = new ValueListOutput<>(StringCodec.UTF8);
        Fixture valueList = new Fixture(valueListOutput, valueListOutput, "hello world".getBytes(), "hello world");

        StringListOutput<String, String> stringListOutput = new StringListOutput<>(StringCodec.UTF8);
        Fixture stringList = new Fixture(stringListOutput, stringListOutput, "hello world".getBytes(), "hello world");

        return Arrays.asList(integerList, nullList, valueList, stringList);
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

        if (fixture.valueBytes == null) {
            fixture.commandOutput.set(null);
        } else if (fixture.value instanceof Long) {
            fixture.commandOutput.set((Long) fixture.value);
        } else {
            fixture.commandOutput.set(ByteBuffer.wrap(fixture.valueBytes));
        }

        if (fixture.valueBytes != null) {
            assertThat(fixture.commandOutput.get()).contains(fixture.value);
        } else {
            assertThat(fixture.commandOutput.get()).isEmpty();
        }
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
