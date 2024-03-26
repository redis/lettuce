package io.lettuce;

import java.lang.annotation.*;

import org.junit.jupiter.api.Disabled;

/**
 * Annotations for tests disabled due to a Redis bug.
 *
 * @author Mark Paluch
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Disabled("Redis Bug")
public @interface RedisBug {
    String value() default "";
}
