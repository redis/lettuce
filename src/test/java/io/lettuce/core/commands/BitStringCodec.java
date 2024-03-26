package io.lettuce.core.commands;

import java.nio.ByteBuffer;

import io.lettuce.core.codec.StringCodec;

/**
 * @author Mark Paluch
 */
public class BitStringCodec extends StringCodec {

    @Override
    public String decodeValue(ByteBuffer bytes) {
        StringBuilder bits = new StringBuilder(bytes.remaining() * 8);
        while (bytes.remaining() > 0) {
            byte b = bytes.get();
            for (int i = 0; i < 8; i++) {
                bits.append(Integer.valueOf(b >>> i & 1));
            }
        }
        return bits.toString();
    }
}
