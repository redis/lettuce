package io.lettuce.test.condition;

import java.lang.annotation.*;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code @EnabledOnCommand} is used to signal that the annotated test class or test method is only <em>enabled</em>if the
 * specified command is available.
 *
 * <p/>
 * When applied at the class level, all test methods within that class will be enabled .
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@ExtendWith(EnabledOnCommandCondition.class)
public @interface EnabledOnCommand {

    /**
     * Name of the Redis command to be available.
     *
     * @return
     */
    String value();
}
