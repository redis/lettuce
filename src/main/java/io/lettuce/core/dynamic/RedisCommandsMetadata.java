package io.lettuce.core.dynamic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Interface exposing Redis command interface metadata.
 *
 * @author Mark Paluch
 * @since 5.0
 */
interface RedisCommandsMetadata {

    Collection<Method> getMethods();

    /**
     * Returns the Redis Commands interface.
     *
     * @return
     */
    Class<?> getCommandsInterface();

    /**
     * Lookup an interface annotation.
     *
     * @param annotationClass the annotation class.
     * @return the annotation object or {@code null} if not found.
     */
    <A extends Annotation> A getAnnotation(Class<A> annotationClass);

    /**
     * @param annotationClass the annotation class.
     * @return {@code true} if the interface is annotated with {@code annotationClass}.
     */
    boolean hasAnnotation(Class<? extends Annotation> annotationClass);

}
