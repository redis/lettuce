package io.lettuce.core.dynamic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.Future;

import io.lettuce.core.dynamic.parameter.Parameter;
import io.lettuce.core.dynamic.parameter.Parameters;
import io.lettuce.core.dynamic.support.ResolvableType;

/**
 * Abstraction of a method that is designated to execute a Redis command method. Enriches the standard {@link Method} interface
 * with specific information that is necessary to construct {@link io.lettuce.core.protocol.RedisCommand} for the method.
 *
 * @author Mark Paluch
 * @since 5.0
 */
public interface CommandMethod {

    /**
     * @return the method {@link Parameters}.
     */
    Parameters<? extends Parameter> getParameters();

    /**
     * @return the {@link Method}.
     */
    Method getMethod();

    /**
     * @return declared {@link Method} return {@link io.lettuce.core.dynamic.support.TypeInformation}.
     */
    ResolvableType getReturnType();

    /**
     * @return the actual {@link Method} return {@link io.lettuce.core.dynamic.support.TypeInformation} after unwrapping.
     */
    ResolvableType getActualReturnType();

    /**
     * Lookup a method annotation.
     *
     * @param annotationClass the annotation class.
     * @return the annotation object or {@code null} if not found.
     */
    <A extends Annotation> A getAnnotation(Class<A> annotationClass);

    /**
     * @param annotationClass the annotation class.
     * @return {@code true} if the method is annotated with {@code annotationClass}.
     */
    boolean hasAnnotation(Class<? extends Annotation> annotationClass);

    /**
     * @return the method name.
     */
    String getName();

    /**
     * @return {@code true} if the method uses asynchronous execution declaring {@link Future} as result type.
     */
    boolean isFutureExecution();

    /**
     * @return {@code true} if the method uses reactive execution declaring {@link org.reactivestreams.Publisher} as result
     *         type.
     */
    boolean isReactiveExecution();

    /**
     * @return {@code true} if the method defines a {@link io.lettuce.core.dynamic.batch.CommandBatching} argument.
     */
    boolean isBatchExecution();

}
