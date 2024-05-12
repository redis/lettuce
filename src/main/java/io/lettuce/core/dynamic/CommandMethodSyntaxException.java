package io.lettuce.core.dynamic;

/**
 * Exception thrown if the command syntax is invalid.
 *
 * @author Mark Paluch
 * @since 5.0
 */
@SuppressWarnings("serial")
public class CommandMethodSyntaxException extends CommandCreationException {

    /**
     * Create a new {@link CommandMethodSyntaxException} given {@link CommandMethod} and a message.
     *
     * @param commandMethod must not be {@code null}.
     * @param msg must not be {@code null}.
     */
    public CommandMethodSyntaxException(CommandMethod commandMethod, String msg) {
        super(commandMethod, msg);
    }

}
