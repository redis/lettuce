package io.lettuce.core.cluster.event;

/**
 * Event emitted on a {@code ASK} redirection.
 *
 * @author Mark Paluch
 * @since 6.1
 */
public class AskRedirectionEvent extends RedirectionEventSupport {

    public AskRedirectionEvent(String command, String key, int slot, String message) {
        super(command, key, slot, message);
    }

}
