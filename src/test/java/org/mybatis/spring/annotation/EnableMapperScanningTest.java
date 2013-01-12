/*
 *    Copyright 2010-2013 The MyBatis Team
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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.EnableMapperScanning;
import org.mybatis.spring.mapper.MapperInterface;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.mockrunner.mock.jdbc.MockDataSource;

/**
 * Test for the MapperScannerRegistrar.
 * <p>
 * This test works fine with Spring 3.1 and 3.2 but with 3.1 the registrar is called twice.
 * 
 * @version $Id$
 */
public final class EnableMapperScanningTest {
  private AnnotationConfigApplicationContext applicationContext;

  @Before
  public void setupContext() {
    applicationContext = new AnnotationConfigApplicationContext();

    setupSqlSessionFactory("sqlSessionFactory");

    // assume support for autowiring fields is added by MapperScannerConfigurer
    // via
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
    // assertBeanNotLoaded("annotatedMapperZeroMethods"); // as of 1.1.0 mappers
    // with no methods are loaded
  }

  @After
  public void destroyContext() {
    applicationContext.destroy();
  }

  @Test
  public void testInterfaceScan() {
    applicationContext.register(AppConfigWithPackageScan.class);

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
  }

  @Test
  public void testMarkerInterfaceScan() {
    applicationContext.register(AppConfigWithMarkerInterface.class);

    startContext();

    // only child inferfaces should be loaded
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
    assertBeanNotLoaded("annotatedMapper");
  }

  @Test
  public void testAnnotationScan() {
    applicationContext.register(AppConfigWithAnnotation.class);

    startContext();

    // only annotated mappers should be loaded
    applicationContext.getBean("annotatedMapper");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
    assertBeanNotLoaded("mapperSubinterface");
  }

  @Test
  public void testMarkerInterfaceAndAnnotationScan() {
    applicationContext.register(AppConfigWithMarkerInterfaceAndAnnotation.class);

    startContext();

    // everything should be loaded but the marker interface
    applicationContext.getBean("annotatedMapper");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
  }

  @Test
  public void testScanWithNameConflict() {
    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(Object.class);
    applicationContext.registerBeanDefinition("mapperInterface", definition);

    applicationContext.register(AppConfigWithPackageScan.class);

    startContext();

    assertSame("scanner should not overwite existing bean definition", applicationContext.getBean("mapperInterface").getClass(), Object.class);
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

  @Test
  public void testScanWithExplicitSqlSessionTemplate() throws Exception {
    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(SqlSessionTemplate.class);
    ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
    constructorArgs.addGenericArgumentValue(new RuntimeBeanReference("sqlSessionFactory"));
    definition.setConstructorArgumentValues(constructorArgs);
    applicationContext.registerBeanDefinition("sqlSessionTemplate", definition);

    applicationContext.register(AppConfigWithSqlSessionTemplate.class);
    
    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
    
  }

  @Configuration
  @EnableMapperScanning("org.mybatis.spring.mapper")
  public static class AppConfigWithPackageScan {
  }

  @Configuration
  @EnableMapperScanning(basePackages = "org.mybatis.spring.mapper", markerInterface = MapperInterface.class)
  public static class AppConfigWithMarkerInterface {
  }

  @Configuration
  @EnableMapperScanning(basePackages = "org.mybatis.spring.mapper", annotationClass = Component.class)
  public static class AppConfigWithAnnotation {
  }

  @Configuration
  @EnableMapperScanning(basePackages = "org.mybatis.spring.mapper", annotationClass = Component.class, markerInterface = MapperInterface.class)
  public static class AppConfigWithMarkerInterfaceAndAnnotation {
  }

  @Configuration
  @EnableMapperScanning(basePackages = "org.mybatis.spring.mapper", sqlSessionTemplateBeanName = "sqlSessionTemplate")
  public static class AppConfigWithSqlSessionTemplate {
  }

}
