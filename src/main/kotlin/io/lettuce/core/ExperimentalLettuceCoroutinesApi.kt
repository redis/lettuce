package io.lettuce.core

import kotlin.RequiresOptIn.Level.WARNING
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.*

/**
 * @author sokomishalov
 */
@RequiresOptIn(level = WARNING)
@Retention(BINARY)
@Target(
        CLASS,
        ANNOTATION_CLASS,
        PROPERTY,
        FIELD,
        LOCAL_VARIABLE,
        VALUE_PARAMETER,
        CONSTRUCTOR,
        FUNCTION,
        PROPERTY_GETTER,
        PROPERTY_SETTER,
        TYPEALIAS
)
annotation class ExperimentalLettuceCoroutinesApi