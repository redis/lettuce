/*
 * Copyright 2011-2022 the original author or authors.
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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.protocol.ReadOnlyCommands;

/**
 * Contains all command names that are read-only commands.
 *
 * @author Mark Paluch
 * @since 6.2.5
 */
public class ClusterReadOnlyCommands {

    private static final Set<CommandType> READ_ONLY_COMMANDS = EnumSet.noneOf(CommandType.class);

    private static final ReadOnlyCommands.ReadOnlyPredicate PREDICATE = command -> isReadOnlyCommand(command.getType());

    static {

        READ_ONLY_COMMANDS.addAll(ReadOnlyCommands.getReadOnlyCommands());

        for (CommandName commandNames : CommandName.values()) {
            READ_ONLY_COMMANDS.add(CommandType.valueOf(commandNames.name()));
        }
    }

    /**
     * @param protocolKeyword must not be {@code null}.
     * @return {@code true} if {@link ProtocolKeyword} is a read-only command.
     */
    public static boolean isReadOnlyCommand(ProtocolKeyword protocolKeyword) {
        return READ_ONLY_COMMANDS.contains(protocolKeyword);
    }

    /**
     * @return an unmodifiable {@link Set} of {@link CommandType read-only} commands.
     */
    public static Set<CommandType> getReadOnlyCommands() {
        return Collections.unmodifiableSet(READ_ONLY_COMMANDS);
    }

    /**
     * Return a {@link ReadOnlyCommands.ReadOnlyPredicate} to test against the underlying
     * {@link #isReadOnlyCommand(ProtocolKeyword) known commands}.
     *
     * @return a {@link ReadOnlyCommands.ReadOnlyPredicate} to test against the underlying
     *         {@link #isReadOnlyCommand(ProtocolKeyword) known commands}.
     */
    public static ReadOnlyCommands.ReadOnlyPredicate asPredicate() {
        return PREDICATE;
    }

    enum CommandName {

        // Pub/Sub commands are no key-space commands so they are safe to execute on replica nodes
        PUBLISH, PUBSUB, PSUBSCRIBE, PUNSUBSCRIBE, SUBSCRIBE, UNSUBSCRIBE
    }

}
