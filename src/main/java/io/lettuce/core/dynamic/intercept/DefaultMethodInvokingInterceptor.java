package io.lettuce.core.dynamic.intercept;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.lettuce.core.internal.DefaultMethods;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Invokes default interface methods. Requires {@link MethodInvocation} to implement {@link InvocationTargetProvider} to
 * determine the target object.
 *
 * @author Mark Paluch
 * @since 5.0
 * @see MethodInvocation
 * @see InvocationTargetProvider
 */
public class DefaultMethodInvokingInterceptor implements MethodInterceptor {

    private final Map<Method, MethodHandle> methodHandleCache = new ConcurrentHashMap<>();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        Method method = invocation.getMethod();

        if (!method.isDefault()) {
            return invocation.proceed();
        }

        LettuceAssert.isTrue(invocation instanceof InvocationTargetProvider,
                "Invocation must provide a target object via InvocationTargetProvider");

        InvocationTargetProvider targetProvider = (InvocationTargetProvider) invocation;

        return methodHandleCache.computeIfAbsent(method, DefaultMethodInvokingInterceptor::lookupMethodHandle)
                .bindTo(targetProvider.getInvocationTarget()).invokeWithArguments(invocation.getArguments());
    }

    private static MethodHandle lookupMethodHandle(Method method) {
        try {
            return DefaultMethods.lookupMethodHandle(method);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
