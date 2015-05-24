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
/**
 * MyBatis @Configuration style sample 
 * 
 * @version $Id$
 */
package org.mybatis.spring.sample;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.sample.dao.UserDao;
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
    public DataSource dataSource() {
      return new EmbeddedDatabaseBuilder()
          .setType(EmbeddedDatabaseType.HSQL)
          .addScript("org/mybatis/spring/sample/db/database-schema.sql")
          .addScript("org/mybatis/spring/sample/db/database-test-data.sql")
          .build();
    }

    @Bean
    public PlatformTransactionManager transactionalManagerOne() {
      return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
      SqlSessionFactoryBean ss = new SqlSessionFactoryBean();
      ss.setDataSource(dataSource());
      ss.setMapperLocations(new Resource[] { new ClassPathResource("org/mybatis/spring/sample/dao/UserDao.xml") });
      return (SqlSessionFactory) ss.getObject();
    }

    @Bean
    public UserDao userDao() throws Exception {
      // when using javaconfig a template requires less lines than a MapperFactoryBean
      SqlSessionTemplate sessionTemplate = new SqlSessionTemplate(sqlSessionFactory());
      return sessionTemplate.getMapper(UserDao.class);
    }

    @Bean
    public UserDao userDaoWithFactory() throws Exception {
      MapperFactoryBean<UserDao> mapperFactoryBean = new MapperFactoryBean<UserDao>();
      mapperFactoryBean.setMapperInterface(UserDao.class);
      mapperFactoryBean.setSqlSessionFactory(sqlSessionFactory());
      mapperFactoryBean.afterPropertiesSet();
      return mapperFactoryBean.getObject();
    }
    
    @Bean
    public FooService fooService() throws Exception {
      FooService fooService = new FooService();
      fooService.setUserDao(userDao());
      return fooService;
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
