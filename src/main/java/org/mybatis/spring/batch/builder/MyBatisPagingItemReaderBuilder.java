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
import org.mybatis.spring.batch.MyBatisPagingItemReader;

/**
 * A builder for the {@link MyBatisPagingItemReader}.
 *
 * @author Kazuki Shimizu
 * @since 2.0.0
 * @see MyBatisPagingItemReader
 */
public class MyBatisPagingItemReaderBuilder<T> {

  private SqlSessionFactory sqlSessionFactory;
  private String queryId;
  private Map<String, Object> parameterValues;
  private Integer pageSize;
  private Boolean saveState;
  private Integer maxItemCount;

  /**
   * Set the {@link SqlSessionFactory} to be used by writer for database access.
   *
   * @param sqlSessionFactory
   *          the {@link SqlSessionFactory} to be used by writer for database access
   * @return this instance for method chaining
   * @see MyBatisPagingItemReader#setSqlSessionFactory(SqlSessionFactory)
   */
  public MyBatisPagingItemReaderBuilder<T> sqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    return this;
  }

  /**
   * Set the query id identifying the statement in the SqlMap configuration file.
   *
   * @param queryId
   *          the id for the query
   * @return this instance for method chaining
   * @see MyBatisPagingItemReader#setQueryId(String)
   */
  public MyBatisPagingItemReaderBuilder<T> queryId(String queryId) {
    this.queryId = queryId;
    return this;
  }

  /**
   * Set the parameter values to be used for the query execution.
   *
   * @param parameterValues
   *          the parameter values to be used for the query execution
   * @return this instance for method chaining
   * @see MyBatisPagingItemReader#setParameterValues(Map)
   */
  public MyBatisPagingItemReaderBuilder<T> parameterValues(Map<String, Object> parameterValues) {
    this.parameterValues = parameterValues;
    return this;
  }

  /**
   * The number of records to request per page/query. Defaults to 10. Must be greater than zero.
   *
   * @param pageSize
   *          number of items
   * @return this instance for method chaining
   * @see org.springframework.batch.item.database.AbstractPagingItemReader#setPageSize(int)
   */
  public MyBatisPagingItemReaderBuilder<T> pageSize(int pageSize) {
    this.pageSize = pageSize;
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
  public MyBatisPagingItemReaderBuilder<T> saveState(boolean saveState) {
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
  public MyBatisPagingItemReaderBuilder<T> maxItemCount(int maxItemCount) {
    this.maxItemCount = maxItemCount;
    return this;
  }

  /**
   * Returns a fully built {@link MyBatisPagingItemReader}.
   *
   * @return the reader
   */
  public MyBatisPagingItemReader<T> build() {
    MyBatisPagingItemReader<T> reader = new MyBatisPagingItemReader<>();
    reader.setSqlSessionFactory(this.sqlSessionFactory);
    reader.setQueryId(this.queryId);
    reader.setParameterValues(this.parameterValues);
    Optional.ofNullable(this.pageSize).ifPresent(reader::setPageSize);
    Optional.ofNullable(this.saveState).ifPresent(reader::setSaveState);
    Optional.ofNullable(this.maxItemCount).ifPresent(reader::setMaxItemCount);
    return reader;
  }

}
