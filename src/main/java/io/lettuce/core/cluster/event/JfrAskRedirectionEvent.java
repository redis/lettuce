package io.lettuce.core.cluster.event;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;

/**
 * Flight recorder event variant of {@link AskRedirectionEvent}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
@Category({ "Lettuce", "Cluster Events" })
@Label("ASK Redirection")
@StackTrace(false)
class JfrAskRedirectionEvent extends Event {

    private final String command;

    private final String key;

    private final int slot;

    private final String message;

    public JfrAskRedirectionEvent(RedirectionEventSupport event) {
        this.command = event.getCommand();
        this.key = event.getKey();
        this.slot = event.getSlot();
        this.message = event.getMessage();
    }

}
