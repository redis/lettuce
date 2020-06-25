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
package io.lettuce.core.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisException;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;

/**
 * @author Mark Paluch
 */
class TransactionalCommandUnitTests {

    @Test
    void shouldCompleteOnException() {

        RedisCommand<String, String, String> inner = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8));

        TransactionalCommand<String, String, String> command = new TransactionalCommand<>(new AsyncCommand<>(inner));

        command.completeExceptionally(new RedisException("foo"));

        assertThat(command).isCompletedExceptionally();
    }

}
