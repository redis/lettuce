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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Unit tests for {@link AclSetuserArgs}.
 *
 * @author Mark Paluch
 * @author Rohan Nagar
 */
class AclSetuserArgsUnitTests {

    @Test
    void shouldRemoveAllCommands() {

        AclSetuserArgs args = AclSetuserArgs.Builder.on().noCommands();
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        assertThat(commandArgs.toCommandString()).isEqualTo("ON NOCOMMANDS");
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
