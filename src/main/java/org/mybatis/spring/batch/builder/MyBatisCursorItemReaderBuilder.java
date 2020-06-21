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

import java.util.Map;
import java.util.Optional;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisCursorItemReader;

/**
 * A builder for the {@link MyBatisCursorItemReader}.
 *
 * @author Kazuki Shimizu
 * @since 2.0.0
 * @see MyBatisCursorItemReader
 */
public class MyBatisCursorItemReaderBuilder<T> {

  private SqlSessionFactory sqlSessionFactory;
  private String queryId;
  private Map<String, Object> parameterValues;
  private Boolean saveState;
  private Integer maxItemCount;

  /**
   * Set the {@link SqlSessionFactory} to be used by reader for database access.
   *
   * @param sqlSessionFactory
   *          the {@link SqlSessionFactory} to be used by writer for database access
   * @return this instance for method chaining
   * @see MyBatisCursorItemReader#setSqlSessionFactory(SqlSessionFactory)
   */
  public MyBatisCursorItemReaderBuilder<T> sqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    return this;
  }

  /**
   * Set the query id identifying the statement in the SqlMap configuration file.
   *
   * @param queryId
   *          the id for the query
   * @return this instance for method chaining
   * @see MyBatisCursorItemReader#setQueryId(String)
   */
  public MyBatisCursorItemReaderBuilder<T> queryId(String queryId) {
    this.queryId = queryId;
    return this;
  }

  /**
   * Set the parameter values to be used for the query execution.
   *
   * @param parameterValues
   *          the parameter values to be used for the query execution
   * @return this instance for method chaining
   * @see MyBatisCursorItemReader#setParameterValues(Map)
   */
  public MyBatisCursorItemReaderBuilder<T> parameterValues(Map<String, Object> parameterValues) {
    this.parameterValues = parameterValues;
    return this;
  }

  /**
   * Configure if the state of the {@link org.springframework.batch.item.ItemStreamSupport} should be persisted within
   * the {@link org.springframework.batch.item.ExecutionContext} for restart purposes.
   *
   * @param saveState
   *          defaults to true
   * @return The current instance of the builder.
   * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setSaveState(boolean)
   */
  public MyBatisCursorItemReaderBuilder<T> saveState(boolean saveState) {
    this.saveState = saveState;
    return this;
  }

  /**
   * Configure the max number of items to be read.
   *
   * @param maxItemCount
   *          the max items to be read
   * @return The current instance of the builder.
   * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
   */
  public MyBatisCursorItemReaderBuilder<T> maxItemCount(int maxItemCount) {
    this.maxItemCount = maxItemCount;
    return this;
  }

  /**
   * Returns a fully built {@link MyBatisCursorItemReader}.
   *
   * @return the reader
   */
  public MyBatisCursorItemReader<T> build() {
    MyBatisCursorItemReader<T> reader = new MyBatisCursorItemReader<>();
    reader.setSqlSessionFactory(this.sqlSessionFactory);
    reader.setQueryId(this.queryId);
    reader.setParameterValues(this.parameterValues);
    Optional.ofNullable(this.saveState).ifPresent(reader::setSaveState);
    Optional.ofNullable(this.maxItemCount).ifPresent(reader::setMaxItemCount);
    return reader;
  }

}
