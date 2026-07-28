/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api.consistency;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reflection helpers for the API consistency test suite: normalized type rendering, method matching across interface flavors
 * and computation of the expected wrapped return types per flavor.
 */
public final class TypeSignatures {

    private static final Map<Class<?>, String> PRIMITIVE_BOX = new HashMap<>();

    static {
        PRIMITIVE_BOX.put(void.class, "Void");
        PRIMITIVE_BOX.put(boolean.class, "Boolean");
        PRIMITIVE_BOX.put(byte.class, "Byte");
        PRIMITIVE_BOX.put(short.class, "Short");
        PRIMITIVE_BOX.put(int.class, "Integer");
        PRIMITIVE_BOX.put(long.class, "Long");
        PRIMITIVE_BOX.put(float.class, "Float");
        PRIMITIVE_BOX.put(double.class, "Double");
        PRIMITIVE_BOX.put(char.class, "Character");
    }

    private TypeSignatures() {
    }

    /**
     * The command methods of an interface: declared, non-static, non-synthetic, sorted for stable test output.
     */
    public static List<Method> apiMethods(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(m -> !m.isSynthetic() && !m.isBridge() && !Modifier.isStatic(m.getModifiers()))
                .sorted(Comparator.comparing(Method::toString)).collect(Collectors.toList());
    }

    /**
     * Find the counterpart of {@code method} on {@code target} by name and erased parameter types (the same matching the sync
     * proxy's {@code MethodTranslator} performs at runtime).
     *
     * @return the matching method or {@code null}.
     */
    public static Method findCounterpart(Class<?> target, Method method) {
        try {
            return target.getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Render a type as a normalized, package-less string, e.g. {@code Mono<List<Double>>} or {@code Flux<KeyValue<K, V>>}.
     */
    public static String normalize(Type type) {
        return type.getTypeName().replace('$', '.').replaceAll("(?:[a-z][A-Za-z0-9_]*\\.)+", "");
    }

    /**
     * Render a type like {@link #normalize(Type)}, boxing primitives ({@code long} → {@code Long}, {@code void} →
     * {@code Void}).
     */
    public static String normalizeBoxed(Type type) {
        if (type instanceof Class<?> && ((Class<?>) type).isPrimitive()) {
            return PRIMITIVE_BOX.get(type);
        }
        return normalize(type);
    }

    /**
     * The expected async return type for a sync method: {@code RedisFuture<T>} unless the method keeps its sync signature.
     */
    public static String expectedAsyncReturnType(Method syncMethod, Class<?> syncGroup) {
        if (KnownApiDeviations.contains(KnownApiDeviations.KEEP_SYNC_RESULT_TYPE_ASYNC, syncMethod, syncGroup)) {
            return normalize(syncMethod.getGenericReturnType());
        }
        return "RedisFuture<" + normalizeBoxed(syncMethod.getGenericReturnType()) + ">";
    }

    /**
     * The expected reactive return type for a sync method, applying the override, keep-type, force-Flux, collection-to-Flux and
     * Value-wrap rules in the same order as the former {@code CreateReactiveApi} generator.
     */
    public static String expectedReactiveReturnType(Method syncMethod, Class<?> syncGroup) {
        String override = KnownApiDeviations.lookup(KnownApiDeviations.REACTIVE_RESULT_OVERRIDES, syncMethod, syncGroup);
        if (override != null) {
            return override;
        }
        if (KnownApiDeviations.contains(KnownApiDeviations.KEEP_SYNC_RESULT_TYPE_REACTIVE, syncMethod, syncGroup)) {
            return normalize(syncMethod.getGenericReturnType());
        }

        String baseType = "Mono";
        String typeArgument = normalizeBoxed(syncMethod.getGenericReturnType());

        if (KnownApiDeviations.contains(KnownApiDeviations.FORCE_FLUX, syncMethod, syncGroup)) {
            baseType = "Flux";
        } else if (typeArgument.startsWith("List<")) {
            baseType = "Flux";
            typeArgument = typeArgument.substring(5, typeArgument.length() - 1);
        } else if (typeArgument.startsWith("Set<")) {
            baseType = "Flux";
            typeArgument = typeArgument.substring(4, typeArgument.length() - 1);
        }

        if (KnownApiDeviations.contains(KnownApiDeviations.REACTIVE_VALUE_WRAP, syncMethod, syncGroup)) {
            typeArgument = "Value<" + typeArgument + ">";
        }

        return baseType + "<" + typeArgument + ">";
    }

    /**
     * The expected node-selection return type for a sync method: {@code Executions<T>} or {@code AsyncExecutions<T>}.
     */
    public static String expectedNodeSelectionReturnType(Method syncMethod, String wrapper) {
        return wrapper + "<" + normalizeBoxed(syncMethod.getGenericReturnType()) + ">";
    }

    /**
     * Whether a method consumes a {@code *StreamingChannel} (those variants are deprecated on the reactive API and absent from
     * the coroutine API).
     */
    public static boolean isStreamingChannelMethod(Method method) {
        return Arrays.stream(method.getParameterTypes()).anyMatch(p -> p.getSimpleName().contains("StreamingChannel"));
    }

    /**
     * The names of the type parameters declared by an interface, e.g. {@code [K, V]}.
     */
    public static List<String> typeParameterNames(Class<?> type) {
        List<String> names = new ArrayList<>();
        for (TypeVariable<?> variable : type.getTypeParameters()) {
            names.add(variable.getName());
        }
        return names;
    }

    /**
     * Describe a method as {@code Interface.name(Param, ...)} for assertion messages.
     */
    public static String describe(Class<?> group, Method method) {
        return group.getSimpleName() + "." + KnownApiDeviations.signatureKey(method);
    }

}
