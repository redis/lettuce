package io.lettuce.core.cluster.event;

import io.lettuce.core.event.Event;

/**
 * @author Mark Paluch
 */
public class RedirectionEventSupport implements Event {

    private final String command;

    private final String key;

    private final int slot;

    private final String message;

    public RedirectionEventSupport(String command, String key, int slot, String message) {
        this.command = command;
        this.key = key;
        this.slot = slot;
        this.message = message;
    }

    public String getCommand() {
        return command;
    }

    public String getKey() {
        return key;
    }

    public int getSlot() {
        return slot;
    }

    public String getMessage() {
        return message;
    }

}
