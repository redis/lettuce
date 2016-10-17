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
