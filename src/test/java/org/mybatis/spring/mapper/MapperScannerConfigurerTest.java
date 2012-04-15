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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;

import com.mockrunner.mock.jdbc.MockDataSource;

/**
 * @version $Id$
 */
public final class MapperScannerConfigurerTest {
  private GenericApplicationContext applicationContext;

  @Before
  public void setupContext() {
    applicationContext = new GenericApplicationContext();

    // add the mapper scanner as a bean definition rather than explicitly setting a
    // postProcessor on the context so initialization follows the same code path as reading from
    // an XML config file
    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(MapperScannerConfigurer.class);
    definition.getPropertyValues().add("basePackage", "org.mybatis.spring.mapper");
    applicationContext.registerBeanDefinition("mapperScanner", definition);

    setupSqlSessionFactory("sqlSessionFactory");

    // assume support for autowiring fields is added by MapperScannerConfigurer via
    // org.springframework.context.annotation.ClassPathBeanDefinitionScanner.includeAnnotationConfig
  }

  private void startContext() {
    applicationContext.refresh();
    applicationContext.start();

    // this will throw an exception if the beans cannot be found
    applicationContext.getBean("sqlSessionFactory");
  }

  @After
  public void assertNoMapperClass() {
    // concrete classes should always be ignored by MapperScannerPostProcessor
    assertBeanNotLoaded("mapperClass");

    // no method interfaces should be ignored too
    assertBeanNotLoaded("package-info");
    //        assertBeanNotLoaded("annotatedMapperZeroMethods"); // as of 1.1.0 mappers with no methods are loaded
  }

  @After
  public void destroyContext() {
    applicationContext.destroy();
  }

  @Test
  public void testInterfaceScan() {
    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
  }

  @Test
  public void testMarkerInterfaceScan() {
    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add(
        "markerInterface", MapperInterface.class);

    startContext();

    // only child inferfaces should be loaded
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
    assertBeanNotLoaded("annotatedMapper");
  }

  @Test
  public void testAnnotationScan() {
    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add(
        "annotationClass", Component.class);

    startContext();

    // only annotated mappers should be loaded
    applicationContext.getBean("annotatedMapper");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
    assertBeanNotLoaded("mapperSubinterface");
  }

  @Test
  public void testMarkerInterfaceAndAnnotationScan() {
    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add(
        "markerInterface", MapperInterface.class);
    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add(
        "annotationClass", Component.class);

    startContext();

    // everything should be loaded but the marker interface
    applicationContext.getBean("annotatedMapper");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
  }

  @Test
  public void testScanWithExplicitSqlSessionFactory() throws Exception {
    setupSqlSessionFactory("sqlSessionFactory2");

    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add(
        "sqlSessionFactoryBeanName", "sqlSessionFactory2");

    testInterfaceScan();
  }

  @Test
  public void testScanWithExplicitSqlSessionTemplate() throws Exception {
    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(SqlSessionTemplate.class);
    ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
    constructorArgs.addGenericArgumentValue(new RuntimeBeanReference("sqlSessionFactory"));
    definition.setConstructorArgumentValues(constructorArgs);
    applicationContext.registerBeanDefinition("sqlSessionTemplate", definition);

    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add(
        "sqlSessionTemplateBeanName", "sqlSessionTemplate");

    testInterfaceScan();
  }

  @Test
  public void testScanWithExplicitSqlSessionFactoryViaPlaceholder() throws Exception {
    setupSqlSessionFactory("sqlSessionFactory2");

    // use a property placeholder for the session factory name
    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add(
        "sqlSessionFactoryBeanName", "${sqlSessionFactoryBeanNameProperty}");

    Properties props = new java.util.Properties();
    props.put("sqlSessionFactoryBeanNameProperty", "sqlSessionFactory2");

    GenericBeanDefinition propertyDefinition = new GenericBeanDefinition();
    propertyDefinition.setBeanClass(PropertyPlaceholderConfigurer.class);
    propertyDefinition.getPropertyValues().add("properties", props);

    applicationContext.registerBeanDefinition("propertiesPlaceholder", propertyDefinition);

    testInterfaceScan();
  }

  @Test
  public void testScanWithNameConflict() {
    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(Object.class);
    applicationContext.registerBeanDefinition("mapperInterface", definition);

    startContext();

    assertSame("scanner should not overwite existing bean definition", applicationContext
        .getBean("mapperInterface").getClass(), Object.class);
  }

  @Test
  public void testScanWithPropertyPlaceholders() {
    GenericBeanDefinition definition = (GenericBeanDefinition) applicationContext
        .getBeanDefinition("mapperScanner");

    // use a property placeholder for basePackage
    definition.getPropertyValues().removePropertyValue("basePackage");
    definition.getPropertyValues().add("basePackage", "${basePackageProperty}");
    definition.getPropertyValues().add("processPropertyPlaceHolders", true);

    // also use a property placeholder for an SqlSessionFactory property
    // to make sure the configLocation was setup correctly and MapperScanner did not change
    // regular property placeholder substitution
    definition = (GenericBeanDefinition) applicationContext
        .getBeanDefinition("sqlSessionFactory");
    definition.getPropertyValues().removePropertyValue("configLocation");
    definition.getPropertyValues().add("configLocation", "${configLocationProperty}");

    Properties props = new java.util.Properties();
    props.put("basePackageProperty", "org.mybatis.spring.mapper");
    props.put("configLocationProperty", "classpath:org/mybatis/spring/mybatis-config.xml");

    GenericBeanDefinition propertyDefinition = new GenericBeanDefinition();
    propertyDefinition.setBeanClass(PropertyPlaceholderConfigurer.class);
    propertyDefinition.getPropertyValues().add("properties", props);

    applicationContext.registerBeanDefinition("propertiesPlaceholder", propertyDefinition);

    testInterfaceScan();

    // make sure the configLocation was setup correctly
    // mybatis-config.xml changes the executor from the default SIMPLE type
    SqlSessionFactory sessionFactory = (SqlSessionFactory) applicationContext
        .getBean("sqlSessionFactory");
    assertSame(ExecutorType.REUSE, sessionFactory.getConfiguration().getDefaultExecutorType());
  }

  private void setupSqlSessionFactory(String name) {
    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(SqlSessionFactoryBean.class);
    definition.getPropertyValues().add("dataSource", new MockDataSource());
    applicationContext.registerBeanDefinition(name, definition);
  }

  private void assertBeanNotLoaded(String name) {
    try {
      applicationContext.getBean(name);
      fail("Spring bean should not be defined for class " + name);
    } catch (NoSuchBeanDefinitionException nsbde) {
      // success
    }
  }
}
