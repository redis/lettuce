/**
 *    Copyright 2010-2017 the original author or authors.
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
 */
package org.mybatis.spring.sample;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.sample.domain.User;
import org.mybatis.spring.sample.mapper.UserMapper;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@SpringJUnitConfig()
public class SampleConfigurationTest {

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
      ss.setMapperLocations(new Resource[] { new ClassPathResource("org/mybatis/spring/sample/mapper/UserMapper.xml") });
      return ss.getObject();
    }

    @Bean
    public UserMapper userMapper() throws Exception {
      // when using javaconfig a template requires less lines than a MapperFactoryBean
      SqlSessionTemplate sessionTemplate = new SqlSessionTemplate(sqlSessionFactory());
      return sessionTemplate.getMapper(UserMapper.class);
    }

    @Bean
    public UserMapper userMapperWithFactory() throws Exception {
      MapperFactoryBean<UserMapper> mapperFactoryBean = new MapperFactoryBean<>();
      mapperFactoryBean.setMapperInterface(UserMapper.class);
      mapperFactoryBean.setSqlSessionFactory(sqlSessionFactory());
      mapperFactoryBean.afterPropertiesSet();
      return mapperFactoryBean.getObject();
    }
    
    @Bean
    public FooService fooService() throws Exception {
      FooService fooService = new FooService();
      fooService.setUserMapper(userMapper());
      return fooService;
    }
  }

  @Autowired
  private FooService fooService;

  @Test
  void test() {
    User user = fooService.doSomeBusinessStuff("u1");
    assertThat(user.getName()).isEqualTo("Pocoyo");
  }

}
