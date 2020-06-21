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
package org.mybatis.spring.batch.builder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mybatis.spring.batch.MyBatisPagingItemReader;
import org.springframework.batch.item.ExecutionContext;

/**
 * Tests for {@link MyBatisPagingItemReaderBuilder}.
 *
 * @since 2.0.0
 * @author Kazuki Shimizu
 */
class MyBatisPagingItemReaderBuilderTest {

  @Mock
  private DataSource dataSource;

  @Mock
  private SqlSessionFactory sqlSessionFactory;

  @Mock
  private SqlSession sqlSession;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.initMocks(this);

    Configuration configuration = new Configuration();
    Environment environment = new Environment("unittest", new JdbcTransactionFactory(), dataSource);
    configuration.setEnvironment(environment);
    Mockito.when(this.sqlSessionFactory.getConfiguration()).thenReturn(configuration);
    Mockito.when(this.sqlSessionFactory.openSession(ExecutorType.BATCH)).thenReturn(this.sqlSession);
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("id", 1);
    parameters.put("_page", 0);
    parameters.put("_pagesize", 10);
    parameters.put("_skiprows", 0);
    Mockito.when(this.sqlSession.selectList("selectFoo", parameters)).thenReturn(getFoos());
  }

  @Test
  void testConfiguration() throws Exception {
    // @formatter:off
    MyBatisPagingItemReader<Foo> itemReader = new MyBatisPagingItemReaderBuilder<Foo>()
            .sqlSessionFactory(this.sqlSessionFactory)
            .queryId("selectFoo")
            .parameterValues(Collections.singletonMap("id", 1))
            .build();
    // @formatter:on
    itemReader.afterPropertiesSet();

    ExecutionContext executionContext = new ExecutionContext();
    itemReader.open(executionContext);

    Assertions.assertThat(itemReader.read()).extracting(Foo::getName).isEqualTo("foo1");
    Assertions.assertThat(itemReader.read()).extracting(Foo::getName).isEqualTo("foo2");
    Assertions.assertThat(itemReader.read()).extracting(Foo::getName).isEqualTo("foo3");

    itemReader.update(executionContext);
    Assertions.assertThat(executionContext.getInt("MyBatisPagingItemReader.read.count")).isEqualTo(3);
    Assertions.assertThat(executionContext.containsKey("MyBatisPagingItemReader.read.count.max")).isFalse();

    Assertions.assertThat(itemReader.read()).isNull();
  }

  @Test
  void testConfigurationSaveStateIsFalse() throws Exception {
    // @formatter:off
    MyBatisPagingItemReader<Foo> itemReader = new MyBatisPagingItemReaderBuilder<Foo>()
            .sqlSessionFactory(this.sqlSessionFactory)
            .queryId("selectFoo")
            .parameterValues(Collections.singletonMap("id", 1))
            .saveState(false)
            .build();
    // @formatter:on
    itemReader.afterPropertiesSet();

    ExecutionContext executionContext = new ExecutionContext();
    itemReader.open(executionContext);

    Assertions.assertThat(itemReader.read()).extracting(Foo::getName).isEqualTo("foo1");
    Assertions.assertThat(itemReader.read()).extracting(Foo::getName).isEqualTo("foo2");
    Assertions.assertThat(itemReader.read()).extracting(Foo::getName).isEqualTo("foo3");

    itemReader.update(executionContext);
    Assertions.assertThat(executionContext.isEmpty()).isTrue();
  }

  @Test
  void testConfigurationMaxItemCount() throws Exception {
    // @formatter:off
    MyBatisPagingItemReader<Foo> itemReader = new MyBatisPagingItemReaderBuilder<Foo>()
            .sqlSessionFactory(this.sqlSessionFactory)
            .queryId("selectFoo")
            .parameterValues(Collections.singletonMap("id", 1))
            .maxItemCount(2)
            .build();
    // @formatter:on
    itemReader.afterPropertiesSet();

    ExecutionContext executionContext = new ExecutionContext();
    itemReader.open(executionContext);

    Assertions.assertThat(itemReader.read()).extracting(Foo::getName).isEqualTo("foo1");
    Assertions.assertThat(itemReader.read()).extracting(Foo::getName).isEqualTo("foo2");

    itemReader.update(executionContext);
    Assertions.assertThat(executionContext.getInt("MyBatisPagingItemReader.read.count.max")).isEqualTo(2);

    Assertions.assertThat(itemReader.read()).isNull();
  }

  @Test
  void testConfigurationPageSize() throws Exception {
    // @formatter:off
    MyBatisPagingItemReader<Foo> itemReader = new MyBatisPagingItemReaderBuilder<Foo>()
            .sqlSessionFactory(this.sqlSessionFactory)
            .queryId("selectFoo")
            .parameterValues(Collections.singletonMap("id", 1))
            .pageSize(2)
            .build();
    // @formatter:on
    itemReader.afterPropertiesSet();

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("id", 1);
    parameters.put("_page", 0);
    parameters.put("_pagesize", 2);
    parameters.put("_skiprows", 0);
    Mockito.when(this.sqlSession.selectList("selectFoo", parameters)).thenReturn(getFoos());

    ExecutionContext executionContext = new ExecutionContext();
    itemReader.open(executionContext);

    Assertions.assertThat(itemReader.read()).extracting(Foo::getName).isEqualTo("foo1");
    Assertions.assertThat(itemReader.read()).extracting(Foo::getName).isEqualTo("foo2");
    Assertions.assertThat(itemReader.read()).isNull();
  }

  private List<Object> getFoos() {
    return Arrays.asList(new Foo("foo1"), new Foo("foo2"), new Foo("foo3"));
  }

  private static class Foo {
    private final String name;

    Foo(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  }

}
