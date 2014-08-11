package com.lambdaworks.redis.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

import com.lambdaworks.redis.RedisURI;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 09.08.14 20:32
 */
abstract class AbstractCdiBean<T> implements Bean<T> {

    protected final BeanManager beanManager;
    protected final Set<Annotation> qualifiers;
    protected final Bean<RedisURI> redisURIBean;
    protected final String name;

    public AbstractCdiBean(Bean<RedisURI> redisURIBean, BeanManager beanManager, Set<Annotation> qualifiers, String name) {
        this.redisURIBean = redisURIBean;
        this.beanManager = beanManager;
        this.qualifiers = qualifiers;
        this.name = name;
    }

    @Override
    public Set<Type> getTypes() {
        return (Set) Collections.singleton(getBeanClass());
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        Set<Class<? extends Annotation>> stereotypes = new HashSet<Class<? extends Annotation>>();

        for (Annotation annotation : getQualifiers()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.isAnnotationPresent(Stereotype.class)) {
                stereotypes.add(annotationType);
            }
        }

        return stereotypes;
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }
}
