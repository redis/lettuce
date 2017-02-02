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
package com.lambdaworks.redis.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.Future;

import org.junit.Test;

import com.lambdaworks.redis.ScanArgs;
import com.lambdaworks.redis.SetArgs;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.dynamic.annotation.Command;
import com.lambdaworks.redis.dynamic.annotation.Param;
import com.lambdaworks.redis.dynamic.annotation.Value;
import com.lambdaworks.redis.dynamic.domain.Timeout;
import com.lambdaworks.redis.dynamic.output.CodecAwareOutputFactoryResolver;
import com.lambdaworks.redis.dynamic.output.OutputRegistry;
import com.lambdaworks.redis.dynamic.output.OutputRegistryCommandOutputFactoryResolver;
import com.lambdaworks.redis.dynamic.segment.AnnotationCommandSegmentFactory;
import com.lambdaworks.redis.dynamic.segment.CommandSegmentFactory;
import com.lambdaworks.redis.dynamic.support.ReflectionUtils;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * @author Mark Paluch
 */
public class CommandSegmentCommandFactoryTest {

    @Test
    public void setKeyValue() {

        RedisCommand<?, ?, ?> command = createCommand(methodOf(Commands.class, "set", String.class, String.class),
                new StringCodec(), "key", "value");

        assertThat(toString(command)).isEqualTo("SET key<key> key<value>");
    }

    @Test
    public void setKeyValueWithByteArrayCodec() {

        RedisCommand<?, ?, ?> command = createCommand(methodOf(Commands.class, "set", String.class, String.class),
                new ByteArrayCodec(), "key", "value");

        assertThat(toString(command)).isEqualTo("SET key value");
    }

    @Test
    public void setKeyValueWithHintedValue() {

        RedisCommand<?, ?, ?> command = createCommand(methodOf(Commands.class, "set2", String.class, String.class),
                new StringCodec(), "key", "value");

        assertThat(toString(command)).isEqualTo("SET key<key> value<value>");
    }

    @Test
    public void setWithArgs() {

        RedisCommand<?, ?, ?> command = createCommand(
                methodOf(Commands.class, "set", String.class, String.class, SetArgs.class), new StringCodec(), "key", "value",
                SetArgs.Builder.ex(123).nx());

        assertThat(toString(command)).isEqualTo("SET key<key> key<value> EX 123 NX");
    }

    @Test
    public void varargsMethodWithParameterIndexAccess() {

        RedisCommand<?, ?, ?> command = createCommand(
                methodOf(Commands.class, "varargsWithParamIndexes", ScanArgs.class, String[].class), new StringCodec(),
                ScanArgs.Builder.limit(1), new String[] { "a", "b" });

        assertThat(toString(command)).isEqualTo("MGET a b COUNT 1");
    }

    @Test
    public void clientSetname() {

        RedisCommand<?, ?, ?> command = createCommand(methodOf(Commands.class, "clientSetname", String.class),
                new ByteArrayCodec(), "name");

        assertThat(toString(command)).isEqualTo("CLIENT SETNAME name");
    }

    @Test
    public void annotatedClientSetname() {

        RedisCommand<?, ?, ?> command = createCommand(methodOf(Commands.class, "methodWithNamedParameters", String.class),
                new StringCodec(), "name");

        assertThat(toString(command)).isEqualTo("CLIENT SETNAME key<name>");
    }

    @Test
    public void asyncWithTimeout() {

        try {
            createCommand(methodOf(MethodsWithTimeout.class, "async", String.class, Timeout.class), new StringCodec());
            fail("Missing CommandCreationException");
        } catch (CommandCreationException e) {
            assertThat(e).hasMessageContaining("Asynchronous command methods do not support Timeout parameters");
        }
    }

    @Test
    public void syncWithTimeout() {

        createCommand(methodOf(MethodsWithTimeout.class, "sync", String.class, Timeout.class), new StringCodec(), "hello",
                null);
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
        builder.append(' ').append(command.getArgs().toCommandString());

        return builder.toString();
    }

    interface Commands {

        boolean set(String key, String value);

        @Command("SET")
        boolean set2(String key, @Value String value);

        boolean set(String key, String value, SetArgs setArgs);

        boolean clientSetname(String connectionName);

        @Command("CLIENT SETNAME :connectionName")
        boolean methodWithNamedParameters(@Param("connectionName") String connectionName);

        @Command("MGET ?1 ?0")
        String varargsWithParamIndexes(ScanArgs scanArgs, String... keys);
    }

    static interface MethodsWithTimeout {

        Future<String> async(String key, Timeout timeout);

        String sync(String key, Timeout timeout);
    }
}
