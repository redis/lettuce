/*
 * Copyright 2021-2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;

/**
 * Unit tests for {@link AclSetuserArgs}.
 *
 * @author Mark Paluch
 * @author Rohan Nagar
 */
class AclSetuserArgsUnitTests {

    @Test
    void shouldMaintainCommandOrder() {

        AclSetuserArgs args = AclSetuserArgs.Builder.off().addCommand(CommandType.HELLO).removeCommand(CommandType.ACL)
                .addCommand(CommandType.PING);
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("OFF +HELLO -ACL +PING");
    }

    @Test
    void shouldAllowAdditionalCommandsWithReset() {

        AclSetuserArgs args = AclSetuserArgs.Builder.reset().on().addCategory(AclCategory.ADMIN);
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("RESET ON +@ADMIN");
    }

    @Test
    void shouldNotAlwaysSetActiveStatus() {

        AclSetuserArgs args = AclSetuserArgs.Builder.addCategory(AclCategory.ADMIN);
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("+@ADMIN");
    }

    @Test
    void shouldAddAllCommands() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().allCommands();
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON ALLCOMMANDS");
    }

    @Test
    void shouldRemoveAllCommands() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().noCommands();
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON NOCOMMANDS");
    }

    @Test
    void shouldAddAllKeys() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().allKeys();
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON ALLKEYS");
    }

    @Test
    void shouldResetKeys() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().resetKeys();
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON RESETKEYS");
    }

    @Test
    void shouldSetKeyPattern() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().keyPattern("*");
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON ~*");
    }

    @Test
    void shouldAddAllChannels() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().allChannels();
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON ALLCHANNELS");
    }

    @Test
    void shouldResetChannels() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().resetChannels();
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON RESETCHANNELS");
    }

    @Test
    void shouldSetChannelPattern() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().channelPattern("*");
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON &*");
    }

    @Test
    void shouldAddPassword() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().addPassword("password");
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON >password");
    }

    @Test
    void shouldAddHashedPassword() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().addHashedPassword("password");
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON #password");
    }

    @Test
    void shouldRemovePassword() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().removePassword("password");
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON <password");
    }

    @Test
    void shouldRemoveHashedPassword() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().removeHashedPassword("password");
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON !password");
    }

    @Test
    void shouldSetNoPassword() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().nopass();
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON NOPASS");
    }

    @Test
    void shouldResetPassword() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().resetpass();
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON RESETPASS");
    }

    @Test
    void shouldAddCategory() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().addCategory(AclCategory.CONNECTION);
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON +@CONNECTION");
    }

    @Test
    void shouldRemoveCategory() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().removeCategory(AclCategory.CONNECTION);
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON -@CONNECTION");
    }

}
