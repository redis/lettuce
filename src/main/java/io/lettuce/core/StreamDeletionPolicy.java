/*
 * Copyright 2025-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.protocol.ProtocolKeyword;

import java.nio.charset.StandardCharsets;

/**
 * Deletion policy for stream commands that handle consumer group references. Used with XDELEX, XACKDEL, and enhanced XADD/XTRIM
 * commands.
 */
public enum StreamDeletionPolicy implements ProtocolKeyword {

    /**
     * Preserves existing references to entries in all consumer groups' PEL. This is the default behavior similar to XDEL.
     */
    KEEP_REFERENCES("KEEPREF"),

    /**
     * Removes all references to entries from all consumer groups' pending entry lists, effectively cleaning up all traces of
     * the messages.
     */
    DELETE_REFERENCES("DELREF"),

    /**
     * Only operates on entries that were read and acknowledged by all consumer groups.
     */
    ACKNOWLEDGED("ACKED");

    public final byte[] bytes;

    StreamDeletionPolicy(String redisParamName) {
        bytes = redisParamName.getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

}
