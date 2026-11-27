/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.protocol.ProtocolKeyword;

import java.nio.charset.StandardCharsets;

/**
 * Nacking mode for the {@literal XNACK} command. Determines how the delivery counter of the negatively-acknowledged messages is
 * adjusted in the consumer group's Pending Entries List.
 *
 * @since 7.6
 */
public enum XNackMode implements ProtocolKeyword {

    /**
     * Used when the consumer is NACKing due to internal errors or shutdown, not because the message is problematic. The
     * delivery counter of each specified message is decremented by 1.
     */
    SILENT,

    /**
     * Used when the message causes problems for this consumer but may succeed for other consumers (e.g., requires more
     * resources than available). The delivery counter stays the same.
     */
    FAIL,

    /**
     * Used for invalid or suspected malicious messages. The delivery counter is set to {@literal LLONG_MAX}.
     */
    FATAL;

    public final byte[] bytes;

    XNackMode() {
        bytes = name().getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

}
