/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.Test;

import io.lettuce.core.AbstractRedisClientTest;

/**
 * @author Mark Paluch
 */
public class RedisCommandsTest extends AbstractRedisClientTest {

    @Test
    public void verifierShouldCatchMisspelledDeclarations() throws Exception {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        try {
            factory.getCommands(WithTypo.class);
            fail("Missing CommandCreationException");
        } catch (CommandCreationException e) {
            assertThat(e).hasMessageContaining("Command GAT does not exist.");
        }

    }

    @Test
    public void verifierShouldCatchTooFewParametersDeclarations() throws Exception {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        try {
            factory.getCommands(TooFewParameters.class);
            fail("Missing CommandCreationException");
        } catch (CommandCreationException e) {
            assertThat(e).hasMessageContaining("Command GET accepts 1 parameters but method declares 0 parameter");
        }

    }

    static interface TooFewParameters extends Commands {

        String get();

    }

    static interface WithTypo extends Commands {

        String gat(String key);

    }
}
