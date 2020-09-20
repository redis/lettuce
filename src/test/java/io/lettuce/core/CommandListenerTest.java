/*
 * Copyright 2020 the original author or authors.
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

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.models.events.CommandFailedEvent;
import io.lettuce.core.models.events.CommandStartedEvent;
import io.lettuce.core.models.events.CommandSucceededEvent;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.LettuceExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.lettuce.core.ScriptOutputType.BOOLEAN;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mikhael Sokolov
 */
@ExtendWith(LettuceExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
public class CommandListenerTest extends TestSupport {

    private RedisURI uri;

    @BeforeEach
    void setUp() {
        this.uri = RedisURI.create(host, port);
    }

    @Test
    void shouldWorkRedisCommandListener() {
        List<CommandStartedEvent> startedEvents = Collections.synchronizedList(new ArrayList<>());
        List<CommandSucceededEvent> succeededEvents = Collections.synchronizedList(new ArrayList<>());
        List<CommandFailedEvent> failedEvents = Collections.synchronizedList(new ArrayList<>());

        CommandListener listener = new CommandListener() {
            @Override
            public void commandStarted(CommandStartedEvent event) {
                event.getContext().put(key, value);
                startedEvents.add(event);

                assertThat(event.getStartedAt()).isNotNull();
            }

            @Override
            public void commandSucceeded(CommandSucceededEvent event) {
                succeededEvents.add(event);

                assertThat(event.getContext().get(key)).isEqualTo(value);
                assertThat(event.getExecuteDuration()).isPositive();
            }

            @Override
            public void commandFailed(CommandFailedEvent event) {
                failedEvents.add(event);

                assertThat(event.getContext().get(key)).isEqualTo(value);
                assertThat(event.getCause()).isInstanceOf(RedisCommandExecutionException.class);
            }
        };

        ClientResources resources = ClientResources.builder().commandListeners(singletonList(listener)).build();
        RedisClient client = RedisClient.create(resources, uri);

        RedisCommands<String, String> sync = client.connect().sync();

        sync.set(key, value);
        sync.get(key);
        try {
            sync.eval("bad script", BOOLEAN);
        } catch (RedisCommandExecutionException ignored) {
        }

        assertThat(startedEvents).hasSize(3);
        assertThat(succeededEvents).hasSize(2);
        assertThat(failedEvents).hasSize(1);
    }
}
