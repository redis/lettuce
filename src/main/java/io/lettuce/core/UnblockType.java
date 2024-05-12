package io.lettuce.core;

import java.nio.charset.StandardCharsets;

import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Unblock type for {@code CLIENT UNBLOCK} command.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public enum UnblockType implements ProtocolKeyword {

    TIMEOUT, ERROR;

    private final byte[] bytes;

    UnblockType() {
        bytes = name().getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

}
