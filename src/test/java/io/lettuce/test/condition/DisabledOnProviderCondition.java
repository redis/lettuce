package io.lettuce.test.condition;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

/**
 * {@link ExecutionCondition} for {@link DisabledOnProvider @DisabledOnProvider}.
 *
 * @see DisabledOnProvider
 */
class DisabledOnProviderCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled("@DisabledOnProvider is not present");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {

        Optional<DisabledOnProvider> optional = AnnotationUtils.findAnnotation(context.getElement(), DisabledOnProvider.class);

        if (optional.isPresent()) {

            String provider = optional.get().value();
            String activeProvider = System.getenv("TEST_ENV_PROVIDER");

            if (provider.equalsIgnoreCase(activeProvider)) {
                return disabled("Disabled on test environment provider '" + provider + "'");
            }

            return enabled("Test environment provider '" + provider + "' is not active");
        }

        return ENABLED_BY_DEFAULT;
    }

}
