package io.lettuce.core.dynamic.annotation;

import java.lang.annotation.*;

/**
 * Marker annotation to declare a method parameter as value.
 *
 * @author Mark Paluch
 * @see Key
 * @since 5.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Documented
public @interface Value {
}
