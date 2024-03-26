package io.lettuce.core.support;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

/**
 * @author Mark Paluch
 * @since 3.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
@interface PersonDB {

}
