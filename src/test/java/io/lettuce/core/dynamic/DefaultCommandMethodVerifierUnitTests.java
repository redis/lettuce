/*
 * Copyright 2016-2020 the original author or authors.
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
import static org.assertj.core.api.Fail.fail;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.GeoCoordinates;
import io.lettuce.core.KeyValue;
import io.lettuce.core.dynamic.annotation.Command;
import io.lettuce.core.dynamic.annotation.Param;
import io.lettuce.core.dynamic.segment.AnnotationCommandSegmentFactory;
import io.lettuce.core.dynamic.segment.CommandSegmentFactory;
import io.lettuce.core.dynamic.segment.CommandSegments;
import io.lettuce.core.dynamic.support.ReflectionUtils;
import io.lettuce.core.internal.LettuceLists;
import io.lettuce.core.models.command.CommandDetail;

/**
 * @author Mark Paluch
 */
class DefaultCommandMethodVerifierUnitTests {

    private DefaultCommandMethodVerifier sut;

    @BeforeEach
    void before() {

        CommandDetail mget = new CommandDetail("mget", -2, null, 0, 0, 0);
        CommandDetail randomkey = new CommandDetail("randomkey", 1, null, 0, 0, 0);
        CommandDetail rpop = new CommandDetail("rpop", 2, null, 0, 0, 0);
        CommandDetail lpop = new CommandDetail("lpop", 2, null, 0, 0, 0);
        CommandDetail set = new CommandDetail("set", 3, null, 0, 0, 0);
        CommandDetail geoadd = new CommandDetail("geoadd", -4, null, 0, 0, 0);

        sut = new DefaultCommandMethodVerifier(LettuceLists.newList(mget, randomkey, rpop, lpop, set, geoadd));
    }

    @Test
    void misspelledName() {

        try {
            validateMethod("megt");
            fail("Missing CommandMethodSyntaxException");
        } catch (CommandMethodSyntaxException e) {
            assertThat(e).hasMessageContaining("Command MEGT does not exist. Did you mean: MGET, SET?");
        }
    }

    @Test
    void tooFewAtLeastParameters() {

        try {
            validateMethod("mget");
            fail("Missing CommandMethodSyntaxException");
        } catch (CommandMethodSyntaxException e) {
            assertThat(e)
                    .hasMessageContaining("Command MGET requires at least 1 parameters but method declares 0 parameter(s)");
        }
    }

    @Test
    void shouldPassWithCorrectParameterCount() {

        validateMethod("lpop", String.class);
        validateMethod("rpop", String.class);
        validateMethod("mget", String.class);
        validateMethod("randomkey");
        validateMethod("set", KeyValue.class);
        validateMethod("geoadd", String.class, String.class, GeoCoordinates.class);
    }

    @Test
    void tooManyParameters() {

        try {
            validateMethod("rpop", String.class, String.class);
            fail("Missing CommandMethodSyntaxException");
        } catch (CommandMethodSyntaxException e) {
            assertThat(e).hasMessageContaining("Command RPOP accepts 1 parameters but method declares 2 parameter(s)");
        }
    }

    @Test
    void methodDoesNotAcceptParameters() {

        try {
            validateMethod("randomkey", String.class);
            fail("Missing CommandMethodSyntaxException");
        } catch (CommandMethodSyntaxException e) {
            assertThat(e).hasMessageContaining("Command RANDOMKEY accepts no parameters");
        }
    }

    private void validateMethod(String methodName, Class<?>... parameterTypes) {

        Method method = ReflectionUtils.findMethod(MyInterface.class, methodName, parameterTypes);
        CommandSegmentFactory commandSegmentFactory = new AnnotationCommandSegmentFactory();
        CommandMethod commandMethod = DeclaredCommandMethod.create(method);
        CommandSegments commandSegments = commandSegmentFactory.createCommandSegments(commandMethod);

        sut.validate(commandSegments, commandMethod);
    }

    private static interface MyInterface {

        void megt();

        void mget();

        void mget(String key);

        void mget(String key1, String key2);

        void set(KeyValue<String, String> keyValue);

        void geoadd(String key, String member, GeoCoordinates geoCoordinates);

        void randomkey();

        void randomkey(String key);

        @Command("RPOP ?0")
        void rpop(String key);

        @Command("LPOP :key")
        void lpop(@Param("key") String key);

        void rpop(String key1, String key2);

    }

}
