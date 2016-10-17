package com.lambdaworks.redis.dynamic;

import com.lambdaworks.redis.RedisException;

/**
 * @author Mark Paluch
 */
public class CommandCreationException extends RedisException {

    private final CommandMethod commandMethod;

    public CommandCreationException(CommandMethod commandMethod, String msg) {
        super(String.format("%s Offending method: %s", msg, commandMethod));
        this.commandMethod = commandMethod;
    }

    public CommandMethod getCommandMethod() {
        return commandMethod;
    }
}
