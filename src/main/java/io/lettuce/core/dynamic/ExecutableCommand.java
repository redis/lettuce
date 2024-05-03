package io.lettuce.core.dynamic;

import java.util.concurrent.ExecutionException;

/**
 * An executable command that can be executed calling {@link #execute(Object[])}.
 *
 * @author Mark Paluch
 * @since 5.0
 */
interface ExecutableCommand {

    /**
     * Executes the {@link ExecutableCommand} with the given parameters.
     *
     * @param parameters
     * @return
     */
    Object execute(Object[] parameters) throws ExecutionException, InterruptedException;

    /**
     * Returns the {@link CommandMethod}.
     *
     * @return
     */
    CommandMethod getCommandMethod();

}
