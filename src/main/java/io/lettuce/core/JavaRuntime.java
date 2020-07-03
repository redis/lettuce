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
package io.lettuce.core;

import static io.lettuce.core.internal.LettuceClassUtils.isPresent;

/**
 * Utility to determine which Java runtime is used.
 *
 * @author Mark Paluch
 * @deprecated since 5.3.2, this class will be removed with Lettuce 6.
 */
@Deprecated
public class JavaRuntime {

    /**
     * Constant whether the current JDK is Java 8 or higher.
     */
    public static final boolean AT_LEAST_JDK_8 = isPresent("java.lang.FunctionalInterface");

}
