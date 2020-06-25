/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.dynamic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.reactivestreams.Publisher;

import io.lettuce.core.dynamic.batch.BatchExecutor;
import io.lettuce.core.dynamic.parameter.ExecutionSpecificParameters;
import io.lettuce.core.dynamic.parameter.Parameter;
import io.lettuce.core.dynamic.parameter.Parameters;
import io.lettuce.core.dynamic.support.ResolvableType;
import io.lettuce.core.dynamic.support.TypeInformation;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Abstraction of a method that is designated to execute a Redis command method. Enriches the standard {@link Method} interface
 * with specific information that is necessary to construct {@link io.lettuce.core.protocol.RedisCommand} for the method.
 *
 * @author Mark Paluch
 * @since 5.0
 */
public class DeclaredCommandMethod implements CommandMethod {

    private final Method method;

    private final ResolvableType returnType;

    private final List<Class<?>> arguments = new ArrayList<>();

    private final ExecutionSpecificParameters parameters;

    private final ResolvableType actualReturnType;

    private final boolean futureExecution;

    private final boolean reactiveExecution;

    /**
     * Create a new {@link DeclaredCommandMethod} given a {@link Method}.
     *
     * @param method must not be null.
     */
    private DeclaredCommandMethod(Method method) {
        this(method, new ExecutionSpecificParameters(method));
    }

    /**
     * Create a new {@link DeclaredCommandMethod} given a {@link Method} and {@link Parameters}.
     *
     * @param method must not be null.
     * @param parameters must not be null.
     */
    private DeclaredCommandMethod(Method method, ExecutionSpecificParameters parameters) {

        LettuceAssert.notNull(method, "Method must not be null");
        LettuceAssert.notNull(parameters, "Parameters must not be null");

        this.method = method;
        this.returnType = ResolvableType.forMethodReturnType(method);
        this.parameters = parameters;
        this.futureExecution = Future.class.isAssignableFrom(getReturnType().getRawClass());
        this.reactiveExecution = ReactiveTypes.supports(getReturnType().getRawClass());

        Collections.addAll(arguments, method.getParameterTypes());

        ResolvableType actualReturnType = this.returnType;

        while (Future.class.isAssignableFrom(actualReturnType.getRawClass())) {
            ResolvableType[] generics = actualReturnType.getGenerics();

            if (generics.length != 1) {
                break;
            }

            actualReturnType = generics[0];
        }

        this.actualReturnType = actualReturnType;
    }

    /**
     * Create a new {@link DeclaredCommandMethod} given a {@link Method}.
     *
     * @param method must not be null.
     */
    public static CommandMethod create(Method method) {
        return new DeclaredCommandMethod(method);
    }

    /**
     * @return the method {@link Parameters}.
     */
    @Override
    public Parameters<? extends Parameter> getParameters() {
        return parameters;
    }

    /**
     * @return the {@link Method}.
     */
    @Override
    public Method getMethod() {
        return method;
    }

    /**
     * @return declared {@link Method} return {@link TypeInformation}.
     */
    @Override
    public ResolvableType getReturnType() {
        return returnType;
    }

    /**
     * @return the actual {@link Method} return {@link TypeInformation} after unwrapping.
     */
    @Override
    public ResolvableType getActualReturnType() {
        return actualReturnType;
    }

    /**
     * Lookup a method annotation.
     *
     * @param annotationClass the annotation class.
     * @return the annotation object or {@code null} if not found.
     */
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return method.getAnnotation(annotationClass);
    }

    /**
     * @param annotationClass the annotation class.
     * @return {@code true} if the method is annotated with {@code annotationClass}.
     */
    @Override
    public boolean hasAnnotation(Class<? extends Annotation> annotationClass) {
        return method.getAnnotation(annotationClass) != null;
    }

    /**
     * @return the method name.
     */
    @Override
    public String getName() {
        return method.getName();
    }

    /**
     * @return {@code true} if the method uses asynchronous execution declaring {@link Future} as result type.
     */
    @Override
    public boolean isFutureExecution() {
        return futureExecution;
    }

    /**
     * @return {@code true} if the method uses reactive execution declaring {@link Publisher} as result type.
     */
    @Override
    public boolean isReactiveExecution() {
        return reactiveExecution;
    }

    /**
     * @return {@code true} if the method defines a {@link io.lettuce.core.dynamic.batch.CommandBatching} argument.
     */
    @Override
    public boolean isBatchExecution() {
        return parameters.hasCommandBatchingIndex()
                || (method.getName().equals("flush") && method.getDeclaringClass().equals(BatchExecutor.class));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DeclaredCommandMethod))
            return false;

        DeclaredCommandMethod that = (DeclaredCommandMethod) o;

        if (method != null ? !method.equals(that.method) : that.method != null)
            return false;
        if (returnType != null ? !returnType.equals(that.returnType) : that.returnType != null)
            return false;
        return arguments != null ? arguments.equals(that.arguments) : that.arguments == null;

    }

    @Override
    public int hashCode() {
        int result = method != null ? method.hashCode() : 0;
        result = 31 * result + (returnType != null ? returnType.hashCode() : 0);
        result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return method.toGenericString();
    }

}
