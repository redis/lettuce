package io.lettuce.core.output;

import io.lettuce.core.IncrexValue;
import io.lettuce.core.codec.StringCodec;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(UNIT_TEST)
class IncrexOutputUnitTests {

    @Test
    void longOutputFromBulkStrings() {
        IncrexLongOutput<String, String> output = new IncrexLongOutput<>(StringCodec.UTF8);
        output.set(wrap("12"));
        output.set(wrap("2"));

        IncrexValue<Long> result = output.get();
        assertThat(result.getValue()).isEqualTo(12L);
        assertThat(result.getIncrement()).isEqualTo(2L);
    }

    @Test
    void longOutputFromIntegers() {
        IncrexLongOutput<String, String> output = new IncrexLongOutput<>(StringCodec.UTF8);
        output.set(12L);
        output.set(2L);

        IncrexValue<Long> result = output.get();
        assertThat(result.getValue()).isEqualTo(12L);
        assertThat(result.getIncrement()).isEqualTo(2L);
    }

    @Test
    void doubleOutputFromBulkStrings() {
        IncrexDoubleOutput<String, String> output = new IncrexDoubleOutput<>(StringCodec.UTF8);
        output.set(wrap("4.5"));
        output.set(wrap("1.25"));

        IncrexValue<Double> result = output.get();
        assertThat(result.getValue()).isEqualTo(4.5);
        assertThat(result.getIncrement()).isEqualTo(1.25);
    }

    @Test
    void doubleOutputFromDoubles() {
        IncrexDoubleOutput<String, String> output = new IncrexDoubleOutput<>(StringCodec.UTF8);
        output.set(4.5);
        output.set(1.25);

        IncrexValue<Double> result = output.get();
        assertThat(result.getValue()).isEqualTo(4.5);
        assertThat(result.getIncrement()).isEqualTo(1.25);
    }

    @Test
    void longOutputNegativeIncrement() {
        IncrexLongOutput<String, String> output = new IncrexLongOutput<>(StringCodec.UTF8);
        output.set(wrap("7"));
        output.set(wrap("-3"));

        IncrexValue<Long> result = output.get();
        assertThat(result.getValue()).isEqualTo(7L);
        assertThat(result.getIncrement()).isEqualTo(-3L);
    }

    @Test
    void doubleOutputZeroIncrement() {
        IncrexDoubleOutput<String, String> output = new IncrexDoubleOutput<>(StringCodec.UTF8);
        output.set(wrap("5.0"));
        output.set(wrap("0"));

        IncrexValue<Double> result = output.get();
        assertThat(result.getValue()).isEqualTo(5.0);
        assertThat(result.getIncrement()).isEqualTo(0.0);
    }

    private static ByteBuffer wrap(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

}
