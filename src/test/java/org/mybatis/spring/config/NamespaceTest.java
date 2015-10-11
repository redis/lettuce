/**
 *    Copyright 2010-2015 the original author or authors.
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
package org.mybatis.spring.config;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.AnnotatedMapper;
import org.mybatis.spring.mapper.MapperInterface;
import org.mybatis.spring.mapper.MapperSubinterface;
import org.mybatis.spring.mapper.child.MapperChildInterface;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import com.mockrunner.mock.jdbc.MockDataSource;

/**
 * Test for the MapperScannerRegistrar.
 * <p>
 * This test works fine with Spring 3.1 and 3.2 but with 3.1 the registrar is
 * called twice.
 * 
 * @version $Id$
 */
public final class NamespaceTest {
  private ClassPathXmlApplicationContext applicationContext;

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

    applicationContext.destroy();
  }

  @Test
  public void testInterfaceScan() {

    applicationContext = new ClassPathXmlApplicationContext(new String[] { "org/mybatis/spring/config/base-package.xml" }, setupSqlSessionFactory());

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
  }

  @Test
  public void testNameGenerator() {

    applicationContext = new ClassPathXmlApplicationContext(new String[] { "org/mybatis/spring/config/name-generator.xml" }, setupSqlSessionFactory());

    startContext();

    // only child inferfaces should be loaded and named with its class name
    applicationContext.getBean(MapperInterface.class.getName());
    applicationContext.getBean(MapperSubinterface.class.getName());
    applicationContext.getBean(MapperChildInterface.class.getName());
    applicationContext.getBean(AnnotatedMapper.class.getName());
  }

  @Test
  public void testMarkerInterfaceScan() {

    applicationContext = new ClassPathXmlApplicationContext(new String[] { "org/mybatis/spring/config/marker-interface.xml" }, setupSqlSessionFactory());

    startContext();

    // only child inferfaces should be loaded
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
    assertBeanNotLoaded("annotatedMapper");
  }

  @Test
  public void testAnnotationScan() {

    applicationContext = new ClassPathXmlApplicationContext(new String[] { "org/mybatis/spring/config/annotation.xml" }, setupSqlSessionFactory());

    startContext();

    // only annotated mappers should be loaded
    applicationContext.getBean("annotatedMapper");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
    assertBeanNotLoaded("mapperSubinterface");
  }

  @Test
  public void testMarkerInterfaceAndAnnotationScan() {

    applicationContext = new ClassPathXmlApplicationContext(new String[] { "org/mybatis/spring/config/marker-and-annotation.xml" }, setupSqlSessionFactory());

    startContext();

    // everything should be loaded but the marker interface
    applicationContext.getBean("annotatedMapper");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
  }

  @Test
  public void testScanWithExplicitSqlSessionFactory() throws Exception {

    applicationContext = new ClassPathXmlApplicationContext(new String[] { "org/mybatis/spring/config/factory-ref.xml" }, setupSqlSessionFactory());

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
  }

  @Test
  public void testScanWithExplicitSqlSessionTemplate() throws Exception {

    applicationContext = new ClassPathXmlApplicationContext(new String[] { "org/mybatis/spring/config/factory-ref.xml" }, setupSqlSessionTemplate());

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
  }

  private GenericApplicationContext setupSqlSessionTemplate() {

    GenericApplicationContext genericApplicationContext = setupSqlSessionFactory();
    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(SqlSessionTemplate.class);
    ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
    constructorArgs.addGenericArgumentValue(new RuntimeBeanReference("sqlSessionFactory"));
    definition.setConstructorArgumentValues(constructorArgs);
    genericApplicationContext.registerBeanDefinition("sqlSessionTemplate", definition);
    return genericApplicationContext;
  }

  private GenericApplicationContext setupSqlSessionFactory() {

    GenericApplicationContext genericApplicationContext = new GenericApplicationContext();

    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(SqlSessionFactoryBean.class);
    definition.getPropertyValues().add("dataSource", new MockDataSource());

    DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
    factory.registerBeanDefinition("sqlSessionFactory", definition);

    genericApplicationContext.registerBeanDefinition("sqlSessionFactory", definition);

    genericApplicationContext.refresh();

    return genericApplicationContext;
  }

  private void assertBeanNotLoaded(String name) {
    try {
      applicationContext.getBean(name);
      fail("Spring bean should not be defined for class " + name);
    } catch (NoSuchBeanDefinitionException nsbde) {
      // success
    }
  }

  public static class BeanNameGenerator implements org.springframework.beans.factory.support.BeanNameGenerator {

    @Override
    public String generateBeanName(BeanDefinition beanDefinition, BeanDefinitionRegistry definitionRegistry) {
      return beanDefinition.getBeanClassName();
    }

  }

}
