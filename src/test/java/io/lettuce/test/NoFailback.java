/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifier annotation for injecting a {@link io.lettuce.core.failover.MultiDbClient} instance with failback disabled.
 * <p>
 * Use this annotation on constructor or method parameters to receive a MultiDbClient configured with
 * {@code failbackSupported(false)}.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre class="code">
 * 
 * &#64;Inject
 * MyTest(MultiDbClient defaultClient, &#64;NoFailback MultiDbClient noFailbackClient) {
 *     // defaultClient has failback enabled (default)
 *     // noFailbackClient has failback disabled
 * }
 * </pre>
 *
 * @author Ali Takavci
 * @see LettuceExtension
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoFailback {

}
