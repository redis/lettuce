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
package io.lettuce.test;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.Command;
import io.lettuce.test.condition.RedisConditions;
import io.lettuce.test.settings.TestSettings;

/**
 * Utility to run a {@link ThrowingCallable callback function} while Redis is configured with a password.
 *
 * @author Mark Paluch
 * @author Tugdual Grall
 */
public class WithPassword {

    /**
     * Run a {@link ThrowingCallable callback function} while Redis is configured with a password.
     *
     * @param client
     * @param callable
     */
    public static void run(RedisClient client, ThrowingCallable callable) {

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> commands = connection.sync();
        try {
            enableAuthentication(commands);
            commands.auth(TestSettings.password());

            try {
                callable.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new UndeclaredThrowableException(e);
            }
        } finally {
            disableAuthentication(commands);
            connection.close();
        }
    }

    /**
     * Enable password authentication via {@code requirepass}.
     *
     * @param commands
     */
    public static void enableAuthentication(RedisCommands<String, String> commands) {

        RedisConditions conditions = RedisConditions.of(commands);

        commands.configSet("requirepass", TestSettings.password());

        // If ACL is supported let's create a test user
        if (conditions.hasCommand("ACL")) {
            Command<String, String, List<Object>> command = CliParser.parse(
                    "ACL SETUSER " + TestSettings.aclUsername() + " on >" + TestSettings.aclPassword() + " ~cached:* +@all");
            commands.dispatch(command.getType(), command.getOutput(), command.getArgs());
        }
    }

    /**
     * Disable password authentication via {@code requirepass} and optionally the {@code ACL} command.
     *
     * @param commands
     */
    public static void disableAuthentication(RedisCommands<String, String> commands) {

        try {
            commands.auth(TestSettings.password()); // reauthenticate as default user before disabling it
        } catch (Exception e) {
        }

        RedisConditions conditions = RedisConditions.of(commands);
        commands.configSet("requirepass", "");

        if (conditions.hasCommand("ACL")) {
            Command<String, String, List<Object>> command = CliParser.parse("ACL DELUSER " + TestSettings.aclUsername());
            commands.dispatch(command.getType(), command.getOutput(), command.getArgs());

            command = CliParser.parse("acl setuser default nopass");
            commands.dispatch(command.getType(), command.getOutput(), command.getArgs());
        }
    }

    public interface ThrowingCallable {
        void call() throws Throwable;
    }
}
