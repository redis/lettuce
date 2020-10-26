/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import io.lettuce.core.ScanArgs;
import io.lettuce.core.SetArgs;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.dynamic.annotation.Command;
import io.lettuce.core.dynamic.annotation.Param;
import io.lettuce.core.dynamic.annotation.Value;
import io.lettuce.core.dynamic.domain.Timeout;
import io.lettuce.core.dynamic.output.CodecAwareOutputFactoryResolver;
import io.lettuce.core.dynamic.output.OutputRegistry;
import io.lettuce.core.dynamic.output.OutputRegistryCommandOutputFactoryResolver;
import io.lettuce.core.dynamic.segment.AnnotationCommandSegmentFactory;
import io.lettuce.core.dynamic.segment.CommandSegmentFactory;
import io.lettuce.core.dynamic.support.ReflectionUtils;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;

/**
 * @author Mark Paluch
 */
class CommandSegmentCommandFactoryUnitTests {

    @Test
    void setKeyValue() {

        RedisCommand<?, ?, ?> command = createCommand(methodOf(Commands.class, "set", String.class, String.class),
                new StringCodec(), "key", "value");

        assertThat(toString(command)).isEqualTo("SET key<key> key<value>");
        assertThat(command.getType()).isSameAs(CommandType.SET);
    }

    @Test
    void setKeyValueWithByteArrayCodec() {

        RedisCommand<?, ?, ?> command = createCommand(methodOf(Commands.class, "set", String.class, String.class),
                new ByteArrayCodec(), "key", "value");

        assertThat(toString(command)).isEqualTo("SET key value");
    }

    @Test
    void setKeyValueWithHintedValue() {

        RedisCommand<?, ?, ?> command = createCommand(methodOf(Commands.class, "set2", String.class, String.class),
                new StringCodec(), "key", "value");

        assertThat(toString(command)).isEqualTo("SET key<key> value<value>");
        assertThat(command.getType()).isSameAs(CommandType.SET);
    }

    @Test
    void lowercaseCommandResolvesToStringCommand() {

        RedisCommand<?, ?, ?> command = createCommand(methodOf(Commands.class, "set3", String.class, String.class),
                new StringCodec(), "key", "value");

        assertThat(toString(command)).isEqualTo("set key<key> value<value>");
        assertThat(command.getType()).isNotInstanceOf(CommandType.class);
    }

    @Test
    void setWithArgs() {

        RedisCommand<?, ?, ?> command = createCommand(
                methodOf(Commands.class, "set", String.class, String.class, SetArgs.class), new StringCodec(), "key", "value",
                SetArgs.Builder.ex(123).nx());

        assertThat(toString(command)).isEqualTo("SET key<key> key<value> EX 123 NX");
    }

    @Test
    void varargsMethodWithParameterIndexAccess() {

        RedisCommand<?, ?, ?> command = createCommand(
                methodOf(Commands.class, "varargsWithParamIndexes", ScanArgs.class, String[].class), new StringCodec(),
                ScanArgs.Builder.limit(1), new String[] { "a", "b" });

        assertThat(toString(command)).isEqualTo("MGET a b COUNT 1");
    }

    @Test
    void clientSetname() {

        RedisCommand<?, ?, ?> command = createCommand(methodOf(Commands.class, "clientSetname", String.class),
                new ByteArrayCodec(), "name");

        assertThat(toString(command)).isEqualTo("CLIENT SETNAME name");
    }

    @Test
    void annotatedClientSetname() {

        RedisCommand<?, ?, ?> command = createCommand(methodOf(Commands.class, "methodWithNamedParameters", String.class),
                new StringCodec(), "name");

        assertThat(toString(command)).isEqualTo("CLIENT SETNAME key<name>");
    }

    @Test
    void asyncWithTimeout() {

        try {
            createCommand(methodOf(MethodsWithTimeout.class, "async", String.class, Timeout.class), new StringCodec());
            fail("Missing CommandCreationException");
        } catch (CommandCreationException e) {
            assertThat(e).hasMessageContaining("Asynchronous command methods do not support Timeout parameters");
        }
    }

    @Test
    void syncWithTimeout() {

        createCommand(methodOf(MethodsWithTimeout.class, "sync", String.class, Timeout.class), new StringCodec(), "hello",
                null);
    }

    @Test
    void resolvesUnknownCommandToStringBackedCommandType() {

        RedisCommand<?, ?, ?> command = createCommand(methodOf(Commands.class, "unknownCommand"), new StringCodec());

        assertThat(toString(command)).isEqualTo("XYZ");
        assertThat(command.getType()).isNotInstanceOf(CommandType.class);
    }

    private CommandMethod methodOf(Class<?> commandInterface, String methodName, Class... args) {
        return DeclaredCommandMethod.create(ReflectionUtils.findMethod(commandInterface, methodName, args));
    }

    @SuppressWarnings("unchecked")
    private RedisCommand<?, ?, ?> createCommand(CommandMethod commandMethod, RedisCodec<?, ?> codec, Object... args) {

        CommandSegmentFactory segmentFactory = new AnnotationCommandSegmentFactory();
        CodecAwareOutputFactoryResolver outputFactoryResolver = new CodecAwareOutputFactoryResolver(
                new OutputRegistryCommandOutputFactoryResolver(new OutputRegistry()), codec);
        CommandSegmentCommandFactory factory = new CommandSegmentCommandFactory(
                segmentFactory.createCommandSegments(commandMethod), commandMethod, codec, outputFactoryResolver);

        return factory.createCommand(args);
    }

    @SuppressWarnings("unchecked")
    private String toString(RedisCommand<?, ?, ?> command) {

        StringBuilder builder = new StringBuilder();

        builder.append(command.getType().name());

        String commandString = command.getArgs().toCommandString();

        if (!commandString.isEmpty()) {
            builder.append(' ').append(commandString);
        }

        return builder.toString();
    }

    private interface Commands {

        boolean set(String key, String value);

        @Command("SET")
        boolean set2(String key, @Value String value);

        @Command("set")
        boolean set3(String key, @Value String value);

        boolean set(String key, String value, SetArgs setArgs);

        boolean clientSetname(String connectionName);

        @Command("CLIENT SETNAME :connectionName")
        boolean methodWithNamedParameters(@Param("connectionName") String connectionName);

        @Command("MGET ?1 ?0")
        String varargsWithParamIndexes(ScanArgs scanArgs, String... keys);

        @Command("XYZ")
        boolean unknownCommand();

    }

    private static interface MethodsWithTimeout {

        Future<String> async(String key, Timeout timeout);

        String sync(String key, Timeout timeout);

    }

}
