/*
 * Copyright 2017-2018 the original author or authors.
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
package io.lettuce.core.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * Collection of utility methods to lookup {@link MethodHandle}s for default interface {@link Method}s.This class is part of the
 * internal API and may change without further notice.
 *
 * @author Mark Paluch
 * @since 4.4
 */
public class DefaultMethods {

    private final static MethodHandleLookup methodHandleLookup = MethodHandleLookup.getMethodHandleLookup();

    /**
     * Lookup a {@link MethodHandle} for a default {@link Method}.
     *
     * @param method must be a {@link Method#isDefault() default} {@link Method}.
     * @return the {@link MethodHandle}.
     */
    public static MethodHandle lookupMethodHandle(Method method) throws ReflectiveOperationException {

        LettuceAssert.notNull(method, "Method must not be null");
        LettuceAssert.isTrue(method.isDefault(), "Method is not a default method");

        return methodHandleLookup.lookup(method);
    }

    /**
     * Strategies for {@link MethodHandle} lookup.
     */
    enum MethodHandleLookup {

        /**
         * Open (via reflection construction of {@link Lookup}) method handle lookup. Works with Java 8 and with Java 9
         * permitting illegal access.
         */
        OPEN {

            private final Optional<Constructor<Lookup>> constructor = getLookupConstructor();

            @Override
            MethodHandle lookup(Method method) throws ReflectiveOperationException {

                Constructor<Lookup> constructor = this.constructor.orElseThrow(() -> new IllegalStateException(
                        "Could not obtain MethodHandles.lookup constructor"));

                return constructor.newInstance(method.getDeclaringClass()).unreflectSpecial(method, method.getDeclaringClass());
            }

            @Override
            boolean isAvailable() {
                return constructor.isPresent();
            }
        },

        /**
         * Encapsulated {@link MethodHandle} lookup working on Java 9.
         */
        ENCAPSULATED {

            Method privateLookupIn = findBridgeMethod();

            @Override
            MethodHandle lookup(Method method) throws ReflectiveOperationException {

                MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());

                return getLookup(method.getDeclaringClass()).findSpecial(method.getDeclaringClass(), method.getName(),
                        methodType, method.getDeclaringClass());
            }

            private Method findBridgeMethod() {

                try {
                    return MethodHandles.class.getDeclaredMethod("privateLookupIn", Class.class, Lookup.class);
                } catch (ReflectiveOperationException e) {
                    return null;
                }
            }

            private Lookup getLookup(Class<?> declaringClass) {

                Lookup lookup = MethodHandles.lookup();

                if (privateLookupIn != null) {
                    try {
                        return (Lookup) privateLookupIn.invoke(null, declaringClass, lookup);
                    } catch (ReflectiveOperationException e) {
                        return lookup;
                    }
                }

                return lookup;
            }

            @Override
            boolean isAvailable() {
                return true;
            }
        };

        /**
         * Lookup a {@link MethodHandle} given {@link Method} to look up.
         *
         * @param method must not be {@literal null}.
         * @return the method handle.
         * @throws ReflectiveOperationException
         */
        abstract MethodHandle lookup(Method method) throws ReflectiveOperationException;

        /**
         * @return {@literal true} if the lookup is available.
         */
        abstract boolean isAvailable();

        /**
         * Obtain the first available {@link MethodHandleLookup}.
         *
         * @return the {@link MethodHandleLookup}
         * @throws IllegalStateException if no {@link MethodHandleLookup} is available.
         */
        public static MethodHandleLookup getMethodHandleLookup() {

            return Arrays.stream(MethodHandleLookup.values()).filter(MethodHandleLookup::isAvailable).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No MethodHandleLookup available!"));
        }

        private static Optional<Constructor<Lookup>> getLookupConstructor() {

            try {

                Constructor<Lookup> constructor = Lookup.class.getDeclaredConstructor(Class.class);
                if (!constructor.isAccessible()) {
                    constructor.setAccessible(true);
                }

                return Optional.of(constructor);

            } catch (Exception ex) {

                // this is the signal that we are on Java 9 (encapsulated) and can't use the accessible constructor approach.
                if (ex.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException")) {
                    return Optional.empty();
                }

                throw new IllegalStateException(ex);
            }
        }
    }
}
