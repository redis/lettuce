package com.lambdaworks.redis.dynamic.parameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.lambdaworks.redis.dynamic.support.*;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.internal.LettuceClassUtils;

/**
 * Abstracts a method parameter and exposes access to type and parameter information.
 * 
 * @author Mark Paluch
 * @since 5.0
 */
public class Parameter {

    private final ParameterNameDiscoverer discoverer = new CompositeParameterNameDiscoverer(
            new StandardReflectionParameterNameDiscoverer(), new AnnotationParameterNameDiscoverer());

    private final Method method;
    private final String name;
    private final int parameterIndex;
    private final TypeInformation<?> typeInformation;
    private final MethodParameter methodParameter;

    public Parameter(Method method, int parameterIndex) {

        this.method = method;
        this.parameterIndex = parameterIndex;
        this.methodParameter = new MethodParameter(method, parameterIndex);
        this.methodParameter.initParameterNameDiscovery(discoverer);
        this.name = methodParameter.getParameterName();
        this.typeInformation = ClassTypeInformation.fromMethodParameter(method, parameterIndex);
    }

    /**
     * Return the parameter annotation of the given type, if available.
     * 
     * @param annotationType the annotation type to look for
     * @return the annotation object, or {@code null} if not found
     */
    public <A extends Annotation> A findAnnotation(Class<A> annotationType) {
        return methodParameter.getParameterAnnotation(annotationType);
    }

    /**
     * Return all parameter annotations.
     * 
     * @return the {@link List} of annotation objects.
     */
    public List<? extends Annotation> getAnnotations() {

        Annotation[] annotations = method.getParameterAnnotations()[parameterIndex];
        List<Annotation> result = new ArrayList<>(annotations.length);
        Collections.addAll(result, annotations);

        return result;
    }

    /**
     *
     * @return the parameter index.
     */
    public int getParameterIndex() {
        return parameterIndex;
    }

    /**
     *
     * @return the parameter type.
     */
    public Class<?> getParameterType() {
        return method.getParameterTypes()[parameterIndex];
    }

    /**
     *
     * @return the parameter {@link TypeInformation}.
     */
    public TypeInformation<?> getTypeInformation() {
        return typeInformation;
    }

    /**
     * Check whether the parameter is assignable to {@code target}.
     * 
     * @param target must not be {@literal null}.
     * @return
     */
    public boolean isAssignableTo(Class<?> target) {

        LettuceAssert.notNull(target, "Target type must not be null");

        return LettuceClassUtils.isAssignable(target, getParameterType());
    }

    /**
     *
     * @return {@literal true} if the parameter is a special parameter.
     */
    public boolean isSpecialParameter() {
        return false;
    }

    /**
     * @return {@literal true} if the {@link Parameter} can be bound to a command.
     */
    boolean isBindable() {
        return !isSpecialParameter();
    }

    /**
     * @return the parameter name or {@literal null} if not available.
     */
    public String getName() {
        return name;
    }
}
