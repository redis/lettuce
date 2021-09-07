/*
 * Copyright 2021 the original author or authors.
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
package io.lettuce.core;

import static io.lettuce.core.protocol.CommandKeyword.*;

import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/acl-setuser">ACL SETUSER</a> command.
 * <p>
 * {@link AclSetuserArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mikhael Sokolov
 * @since 6.1
 */
public class AclSetuserArgs implements CompositeArgument {

    private boolean active;

    private List<String> keyPatterns;

    private boolean allKeys;

    private boolean resetKeys;

    private List<String> channelPatterns;

    private boolean allChannels;

    private boolean resetChannels;

    private List<CommandSubcommandPair> addCommands;

    private boolean allCommands;

    private List<CommandSubcommandPair> removeCommands;

    private boolean noCommands;

    private List<AclCategory> addCategories;

    private List<AclCategory> removeCategories;

    private boolean nopass;

    private List<String> addPasswords;

    private List<String> addHashedPasswords;

    private List<String> removePasswords;

    private List<String> removeHashedPasswords;

    private boolean reset;


    /**
     * Builder entry points for {@link AclSetuserArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link AclSetuserArgs} and set user active.
         *
         * @return new {@link AclSetuserArgs} and set user active.
         * @see AclSetuserArgs#on()
         */
        public static AclSetuserArgs on() {
            return new AclSetuserArgs().on();
        }

        /**
         * Creates new {@link AclSetuserArgs} and set user inactive.
         *
         * @return new {@link AclSetuserArgs} and set user inactive.
         * @see AclSetuserArgs#off()
         */
        public static AclSetuserArgs off() {
            return new AclSetuserArgs().off();
        }

        /**
         * Creates new {@link AclSetuserArgs} and adds accessible key pattern.
         *
         * @param keyPattern accessible key pattern
         * @return new {@link AclSetuserArgs} and adds accessible key pattern.
         * @see AclSetuserArgs#keyPattern(String)
         */
        public static  AclSetuserArgs keyPattern(String keyPattern) {
            return new AclSetuserArgs().keyPattern(keyPattern);
        }

        /**
         * Creates new {@link AclSetuserArgs} and allows the user to access all the keys.
         *
         * @return new {@link AclSetuserArgs} and allows the user to access all the keys.
         * @see AclSetuserArgs#allKeys()
         */
        public static AclSetuserArgs allKeys() {
            return new AclSetuserArgs().allKeys();
        }

        /**
         * Creates new {@link AclSetuserArgs} and removes all the key patterns from the list of key patterns the user can access.
         *
         * @return new {@link AclSetuserArgs} and removes all the key patterns from the list of key patterns the user can access.
         * @see AclSetuserArgs#resetKeys()
         */
        public static AclSetuserArgs resetKeys() {
            return new AclSetuserArgs().resetKeys();
        }

        /**
         * Creates new {@link AclSetuserArgs} and adds accessible channel pattern.
         *
         * @param channelPattern accessible channel pattern
         * @return new {@link AclSetuserArgs} and adds accessible channel pattern.
         * @see AclSetuserArgs#channelPattern(String)
         */
        public static AclSetuserArgs channelPattern(String channelPattern) {
            return new AclSetuserArgs().channelPattern(channelPattern);
        }

        /**
         * Creates new {@link AclSetuserArgs} and allows the user to access all the Pub/Sub channels.
         *
         * @return new {@link AclSetuserArgs} and allows the user to access all the Pub/Sub channels.
         * @see AclSetuserArgs#allChannels()
         */
        public static AclSetuserArgs allChannels() {
            return new AclSetuserArgs().allChannels();
        }

        /**
         * Creates new {@link AclSetuserArgs} and removes all channel patterns from the list of Pub/Sub channel patterns the user can access.
         *
         * @return new {@link AclSetuserArgs} and removes all channel patterns from the list of Pub/Sub channel patterns the user can access.
         * @see AclSetuserArgs#resetChannels()
         */
        public static AclSetuserArgs resetChannels() {
            return new AclSetuserArgs().resetChannels();
        }

        /**
         * Creates new {@link AclSetuserArgs} and adds this command to the list of the commands the user can call.
         *
         * @param command accessible command
         * @return new {@link AclSetuserArgs} and adds this command to the list of the commands the user can call.
         * @see AclSetuserArgs#addCommand(CommandType)
         */
        public static AclSetuserArgs addCommand(CommandType command) {
            return new AclSetuserArgs().addCommand(command);
        }

        /**
         * Creates new {@link AclSetuserArgs} and adds the specified command to the list of the commands the user can execute.
         *
         * @param command accessible command
         * @param subCommand accessible subcommand
         * @return new {@link AclSetuserArgs} and adds the specified command to the list of the commands the user can execute.
         * @see AclSetuserArgs#addCommand(CommandType, ProtocolKeyword)
         */
        public static AclSetuserArgs addCommand(CommandType command, ProtocolKeyword subCommand) {
            return new AclSetuserArgs().addCommand(command, subCommand);
        }

        /**
         * Creates new {@link AclSetuserArgs} and adds all the commands there are in the server.
         *
         * @return new {@link AclSetuserArgs} and adds all the commands there are in the server.
         * @see AclSetuserArgs#allCommands()
         */
        public static AclSetuserArgs allCommands() {
            return new AclSetuserArgs().allCommands();
        }

        /**
         * Creates new {@link AclSetuserArgs} and removes this command to the list of the commands the user can call.
         *
         * @param command inaccessible command
         * @return new {@link AclSetuserArgs} and removes this command to the list of the commands the user can call.
         * @see AclSetuserArgs#removeCommand(CommandType)
         */
        public static AclSetuserArgs removeCommand(CommandType command) {
            return new AclSetuserArgs().removeCommand(command);
        }

        /**
         * Creates new {@link AclSetuserArgs} and removes the specified command to the list of the commands the user can execute.
         *
         * @param command inaccessible command
         * @param subCommand inaccessible subcommand
         * @return new {@link AclSetuserArgs} and removes the specified command to the list of the commands the user can execute.
         * @see AclSetuserArgs#removeCommand(CommandType, ProtocolKeyword)
         */
        public static AclSetuserArgs removeCommand(CommandType command, ProtocolKeyword subCommand) {
            return new AclSetuserArgs().removeCommand(command, subCommand);
        }

        /**
         * Creates new {@link AclSetuserArgs} and removes all the commands the user can execute.
         *
         * @return new {@link AclSetuserArgs} and removes all the commands the user can execute.
         * @see AclSetuserArgs#noCommands()
         */
        public static AclSetuserArgs noCommands() {
            return new AclSetuserArgs().noCommands();
        }

        /**
         * Creates new {@link AclSetuserArgs} and adds all the commands in the specified category to the list of commands the user is able to execute.
         *
         * @param category specified category
         * @return new {@link AclSetuserArgs} and adds all the commands in the specified category to the list of commands the user is able to execute.
         * @see AclSetuserArgs#addCategory(AclCategory)
         */
        public static AclSetuserArgs addCategory(AclCategory category) {
            return new AclSetuserArgs().addCategory(category);
        }

        /**
         * Creates new {@link AclSetuserArgs} and removes all the commands in the specified category to the list of commands the user is able to execute.
         *
         * @param category specified category
         * @return new {@link AclSetuserArgs} and removes all the commands in the specified category to the list of commands the user is able to execute.
         * @see AclSetuserArgs#removeCategory(AclCategory)
         */
        public static AclSetuserArgs removeCategory(AclCategory category) {
            return new AclSetuserArgs().removeCategory(category);
        }

        /**
         * Creates new {@link AclSetuserArgs} and sets the user as a "no password".
         *
         * @return new {@link AclSetuserArgs} and sets the user as a "no password".
         * @see AclSetuserArgs#nopass()
         */
        public static AclSetuserArgs nopass() {
            return new AclSetuserArgs().nopass();
        }

        /**
         * Creates new {@link AclSetuserArgs} and adds the specified clear text password as an hashed password in the list of the users passwords.
         *
         * @param password clear text password
         * @return new {@link AclSetuserArgs} and adds the specified clear text password as an hashed password in the list of the users passwords.
         * @see AclSetuserArgs#addPassword(String)
         */
        public static AclSetuserArgs addPassword(String password) {
            return new AclSetuserArgs().addPassword(password);
        }

        /**
         * Creates new {@link AclSetuserArgs} and adds the specified hashed password to the list of user passwords.
         *
         * @param hashedPassword hashed password
         * @return new {@link AclSetuserArgs} and adds the specified hashed password to the list of user passwords.
         * @see AclSetuserArgs#addHashedPassword(String)
         */
        public static AclSetuserArgs addHashedPassword(String hashedPassword) {
            return new AclSetuserArgs().addHashedPassword(hashedPassword);

        }

        /**
         * Creates new {@link AclSetuserArgs} and removes the specified clear text password as an hashed password in the list of the users passwords.
         *
         * @param password clear text password
         * @return new {@link AclSetuserArgs} and removes the specified clear text password as an hashed password in the list of the users passwords.
         * @see AclSetuserArgs#removePassword(String)
         */
        public static AclSetuserArgs removePassword(String password) {
            return new AclSetuserArgs().removePassword(password);

        }

        /**
         * Creates new {@link AclSetuserArgs} and removes the specified hashed password to the list of user passwords.
         *
         * @param hashedPassword hashed password
         * @return new {@link AclSetuserArgs} and removes the specified hashed password to the list of user passwords.
         * @see AclSetuserArgs#removeHashedPassword(String)
         */
        public static AclSetuserArgs removeHashedPassword(String hashedPassword) {
            return new AclSetuserArgs().removeHashedPassword(hashedPassword);

        }

        /**
         * Creates new {@link AclSetuserArgs} and removes any capability from the user.
         *
         * @return new {@link AclSetuserArgs} and removes any capability from the user.
         * @see AclSetuserArgs#reset()
         */
        public static AclSetuserArgs reset() {
            return new AclSetuserArgs().reset();
        }
    }

    /**
     * Set user active.
     *
     * @return {@code this}
     */
    public AclSetuserArgs on() {
        this.active = true;
        return this;
    }

    /**
     * Set user inactive.
     *
     * @return {@code this}
     */
    public AclSetuserArgs off() {
        this.active = false;
        return this;
    }

    /**
     * Adds accessible key pattern.
     *
     * @param keyPattern accessible key pattern
     * @return {@code this}
     */
    public AclSetuserArgs keyPattern(String keyPattern) {
        if (this.keyPatterns == null) {
            this.keyPatterns = new ArrayList<>();
        }
        this.keyPatterns.add(keyPattern);
        return this;
    }

    /**
     * Allows the user to access all the keys.
     *
     * @return {@code this}
     */
    public AclSetuserArgs allKeys() {
        this.allKeys = true;
        return this;
    }

    /**
     * Removes all the key patterns from the list of key patterns the user can access.
     *
     * @return {@code this}
     */
    public AclSetuserArgs resetKeys() {
        this.resetKeys = true;
        return this;
    }

    /**
     * Adds accessible channel pattern.
     *
     * @param channelPattern accessible channel pattern
     * @return {@code this}
     */
    public AclSetuserArgs channelPattern(String channelPattern) {
        if (this.channelPatterns == null) {
            this.channelPatterns = new ArrayList<>();
        }
        this.channelPatterns.add(channelPattern);
        return this;
    }

    /**
     * Allows the user to access all the Pub/Sub channels.
     *
     * @return {@code this}
     */
    public AclSetuserArgs allChannels() {
        this.allChannels = true;
        return this;
    }

    /**
     * Removes all channel patterns from the list of Pub/Sub channel patterns the user can access.
     *
     * @return {@code this}
     */
    public AclSetuserArgs resetChannels() {
        this.resetChannels = true;
        return this;
    }

    /**
     * Adds this command to the list of the commands the user can call.
     *
     * @param command accessible command
     * @return {@code this}
     */
    public AclSetuserArgs addCommand(CommandType command) {
        return addCommand(command, null);
    }

    /**
     * Adds all the commands there are in the server.
     *
     * @return {@code this}
     */
    public AclSetuserArgs addCommand(CommandType command, ProtocolKeyword subCommand) {
        if (this.addCommands == null) {
            this.addCommands = new ArrayList<>();
        }
        this.addCommands.add(new CommandSubcommandPair(command, subCommand));
        return this;
    }

    /**
     * Adds all the commands there are in the server.
     *
     * @return {@code this}
     */
    public AclSetuserArgs allCommands() {
        this.allCommands = true;
        return this;
    }

    /**
     * Removes this command to the list of the commands the user can call.
     *
     * @param command inaccessible command
     * @return {@code this}
     */
    public AclSetuserArgs removeCommand(CommandType command) {
        return removeCommand(command, null);
    }

    /**
     * Removes the specified command to the list of the commands the user can execute.
     *
     * @param command inaccessible command
     * @param subCommand inaccessible subcommand
     * @return {@code this}
     */
    public AclSetuserArgs removeCommand(CommandType command, ProtocolKeyword subCommand) {
        if (removeCommands == null) {
            this.removeCommands = new ArrayList<>();
        }
        this.removeCommands.add(new CommandSubcommandPair(command, subCommand));
        return this;
    }

    /**
     * Removes all the commands the user can execute.
     *
     * @return {@code this}
     */
    public AclSetuserArgs noCommands() {
        this.noCommands = true;
        return this;
    }

    /**
     * Adds all the commands in the specified category to the list of commands the user is able to execute.
     *
     * @param category specified category
     * @return {@code this}
     */
    public AclSetuserArgs addCategory(AclCategory category) {
        if (this.addCategories == null) {
            this.addCategories = new ArrayList<>();
        }
        this.addCategories.add(category);
        return this;
    }

    /**
     * Removes all the commands in the specified category to the list of commands the user is able to execute.
     *
     * @param category specified category
     * @return {@code this}
     */
    public AclSetuserArgs removeCategory(AclCategory category) {
        if (this.removeCategories == null) {
            this.removeCategories = new ArrayList<>();
        }
        this.removeCategories.add(category);
        return this;
    }

    /**
     * Sets the user as a "no password".
     *
     * @return {@code this}
     */
    public AclSetuserArgs nopass() {
        this.nopass = true;
        return this;
    }

    /**
     * Adds the specified clear text password as an hashed password in the list of the users passwords.
     *
     * @param password clear text password
     * @return {@code this}
     */
    public AclSetuserArgs addPassword(String password) {
        if (this.addPasswords == null) {
            this.addPasswords = new ArrayList<>();
        }
        this.addPasswords.add(password);
        return this;
    }

    /**
     * Adds the specified hashed password to the list of user passwords.
     *
     * @param hashedPassword hashed password
     * @return {@code this}
     */
    public AclSetuserArgs addHashedPassword(String hashedPassword) {
        if (this.addHashedPasswords == null) {
            this.addHashedPasswords = new ArrayList<>();
        }
        this.addHashedPasswords.add(hashedPassword);
        return this;
    }

    /**
     * Removes the specified clear text password as an hashed password in the list of the users passwords.
     *
     * @param password clear text password
     * @return {@code this}
     */
    public AclSetuserArgs removePassword(String password) {
        if (this.removePasswords == null) {
            this.removePasswords = new ArrayList<>();
        }
        this.removePasswords.add(password);
        return this;
    }

    /**
     * Removes the specified hashed password to the list of user passwords.
     *
     * @param hashedPassword hashed password
     * @return {@code this}
     */
    public AclSetuserArgs removeHashedPassword(String hashedPassword) {
        if (this.removeHashedPasswords == null) {
            this.removeHashedPasswords = new ArrayList<>();
        }
        this.removeHashedPasswords.add(hashedPassword);
        return this;
    }

    /**
     * Removes any capability from the user.
     *
     * @return {@code this}
     */
    public AclSetuserArgs reset() {
        this.reset = true;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        if (reset) {
            args.add(RESET);
            return;
        }

        if (active) {
            args.add(ON);
        } else {
            args.add(OFF);
        }

        if (allKeys) {
            args.add(ALLKEYS);
        }

        if (resetKeys) {
            args.add(RESETKEYS);
        }

        if (keyPatterns != null) {
            for (String glob : keyPatterns) {
                args.add("~" + glob);
            }
        }

        if (allChannels) {
            args.add(ALLCHANNELS);
        }

        if (resetChannels) {
            args.add(RESETCHANNELS);
        }

        if (channelPatterns != null) {
            for (String glob : channelPatterns) {
                args.add("&" + glob);
            }
        }

        if (allCommands) {
            args.add(ALLCOMMANDS);
        }

        if (addCommands != null) {
            for (CommandSubcommandPair command : addCommands) {
                if (command.getSubCommand() == null) {
                    args.add("+" + command.getCommand().name());
                } else {
                    args.add("+" + command.getCommand().name() + "|" + command.getSubCommand().name());
                }
            }
        }

        if (noCommands) {
            args.add(NOCOMMANDS);
        }

        if (removeCommands != null) {
            for (CommandSubcommandPair command : removeCommands) {
                if (command.getSubCommand() == null) {
                    args.add("-" + command.getCommand().name());
                } else {
                    args.add("-" + command.getCommand().name() + "|" + command.getSubCommand().name());
                }
            }
        }

        if (addCategories != null) {
            for (AclCategory category : addCategories) {
                args.add("+@" + category.name());
            }
        }

        if (removeCategories != null) {
            for (AclCategory category : removeCategories) {
                args.add("-@" + category.name());
            }
        }

        if (nopass) {
            args.add(NOPASS);
        }

        if (addPasswords != null) {
            for (String password : addPasswords) {
                args.add(">" + password);
            }
        }

        if (addHashedPasswords != null) {
            for (String password : addHashedPasswords) {
                args.add("#" + password);
            }
        }

        if (removePasswords != null) {
            for (String password : removePasswords) {
                args.add("<" + password);
            }
        }

        if (removeHashedPasswords != null) {
            for (String password : removeHashedPasswords) {
                args.add("!" + password);
            }
        }
    }

    private static class CommandSubcommandPair {

        private final CommandType command;
        private final ProtocolKeyword subCommand;

        private CommandSubcommandPair(CommandType command, ProtocolKeyword subCommand) {
            this.command = command;
            this.subCommand = subCommand;
        }

        public CommandType getCommand() {
            return command;
        }

        public ProtocolKeyword getSubCommand() {
            return subCommand;
        }
    }
}
