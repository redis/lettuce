/*
 * Copyright 2020-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.event.command.CommandFailedEvent;
import io.lettuce.core.event.command.CommandListener;
import io.lettuce.core.event.command.CommandStartedEvent;
import io.lettuce.core.event.command.CommandSucceededEvent;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;

/**
 * Integration tests for {@link CommandListener}.
 *
 * @author Mikhael Sokolov
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
public class CommandListenerIntegrationTests extends TestSupport {

    private final RedisClient client;

    @Inject
    public CommandListenerIntegrationTests(RedisClient client) {
        this.client = client;
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

                assertThat(event.getContext()).containsEntry(key, value);
                assertThat(event.getDuration()).isPositive();
            }

            @Override
            public void commandFailed(CommandFailedEvent event) {
                failedEvents.add(event);

                assertThat(event.getContext()).containsEntry(key, value);
                assertThat(event.getCause()).isInstanceOf(RedisCommandExecutionException.class);
            }

        };

        client.addListener(listener);

        RedisCommands<String, String> sync = client.connect().sync();

        sync.set(key, value);
        sync.get(key);
        try {
            sync.llen(key);
        } catch (RedisCommandExecutionException ignored) {
        }

        // There is race condition here between command listener callback execution and the caller who blocks on command
        // completion.
        // Listener callbacks fire on the Netty I/O thread after the sync caller has already been
        // unblocked by command.complete(), so the lists may not be populated yet at this point.
        // That is why we need to 'wait' here.
        Wait.untilEquals(3, startedEvents::size).waitOrTimeout();
        Wait.untilEquals(2, succeededEvents::size).waitOrTimeout();
        Wait.untilEquals(1, failedEvents::size).waitOrTimeout();

        client.removeListener(listener);
    }

}
