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
package io.lettuce.core.dynamic.segment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.lettuce.core.dynamic.CommandMethod;
import io.lettuce.core.dynamic.DeclaredCommandMethod;
import io.lettuce.core.dynamic.annotation.Command;
import io.lettuce.core.dynamic.annotation.CommandNaming;
import io.lettuce.core.dynamic.annotation.CommandNaming.LetterCase;
import io.lettuce.core.dynamic.annotation.CommandNaming.Strategy;
import io.lettuce.core.dynamic.support.ReflectionUtils;

/**
 * @author Mark Paluch
 */
class AnnotationCommandSegmentFactoryUnitTests {

    private AnnotationCommandSegmentFactory factory = new AnnotationCommandSegmentFactory();

    @Test
    void notAnnotatedDotAsIs() {

        CommandMethod commandMethod = DeclaredCommandMethod
                .create(ReflectionUtils.findMethod(CommandMethods.class, "notAnnotated"));

        CommandSegments commandSegments = factory.createCommandSegments(commandMethod);

        assertThat(commandSegments).isEmpty();
        assertThat(commandSegments.getCommandType().name()).isEqualTo("not.Annotated");
    }

    @Test
    void uppercaseDot() {

        CommandMethod commandMethod = DeclaredCommandMethod
                .create(ReflectionUtils.findMethod(CommandMethods.class, "upperCase"));

        CommandSegments commandSegments = factory.createCommandSegments(commandMethod);

        assertThat(commandSegments).isEmpty();
        assertThat(commandSegments.getCommandType().name()).isEqualTo("UPPER.CASE");
    }

    @Test
    void methodNameAsIs() {

        CommandMethod commandMethod = DeclaredCommandMethod
                .create(ReflectionUtils.findMethod(CommandMethods.class, "methodName"));

        CommandSegments commandSegments = factory.createCommandSegments(commandMethod);

        assertThat(commandSegments).isEmpty();
        assertThat(commandSegments.getCommandType().name()).isEqualTo("methodName");
    }

    @Test
    void splitAsIs() {

        CommandMethod commandMethod = DeclaredCommandMethod
                .create(ReflectionUtils.findMethod(CommandMethods.class, "clientSetname"));

        CommandSegments commandSegments = factory.createCommandSegments(commandMethod);

        assertThat(commandSegments).hasSize(1).extracting(CommandSegment::asString).contains("Setname");
        assertThat(commandSegments.getCommandType().name()).isEqualTo("client");
    }

    @Test
    void commandAnnotation() {

        CommandMethod commandMethod = DeclaredCommandMethod
                .create(ReflectionUtils.findMethod(CommandMethods.class, "atCommand"));

        CommandSegments commandSegments = factory.createCommandSegments(commandMethod);

        assertThat(commandSegments).hasSize(1).extracting(CommandSegment::asString).contains("WORLD");
        assertThat(commandSegments.getCommandType().name()).isEqualTo("HELLO");
    }

    @Test
    void splitDefault() {

        CommandMethod commandMethod = DeclaredCommandMethod
                .create(ReflectionUtils.findMethod(Defaulted.class, "clientSetname"));

        CommandSegments commandSegments = factory.createCommandSegments(commandMethod);

        assertThat(commandSegments).hasSize(1).extracting(CommandSegment::asString).contains("SETNAME");
        assertThat(commandSegments.getCommandType().name()).isEqualTo("CLIENT");
    }

    @CommandNaming(strategy = Strategy.DOT, letterCase = LetterCase.AS_IS)
    private static interface CommandMethods {

        void notAnnotated();

        @CommandNaming(letterCase = LetterCase.UPPERCASE)
        void upperCase();

        @CommandNaming(strategy = Strategy.METHOD_NAME)
        void methodName();

        @CommandNaming(strategy = Strategy.SPLIT)
        void clientSetname();

        @Command("HELLO WORLD")
        void atCommand();

    }

    private static interface Defaulted {

        void clientSetname();

    }

}
