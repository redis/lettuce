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
package org.mybatis.spring.mapper;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Set;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * BeanFactoryPostProcessor that searches recursively starting from a base package for interfaces
 * and registers them as {@link MapperFactoryBean}. Note that only interfaces with at least one method will
 * be registered; concrete classes will be ignored.
 * <p>
 * The <code>basePackage</code> property can contain more than one package name, separated by either
 * commas or semicolons.
 * <p>
 * This class supports filtering the mappers created by either specifying a marker interface or an
 * annotation. The <code>annotationClass</code> property specifies an annotation to search for. The
 * <code>markerInterface</code> property specifies a parent interface to search for. If both
 * properties are specified, mappers are added for interfaces that match <em>either</em> criteria.
 * By default, these two properties are null, so all interfaces in the given
 * <code>basePackage</code> are added as mappers.
 * <p>
 * This configurer is usually used with autowire enabled so all the beans it creates are
 * automatically autowired with the proper {@link SqlSessionFactory} or {@link SqlSessionTemplate}.
 * If there is more than one {@link SqlSessionFactory} in the application, however, autowiring
 * cannot be used. In this case you must explicitly specify either an {@link SqlSessionFactory} or
 * an {@link SqlSessionTemplate} to use.
 * <p>
 * Configuration sample:
 * <p>
 *
 * <pre class="code">
 * {@code
 *   <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
 *       <property name="basePackage" value="org.mybatis.spring.sample.mapper" />
 *       <!-- optional unless there are multiple session factories defined -->
 *       <property name="sqlSessionFactory" value="sqlSessionFactory" />
 *   </bean>
 * }
 * </pre>
 *
 * @see org.mybatis.spring.mapper.MapperFactoryBean
 * @version $Id$
 */
public class MapperScannerConfigurer implements BeanFactoryPostProcessor, InitializingBean {

    private String basePackage;

    private boolean addToConfig = true;

    private SqlSessionFactory sqlSessionFactory;

    private SqlSessionTemplate sqlSessionTemplate;

    private Class<? extends Annotation> annotationClass;

    private Class<?> markerInterface;

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public void setAddToConfig(boolean addToConfig) {
        this.addToConfig = addToConfig;
    }

    public void setAnnotationClass(Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
    }

    public void setMarkerInterface(Class<?> superClass) {
        this.markerInterface = superClass;
    }

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
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
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        Scanner scanner = new Scanner((BeanDefinitionRegistry) beanFactory);

        scanner.scan(StringUtils.tokenizeToStringArray(this.basePackage,
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    }

    private final class Scanner extends ClassPathBeanDefinitionScanner {

        public Scanner(BeanDefinitionRegistry registry) {
            super(registry);
        }

        /**
         * Configures parent scanner to search for the right interfaces.
         * It can search for all interfaces or just for those that extends a markerInterface or/and
         * those annotated with the annotationClass
         */        
        @Override
        protected void registerDefaultFilters() {
            boolean acceptAllInterfaces = true;

            // if specified, use the given annotation and / or marker interface
            if (MapperScannerConfigurer.this.annotationClass != null) {
                addIncludeFilter(new AnnotationTypeFilter(MapperScannerConfigurer.this.annotationClass));
                acceptAllInterfaces = false;
            }

            // override AssignableTypeFilter to ignore matches on the actual marker interface
            if (MapperScannerConfigurer.this.markerInterface != null) {
                addIncludeFilter(new AssignableTypeFilter(MapperScannerConfigurer.this.markerInterface) {
                    @Override
                    protected boolean matchClassName(String className) {
                        return false;
                    }
                });
                acceptAllInterfaces = false;
            }

            if (acceptAllInterfaces) {
                // default include filter that accepts all classes
                addIncludeFilter(new TypeFilter() {
                    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
                            throws IOException {
                        return true;
                    }
                });
            }

            // always exclude interfaces with no methods
            addExcludeFilter(new TypeFilter() {
                public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
                        throws IOException {
                    ClassMetadata classMetadata = metadataReader.getClassMetadata();
                    Class<?> candidateClass = null;

                    try {
                        candidateClass = getClass().getClassLoader().loadClass(classMetadata.getClassName());
                    } catch (ClassNotFoundException ex) {
                        return false;
                    }

                    if (candidateClass.getMethods().length == 0) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }

        /**
         * Calls the parent search that will search and register all the candidates.
         * Then the registered objects are post processed to set them as MapperFactoryBeans
         */        
        @Override
        protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
            Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

            if (beanDefinitions.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No MyBatis mapper was found in '"
                            + MapperScannerConfigurer.this.basePackage
                            + "' package. Please check your configuration");
                }
            } else {
                for (BeanDefinitionHolder holder : beanDefinitions) {
                    ScannedGenericBeanDefinition definition = (ScannedGenericBeanDefinition) holder.getBeanDefinition();

                    if (logger.isDebugEnabled()) {
                        logger.debug("Creating MapperFactoryBean with '"
                                + holder.getBeanName()
                                + "' name and '"
                                + definition.getBeanClassName()
                                + "' mapperInterface");
                    }

                    // the mapper interface is the original class of the bean
                    // but, the actual class of the bean is MapperFactoryBean
                    definition.getPropertyValues().add("mapperInterface", definition.getBeanClassName());
                    definition.setBeanClass(MapperFactoryBean.class);

                    definition.getPropertyValues().add("addToConfig", MapperScannerConfigurer.this.addToConfig);

                    if (MapperScannerConfigurer.this.sqlSessionFactory != null) {
                        definition.getPropertyValues().add("sqlSessionFactory",
                                MapperScannerConfigurer.this.sqlSessionFactory);
                    }

                    if (MapperScannerConfigurer.this.sqlSessionTemplate != null) {
                        definition.getPropertyValues().add("sqlSessionTemplate",
                                MapperScannerConfigurer.this.sqlSessionTemplate);
                    }
                }
            }

            return beanDefinitions;
        }

        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            return (beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent());
        }
    }

}
