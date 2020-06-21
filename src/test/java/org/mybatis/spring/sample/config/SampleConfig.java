/**
 * Copyright 2010-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mybatis.spring.sample.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.sample.mapper.UserMapper;
import org.mybatis.spring.sample.service.FooService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class SampleConfig {
  @Bean
  public DataSource dataSource() {
    return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
        .addScript("org/mybatis/spring/sample/db/database-schema.sql")
        .addScript("org/mybatis/spring/sample/db/database-test-data.sql").build();
  }

  @Bean
  public PlatformTransactionManager transactionalManager() {
    return new DataSourceTransactionManager(dataSource());
  }

  @Bean
  public SqlSessionFactory sqlSessionFactory() throws Exception {
    SqlSessionFactoryBean ss = new SqlSessionFactoryBean();
    ss.setDataSource(dataSource());
    ss.setMapperLocations(new ClassPathResource("org/mybatis/spring/sample/mapper/UserMapper.xml"));
    return ss.getObject();
  }

  @Bean
  public UserMapper userMapper() throws Exception {
    // when using javaconfig a template requires less lines than a MapperFactoryBean
    SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory());
    return sqlSessionTemplate.getMapper(UserMapper.class);
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
    return new FooService(userMapper());
  }

  @Bean
  public FooService fooServiceWithMapperFactoryBean() throws Exception {
    return new FooService(userMapperWithFactory());
  }

}
