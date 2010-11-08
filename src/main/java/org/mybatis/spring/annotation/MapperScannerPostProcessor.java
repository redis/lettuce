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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * BeanDefinitionRegistryPostProcessor that searches recursively 
 * starting from a basePackage for interfaces with {@link Mapper} annotation
 * and registers MapperFactoryBeans.
 * <p>
 * It is usually used with autowire enabled so all the beans it creates are 
 * automatically autowired with the proper {@link SqlSessionFactory} or 
 * {@link SqlSessionTemplate} 
 * <p>
 * It there is more than one DataSource or {@link SqlSessionFactory} in the application
 * autowire cannot be used. In this case you can specify 
 * {@link SqlSessionFactory} or {@link SqlSessionTemplate} to use.
 * <p>
 * When specifying any of these beans notice that <b>bean names</b> must be
 * used instead of real references. It has to be this way because 
 * the MapperScannerPostProcessor runs very early in the Spring startup process
 * and some other post processors have not started yet (like PropertyPlaceholderConfigurer)
 * and if they are needed (for example to setup the DataSource) the start process
 * will fail. 
 * <p>
 * Configuration sample:
 * <p>
 * <pre class="code">
 * {@code
 *   <bean class="org.mybatis.spring.annotation.MapperScannerPostProcessor">
 *       <property name="basePackage" value="org.mybatis.spring.sample.mapper" />
 *       <!-- optional, notice that "value" is used, not "ref" -->
 *       <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory" />
 *   </bean>
 * }
 * </pre> 
 *
 * @see org.apache.ibatis.session.SqlSessionFactory
 * @see org.mybatis.spring.MapperFactoryBean
 * @version $Id$
 */
public class MapperScannerPostProcessor implements BeanDefinitionRegistryPostProcessor, InitializingBean {

    private final Log logger = LogFactory.getLog(this.getClass());

    private String basePackage;

    private boolean addToConfig = true;

    private String sqlSessionBeanName;

    private String sqlSessionFactoryBeanName;

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public void setAddToConfig(boolean addToConfig) {
        this.addToConfig = addToConfig;
    }

    public void setSqlSessionBeanName(String sqlSessionName) {
        this.sqlSessionBeanName = sqlSessionName;
    }

    public void setSqlSessionFactoryBeanName(String sqlSessionFactoryName) {
        this.sqlSessionFactoryBeanName = sqlSessionFactoryName;
    }

    /**
     * {@inheritDoc}
     */
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.basePackage, "Property 'basePackage' is required");
    }

    /**
     * {@inheritDoc}
     */
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // not needed in this version
    }

    /**
     * {@inheritDoc}
     */
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Set<Class<?>> mapperInterfaces = searchForMappers();
        if (mapperInterfaces.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("No MyBatis mapper was found in '"
                        + this.basePackage
                        + "' package. Make sure your mappers are annotated with @Mapper");
            }
        } else {
            registerMappers(registry, mapperInterfaces);
        }
    }

    private Set<Class<?>> searchForMappers() {
        if (logger.isDebugEnabled()) {
            logger.debug("Searching for MyBatis mappers in '"
                        + this.basePackage
                        + "' package");
        }

        String[] basePackagesArray = 
            StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
        ResolverUtil<Object> resolver = new ResolverUtil<Object>();
        resolver.findAnnotated(Mapper.class, basePackagesArray);
        return resolver.getClasses();
    }

    private void registerMappers(BeanDefinitionRegistry registry, Set<Class<?>> mapperInterfaces) {
        if (logger.isDebugEnabled()) {
            logger.debug("Registering MyBatis mappers");
        }

        for (Class<?> mapperInterface : mapperInterfaces) {
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(InternalMapperFactoryBean.class);
            beanDefinitionBuilder.addPropertyValue("mapperInterface", mapperInterface);
            beanDefinitionBuilder.addPropertyValue("addToConfig", this.addToConfig);
            if (StringUtils.hasLength(this.sqlSessionFactoryBeanName)) {
                beanDefinitionBuilder.addPropertyReference("sqlSessionFactory", this.sqlSessionFactoryBeanName);
            }
            if (StringUtils.hasLength(this.sqlSessionBeanName)) {
                beanDefinitionBuilder.addPropertyReference("sqlSession", this.sqlSessionBeanName);
            }
            String name = mapperInterface.getAnnotation(Mapper.class).value();
            if (!StringUtils.hasLength(name)) {
                name = mapperInterface.getName();
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Registering MyBatis mapper with '" 
                        + name + "' name and '" 
                        + mapperInterface.getName() + "' mapperInterface");
            }

            registry.registerBeanDefinition(name, beanDefinitionBuilder.getBeanDefinition());
        }
    }

}
