/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

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
     * Run {@code FT.SEARCH} until the reply reports exactly {@code expectedCount} results and return that reply. FT.INFO
     * {@code num_docs} can already report all documents while the inverted index is still being built, so a query issued right
     * after {@link #awaitIndexReady} may still see fewer hits. Gives up after the default {@link Wait} period and returns the
     * last reply, so the caller's assertion reports the actual count.
     */
    static SearchReply<String> searchUntilCount(RedisCommands<String, String> redis, String index, String query,
            long expectedCount) {
        return searchUntilCount(() -> redis.ftSearch(index, query), expectedCount);
    }

    /**
     * Variant of {@link #searchUntilCount(RedisCommands, String, String, long)} for any connection codec and any search
     * arguments: {@code search} issues the query and is called until its reply reports exactly {@code expectedCount} results.
     */
    static <K> SearchReply<K> searchUntilCount(Supplier<SearchReply<K>> search, long expectedCount) {
        awaitQuietly(Wait.untilEquals(expectedCount, () -> search.get().getCount()));
        return search.get();
    }

    /**
     * Run {@code FT.SEARCH} until the reply reports at least {@code minCount} results and return that reply. See
     * {@link #searchUntilCount} for the rationale and the timeout behavior.
     */
    static SearchReply<String> searchUntilAtLeast(RedisCommands<String, String> redis, String index, String query,
            long minCount) {
        return searchUntilAtLeast(() -> redis.ftSearch(index, query), minCount);
    }

    /**
     * Variant of {@link #searchUntilAtLeast(RedisCommands, String, String, long)} for any connection codec and any search
     * arguments: {@code search} issues the query and is called until its reply reports at least {@code minCount} results.
     */
    static <K> SearchReply<K> searchUntilAtLeast(Supplier<SearchReply<K>> search, long minCount) {
        awaitQuietly(Wait.untilTrue(() -> search.get().getCount() >= minCount));
        return search.get();
    }

    private static void awaitQuietly(Wait.WaitBuilder<?> wait) {
        try {
            wait.waitOrTimeout();
        } catch (IllegalStateException e) {
            // Condition not reached within the timeout; the caller asserts on the returned reply and reports the actual value.
        }
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
