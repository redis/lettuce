/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import java.nio.charset.StandardCharsets;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Blessing flag for the {@literal BLESS} command family. A blessed key carries one or more flags that protect it against
 * server-side memory management, such as {@link #NO_EVICT eviction}.
 * <p>
 * The set of flags is open: {@link #NO_EVICT} is the only flag known today, and {@link #of(String)} passes any other token
 * verbatim to the server so that flags introduced by newer servers can be used without a client upgrade. Unknown tokens are
 * rejected by the server with a syntax error.
 *
 * @since 7.9
 */
public final class BlessFlag implements ProtocolKeyword {

    /**
     * Protect the key against {@literal maxmemory} eviction under any eviction policy. Wire token {@literal NO-EVICT}.
     *
     * @since 7.9
     */
    public static final BlessFlag NO_EVICT = new BlessFlag("NO-EVICT");

    private final String token;

    private final byte[] bytes;

    private BlessFlag(String token) {
        this.token = token;
        this.bytes = token.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Create a {@link BlessFlag} from its wire token. Returns the {@link #NO_EVICT} constant for its token; any other token is
     * passed to the server as-is.
     *
     * @param token the flag token as understood by the server, must not be {@code null} or empty.
     * @return the {@link BlessFlag} for {@code token}.
     * @throws IllegalArgumentException if {@code token} is {@code null} or empty.
     * @since 7.9
     */
    public static BlessFlag of(String token) {

        LettuceAssert.notEmpty(token, "Token must not be null or empty");

        if (NO_EVICT.token.equals(token)) {
            return NO_EVICT;
        }

        return new BlessFlag(token);
    }

    /**
     * @return the wire token of this flag, for example {@literal NO-EVICT}.
     * @since 7.9
     */
    public String getToken() {
        return token;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof BlessFlag)) {
            return false;
        }

        return token.equals(((BlessFlag) o).token);
    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }

    @Override
    public String toString() {
        return token;
    }

}
