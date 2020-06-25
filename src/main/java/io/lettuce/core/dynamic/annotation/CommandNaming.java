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
package io.lettuce.core.dynamic.annotation;

import java.lang.annotation.*;

/**
 * Command naming strategy for Redis command methods. Redis command methods name can be provided either by annotating method
 * with {@link Command} or derived from its name. Annotate a command interface or method with {@link CommandNaming} to set a
 * command naming {@link Strategy}.
 *
 * @author Mark Paluch
 * @since 5.0
 * @see Command
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
public @interface CommandNaming {

    /**
     * Apply a naming {@link Strategy} to transform the method name into a Redis command name.
     */
    Strategy strategy() default Strategy.DEFAULT;

    /**
     * Adjust letter case, defaults to {@link LetterCase#UPPERCASE}.
     */
    LetterCase letterCase() default LetterCase.DEFAULT;

    public enum Strategy {

        /**
         * Replace camel humps with spaces and split the method name into multiple command segments. A method named
         * {@code clientSetname} would issue a command {@code CLIENT SETNAME}.
         */
        SPLIT,

        /**
         * Replace camel humps with spaces. A method named {@code nrRun} would issue a command named {@code NR.RUN}.
         */
        DOT,

        /**
         * Passthru the command as-is. A method named {@code clientSetname} would issue a command named {@code CLIENTSETNAME}.
         */
        METHOD_NAME,

        /**
         * Not defined here which defaults to {@link #SPLIT} if nothing else found.
         */
        DEFAULT;
    }

    public enum LetterCase {
        /**
         * Keep command name as specified.
         */
        AS_IS,

        /**
         * Convert command to uppercase.
         */
        UPPERCASE,

        /**
         * Not defined here which defaults to {@link #UPPERCASE} if nothing else found.
         */
        DEFAULT;
    }

}
