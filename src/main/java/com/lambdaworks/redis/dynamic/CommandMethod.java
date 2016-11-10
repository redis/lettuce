/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.dynamic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.reactivestreams.Publisher;

import com.lambdaworks.redis.dynamic.parameter.ExecutionSpecificParameters;
import com.lambdaworks.redis.dynamic.parameter.Parameter;
import com.lambdaworks.redis.dynamic.parameter.Parameters;
import com.lambdaworks.redis.dynamic.support.ClassTypeInformation;
import com.lambdaworks.redis.dynamic.support.TypeInformation;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * Abstraction of a method that is designated to execute a Redis command method. Enriches the standard {@link Method} interface
 * with specific information that is necessary to construct {@link com.lambdaworks.redis.protocol.RedisCommand} for the method.
 * 
 * @author Mark Paluch
 * @since 5.0
 */
public class CommandMethod {

    private final Method method;
    private final TypeInformation<?> returnType;
    private final List<Class<?>> arguments = new ArrayList<>();
    private final Parameters<? extends Parameter> parameters;
    private final TypeInformation<?> actualReturnType;

    /**
     * Create a new {@link CommandMethod} given a {@link Method}.
     * 
     * @param method must not be null.
     */
    public CommandMethod(Method method) {
        this(method, new ExecutionSpecificParameters(method));
    }

    /**
     * Create a new {@link CommandMethod} given a {@link Method} and {@link Parameters}.
     * 
     * @param method must not be null.
     * @param parameters must not be null.
     */
    public CommandMethod(Method method, Parameters<?> parameters) {

        LettuceAssert.notNull(method, "Method must not be null");
        LettuceAssert.notNull(parameters, "Parameters must not be null");

        this.method = method;
        this.returnType = ClassTypeInformation.fromReturnTypeOf(method);
        this.parameters = parameters;
        Collections.addAll(arguments, method.getParameterTypes());

        TypeInformation<?> actualReturnType = this.returnType;

        while (Future.class.isAssignableFrom(actualReturnType.getType())
                || ReactiveTypes.supports(actualReturnType.getType())) {
            actualReturnType = actualReturnType.getComponentType();
        }

        this.actualReturnType = actualReturnType;
    }

    /**
     *
     * @return the method {@link Parameters}.
     */
    public Parameters<? extends Parameter> getParameters() {
        return parameters;
    }

    /**
     *
     * @return the {@link Method}.
     */
    public Method getMethod() {
        return method;
    }

    /**
     *
     * @return declared {@link Method} return {@link TypeInformation}.
     */
    public TypeInformation<?> getReturnType() {
        return returnType;
    }

    /**
     *
     * @return the actual {@link Method} return {@link TypeInformation} after unwrapping.
     */
    public TypeInformation<?> getActualReturnType() {
        return actualReturnType;
    }

    /**
     * Lookup a method annotation.
     * 
     * @param annotationClass the annotation class.
     * @return the annotation object or {@literal null} if not found.
     */
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return method.getAnnotation(annotationClass);
    }

    /**
     *
     * @param annotationClass the annotation class.
     * @return {@literal true} if the method is annotated with {@code annotationClass}.
     */
    public boolean hasAnnotation(Class<? extends Annotation> annotationClass) {
        return method.getAnnotation(annotationClass) != null;
    }

    /**
     *
     * @return the method name.
     */
    public String getName() {
        return method.getName();
    }

    /**
     *
     * @return {@literal true} if the method uses asynchronous execution declaring {@link Future} as result type.
     */
    public boolean isFutureExecution() {
        return Future.class.isAssignableFrom(getReturnType().getType());
    }

    /**
     *
     * @return {@literal true} if the method uses reactive execution declaring {@link Publisher} as result type.
     */
    public boolean isReactiveExecution() {
        return ReactiveTypes.supports(getReturnType().getType());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CommandMethod))
            return false;

        CommandMethod that = (CommandMethod) o;

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
