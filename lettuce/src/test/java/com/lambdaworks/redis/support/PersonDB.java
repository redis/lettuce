package com.lambdaworks.redis.support;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface PersonDB {

}
