package io.lettuce.test.condition;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

import io.lettuce.test.settings.RedisEnterpriseSettings;

/**
 * {@link ExecutionCondition} for {@link DisabledOnRedisEnterprise @DisabledOnRedisEnterprise}.
 *
 * @see DisabledOnRedisEnterprise
 */
class DisabledOnRedisEnterpriseCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled("@DisabledOnRedisEnterprise is not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {

        if (!RedisEnterpriseSettings.isEnabled()) {
            return ENABLED_BY_DEFAULT;
        }

        Optional<DisabledOnRedisEnterprise> optional = AnnotationUtils.findAnnotation(context.getElement(),
                DisabledOnRedisEnterprise.class);

        if (optional.isPresent()) {
            String reason = optional.get().value();
            return disabled("Disabled on Redis Enterprise" + (reason.isEmpty() ? "" : ": " + reason));
        }

        return ENABLED_BY_DEFAULT;
    }

}
