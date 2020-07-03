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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.lettuce.core.*;
import io.lettuce.core.dynamic.parameter.Parameter;
import io.lettuce.core.dynamic.segment.CommandSegment;
import io.lettuce.core.dynamic.segment.CommandSegments;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceLists;
import io.lettuce.core.internal.LettuceStrings;
import io.lettuce.core.models.command.CommandDetail;

/**
 * Verifies {@link CommandMethod} declarations by checking available Redis commands.
 *
 * @author Mark Paluch
 * @since 5.0
 */
class DefaultCommandMethodVerifier implements CommandMethodVerifier {

    /**
     * Default maximum property distance: 2
     */
    public static final int DEFAULT_MAX_DISTANCE = 2;

    private List<CommandDetail> commandDetails;

    /**
     * Create a new {@link DefaultCommandMethodVerifier} given a {@link List} of {@link CommandDetail}
     *
     * @param commandDetails must not be {@code null}.
     */
    public DefaultCommandMethodVerifier(List<CommandDetail> commandDetails) {

        LettuceAssert.notNull(commandDetails, "Command details must not be null");

        this.commandDetails = LettuceLists.newList(commandDetails);
    }

    /**
     * Verify a {@link CommandMethod} with its {@link CommandSegments}. This method verifies that the command exists and that
     * the required number of arguments is declared.
     *
     * @param commandSegments
     * @param commandMethod
     */
    public void validate(CommandSegments commandSegments, CommandMethod commandMethod) throws CommandMethodSyntaxException {

        LettuceAssert.notEmpty(commandSegments.getCommandType().name(), "Command name must not be empty");

        CommandDetail commandDetail = findCommandDetail(commandSegments.getCommandType().name())
                .orElseThrow(() -> syntaxException(commandSegments.getCommandType().name(), commandMethod));

        validateParameters(commandDetail, commandSegments, commandMethod);
    }

    private void validateParameters(CommandDetail commandDetail, CommandSegments commandSegments, CommandMethod commandMethod) {

        List<? extends Parameter> bindableParameters = commandMethod.getParameters().getBindableParameters();

        int availableParameterCount = calculateAvailableParameterCount(commandSegments, bindableParameters);

        // exact parameter count
        if (commandDetail.getArity() - 1 == availableParameterCount) {
            return;
        }

        // more or same parameter cound for dynamic arg count commands
        if (0 > commandDetail.getArity() && availableParameterCount >= -(commandDetail.getArity() + 1)) {
            return;
        }

        for (Parameter bindableParameter : bindableParameters) {

            // Can't verify collection-like arguments as they may contain multiple elements.
            if (bindableParameter.getTypeInformation().isCollectionLike()) {
                return;
            }
        }

        String message;
        if (commandDetail.getArity() == 1) {
            message = String.format("Command %s accepts no parameters.", commandDetail.getName().toUpperCase());
        } else if (commandDetail.getArity() < -1) {
            message = String.format("Command %s requires at least %d parameters but method declares %d parameter(s).",
                    commandDetail.getName().toUpperCase(), Math.abs(commandDetail.getArity()) - 1, availableParameterCount);
        } else {
            message = String.format("Command %s accepts %d parameters but method declares %d parameter(s).",
                    commandDetail.getName().toUpperCase(), commandDetail.getArity() - 1, availableParameterCount);
        }

        throw new CommandMethodSyntaxException(commandMethod, message);
    }

    private int calculateAvailableParameterCount(CommandSegments commandSegments,
            List<? extends Parameter> bindableParameters) {

        int count = commandSegments.size();

        for (int i = 0; i < bindableParameters.size(); i++) {

            Parameter bindableParameter = bindableParameters.get(i);

            boolean consumed = isConsumed(commandSegments, bindableParameter);

            if (consumed) {
                continue;
            }

            if (bindableParameter.isAssignableTo(KeyValue.class) || bindableParameter.isAssignableTo(ScoredValue.class)) {
                count++;
            }

            if (bindableParameter.isAssignableTo(GeoCoordinates.class) || bindableParameter.isAssignableTo(Range.class)) {
                count++;
            }

            if (bindableParameter.isAssignableTo(Limit.class)) {
                count += 2;
            }

            count++;
        }

        return count;
    }

    private boolean isConsumed(CommandSegments commandSegments, Parameter bindableParameter) {

        for (CommandSegment commandSegment : commandSegments) {
            if (commandSegment.canConsume(bindableParameter)) {
                return true;
            }
        }

        return false;
    }

    private CommandMethodSyntaxException syntaxException(String commandName, CommandMethod commandMethod) {

        CommandMatches commandMatches = CommandMatches.forCommand(commandName, commandDetails);

        if (commandMatches.hasMatches()) {
            return new CommandMethodSyntaxException(commandMethod,
                    String.format("Command %s does not exist. Did you mean: %s?", commandName, commandMatches));
        }

        return new CommandMethodSyntaxException(commandMethod, String.format("Command %s does not exist", commandName));

    }

    private Optional<CommandDetail> findCommandDetail(String commandName) {
        return commandDetails.stream().filter(commandDetail -> commandDetail.getName().equalsIgnoreCase(commandName))
                .findFirst();
    }

    static class CommandMatches {

        private final List<String> matches = new ArrayList<>();

        private CommandMatches(List<String> matches) {
            this.matches.addAll(matches);
        }

        public static CommandMatches forCommand(String command, List<CommandDetail> commandDetails) {
            return new CommandMatches(calculateMatches(command, commandDetails));
        }

        private static List<String> calculateMatches(String command, List<CommandDetail> commandDetails) {

            return commandDetails.stream()
                    //
                    .filter(commandDetail -> calculateStringDistance(commandDetail.getName().toLowerCase(),
                            command.toLowerCase()) <= DEFAULT_MAX_DISTANCE)
                    .map(CommandDetail::getName) //
                    .map(String::toUpperCase) //
                    .sorted(CommandMatches::calculateStringDistance).collect(Collectors.toList());
        }

        public boolean hasMatches() {
            return !matches.isEmpty();
        }

        @Override
        public String toString() {
            return LettuceStrings.collectionToDelimitedString(matches, ", ", "", "");
        }

        /**
         * Calculate the distance between the given two Strings according to the Levenshtein algorithm.
         *
         * @param s1 the first String
         * @param s2 the second String
         * @return the distance value
         */
        private static int calculateStringDistance(String s1, String s2) {

            if (s1.length() == 0) {
                return s2.length();
            }

            if (s2.length() == 0) {
                return s1.length();
            }

            int d[][] = new int[s1.length() + 1][s2.length() + 1];

            for (int i = 0; i <= s1.length(); i++) {
                d[i][0] = i;
            }

            for (int j = 0; j <= s2.length(); j++) {
                d[0][j] = j;
            }

            for (int i = 1; i <= s1.length(); i++) {
                char s_i = s1.charAt(i - 1);
                for (int j = 1; j <= s2.length(); j++) {
                    int cost;
                    char t_j = s2.charAt(j - 1);
                    if (s_i == t_j) {
                        cost = 0;
                    } else {
                        cost = 1;
                    }
                    d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                }
            }

            return d[s1.length()][s2.length()];
        }

    }

}
