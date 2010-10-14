/*
 *    Copyright 2010 The myBatis Team
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.mybatis.spring.annotation;

import java.util.Set;

import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.MapperFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * BeanDefinitionRegistryPostProcessor that searches recursively 
 * starting from a basePackage for interfaces with
 * org.mybatis.spring.annotation.Mapper annotation.
 *
 * @see org.apache.ibatis.session.SqlSessionFactory
 * @see org.mybatis.spring.MapperFactoryBean
 * @version $Id$
 */

public class MapperScanner implements BeanDefinitionRegistryPostProcessor, InitializingBean {

    private static final Log logger = LogFactory.getLog(MapperScanner.class);

    private String basePackage;
    private SqlSessionFactory sqlSessionFactory;

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(sqlSessionFactory, "Property 'sqlSessionFactory' is required");
        Assert.notNull(basePackage, "Property 'basePackage' is required");
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Set<Class<?>> mapperInterfaces = searchForMappers();
        if (mapperInterfaces.size() == 0) {
            logger.debug("No MyBatis mapper was found. Make sure your mappers are annotated with @Mapper");
        } else {
            registerMappers(registry, mapperInterfaces);
        }
    }

    private Set<Class<?>> searchForMappers() {
        String[] basePackagesArray = 
            StringUtils.tokenizeToStringArray(basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
        ResolverUtil<Object> resolver = new ResolverUtil<Object>();
        resolver.findAnnotated(Mapper.class, basePackagesArray);
        return resolver.getClasses();
    }

    private void registerMappers(BeanDefinitionRegistry registry, Set<Class<?>> mapperInterfaces) {
        for (Class<?> mapperInterface : mapperInterfaces) {
            BeanDefinition beanDefinition = 
                BeanDefinitionBuilder.genericBeanDefinition(MapperFactoryBean.class).getBeanDefinition();
            MutablePropertyValues mutablePropertyValues = beanDefinition.getPropertyValues();
            mutablePropertyValues.addPropertyValue("sqlSessionFactory", sqlSessionFactory);
            mutablePropertyValues.addPropertyValue("mapperInterface", mapperInterface);
            String name = mapperInterface.getAnnotation(Mapper.class).value();
            if (name == null || "".equals(name)) {
                name = mapperInterface.getName();
            }
            registry.registerBeanDefinition(name, beanDefinition);
        }
    }
}
