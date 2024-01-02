/*
 * Copyright 2023-2024 the original author or authors.
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

/**
 * Class that exposes the Lettuce version. Fetches the "Implementation-Version" manifest attribute from the jar file.
 * <p>
 * Note that some ClassLoaders do not expose the package metadata, hence this class might not be able to determine the Lettuce
 * version in all environments. Consider using a reflection-based check instead &mdash; for example, checking for the presence
 * of a specific Lettuce method that you intend to call.
 *
 * @author Mark Paluch
 * @since 6.3
 */
public final class LettuceVersion {

    private LettuceVersion() {
    }

    /**
     * Return the library name.
     */
    public static String getName() {
        return "Lettuce";
    }

    /**
     * Return the full version string of the present Lettuce codebase, or {@code null} if it cannot be determined.
     *
     * @see Package#getImplementationVersion()
     */
    public static String getVersion() {
        Package pkg = LettuceVersion.class.getPackage();
        return (pkg != null ? pkg.getImplementationVersion() : null);
    }

}
