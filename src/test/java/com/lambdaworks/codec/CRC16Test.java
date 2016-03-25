package com.lambdaworks.codec;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.lambdaworks.redis.internal.LettuceLists;

@RunWith(Parameterized.class)
public class CRC16Test {

    private byte[] bytes;
    private int expected;

    public CRC16Test(byte[] bytes, int expected, String hex) {
        this.bytes = bytes;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "{2}")
    public static List<Object[]> parameters() {

        List<Object[]> parameters = LettuceLists.newList();

        params(parameters, "".getBytes(), 0x0);
        params(parameters, "123456789".getBytes(), 0x31C3);
        params(parameters, "sfger132515".getBytes(), 0xA45C);
        params(parameters, "hae9Napahngaikeethievubaibogiech".getBytes(), 0x58CE);
        params(parameters, "AAAAAAAAAAAAAAAAAAAAAA".getBytes(), 0x92cd);
        params(parameters, "Hello, World!".getBytes(), 0x4FD6);

        return parameters;
    }

    private static void params(List<Object[]> parameters, byte[] bytes, int expectation) {
        parameters.add(new Object[] { bytes, expectation, "0x" + Integer.toHexString(expectation).toUpperCase() });
    }

    @Test
    public void testCRC16() throws Exception {

        int result = CRC16.crc16(bytes);
        assertThat(result).describedAs("Expects " + Integer.toHexString(expected)).isEqualTo(expected);

    }
}
