/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.dynamic.annotation;

import java.lang.annotation.*;

import io.lettuce.core.dynamic.domain.Timeout;

/**
 * Redis command method annotation specifying a command string. A command string can contain the command name, a sequence of
 * command string bytes and parameter references.
 * <p>
 * Parameters: Parameters can be referenced by their name {@code :myArg} or index {@code ?0}. Additional, not referenced
 * parameters are appended to the command in the order of their appearance. Declared parameters are matched against
 * {@link io.lettuce.core.codec.RedisCodec} for codec resolution. Additional parameter types such as {@link Timeout}
 * control execution behavior and are not added to command arguments.
 * <p>
 * Usage:
 *
 * <pre class="code">
 *     &#64;Command("SET ?0 ?1")
 *     public String setKey(String key, String value)
 *
 *     &#64;Command("SET :key :value")
 *     public String setKeyNamed(@Param("key") String key, @Param("value") String value)
 * </pre>
 * <p>
 * Implementation notes: A {@link Command#value()} is split into command segments of which each segment is represented as ASCII
 * string or parameter reference.
 *
 * @author Mark Paluch
 * @since 5.0
 * @see CommandNaming
 * @see Param
 * @see Key
 * @see Value
 * @see io.lettuce.core.dynamic.codec.RedisCodecResolver
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Command {

    /**
     * Command string.
     */
    String value();
}
