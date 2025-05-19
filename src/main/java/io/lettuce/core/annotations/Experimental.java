/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark APIs as experimental development.
 * <p>
 * Classes, methods, fields and constructors marked with this annotation may be renamed, changed or even removed in a future minor version. This annotation doesn't mean that the implementation has an experimental quality.
 * <p>
 * If a type is marked with this annotation, all its members are considered experimental.
 *
 * @author Tihomir Mateev
 * @since 6.7
 */
@Documented
@Target({ ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
public @interface Experimental {
}
