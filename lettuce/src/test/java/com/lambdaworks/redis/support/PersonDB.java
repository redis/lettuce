package com.lambdaworks.redis.support;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 11.08.14 16:43
 */
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface PersonDB {

}
