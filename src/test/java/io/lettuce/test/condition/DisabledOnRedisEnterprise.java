package io.lettuce.test.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code @DisabledOnRedisEnterprise} signals that the annotated test class or test method is <em>disabled</em> when the suite
 * is running against a managed Redis Enterprise database ({@code RE_CLUSTER=true}).
 *
 * <p>
 * Use it for specs that rely on behaviour a managed Enterprise deployment does not expose (OSS-only admin commands such as
 * {@code CONFIG}/{@code ACL SETUSER}/{@code DEBUG}, OSS preview commands, or localhost-specific assumptions). When
 * {@code RE_CLUSTER} is unset the annotation has no effect and the tests run normally.
 * </p>
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@ExtendWith(DisabledOnRedisEnterpriseCondition.class)
public @interface DisabledOnRedisEnterprise {

    /**
     * @return rationale describing why the annotated element cannot run against Redis Enterprise.
     */
    String value() default "";

}
