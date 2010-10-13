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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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
 * BeanDefinitionRegistryPostProcessor that searchs recursively 
 * starting from a basePackage for interfaces with
 * org.mybatis.spring.annotation.Mapper annotation.
 *
 * @see org.apache.ibatis.session.SqlSessionFactory
 * @see org.mybatis.spring.MapperFactoryBean
 * @version $Id$
 */

public class MapperScanner implements BeanDefinitionRegistryPostProcessor, InitializingBean {

    private static final Log logger = LogFactory.getLog(MapperScanner.class);

    private String basePackages;
    private SqlSessionFactory sqlSessionFactory;

    public void setBasePackage(String basePackages) {
        this.basePackages = basePackages;
    }

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(sqlSessionFactory, "Property 'sqlSessionFactory' is required");
        if (basePackages == null) {
            basePackages = "";
        }
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        try {
            List<Class<?>> classes = searchForMappers();
            if (classes.size() == 0) {
                logger.debug("No MyBatis mapper was found. Make sure your mappers are annotated with @Mapper");
            } else {
                for (Class<?> mapperInterface : classes) {
                    BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(MapperFactoryBean.class).getBeanDefinition();
                    MutablePropertyValues mutablePropertyValues = beanDefinition.getPropertyValues();
                    mutablePropertyValues.addPropertyValue("sqlSessionFactory", sqlSessionFactory);
                    mutablePropertyValues.addPropertyValue("mapperInterface", mapperInterface.getCanonicalName());
                    String name = mapperInterface.getAnnotation(Mapper.class).value();
                    if (name == null || "".equals(name)) {
                        name = mapperInterface.getName();
                    }
                    registry.registerBeanDefinition(name, beanDefinition);
                }
            }
        } catch (Exception e) {
            throw new MapperScannerException("Error while scanning for MyBatis mappers", e);
        }
    }

    private List<Class<?>> searchForMappers() throws ClassNotFoundException, IOException {

        String[] basePackagesArray = StringUtils.tokenizeToStringArray(basePackages, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);

        List<Class<?>> mapperInterfaces = new ArrayList<Class<?>>();

        for (String basePackage : basePackagesArray) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = basePackage.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);
            while (resources.hasMoreElements()) {
                File dir = new File(resources.nextElement().getFile());
                searchForMappersInDirectory(dir, basePackage, mapperInterfaces);
            }
        }
        return mapperInterfaces;

    }

    private List<Class<?>> searchForMappersInDirectory(File directory, String packageName, List<Class<?>> mapperInterfaces)
            throws ClassNotFoundException {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                searchForMappersInDirectory(file, packageName + "." + file.getName(), mapperInterfaces);
            } else if (file.getName().endsWith(".class")) {
                Class<?> candidate = Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6));
                if (candidate.isAnnotationPresent(Mapper.class)) {
                    mapperInterfaces.add(candidate);
                }
            }
        }
        return mapperInterfaces;
    }

}
