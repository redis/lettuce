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

/**
 * @version $Id: AbstractSampleTest.java 4871 2012-03-12 07:50:06Z eduardo.macarron@gmail.com $
 */
package org.mybatis.spring.sample;

import javax.sql.DataSource;

import junit.framework.Assert;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.sample.dao.UserDao;
import org.mybatis.spring.sample.dao.UserDaoImpl;
import org.mybatis.spring.sample.domain.User;
import org.mybatis.spring.sample.service.FooService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.PlatformTransactionManager;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class ConfigurationSampleTest {

  @Configuration
  static class ContextConfiguration {

    @Bean
    public FooService fooService() {
      FooService fooService = new FooService();
      fooService.setUserDao(userDao());
      return fooService;
    }

    @Bean
    public UserDao userDao() {
      UserDaoImpl userDao = new UserDaoImpl();
      userDao.setSqlSessionFactory(sqlSessionFactory());
      return userDao;
    }

    @Bean
    public PlatformTransactionManager transactionalManagerOne() {
      return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    public DataSource dataSource() {
      return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
          .addScript("org/mybatis/spring/sample/db/database-schema.sql")
          .addScript("org/mybatis/spring/sample/db/database-test-data.sql")
          .build();
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() {
      SqlSessionFactoryBean ss = new SqlSessionFactoryBean();
      ss.setDataSource(dataSource());
      ss.setMapperLocations(new Resource[] { new ClassPathResource(
          "org/mybatis/spring/sample/dao/UserDao.xml") });
      try {
        return (SqlSessionFactory) ss.getObject();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Autowired
  private FooService fooService;

  @Test
  public void test() {
    User user = fooService.doSomeBusinessStuff("u1");
    Assert.assertEquals("Pocoyo", user.getName());
  }

}
