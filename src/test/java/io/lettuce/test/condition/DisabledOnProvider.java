package io.lettuce.test.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code @DisabledOnProvider} is used to signal that the annotated test class or test method is <em>disabled</em> when the
 * tests run against an externally provisioned test environment selected through the {@code TEST_ENV_PROVIDER} environment
 * variable (e.g. {@code re} for Redis Enterprise).
 *
 * <p/>
 * When applied at the class level, all test methods within that class will be disabled.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@ExtendWith(DisabledOnProviderCondition.class)
public @interface DisabledOnProvider {

    /**
     * Name of the test environment provider on which the test is disabled, matched case-insensitively against the
     * {@code TEST_ENV_PROVIDER} environment variable.
     *
     * @return
     */
    String value() default "re";

}
