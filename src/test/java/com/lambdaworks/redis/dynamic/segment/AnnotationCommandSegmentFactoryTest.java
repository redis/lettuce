package com.lambdaworks.redis.dynamic.segment;

import static org.assertj.core.api.Assertions.assertThat;

import com.lambdaworks.redis.dynamic.CommandMethod;
import org.junit.Test;

import com.lambdaworks.redis.dynamic.annotation.Command;
import com.lambdaworks.redis.dynamic.annotation.CommandNaming;
import com.lambdaworks.redis.dynamic.annotation.CommandNaming.LetterCase;
import com.lambdaworks.redis.dynamic.annotation.CommandNaming.Strategy;
import com.lambdaworks.redis.dynamic.support.ReflectionUtils;

/**
 * @author Mark Paluch
 */
public class AnnotationCommandSegmentFactoryTest {

    AnnotationCommandSegmentFactory factory = new AnnotationCommandSegmentFactory();

    @Test
    public void notAnnotatedDotAsIs() {

        CommandMethod commandMethod = new CommandMethod(ReflectionUtils.findMethod(CommandMethods.class, "notAnnotated"));

        CommandSegments commandSegments = factory.createCommandSegments(commandMethod);

        assertThat(commandSegments).isEmpty();
        assertThat(commandSegments.getCommandType().name()).isEqualTo("not.Annotated");
    }

    @Test
    public void uppercaseDot() {

        CommandMethod commandMethod = new CommandMethod(ReflectionUtils.findMethod(CommandMethods.class, "upperCase"));

        CommandSegments commandSegments = factory.createCommandSegments(commandMethod);

        assertThat(commandSegments).isEmpty();
        assertThat(commandSegments.getCommandType().name()).isEqualTo("UPPER.CASE");
    }

    @Test
    public void methodNameAsIs() {

        CommandMethod commandMethod = new CommandMethod(ReflectionUtils.findMethod(CommandMethods.class, "methodName"));

        CommandSegments commandSegments = factory.createCommandSegments(commandMethod);

        assertThat(commandSegments).isEmpty();
        assertThat(commandSegments.getCommandType().name()).isEqualTo("methodName");
    }

    @Test
    public void splitAsIs() {

        CommandMethod commandMethod = new CommandMethod(ReflectionUtils.findMethod(CommandMethods.class, "clientSetname"));

        CommandSegments commandSegments = factory.createCommandSegments(commandMethod);

        assertThat(commandSegments).hasSize(1).extracting(CommandSegment::asString).contains("Setname");
        assertThat(commandSegments.getCommandType().name()).isEqualTo("client");
    }

    @Test
    public void commandAnnotation() {

        CommandMethod commandMethod = new CommandMethod(ReflectionUtils.findMethod(CommandMethods.class, "atCommand"));

        CommandSegments commandSegments = factory.createCommandSegments(commandMethod);

        assertThat(commandSegments).hasSize(1).extracting(CommandSegment::asString).contains("WORLD");
        assertThat(commandSegments.getCommandType().name()).isEqualTo("HELLO");
    }

    @Test
    public void splitDefault() {

        CommandMethod commandMethod = new CommandMethod(ReflectionUtils.findMethod(Defaulted.class, "clientSetname"));

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