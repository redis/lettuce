package com.lambdaworks.redis.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * Abstract base class for invocation handlers.
 * 
 * @since 4.2
 */
public abstract class AbstractInvocationHandler implements InvocationHandler {

    private static final Object[] NO_ARGS = {};

    /**
     * {@inheritDoc}
     *
     * <p>
     * <ul>
     * <li>{@code proxy.hashCode()} delegates to {@link AbstractInvocationHandler#hashCode}
     * <li>{@code proxy.toString()} delegates to {@link AbstractInvocationHandler#toString}
     * <li>{@code proxy.equals(argument)} returns true if:
     * <ul>
     * <li>{@code proxy} and {@code argument} are of the same type
     * <li>and {@link AbstractInvocationHandler#equals} returns true for the {@link InvocationHandler} of {@code argument}
     * </ul>
     * <li>other method calls are dispatched to {@link #handleInvocation}.
     * </ul>
     */
    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (args == null) {
            args = NO_ARGS;
        }
        if (args.length == 0 && method.getName().equals("hashCode")) {
            return hashCode();
        }
        if (args.length == 1 && method.getName().equals("equals") && method.getParameterTypes()[0] == Object.class) {
            Object arg = args[0];
            if (arg == null) {
                return false;
            }
            if (proxy == arg) {
                return true;
            }
            return isProxyOfSameInterfaces(arg, proxy.getClass()) && equals(Proxy.getInvocationHandler(arg));
        }
        if (args.length == 0 && method.getName().equals("toString")) {
            return toString();
        }
        return handleInvocation(proxy, method, args);
    }

    /**
     * {@link #invoke} delegates to this method upon any method invocation on the proxy instance, except {@link Object#equals},
     * {@link Object#hashCode} and {@link Object#toString}. The result will be returned as the proxied method's return value.
     * 
     * <p>
     * Unlike {@link #invoke}, {@code args} will never be null. When the method has no parameter, an empty array is passed in.
     */
    protected abstract Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable;

    /**
     * By default delegates to {@link Object#equals} so instances are only equal if they are identical.
     * {@code proxy.equals(argument)} returns true if:
     * <ul>
     * <li>{@code proxy} and {@code argument} are of the same type
     * <li>and this method returns true for the {@link InvocationHandler} of {@code argument}
     * </ul>
     * <p>
     * Subclasses can override this method to provide custom equality.
     */
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    /**
     * By default delegates to {@link Object#hashCode}. The dynamic proxies' {@code hashCode()} will delegate to this method.
     * Subclasses can override this method to provide custom equality.
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * By default delegates to {@link Object#toString}. The dynamic proxies' {@code toString()} will delegate to this method.
     * Subclasses can override this method to provide custom string representation for the proxies.
     */
    @Override
    public String toString() {
        return super.toString();
    }

    private static boolean isProxyOfSameInterfaces(Object arg, Class<?> proxyClass) {
        return proxyClass.isInstance(arg)
                // Equal proxy instances should mostly be instance of proxyClass
                // Under some edge cases (such as the proxy of JDK types serialized and then deserialized)
                // the proxy type may not be the same.
                // We first check isProxyClass() so that the common case of comparing with non-proxy objects
                // is efficient.
                || (Proxy.isProxyClass(arg.getClass())
                        && Arrays.equals(arg.getClass().getInterfaces(), proxyClass.getInterfaces()));
    }

    protected static class MethodTranslator {

        private final Map<Method, Method> map = new HashMap<>();

        public MethodTranslator(Class<?> delegate, Class<?>... methodSources) {

            List<Method> methods = new ArrayList<>();
            for (Class<?> sourceClass : methodSources) {
                methods.addAll(getMethods(sourceClass));
            }

            for (Method method : methods) {

                try {
                    map.put(method, delegate.getMethod(method.getName(), method.getParameterTypes()));
                } catch (NoSuchMethodException ignore) {
                }
            }
        }

        private Collection<? extends Method> getMethods(Class<?> sourceClass) {

            Set<Method> result = new HashSet<>();

            Class<?> searchType = sourceClass;
            while (searchType != null && searchType != Object.class) {

                result.addAll(filterPublicMethods(Arrays.asList(sourceClass.getDeclaredMethods())));

                if (sourceClass.isInterface()) {
                    Class<?>[] interfaces = sourceClass.getInterfaces();
                    for (Class<?> interfaceClass : interfaces) {
                        result.addAll(getMethods(interfaceClass));
                    }

                    searchType = null;
                } else {

                    searchType = searchType.getSuperclass();
                }
            }

            return result;
        }

        private Collection<? extends Method> filterPublicMethods(List<Method> methods) {
            List<Method> result = new ArrayList<>(methods.size());

            for (Method method : methods) {
                if (Modifier.isPublic(method.getModifiers())) {
                    result.add(method);
                }
            }

            return result;
        }

        public Method get(Method key) {

            Method result = map.get(key);
            if (result != null) {
                return result;
            }
            throw new IllegalStateException("Cannot find source method " + key);
        }
    }
}