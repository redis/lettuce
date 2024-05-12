package io.lettuce.test.condition;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

import io.lettuce.test.LettuceExtension;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * {@link ExecutionCondition} for {@link EnabledOnCommandCondition @EnabledOnCommand}.
 *
 * @see EnabledOnCommandCondition
 */
class EnabledOnCommandCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled("@EnabledOnCommand is not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {

        Optional<EnabledOnCommand> optional = AnnotationUtils.findAnnotation(context.getElement(), EnabledOnCommand.class);

        if (optional.isPresent()) {

            String command = optional.get().value();

            StatefulRedisConnection connection = new LettuceExtension().resolve(context, StatefulRedisConnection.class);

            RedisConditions conditions = RedisConditions.of(connection);
            boolean hasCommand = conditions.hasCommand(command);
            return hasCommand ? enabled("Enabled on command " + command)
                    : disabled(
                            "Disabled, command " + command + " not available on Redis version " + conditions.getRedisVersion());
        }

        return ENABLED_BY_DEFAULT;
    }

}
