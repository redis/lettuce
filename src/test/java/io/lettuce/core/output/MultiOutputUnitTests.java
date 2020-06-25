/*
 * Copyright 2018-2020 the original author or authors.
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
package io.lettuce.core.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;

/**
 * @author Mark Paluch
 */
class MultiOutputUnitTests {

    @Test
    void shouldCompleteCommand() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        Command<String, String, String> command = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8));

        output.add(command);

        output.multi(1);
        output.set(ByteBuffer.wrap("OK".getBytes()));
        output.complete(1);

        assertThat(command.getOutput().get()).isEqualTo("OK");
    }

    @Test
    void shouldReportErrorForCommand() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        Command<String, String, String> command = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8));

        output.add(command);

        output.multi(1);
        output.setError(ByteBuffer.wrap("Fail".getBytes()));
        output.complete(1);

        assertThat(command.getOutput().getError()).isEqualTo("Fail");
        assertThat(output.getError()).isNull();
    }

    @Test
    void shouldFailMulti() {

        MultiOutput<String, String> output = new MultiOutput<>(StringCodec.UTF8);
        Command<String, String, String> command = new Command<>(CommandType.APPEND, new StatusOutput<>(StringCodec.UTF8));

        output.add(command);

        output.setError(ByteBuffer.wrap("Fail".getBytes()));
        output.complete(0);

        assertThat(command.getOutput().getError()).isNull();
        assertThat(output.getError()).isEqualTo("Fail");
    }

}
