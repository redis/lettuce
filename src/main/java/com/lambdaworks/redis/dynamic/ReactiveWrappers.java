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

import java.util.HashSet;
import java.util.Set;

import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.internal.LettuceClassUtils;

/**
 * Converters to potentially convert the execution of a command method into a variety of wrapper types potentially being
 * available on the classpath. Currently supported:
 * <ul>
 * <li>{@link reactor.core.publisher.Mono}</li>
 * <li>{@link reactor.core.publisher.Flux}</li>
 * <li>{@link org.reactivestreams.Publisher}</li>
 * </ul>
 * 
 * @author Mark Paluch
 * @since 5.0
 */
abstract class ReactiveWrappers {

    private static final Class<?> MONO = LettuceClassUtils.findClass("reactor.core.publisher.Mono");
    private static final Class<?> FLUX = LettuceClassUtils.findClass("reactor.core.publisher.Flux");
    private static final Class<?> PUBLISHER = LettuceClassUtils.findClass("org.reactivestreams.Publisher");

    private static final Set<Class<?>> REACTIVE_WRAPPERS = new HashSet<>();
    private static final Set<Class<?>> SINGLE_WRAPPERS = new HashSet<>();
    private static final Set<Class<?>> MULTI_WRAPPERS = new HashSet<>();

    static {

        if (MONO != null) {
            REACTIVE_WRAPPERS.add(MONO);
            SINGLE_WRAPPERS.add(MONO);
        }

        if (FLUX != null) {
            REACTIVE_WRAPPERS.add(FLUX);
            MULTI_WRAPPERS.add(FLUX);
        }

        if (PUBLISHER != null) {
            REACTIVE_WRAPPERS.add(PUBLISHER);
            MULTI_WRAPPERS.add(PUBLISHER);
        }
    }

    public static boolean supports(Class<?> wrapperType) {

        LettuceAssert.notNull(wrapperType, "Wrapper type must not be null");

        return REACTIVE_WRAPPERS.contains(wrapperType);
    }

    public static boolean isSingle(Class<?> wrapperType) {

        LettuceAssert.notNull(wrapperType, "Wrapper type must not be null");

        return SINGLE_WRAPPERS.contains(wrapperType) && !isMulti(wrapperType);
    }

    public static boolean isMulti(Class<?> wrapperType) {

        LettuceAssert.notNull(wrapperType, "Wrapper type must not be null");

        return MULTI_WRAPPERS.contains(wrapperType);
    }
}
