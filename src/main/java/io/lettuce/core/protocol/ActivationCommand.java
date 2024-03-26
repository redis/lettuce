package io.lettuce.core.protocol;

/**
 * Command wrapper to identify activation commands.
 *
 * @author Mark Paluch
 */
class ActivationCommand<K, V, T> extends CommandWrapper<K, V, T> {

    public ActivationCommand(RedisCommand<K, V, T> command) {
        super(command);
    }

    public static boolean isActivationCommand(RedisCommand<?, ?, ?> command) {

        if (command instanceof ActivationCommand) {
            return true;
        }

        while (command instanceof CommandWrapper) {
            command = ((CommandWrapper<?, ?, ?>) command).getDelegate();

            if (command instanceof ActivationCommand) {
                return true;
            }
        }

        return false;
    }

}
