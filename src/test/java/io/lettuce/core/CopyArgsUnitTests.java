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

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link CopyArgs}.
 *
 * @author Mark Paluch
 */
class CopyArgsUnitTests {

    @Test
    void shouldRenderFullArgs() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        CopyArgs.Builder.destinationDb(1).replace(true).build(args);

        assertThat(args.toCommandString()).isEqualTo("DB 1 REPLACE");
    }

    @Test
    void shouldRenderNoArgs() {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        CopyArgs.Builder.replace(false).build(args);

        assertThat(args.toCommandString()).isEmpty();
    }

}
