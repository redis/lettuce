/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 *
 * Argument list builder for the Redis <a href="http://redis.io/commands/client-tracking">CLIENT TRACKING</a> command. Static
 * import the methods from {@link Builder} and chain the method calls: {@code enabled().bcast()}.
 * <p>
 * {@link TrackingArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public class TrackingArgs implements CompositeArgument {

    private boolean enabled;

    private Long redirect;

    private boolean bcast;

    private String[] prefixes;

    private Charset prefixCharset = StandardCharsets.UTF_8;

    private boolean optin;

    private boolean optout;

    private boolean noloop;

    /**
     * Builder entry points for {@link TrackingArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link TrackingArgs} with {@literal CLIENT TRACKING ON}.
         *
         * @return new {@link TrackingArgs}.
         * @see TrackingArgs#enabled(boolean)
         */
        public static TrackingArgs enabled() {
            return enabled(true);
        }

        /**
         * Creates new {@link TrackingArgs} with {@literal CLIENT TRACKING ON} if {@code enabled} is {@code true}.
         *
         * @param enabled whether to enable key tracking for the currently connected client.
         * @return new {@link TrackingArgs}.
         * @see TrackingArgs#enabled(boolean)
         */
        public static TrackingArgs enabled(boolean enabled) {
            return new TrackingArgs().enabled(enabled);
        }

    }

    /**
     * Controls whether to enable key tracking for the currently connected client.
     *
     * @param enabled whether to enable key tracking for the currently connected client.
     * @return {@code this} {@link TrackingArgs}.
     */
    public TrackingArgs enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Send redirection messages to the connection with the specified ID. The connection must exist, you can get the ID of such
     * connection using {@code CLIENT ID}. If the connection we are redirecting to is terminated, when in RESP3 mode the
     * connection with tracking enabled will receive tracking-redir-broken push messages in order to signal the condition.
     *
     * @param clientId process Id of the client for notification redirection.
     * @return {@code this} {@link TrackingArgs}.
     */
    public TrackingArgs redirect(long clientId) {
        this.redirect = clientId;
        return this;
    }

    /**
     * Enable tracking in broadcasting mode. In this mode invalidation messages are reported for all the prefixes specified,
     * regardless of the keys requested by the connection. Instead when the broadcasting mode is not enabled, Redis will track
     * which keys are fetched using read-only commands, and will report invalidation messages only for such keys.
     *
     * @return {@code this} {@link TrackingArgs}.
     */
    public TrackingArgs bcast() {
        this.bcast = true;
        return this;
    }

    /**
     * For broadcasting, register a given key prefix, so that notifications will be provided only for keys starting with this
     * string. This option can be given multiple times to register multiple prefixes. If broadcasting is enabled without this
     * option, Redis will send notifications for every key.
     *
     * @param prefixes the key prefixes for broadcasting of change notifications. Encoded using
     *        {@link java.nio.charset.StandardCharsets#UTF_8}.
     * @return {@code this} {@link TrackingArgs}.
     */
    public TrackingArgs prefixes(String... prefixes) {
        return prefixes(StandardCharsets.UTF_8, prefixes);
    }

    /**
     * For broadcasting, register a given key prefix, so that notifications will be provided only for keys starting with this
     * string. This option can be given multiple times to register multiple prefixes. If broadcasting is enabled without this
     * option, Redis will send notifications for every key.
     *
     * @param charset the charset to use for {@code prefixes} encoding.
     * @param prefixes the key prefixes for broadcasting of change notifications.
     * @return {@code this} {@link TrackingArgs}.
     */
    public TrackingArgs prefixes(Charset charset, String... prefixes) {
        LettuceAssert.notNull(charset, "Charset must not be null");
        this.prefixCharset = charset;
        this.prefixes = prefixes;
        return this;
    }

    /**
     * When broadcasting is NOT active, normally don't track keys in read only commands, unless they are called immediately
     * after a CLIENT CACHING yes command.
     *
     * @return {@code this} {@link TrackingArgs}.
     */
    public TrackingArgs optin() {
        this.optin = true;
        return this;
    }

    /**
     * When broadcasting is NOT active, normally track keys in read only commands, unless they are called immediately after a
     * CLIENT CACHING no command.
     *
     * @return {@code this} {@link TrackingArgs}.
     */
    public TrackingArgs optout() {
        this.optout = true;
        return this;
    }

    /**
     * Don't send notifications about keys modified by this connection itself.
     *
     * @return {@code this} {@link TrackingArgs}.
     */
    public TrackingArgs noloop() {
        this.noloop = true;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        args.add(enabled ? CommandKeyword.ON : CommandKeyword.OFF);

        if (redirect != null) {
            args.add("REDIRECT").add(redirect);
        }

        if (prefixes != null) {
            for (String prefix : prefixes) {
                args.add("PREFIX").add(prefix.getBytes(prefixCharset));
            }
        }

        if (bcast) {
            args.add("BCAST");
        }

        if (optin) {
            args.add("OPTIN");
        }

        if (optout) {
            args.add("OPTOUT");
        }

        if (noloop) {
            args.add("NOLOOP");
        }
    }

}
