package io.lettuce.core.cluster;

import java.util.*;

import io.lettuce.core.models.command.CommandDetail;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Value object representing the current Redis state regarding its commands.
 * <p>
 * {@link CommandSet} caches command details and uses {@link CommandType}.
 *
 * @author Mark Paluch
 * @since 4.4
 */
class CommandSet {

    private final Map<String, CommandDetail> commands;

    private final EnumSet<CommandType> availableCommands = EnumSet.noneOf(CommandType.class);

    public CommandSet(Collection<CommandDetail> commands) {

        Map<String, CommandDetail> map = new HashMap<>();

        for (CommandDetail command : commands) {

            map.put(command.getName().toLowerCase(), command);

            CommandType commandType = getCommandType(command);
            if (commandType != null) {
                availableCommands.add(commandType);
            }
        }

        this.commands = map;
    }

    private static CommandType getCommandType(CommandDetail command) {

        try {
            return CommandType.valueOf(command.getName().toUpperCase(Locale.US));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Check whether Redis supports a particular command given a {@link ProtocolKeyword}. Querying commands using
     * {@link CommandType} yields a better performance than other subtypes of {@link ProtocolKeyword}.
     *
     * @param commandName the command name, must not be {@code null}.
     * @return {@code true} if the command is supported/available.
     */
    public boolean hasCommand(ProtocolKeyword commandName) {

        if (commandName instanceof CommandType) {
            return availableCommands.contains(commandName);
        }

        return commands.containsKey(commandName.toString().toLowerCase());
    }

}
