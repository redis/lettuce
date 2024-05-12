package io.lettuce.core.dynamic;

import io.lettuce.core.RedisException;

/**
 * Exception thrown if a command cannot be constructed from a {@link CommandMethod}.
 *
 * @author Mark Paluch
 * @since 5.0
 */
@SuppressWarnings("serial")
public class CommandCreationException extends RedisException {

    private final CommandMethod commandMethod;

    /**
     * Create a new {@link CommandCreationException} given {@link CommandMethod} and a message.
     *
     * @param commandMethod must not be {@code null}.
     * @param msg must not be {@code null}.
     */
    public CommandCreationException(CommandMethod commandMethod, String msg) {

        super(String.format("%s Offending method: %s", msg, commandMethod));
        this.commandMethod = commandMethod;
    }

    /**
     * @return the offending {@link CommandMethod}.
     */
    public CommandMethod getCommandMethod() {
        return commandMethod;
    }

}
