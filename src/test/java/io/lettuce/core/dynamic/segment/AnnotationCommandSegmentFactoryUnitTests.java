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
        assertThat(commandSegments.getCommandType().toString()).isEqualTo("not.Annotated");
    }

    @Test
    void uppercaseDot() {

        CommandMethod commandMethod = DeclaredCommandMethod
                .create(ReflectionUtils.findMethod(CommandMethods.class, "upperCase"));

        CommandSegments commandSegments = factory.createCommandSegments(commandMethod);

        assertThat(commandSegments).isEmpty();
        assertThat(commandSegments.getCommandType().toString()).isEqualTo("UPPER.CASE");
    }

    @Test
    void methodNameAsIs() {

        CommandMethod commandMethod = DeclaredCommandMethod
                .create(ReflectionUtils.findMethod(CommandMethods.class, "methodName"));

        CommandSegments commandSegments = factory.createCommandSegments(commandMethod);

        assertThat(commandSegments).isEmpty();
        assertThat(commandSegments.getCommandType().toString()).isEqualTo("methodName");
    }

    @Test
    void splitAsIs() {

        CommandMethod commandMethod = DeclaredCommandMethod
                .create(ReflectionUtils.findMethod(CommandMethods.class, "clientSetname"));

        CommandSegments commandSegments = factory.createCommandSegments(commandMethod);

        assertThat(commandSegments).hasSize(1).extracting(CommandSegment::asString).contains("Setname");
        assertThat(commandSegments.getCommandType().toString()).isEqualTo("client");
    }

    @Test
    void commandAnnotation() {

        CommandMethod commandMethod = DeclaredCommandMethod
                .create(ReflectionUtils.findMethod(CommandMethods.class, "atCommand"));

        CommandSegments commandSegments = factory.createCommandSegments(commandMethod);

        assertThat(commandSegments).hasSize(1).extracting(CommandSegment::asString).contains("WORLD");
        assertThat(commandSegments.getCommandType().toString()).isEqualTo("HELLO");
    }

    @Test
    void splitDefault() {

        CommandMethod commandMethod = DeclaredCommandMethod
                .create(ReflectionUtils.findMethod(Defaulted.class, "clientSetname"));

        CommandSegments commandSegments = factory.createCommandSegments(commandMethod);

        assertThat(commandSegments).hasSize(1).extracting(CommandSegment::asString).contains("SETNAME");
        assertThat(commandSegments.getCommandType().toString()).isEqualTo("CLIENT");
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
