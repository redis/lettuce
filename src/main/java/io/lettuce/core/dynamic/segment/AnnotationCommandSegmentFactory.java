/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.dynamic.segment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import io.lettuce.core.LettuceStrings;
import io.lettuce.core.dynamic.CommandMethod;
import io.lettuce.core.dynamic.annotation.Command;
import io.lettuce.core.dynamic.annotation.CommandNaming;
import io.lettuce.core.dynamic.annotation.CommandNaming.LetterCase;
import io.lettuce.core.dynamic.annotation.CommandNaming.Strategy;
import io.lettuce.core.internal.LettuceAssert;

/**
 * {@link CommandSegmentFactory} implementation that creates {@link CommandSegments} considering {@link Command} and
 * {@link CommandNaming} annotations.
 *
 * @author Mark Paluch
 * @since 5.0
 */
public class AnnotationCommandSegmentFactory implements CommandSegmentFactory {

    private static final Pattern SPACE = Pattern.compile("\\s");
    private static final String INDEX_BASED_PARAM_START = "?";
    private static final String NAME_BASED_PARAM_START = ":";

    @Override
    public CommandSegments createCommandSegments(CommandMethod commandMethod) {

        if (CommandSegmentParser.INSTANCE.hasCommandString(commandMethod)) {
            return CommandSegmentParser.INSTANCE.createCommandSegments(commandMethod);
        }

        LetterCase letterCase = getLetterCase(commandMethod);
        Strategy strategy = getNamingStrategy(commandMethod);

        List<String> parts = parseMethodName(commandMethod.getName(), strategy);
        return createCommandSegments(parts, letterCase);
    }

    private CommandSegments createCommandSegments(List<String> parts, LetterCase letterCase) {

        List<CommandSegment> segments = new ArrayList<>(parts.size());

        for (String part : parts) {

            if (letterCase == LetterCase.AS_IS) {
                segments.add(CommandSegment.constant(part));
            } else {
                segments.add(CommandSegment.constant(part.toUpperCase()));
            }
        }

        return new CommandSegments(segments);
    }

    private List<String> parseMethodName(String name, Strategy strategy) {

        if (strategy == Strategy.METHOD_NAME) {
            return Collections.singletonList(name);
        }

        List<String> parts = new ArrayList<>();

        char[] chars = name.toCharArray();

        boolean previousUpperCase = false;
        StringBuffer buffer = new StringBuffer(chars.length);
        for (char theChar : chars) {

            if (!Character.isUpperCase(theChar)) {
                buffer.append(theChar);
                previousUpperCase = false;
                continue;

            }

            // Camel hump
            if (!previousUpperCase) {

                if (!LettuceStrings.isEmpty(buffer)) {

                    if (strategy == Strategy.DOT) {
                        buffer.append('.');
                    }

                    if (strategy == Strategy.SPLIT) {

                        parts.add(buffer.toString());
                        buffer = new StringBuffer(chars.length);
                    }
                }
            }

            previousUpperCase = true;
            buffer.append(theChar);
        }

        if (LettuceStrings.isNotEmpty(buffer)) {
            parts.add(buffer.toString());
        }

        return parts;
    }

    private LetterCase getLetterCase(CommandMethod commandMethod) {

        if (commandMethod.hasAnnotation(CommandNaming.class)) {
            LetterCase letterCase = commandMethod.getMethod().getAnnotation(CommandNaming.class).letterCase();
            if (letterCase != LetterCase.DEFAULT) {
                return letterCase;
            }
        }

        Class<?> declaringClass = commandMethod.getMethod().getDeclaringClass();
        CommandNaming annotation = declaringClass.getAnnotation(CommandNaming.class);
        if (annotation != null && annotation.letterCase() != LetterCase.DEFAULT) {
            return annotation.letterCase();
        }

        return LetterCase.UPPERCASE;
    }

    private Strategy getNamingStrategy(CommandMethod commandMethod) {

        if (commandMethod.hasAnnotation(CommandNaming.class)) {
            Strategy strategy = commandMethod.getMethod().getAnnotation(CommandNaming.class).strategy();
            if (strategy != Strategy.DEFAULT) {
                return strategy;
            }
        }

        Class<?> declaringClass = commandMethod.getMethod().getDeclaringClass();
        CommandNaming annotation = declaringClass.getAnnotation(CommandNaming.class);
        if (annotation != null && annotation.strategy() != Strategy.DEFAULT) {
            return annotation.strategy();
        }

        return Strategy.SPLIT;
    }

    private enum CommandSegmentParser implements CommandSegmentFactory {

        INSTANCE;

        @Override
        public CommandSegments createCommandSegments(CommandMethod commandMethod) {
            return parse(getCommandString(commandMethod));
        }

        private CommandSegments parse(String command) {

            String[] split = SPACE.split(command);

            LettuceAssert.notEmpty(split, "Command must not be empty");

            return getCommandSegments(split);
        }

        private CommandSegments getCommandSegments(String[] split) {

            List<CommandSegment> segments = new ArrayList<>();

            for (String segment : split) {

                if (segment.startsWith(INDEX_BASED_PARAM_START)) {
                    segments.add(parseIndexBasedArgument(segment));
                    continue;
                }

                if (segment.startsWith(NAME_BASED_PARAM_START)) {
                    segments.add(parseNameBasedArgument(segment));
                    continue;
                }

                segments.add(CommandSegment.constant(segment));
            }

            return new CommandSegments(segments);
        }

        private CommandSegment parseIndexBasedArgument(String segment) {

            String index = segment.substring(INDEX_BASED_PARAM_START.length());
            return getIndexBasedArgument(index);
        }

        private CommandSegment parseNameBasedArgument(String segment) {
            return CommandSegment.namedParameter(segment.substring(NAME_BASED_PARAM_START.length()));
        }

        private CommandSegment getIndexBasedArgument(String index) {
            return CommandSegment.indexedParameter(Integer.parseInt(index));
        }

        private String getCommandString(CommandMethod commandMethod) {

            Command annotation = commandMethod.getAnnotation(Command.class);
            return annotation.value();
        }

        private boolean hasCommandString(CommandMethod commandMethod) {

            if (commandMethod.hasAnnotation(Command.class)) {
                Command annotation = commandMethod.getAnnotation(Command.class);
                return LettuceStrings.isNotEmpty(annotation.value());
            }

            return false;
        }
    }
}
