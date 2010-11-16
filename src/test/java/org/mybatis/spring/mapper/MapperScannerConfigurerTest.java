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

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;

import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;

import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.mybatis.spring.mapper.child.MapperChildInterface;

import com.mockrunner.mock.jdbc.MockDataSource;

public class MapperScannerConfigurerTest {
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

    public void startContext() {
        applicationContext.refresh();
        applicationContext.start();

        // this will throw an exception if the beans cannot be found
        applicationContext.getBean("sqlSessionFactory");
    }

    @After
    public void assertNoMapperClass() {
        // MapperClass should always be ignored by MapperScannerPostProcessor
        try {
            applicationContext.getBean(MapperClass.class);
            fail("Spring bean should not be defined for class " + MapperClass.class);
        } catch (NoSuchBeanDefinitionException nsbde) {
            // success
        }

    }

    @After
    public void destroyContext() {
        applicationContext.destroy();
    }

    @Test
    public void testInterfaceScan() {
        startContext();

        // in most use cases, you would not want the marker interface to be a bean too
        // but, for testing, it is easier to do this rather than create a different package to
        // isolate the sub-interface
        // regardless, the MapperInterface should exist, which is what we want
        assertEquals("MapperInterface, MapperSubinterface & MapperChildInterface should all be loaded as beans", 3,
                applicationContext.getBeanNamesForType(MapperInterface.class).length);
        applicationContext.getBean("mapperInterface");
    }

    @Test
    public void testMarkerInterfaceScan() {
        applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("markerInterface",
                MapperInterface.class);

        startContext();

        assertEquals("MapperSubinterface should be loaded as a bean", 1, applicationContext
                .getBeanNamesForType(MapperSubinterface.class).length);
        // 2 => MapperSubinterface & MapperChildInterface, since they are of MapperInterface type
        assertEquals("MapperInterface should not be loaded as a bean", 2, applicationContext
                .getBeanNamesForType(MapperInterface.class).length);
    }

    @Test
    public void testAnnotationScan() {
        applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("annotationClass",
                Component.class);

        startContext();

        assertEquals("AnnotatedMapper should be loaded as a bean", 1, applicationContext
                .getBeanNamesForType(AnnotatedMapper.class).length);
        assertEquals("MapperChildInterface should be loaded as a bean", 1, applicationContext
                .getBeanNamesForType(MapperChildInterface.class).length);

        try {
            // test by name here sicne MapperChildInterface subclasses MapperInterface in addition
            // to the annotation
            applicationContext.getBean("mapperInterface");
            fail("MapperInterface should not be loaded as a bean");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testMarkerInterfaceAndAnnotationScan() {
        applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("markerInterface",
                MapperInterface.class);
        applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("annotationClass",
                Component.class);

        startContext();

        assertEquals("AnnotatedMapper should be loaded as a bean", 1, applicationContext
                .getBeanNamesForType(AnnotatedMapper.class).length);
        assertEquals("MapperSubinterface should be loaded as a bean", 1, applicationContext
                .getBeanNamesForType(MapperSubinterface.class).length);
        // 2 => MapperSubinterface & MapperChildInterface, since they are of MapperInterface type
        assertEquals("MapperInterface should not be loaded as a bean", 2, applicationContext
                .getBeanNamesForType(MapperInterface.class).length);
    }

    @Test
    public void testScanWithExplicitSqlSessionFactory() throws Exception {
        setupSqlSessionFactory("sqlSessionFactory2");

        applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("sqlSessionFactory",
                new RuntimeBeanReference("sqlSessionFactory2"));

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

        applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("sqlSessionTemplate",
                new RuntimeBeanReference("sqlSessionTemplate"));

        testInterfaceScan();
    }

    private void setupSqlSessionFactory(String name) {
        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setBeanClass(SqlSessionFactoryBean.class);
        definition.getPropertyValues().add("dataSource", new MockDataSource());
        applicationContext.registerBeanDefinition(name, definition);
    }

}
