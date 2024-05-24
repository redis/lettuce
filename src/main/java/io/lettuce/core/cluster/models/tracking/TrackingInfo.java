package io.lettuce.core.cluster.models.tracking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

/**
 * Contains the output of a <a href="https://redis.io/docs/latest/commands/client-trackinginfo/">CLIENT TRACKINGINFO</a>
 * command.
 *
 * @author Tihomir Mateev
 * @since 7.0
 */
public class TrackingInfo {

    private final Set<TrackingFlag> flags = new HashSet<>();

    private final long redirect;

    private final List<String> prefixes = new ArrayList<>();

    /**
     * Constructor
     * 
     * @param flags a {@link Set} of {@link TrackingFlag}s that the command returned
     * @param redirect the client ID used for notification redirection, -1 when none
     * @param prefixes a {@link List} of key prefixes for which notifications are sent to the client
     *
     * @see TrackingFlag
     */
    public TrackingInfo(Set<TrackingFlag> flags, long redirect, List<String> prefixes) {
        this.flags.addAll(flags);
        this.redirect = redirect;
        this.prefixes.addAll(prefixes);
    }

    /**
     * @return set of all the {@link TrackingFlag}s currently enabled on the client connection
     */
    public Set<TrackingFlag> getFlags() {
        return Collections.unmodifiableSet(flags);
    }

    /**
     * @return the client ID used for notification redirection, -1 when none
     */
    public long getRedirect() {
        return redirect;
    }

    /**
     * @return a {@link List} of key prefixes for which notifications are sent to the client
     */
    public List<String> getPrefixes() {
        return Collections.unmodifiableList(prefixes);
    }

    /**
     * CLIENT TRACKINGINFO flags
     *
     * @see <a href="https://redis.io/docs/latest/commands/client-trackinginfo/">CLIENT TRACKINGINFO</a>
     */
    public enum TrackingFlag {

        /**
         * The connection isn't using server assisted client side caching.
         */
        OFF,
        /**
         * Server assisted client side caching is enabled for the connection.
         */
        ON,
        /**
         * The client uses broadcasting mode.
         */
        BCAST,
        /**
         * The client does not cache keys by default.
         */
        OPTIN,
        /**
         * The client caches keys by default.
         */
        OPTOUT,
        /**
         * The next command will cache keys (exists only together with optin).
         */
        CACHING_YES,
        /**
         * The next command won't cache keys (exists only together with optout).
         */
        CACHING_NO,
        /**
         * The client isn't notified about keys modified by itself.
         */
        NOLOOP,
        /**
         * The client ID used for redirection isn't valid anymore.
         */
        BROKEN_REDIRECT;

        /**
         * Convert a given {@link String} flag to the corresponding {@link TrackingFlag}
         * 
         * @param flag a {@link String} representation of the flag
         * @return the resulting {@link TrackingFlag} or {@link IllegalArgumentException} if unrecognized
         */
        public static TrackingFlag from(String flag) {
            switch (flag) {
                case "off":
                    return OFF;
                case "on":
                    return ON;
                case "bcast":
                    return BCAST;
                case "optin":
                    return OPTIN;
                case "optout":
                    return OPTOUT;
                case "caching-yes":
                    return CACHING_YES;
                case "caching-no":
                    return CACHING_NO;
                case "noloop":
                    return NOLOOP;
                case "broken_redirect":
                    return BROKEN_REDIRECT;
                default:
                    throw new RuntimeException("Unsupported flag: " + flag);
            }
        }

    }

}
