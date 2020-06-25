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
package io.lettuce.core.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.StatusOutput;

/**
 * @author Mark Paluch
 */
class CommandWrapperUnitTests {

    private RedisCodec<String, String> codec = StringCodec.UTF8;

    private Command<String, String, String> sut;

    @BeforeEach
    final void createCommand() {

        CommandOutput<String, String, String> output = new StatusOutput<>(codec);
        sut = new Command<>(CommandType.INFO, output, null);
    }

    @Test
    void shouldAppendOnComplete() {

        AtomicReference<Boolean> v1 = new AtomicReference<>();
        AtomicReference<Boolean> v2 = new AtomicReference<>();

        CommandWrapper<String, String, String> commandWrapper = new CommandWrapper<>(sut);

        commandWrapper.onComplete(s -> v1.set(true));
        commandWrapper.onComplete(s -> v2.set(true));

        commandWrapper.complete();

        assertThat(v1.get()).isEqualTo(true);
        assertThat(v2.get()).isEqualTo(true);
    }

}
