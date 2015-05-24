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
package org.mybatis.spring.support;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mybatis.spring.AbstractMyBatisSpringTest;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @version $Id$
 */
public final class SqlSessionDaoSupportTest extends AbstractMyBatisSpringTest {
  private SqlSessionDaoSupport sqlSessionDaoSupport;

  private GenericApplicationContext applicationContext;

  @Before
  public void setup() {
    sqlSessionDaoSupport = new MockSqlSessionDao();
  }

  @After
  public void closeConnection() throws SQLException {
    connection.close();
  }

  @Test
  public void testWithSqlSessionTemplate() {
    SqlSessionTemplate sessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
    sqlSessionDaoSupport.setSqlSessionTemplate(sessionTemplate);
    sqlSessionDaoSupport.afterPropertiesSet();

    assertEquals("should store the Template", sessionTemplate, sqlSessionDaoSupport.getSqlSession());
  }

  @Test
  public void testWithSqlSessionFactory() {
    sqlSessionDaoSupport.setSqlSessionFactory(sqlSessionFactory);
    sqlSessionDaoSupport.afterPropertiesSet();

    assertEquals("should store the Factory", sqlSessionFactory, ((SqlSessionTemplate) sqlSessionDaoSupport
        .getSqlSession()).getSqlSessionFactory());
  }

  @Test
  public void testWithBothFactoryAndTemplate() {
    SqlSessionTemplate sessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
    sqlSessionDaoSupport.setSqlSessionTemplate(sessionTemplate);
    sqlSessionDaoSupport.setSqlSessionFactory(sqlSessionFactory);
    sqlSessionDaoSupport.afterPropertiesSet();

    assertEquals("should ignore the Factory", sessionTemplate, sqlSessionDaoSupport.getSqlSession());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWithNoFactoryOrSession() {
    sqlSessionDaoSupport.afterPropertiesSet();
  }

  @Test(expected = BeanCreationException.class)
  public void testAutowireWithNoFactoryOrSession() {
    setupContext();
    startContext();
  }

  @Test(expected = BeanCreationException.class)
  public void testAutowireWithTwoFactories() {
    setupContext();

    setupSqlSessionFactory("factory1");
    setupSqlSessionFactory("factory2");

    startContext();
  }

  private void setupContext() {
    applicationContext = new GenericApplicationContext();

    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(MockSqlSessionDao.class);
    applicationContext.registerBeanDefinition("dao", definition);

    // add support for autowiring fields
    AnnotationConfigUtils.registerAnnotationConfigProcessors(applicationContext);
  }

  private void startContext() {
    applicationContext.refresh();
    applicationContext.start();

    sqlSessionDaoSupport = applicationContext.getBean(MockSqlSessionDao.class);
  }

  private void setupSqlSessionFactory(String name) {
    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(SqlSessionFactoryBean.class);
    definition.getPropertyValues().add("dataSource", dataSource);

    applicationContext.registerBeanDefinition(name, definition);
  }

  private static final class MockSqlSessionDao extends SqlSessionDaoSupport {
  }
}
