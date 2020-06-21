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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.mybatis.spring.batch.builder.MyBatisCursorItemReaderBuilder;
import org.mybatis.spring.sample.batch.UserToPersonItemProcessor;
import org.mybatis.spring.sample.domain.Person;
import org.mybatis.spring.sample.domain.User;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
public class SampleJobConfig {

  @Autowired
  private JobBuilderFactory jobBuilderFactory;

  @Autowired
  private StepBuilderFactory stepBuilderFactory;

  @Bean
  public DataSource dataSource() {
    return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
        .addScript("org/mybatis/spring/sample/db/database-schema.sql")
        .addScript("org/springframework/batch/core/schema-drop-hsqldb.sql")
        .addScript("org/springframework/batch/core/schema-hsqldb.sql")
        .addScript("org/mybatis/spring/sample/db/database-test-data.sql").build();
  }

  @Bean
  public PlatformTransactionManager transactionalManager() {
    return new DataSourceTransactionManager(dataSource());
  }

  @Bean
  public SqlSessionFactory sqlSessionFactory() throws Exception {
    PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    SqlSessionFactoryBean ss = new SqlSessionFactoryBean();
    ss.setDataSource(dataSource());
    ss.setMapperLocations(resourcePatternResolver.getResources("org/mybatis/spring/sample/mapper/*.xml"));
    org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
    configuration.setDefaultExecutorType(ExecutorType.BATCH);
    ss.setConfiguration(configuration);
    return ss.getObject();
  }

  @Bean
  public MyBatisCursorItemReader<User> reader() throws Exception {
    // @formatter:off
    return new MyBatisCursorItemReaderBuilder<User>()
        .sqlSessionFactory(sqlSessionFactory())
        .queryId("org.mybatis.spring.sample.mapper.UserMapper.getUsers")
        .build();
    // @formatter:on
  }

  @Bean
  public UserToPersonItemProcessor processor() {
    return new UserToPersonItemProcessor();
  }

  @Bean
  public MyBatisBatchItemWriter<Person> writer() throws Exception {
    // @formatter:off
    return new MyBatisBatchItemWriterBuilder<Person>()
        .sqlSessionFactory(sqlSessionFactory())
        .statementId("org.mybatis.spring.sample.mapper.PersonMapper.createPerson")
        .itemToParameterConverter(createItemToParameterMapConverter("batch_java_config_user", LocalDateTime.now()))
        .build();
    // @formatter:on
  }

  public static <T> Converter<T, Map<String, Object>> createItemToParameterMapConverter(String operationBy,
      LocalDateTime operationAt) {
    return item -> {
      Map<String, Object> parameter = new HashMap<>();
      parameter.put("item", item);
      parameter.put("operationBy", operationBy);
      parameter.put("operationAt", operationAt);
      return parameter;
    };
  }

  @Bean
  public Job importUserJob() throws Exception {
    // @formatter:off
    return jobBuilderFactory.get("importUserJob")
        .flow(step1())
        .end()
        .build();
    // @formatter:on
  }

  @Bean
  public Step step1() throws Exception {
    // @formatter:off
    return stepBuilderFactory.get("step1")
        .transactionManager(transactionalManager())
        .<User, Person>chunk(10)
        .reader(reader())
        .processor(processor())
        .writer(writer())
        .build();
    // @formatter:on
  }

}
