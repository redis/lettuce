/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.test.Wait;

/**
 * Shared helpers for RediSearch integration tests to synchronize with the server-side indexer. RediSearch indexes documents
 * asynchronously (keyspace-notification driven for documents written after {@code FT.CREATE}), so a query issued right after
 * the writes can observe a partially built index.
 */
class SearchTestSupport {

    private SearchTestSupport() {
    }

    /**
     * Wait until {@code index} reports exactly {@code expectedDocs} indexed documents via FT.INFO {@code num_docs}, allowing
     * the notification-driven indexer to catch up with preceding document writes. Times out after the default {@link Wait}
     * period (10 seconds).
     */
    static void awaitIndexReady(RedisCommands<String, String> redis, String index, long expectedDocs) {
        Wait.untilEquals(expectedDocs, () -> ftInfoNumDocs(redis, index)).waitOrTimeout();
    }

    /**
     * FT.INFO is not exposed by Lettuce's {@code RediSearchCommands} API, so we issue it as a raw command via
     * {@link io.lettuce.core.api.sync.BaseRedisCommands#dispatch dispatch()} and read the {@code num_docs} field. This mirrors
     * how the Jedis and NRedisStack helpers assert the index size (via FT.INFO {@code num_docs}) instead of an FT.SEARCH count.
     */
    static long ftInfoNumDocs(RedisCommands<String, String> redis, String index) {
        return redis.dispatch(FT_INFO, new NumDocsOutput(), new CommandArgs<>(StringCodec.UTF8).add(index));
    }

    private static final ProtocolKeyword FT_INFO = new ProtocolKeyword() {

        private final byte[] bytes = "FT.INFO".getBytes(StandardCharsets.US_ASCII);

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public String toString() {
            return "FT.INFO";
        }

    };

    /**
     * Extracts only the {@code num_docs} value from the flat FT.INFO reply. Works on both RESP2 and RESP3: the field names and
     * values arrive as a flat token stream, so when the {@code num_docs} key is seen, the next scalar (bulk string on RESP2 or
     * integer on RESP3) is its value.
     */
    private static final class NumDocsOutput extends CommandOutput<String, String, Long> {

        private boolean valueExpected;

        NumDocsOutput() {
            super(StringCodec.UTF8, -1L);
        }

        @Override
        public void set(ByteBuffer bytes) {
            if (bytes == null) {
                return;
            }
            String token = StringCodec.UTF8.decodeValue(bytes);
            if (valueExpected) {
                output = Long.parseLong(token);
                valueExpected = false;
            } else if ("num_docs".equals(token)) {
                valueExpected = true;
            }
        }

        @Override
        public void set(long integer) {
            if (valueExpected) {
                output = integer;
                valueExpected = false;
            }
        }

        // Other FT.INFO fields carry double/boolean values (e.g. percent_indexed); accept and skip them so decoding of the
        // full reply doesn't fail before num_docs is read.
        @Override
        public void set(double number) {
            if (valueExpected) {
                output = (long) number;
                valueExpected = false;
            }
        }

        @Override
        public void set(boolean value) {
            valueExpected = false;
        }

    }

}
