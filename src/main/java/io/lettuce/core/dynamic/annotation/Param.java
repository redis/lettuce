package io.lettuce.core.dynamic.annotation;

import java.lang.annotation.*;

/**
 * Annotation to bind method parameters using their name.
 *
 * @author Mark Paluch
 * @see Key
 * @since 5.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Documented
public @interface Param {

    /**
     * Name of the parameter.
     *
     * @return
     */
    String value();

}
