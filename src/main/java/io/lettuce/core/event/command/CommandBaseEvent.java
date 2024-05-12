package io.lettuce.core.event.command;

import java.util.Map;

import io.lettuce.core.protocol.RedisCommand;

/**
 * Base class for Redis command events.
 *
 * @author Mikhael Sokolov
 * @since 6.1
 */
public abstract class CommandBaseEvent {

    private final RedisCommand<Object, Object, Object> command;

    private final Map<String, Object> context;

    protected CommandBaseEvent(RedisCommand<Object, Object, Object> command, Map<String, Object> context) {
        this.command = command;
        this.context = context;
    }

    /**
     * @return the actual command.
     */
    public RedisCommand<Object, Object, Object> getCommand() {
        return command;
    }

    /**
     * @return shared context.
     */
    public Map<String, Object> getContext() {
        synchronized (this) {
            return context;
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [command=").append(command);
        sb.append(", context=").append(context);
        sb.append(']');
        return sb.toString();
    }

}
