package io.lettuce.core;

import java.nio.charset.StandardCharsets;

import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Restore mode for {@code FUNCTION RESTORE}.
 *
 * @author Mark Paluch
 * @since 6.3
 */
public enum FunctionRestoreMode implements ProtocolKeyword {

    /**
     * Appends the restored libraries to the existing libraries and aborts on collision. This is the default policy.
     */
    APPEND,

    /**
     * Deletes all existing libraries before restoring the payload.
     */
    FLUSH,

    /**
     * Appends the restored libraries to the existing libraries, replacing any existing ones in case of name collisions. Note
     * that this policy doesn't prevent function name collisions, only libraries.
     */
    REPLACE;

    public final byte[] bytes;

    FunctionRestoreMode() {
        bytes = name().getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

}
