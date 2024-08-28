package io.lettuce.core.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import io.lettuce.core.StringMatchResult;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.protocol.RedisStateMachine;

public class StringMatchResultOutputUnitTests {

    @Test
    void parseManually() {
        byte[] rawOne = "%2\r\n$7\r\nmatches\r\n*1\r\n*3\r\n*2\r\n:4\r\n:7\r\n*2\r\n:5\r\n:8\r\n:4\r\n$3\r\nlen\r\n:6\r\n"
                .getBytes(StandardCharsets.US_ASCII);
        byte[] rawTwo = "%2\r\n$3\r\nlen\r\n:6\r\n$7\r\nmatches\r\n*1\r\n*3\r\n*2\r\n:4\r\n:7\r\n*2\r\n:5\r\n:8\r\n:4\r\n"
                .getBytes(StandardCharsets.US_ASCII);
        RedisStateMachine rsm = new RedisStateMachine();
        rsm.setProtocolVersion(ProtocolVersion.RESP3);

        StringMatchResultOutput<String, String> o1 = new StringMatchResultOutput<>(StringCodec.ASCII);
        assertThat(rsm.decode(Unpooled.wrappedBuffer(rawOne), o1)).isTrue();

        StringMatchResultOutput<String, String> o2 = new StringMatchResultOutput<>(StringCodec.ASCII);
        assertThat(rsm.decode(Unpooled.wrappedBuffer(rawTwo), o2)).isTrue();

        Map<String, Object> res1 = transform(o1.get());
        Map<String, Object> res2 = transform(o2.get());

        assertThat(res1).isEqualTo(res2);
    }

    private Map<String, Object> transform(StringMatchResult result) {
        Map<String, Object> obj = new HashMap<>();
        List<Object> matches = new ArrayList<>();
        for (StringMatchResult.MatchedPosition match : result.getMatches()) {
            Map<String, Object> intra = new HashMap<>();
            Map<String, Object> a = new HashMap<>();
            Map<String, Object> b = new HashMap<>();
            a.put("start", match.getA().getStart());
            a.put("end", match.getA().getEnd());

            b.put("start", match.getB().getStart());
            b.put("end", match.getB().getEnd());
            intra.put("a", a);
            intra.put("b", b);
            intra.put("matchLen", match.getMatchLen());
            matches.add(intra);
        }
        obj.put("matches", matches);
        obj.put("len", result.getLen());
        return obj;
    }

    @Test
    void parseOnlyStringMatch() {
        StringMatchResultOutput<String, String> output = new StringMatchResultOutput<>(StringCodec.ASCII);

        String matchString = "some-string";
        output.set(ByteBuffer.wrap(matchString.getBytes()));
        output.complete(0);

        StringMatchResult result = output.get();
        assertThat(result.getMatchString()).isEqualTo(matchString);
        assertThat(result.getMatches()).isEmpty();
        assertThat(result.getLen()).isZero();
    }

    @Test
    void parseOnlyLen() {
        StringMatchResultOutput<String, String> output = new StringMatchResultOutput<>(StringCodec.ASCII);

        output.set(42);
        output.complete(0);

        StringMatchResult result = output.get();
        assertThat(result.getMatchString()).isNull();
        assertThat(result.getMatches()).isEmpty();
        assertThat(result.getLen()).isEqualTo(42);
    }

    @Test
    void parseLenAndMatchesWithIdx() {
        StringMatchResultOutput<String, String> output = new StringMatchResultOutput<>(StringCodec.ASCII);

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

        assertThat(result.getMatchString()).isNull();
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
