package com.lambdaworks.redis.dynamic;

/**
 * @author Mark Paluch
 */
public class CommandMethodSyntaxException extends CommandCreationException {

    public CommandMethodSyntaxException(CommandMethod commandMethod, String msg) {
        super(commandMethod, msg);
    }
}
