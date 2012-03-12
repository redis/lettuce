/*
 *    Copyright 2010-2012 The MyBatis Team
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
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * BeanDefinitionRegistryPostProcessor that searches recursively starting from a base package for
 * interfaces and registers them as {@code MapperFactoryBean}. Note that only interfaces with at
 * least one method will be registered; concrete classes will be ignored.
 * <p>
 * The {@code basePackage} property can contain more than one package name, separated by either
 * commas or semicolons.
 * <p>
 * This class supports filtering the mappers created by either specifying a marker interface or an
 * annotation. The {@code annotationClass} property specifies an annotation to search for. The
 * {@code markerInterface} property specifies a parent interface to search for. If both properties
 * are specified, mappers are added for interfaces that match <em>either</em> criteria. By default,
 * these two properties are null, so all interfaces in the given {@code basePackage} are added as
 * mappers.
 * <p>
 * This configurer is usually used with autowire enabled so all the beans it creates are
 * automatically autowired with the proper {@code SqlSessionFactory} or {@code SqlSessionTemplate}.
 * If there is more than one {@code SqlSessionFactory} in the application, however, autowiring
 * cannot be used. In this case you must explicitly specify either an {@code SqlSessionFactory} or
 * an {@code SqlSessionTemplate} to use via the <em>bean name</em> properties. Bean names are used
 * rather than actual objects because Spring does not initialize property placeholders until after
 * this class is processed. Passing in an actual object which may require placeholders (i.e. DB user
 * / password) will fail. Using bean names defers actual object creation until later in the startup
 * process, after all placeholder substituation is completed. However, note that this configurer
 * does support property placeholders of its <em>own</em> properties. The <code>basePackage</code>
 * and bean name properties all support <code>${property}</code> style substitution.
 * <p>
 * Configuration sample:
 * <p>
 * 
 * <pre class="code">
 * {@code
 *   <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
 *       <property name="basePackage" value="org.mybatis.spring.sample.mapper" />
 *       <!-- optional unless there are multiple session factories defined -->
 *       <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory" />
 *   </bean>
 * }
 * </pre>
 * 
 * @see MapperFactoryBean
 * @version $Id$
 */
public class MapperScannerConfigurer implements BeanDefinitionRegistryPostProcessor, InitializingBean, ApplicationContextAware, BeanNameAware {

  private String basePackage;

  private boolean addToConfig = true;

  private SqlSessionFactory sqlSessionFactory;

  private SqlSessionTemplate sqlSessionTemplate;

  private String sqlSessionTemplateBeanName;

  private String sqlSessionFactoryBeanName;

  private Class<? extends Annotation> annotationClass;

  private Class<?> markerInterface;

  private ApplicationContext applicationContext;

  private String beanName;

  /**
   * This property lets you set the base package for your mapper interface files. 
   * <p>
   * You can set more than one package by using a semicolon or comma as a separator. 
   * <p>
   * Mappers will be searched for recursively starting in the specified package(s).
   * 
   * @param basePackage base package name
   */
  public void setBasePackage(String basePackage) {
    this.basePackage = basePackage;
  }

  /**
   * Same as {@code MapperFactoryBean#setAddToConfig(boolean)}
   * 
   * @param addToConfig
   * @see MapperFactoryBean#setAddToConfig(boolean)
   */
  public void setAddToConfig(boolean addToConfig) {
    this.addToConfig = addToConfig;
  }

  /**
   * This property specifies the annotation that the scanner will search for. 
   * <p>
   * The scanner will register all interfaces in the base package that also have the
   * specified annotation.
   * <p>
   * Note this can be combined with markerInterface.
   * 
   * @param annotationClass annotation class
   */
  public void setAnnotationClass(Class<? extends Annotation> annotationClass) {
    this.annotationClass = annotationClass;
  }

  /**
   * This property specifies the parent that the scanner will search for. 
   * <p>
   * The scanner will register all interfaces in the base package that also have the
   * specified interface class as a parent.
   * <p>
   * Note this can be combined with annotationClass.
   * 
   * @param superClass parent class
   */
  public void setMarkerInterface(Class<?> superClass) {
    this.markerInterface = superClass;
  }

  /**
   * Specifies which {@code SqlSessionTemplate} to use in the case that there is 
   * more than one in the spring context. Usually this is only needed when you 
   * have more than one datasource.
   * <p>
   * Use {@link #setSqlSessionTemplateBeanName(String)} instead
   * 
   * @param sqlSessionTemplate
   */
  @Deprecated
  public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
    this.sqlSessionTemplate = sqlSessionTemplate;
  }

  /**
   * Specifies which {@code SqlSessionTemplate} to use in the case that there is 
   * more than one in the spring context. Usually this is only needed when you 
   * have more than one datasource.
   * <p>
   * Note bean names are used, not bean references. This is because the scanner 
   * loads early during the start process and it is too early to build mybatis
   * object instances. 
   * 
   * @since 1.1.0
   * 
   * @param sqlSessionTemplateName Bean name of the {@code SqlSessionTemplate}
   */
  public void setSqlSessionTemplateBeanName(String sqlSessionTemplateName) {
    this.sqlSessionTemplateBeanName = sqlSessionTemplateName;
  }

  /**
   * Specifies which {@code SqlSessionFactory} to use in the case that there is 
   * more than one in the spring context. Usually this is only needed when you 
   * have more than one datasource.
   * <p>
   * Use {@link #setSqlSessionFactoryBeanName(String)} instead.
   * 
   * @param sqlSessionFactory
   */
  @Deprecated
  public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
  }

  /**
   * Specifies which {@code SqlSessionFactory} to use in the case that there is 
   * more than one in the spring context. Usually this is only needed when you 
   * have more than one datasource.
   * <p>
   * Note bean names are used, not bean references. This is because the scanner 
   * loads early during the start process and it is too early to build mybatis
   * object instances. 
   * 
   * @since 1.1.0
   * 
   * @param sqlSessionFactoryName Bean name of the {@code SqlSessionFactory}
   */
  public void setSqlSessionFactoryBeanName(String sqlSessionFactoryName) {
    this.sqlSessionFactoryBeanName = sqlSessionFactoryName;
  }

  /**
   * {@inheritDoc}
   */
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * {@inheritDoc}
   */
  public void setBeanName(String name) {
    this.beanName = name;
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
  }

  /**
   * {@inheritDoc}
   */
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
    processPropertyPlaceHolders();

    Scanner scanner = new Scanner(beanDefinitionRegistry);
    scanner.setResourceLoader(this.applicationContext);

    scanner.scan(StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
  }

  /*
   * BeanDefinitionRegistries are called early in application startup, before
   * BeanFactoryPostProcessors. This means that PropertyResourceConfigurers will not have been
   * loaded and any property substitution of this class' properties will fail. To avoid this, find
   * any PropertyResourceConfigurers defined in the context and run them on this class' bean
   * definition. Then update the values.
   */
  private void processPropertyPlaceHolders() {
    Map<String, PropertyResourceConfigurer> prcs = applicationContext.getBeansOfType(PropertyResourceConfigurer.class);

    if (!prcs.isEmpty() && applicationContext instanceof GenericApplicationContext) {
      BeanDefinition mapperScannerBean = ((GenericApplicationContext) applicationContext)
          .getBeanFactory().getBeanDefinition(beanName);

      // PropertyResourceConfigurer does not expose any methods to explicitly perform
      // property placeholder substitution. Instead, create a BeanFactory that just
      // contains this mapper scanner and post process the factory.
      DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
      factory.registerBeanDefinition(beanName, mapperScannerBean);

      for (PropertyResourceConfigurer prc : prcs.values()) {
        prc.postProcessBeanFactory(factory);
      }

      PropertyValues values = mapperScannerBean.getPropertyValues();

      this.basePackage = updatePropertyValue("basePackage", values);
      this.sqlSessionFactoryBeanName = updatePropertyValue("sqlSessionFactoryBeanName", values);
      this.sqlSessionTemplateBeanName = updatePropertyValue("sqlSessionTemplateBeanName", values);
    }
  }

  private String updatePropertyValue(String propertyName, PropertyValues values) {
    PropertyValue property = values.getPropertyValue(propertyName);

    if (property == null) {
      return null;
    }

    Object value = property.getValue();

    if (value == null) {
      return null;
    } else if (value instanceof String) {
      return value.toString();
    } else if (value instanceof TypedStringValue) {
      return ((TypedStringValue) value).getValue();
    } else {
      return null;
    }
  }

  private final class Scanner extends ClassPathBeanDefinitionScanner {

    public Scanner(BeanDefinitionRegistry registry) {
      super(registry);
    }

    /**
     * Configures parent scanner to search for the right interfaces. It can search for all
     * interfaces or just for those that extends a markerInterface or/and those annotated with
     * the annotationClass
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
          public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
            return true;
          }
        });
      }

      // exclude package-info.java
      addExcludeFilter(new TypeFilter() {
        public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
          String className = metadataReader.getClassMetadata().getClassName();
          return className.endsWith("package-info");
        }
      });
    }

    /**
     * Calls the parent search that will search and register all the candidates. Then the
     * registered objects are post processed to set them as MapperFactoryBeans
     */
    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
      Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

      if (beanDefinitions.isEmpty()) {
        logger.warn("No MyBatis mapper was found in '" + MapperScannerConfigurer.this.basePackage
            + "' package. Please check your configuration.");
      } else {
        for (BeanDefinitionHolder holder : beanDefinitions) {
          GenericBeanDefinition definition = (GenericBeanDefinition) holder.getBeanDefinition();

          if (logger.isDebugEnabled()) {
            logger.debug("Creating MapperFactoryBean with name '" + holder.getBeanName()
                + "' and '" + definition.getBeanClassName() + "' mapperInterface");
          }

          // the mapper interface is the original class of the bean
          // but, the actual class of the bean is MapperFactoryBean
          definition.getPropertyValues().add("mapperInterface", definition.getBeanClassName());
          definition.setBeanClass(MapperFactoryBean.class);

          definition.getPropertyValues().add("addToConfig", MapperScannerConfigurer.this.addToConfig);

          if (StringUtils.hasLength(MapperScannerConfigurer.this.sqlSessionFactoryBeanName)) {
            definition.getPropertyValues().add("sqlSessionFactory",
                new RuntimeBeanReference(MapperScannerConfigurer.this.sqlSessionFactoryBeanName));
          } else if (MapperScannerConfigurer.this.sqlSessionFactory != null) {
            definition.getPropertyValues().add("sqlSessionFactory", MapperScannerConfigurer.this.sqlSessionFactory);
          }

          if (StringUtils.hasLength(MapperScannerConfigurer.this.sqlSessionTemplateBeanName)) {
            definition.getPropertyValues().add("sqlSessionTemplate",
                new RuntimeBeanReference(MapperScannerConfigurer.this.sqlSessionTemplateBeanName));
          } else if (MapperScannerConfigurer.this.sqlSessionTemplate != null) {
            definition.getPropertyValues().add("sqlSessionTemplate", MapperScannerConfigurer.this.sqlSessionTemplate);
          }
        }
      }

      return beanDefinitions;
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
      return (beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent());
    }

    @Override
    protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
      if (super.checkCandidate(beanName, beanDefinition)) {
        return true;
      } else {
        logger.warn("Skipping MapperFactoryBean with name '" + beanName
            + "' and '" + beanDefinition.getBeanClassName() + "' mapperInterface"
            + ". Bean already defined with the same name!");
        return false;
      }
    }
  }

}
