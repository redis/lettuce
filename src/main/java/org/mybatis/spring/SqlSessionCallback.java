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
package org.mybatis.spring;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.session.SqlSession;

/**
 * Callback interface for data access code that works with the MyBatis
 * {@link Executor} interface. To be used
 * with {@link SqlSessionTemplate}'s <code>execute</code> method,
 * assumably often as anonymous classes within a method implementation.
 *
 * @see SqlSessionTemplate
 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
 * @version $Id$
 */
public interface SqlSessionCallback<T> {

    /**
     * Gets called by <code>SqlSessionTemplate.execute</code> with an active
     * <code>SqlSession</code>. Does not need to care about activating
     * or closing the <code>SqlSession </code>, or handling transactions.
     *
     * <p>If called without a thread-bound JDBC transaction (initiated by
     * DataSourceTransactionManager), the code will simply get executed on the
     * underlying JDBC connection with its transactional semantics. If using
     * a JTA-aware DataSource, the JDBC connection and thus the callback code
     * will be transactional if a JTA transaction is active.
     *
     * <p>Allows for returning a result object created within the callback,
     * i.e. a domain object or a collection of domain objects.
     * A thrown custom RuntimeException is treated as an application exception:
     * It gets propagated to the caller of the template.
     *
     * @param sqlSession an active MyBatis SqlSession, passed-in as
     * Executor interface here to avoid manual life-cycle handling
     * @return a result object, or <code>null</code> if none
     * @see SqlSessionTemplate#execute
     */
    T doInSqlSession(SqlSession sqlSession);

}
