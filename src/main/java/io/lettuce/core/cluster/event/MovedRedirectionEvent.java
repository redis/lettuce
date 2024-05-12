package io.lettuce.core.cluster.event;

/**
 * Event emitted on a {@code MOVED} redirection.
 *
 * @author Mark Paluch
 * @since 6.1
 */
public class MovedRedirectionEvent extends RedirectionEventSupport {

    public MovedRedirectionEvent(String command, String key, int slot, String message) {
        super(command, key, slot, message);
    }

}
