/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import java.util.*;

import io.lettuce.core.models.command.CommandDetail;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Value object representing the current Redis state regarding its commands.
 * <p>
 * {@link RedisState} caches command details and uses {@link CommandType}.
 *
 * @author Mark Paluch
 * @since 4.4
 */
class RedisState {

    private final Map<String, CommandDetail> commands;

    private final EnumSet<CommandType> availableCommands = EnumSet.noneOf(CommandType.class);

    public RedisState(Collection<CommandDetail> commands) {

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

        return commands.containsKey(commandName.name().toLowerCase());
    }

}
