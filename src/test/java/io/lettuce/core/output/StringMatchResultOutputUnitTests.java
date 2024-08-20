package io.lettuce.core.output;

import io.lettuce.core.StringMatchResult;
import io.lettuce.core.codec.StringCodec;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

public class StringMatchResultOutputUnitTests {

    @Test
    void parseLenAndMatches() {
        StringMatchResultOutput<String, String> output = new StringMatchResultOutput<>(new StringCodec(), false);

        output.set(ByteBuffer.wrap("len".getBytes()));
        output.set(42);

        output.set(ByteBuffer.wrap("matches".getBytes()));
        output.set(0);
        output.set(5);
        output.set(10);
        output.set(15);

        output.complete(2);
        output.complete(0);

        StringMatchResult result = output.get();

        assertThat(result.getLen()).isEqualTo(42);

        assertThat(result.getMatches()).hasSize(1).satisfies(m -> assertMatchedPositions(m.get(0), 0, 5, 10, 15));
    }

    private void assertMatchedPositions(StringMatchResult.MatchedPosition match, int... expected) {
        assertThat(match.getA().getStart()).isEqualTo(expected[0]);
        assertThat(match.getA().getEnd()).isEqualTo(expected[1]);
        assertThat(match.getB().getStart()).isEqualTo(expected[2]);
        assertThat(match.getB().getEnd()).isEqualTo(expected[3]);
    }

}
