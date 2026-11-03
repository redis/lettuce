/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.vector.VSimScoreAttribs;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unified VSIM output that parses WITHSCORES WITHATTRIBS responses for both RESP2 and RESP3.
 *
 * RESP3 format: Map<element, [score(double), attribs(bulk-string)]> RESP2 format: Flat array triplets [element, score, attribs,
 * ...]
 */
public final class VSimScoreAttribsMapOutput<K, V> extends CommandOutput<K, V, Map<V, VSimScoreAttribs>> {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(VSimScoreAttribsMapOutput.class);

    private boolean outputError = false; // retained for structural errors; not used for per-entry malformed scores.

    private boolean skipCurrentEntry = false;

    private enum State {
        KEY, SCORE, ATTRIBS
    }

    private State state = State.KEY;

    private V pendingKey;

    private Double pendingScore;

    private String pendingAttribs;

    public VSimScoreAttribsMapOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void multi(int count) {
        if (output == null) {
            output = new LinkedHashMap<>(Math.max(1, count / 2), 1);
        }
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (outputError) {
            return;
        }
        switch (state) {
            case KEY:
                pendingKey = (bytes == null) ? null : codec.decodeValue(bytes);
                state = State.SCORE;
                return;
            case SCORE:
                try {
                    pendingScore = (bytes == null) ? null : Double.parseDouble(decodeString(bytes));
                    state = State.ATTRIBS;
                } catch (NumberFormatException e) {
                    LOG.warn("Expected double as string for score, skipping this entry");
                    // Consume the next ATTRIBS token without emitting an entry
                    skipCurrentEntry = true;
                    pendingScore = null;
                    state = State.ATTRIBS;
                }
                return;
            case ATTRIBS:
                pendingAttribs = (bytes == null) ? null : decodeString(bytes);
                if (skipCurrentEntry) {
                    // skip this malformed entry
                    resetEntryState();
                } else {
                    putAndReset();
                }
                return;
        }
    }

    @Override
    public void set(double number) {
        if (outputError) {
            return;
        }
        if (state == State.SCORE) {
            pendingScore = number;
            state = State.ATTRIBS;
        } else if (state == State.ATTRIBS) {
            // RESP3 type/order mismatch for current entry: skip this entry and continue
            LOG.warn("Expected attributes as bulk string but got double; skipping current entry");
            resetEntryState();
        } else {
            // Received a double while expecting a key: structural error
            LOG.warn("Expected key but got double, discarding the result");
            output = new HashMap<>(0);
            outputError = true;
        }
    }

    @Override
    public void complete(int depth) {
        // no-op; entries are finalized when ATTRIBS is processed
    }

    private void putAndReset() {
        if (output == null) {
            output = new LinkedHashMap<>(1);
        }
        output.put(pendingKey, new VSimScoreAttribs(pendingScore != null ? pendingScore : Double.NaN, pendingAttribs));
        resetEntryState();
    }

    private void resetEntryState() {
        pendingKey = null;
        pendingScore = null;
        pendingAttribs = null;
        skipCurrentEntry = false;
        state = State.KEY;
    }

}
