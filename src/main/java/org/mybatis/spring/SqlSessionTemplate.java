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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.util.Assert;

/**
 * Thread safe Spring managed {@link SqlSession}
 * <p>
 * It converts SQLExceptions into unchecked DataAccessExceptions, following the
 * <code>org.springframework.dao</code> exception hierarchy. Uses the same
 * {@link SQLExceptionTranslator} mechanism as {@link JdbcTemplate}.
 * <p>
 * Gets the right {@link SqlSession} from Spring
 * {@link TransactionSynchronizationManager} It also works if its not Tx active.
 * <p>
 * The template needs a SqlSessionFactory to create SqlSessions, passed as a
 * constructor argument. It also can be constructed indicating the executor type
 * to be used, if not, default executor type will be used.
 * <p>
 * Exception translation can be changed overriding {@link #getExceptionTranslator()}
 * or {@link #translateException(Throwable)} 
 * <p>
 * SqlSessionTemplate is thread safe, so a single instance can be shared by all
 * DAOs; there should also be a small memory savings by doing this. This pattern
 * can be used in Spring configuration files as follows:
 * 
 * <pre class="code">
 * {@code 
 * <bean id="sqlSessionTemplate" class="org.mybatis.spring.SqlSessionTemplate"> 
 *   <constructor-arg ref="sqlSessionFactory" /> 
 * </bean>
 * }
 * </pre>
 * 
 * @see SqlSessionFactory
 * @see SqlSession
 * @see SQLExceptionTranslator
 * @see ExecutorType
 * @version $Id$
 */
public class SqlSessionTemplate implements SqlSession {

    private final SqlSessionFactory sqlSessionFactory;
    private final ExecutorType executorType;
    private final SqlSession sqlSessionProxy;
    private SQLExceptionTranslator exceptionTranslator;
    private boolean exceptionTranslatorLazyInit;

    public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        this(sqlSessionFactory, sqlSessionFactory.getConfiguration().getDefaultExecutorType());
    }

    public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType) {
        this(sqlSessionFactory, executorType, true);
    }

    public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType,
            boolean exceptionTranslatorLazyInit) {

        Assert.notNull(sqlSessionFactory, "Property 'sqlSessionFactory' is required");
        Assert.notNull(sqlSessionFactory, "Property 'executorType' is required");

        this.sqlSessionFactory = sqlSessionFactory;
        this.executorType = executorType;
        this.exceptionTranslatorLazyInit = exceptionTranslatorLazyInit;
        this.sqlSessionProxy = (SqlSession) Proxy.newProxyInstance(
                SqlSessionFactory.class.getClassLoader(),
                new Class[] { SqlSession.class }, 
                new SqlSessionInterceptor());

        // the exception translator creation can be delayed until the first
        // SqlException is thrown
        if (!this.exceptionTranslatorLazyInit) {
            getExceptionTranslator();
        }
    }

    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    public ExecutorType getExecutorType() {
        return executorType;
    }

    /**
     * Return the exception translator for this instance.
     * <p>
     * Sets by default {@link SQLErrorCodeSQLExceptionTranslator} for the
     * specified DataSource.
     * This can be overridden if other translation is wanted
     * 
     * @see #getDataSource()
     */
    protected synchronized SQLExceptionTranslator getExceptionTranslator() {
        if (this.exceptionTranslator == null) {
            this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(getDataSource());
        }
        return this.exceptionTranslator;
    }

    public DataSource getDataSource() {
        return this.sqlSessionFactory.getConfiguration().getEnvironment().getDataSource();
    }

    public void setLazyInit(boolean lazyInit) {
        this.exceptionTranslatorLazyInit = lazyInit;
    }

    public boolean isLazyInit() {
        return this.exceptionTranslatorLazyInit;
    }

    /**
     * By default translates MyBatis exceptions into Spring DataAccessExceptions. It uses
     * {@link JdbcTemplate#getExceptionTranslator} for the SqlException
     * translation
     * It can be overridden if other translation is wanted
     * 
     * @param t the exception has to be converted
     * @return a converted exception
     */
    protected RuntimeException translateException(Throwable t) {
        if (t instanceof InvocationTargetException) {
            t = ((InvocationTargetException) t).getTargetException();
        } else if (t instanceof UndeclaredThrowableException) {
            t = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
        }

        if (t instanceof PersistenceException) {
            if (t.getCause() instanceof SQLException) {
                return getExceptionTranslator().translate("SqlSession operation", null, (SQLException) t.getCause());
            }
        } else if (t instanceof DataAccessException) {
            return (DataAccessException) t;
        }

        return new MyBatisSystemException("SqlSession operation", t);
    }

    /**
     * {@inheritDoc}
     */
    public Object selectOne(String statement) {
        return sqlSessionProxy.selectOne(statement);
    }

    /**
     * {@inheritDoc}
     */
    public Object selectOne(String statement, Object parameter) {
        return sqlSessionProxy.selectOne(statement, parameter);
    }

    /**
     * {@inheritDoc}
     */
    public List<?> selectList(String statement) {
        return sqlSessionProxy.selectList(statement);
    }

    /**
     * {@inheritDoc}
     */
    public List<?> selectList(String statement, Object parameter) {
        return sqlSessionProxy.selectList(statement, parameter);
    }

    /**
     * {@inheritDoc}
     */
    public List<?> selectList(String statement, Object parameter, RowBounds rowBounds) {
        return sqlSessionProxy.selectList(statement, parameter, rowBounds);
    }

    /**
     * {@inheritDoc}
     */
    public void select(String statement, ResultHandler handler) {
        sqlSessionProxy.select(statement, handler);
    }

    /**
     * {@inheritDoc}
     */
    public void select(String statement, Object parameter, ResultHandler handler) {
        sqlSessionProxy.select(statement, parameter, handler);
    }

    /**
     * {@inheritDoc}
     */
    public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
        sqlSessionProxy.select(statement, parameter, rowBounds, handler);
    }

    /**
     * {@inheritDoc}
     */
    public int insert(String statement) {
        return sqlSessionProxy.insert(statement);
    }

    /**
     * {@inheritDoc}
     */
    public int insert(String statement, Object parameter) {
        return sqlSessionProxy.insert(statement, parameter);
    }

    /**
     * {@inheritDoc}
     */
    public int update(String statement) {
        return sqlSessionProxy.update(statement);
    }

    /**
     * {@inheritDoc}
     */
    public int update(String statement, Object parameter) {
        return sqlSessionProxy.update(statement, parameter);
    }

    /**
     * {@inheritDoc}
     */
    public int delete(String statement) {
        return sqlSessionProxy.delete(statement);
    }

    /**
     * {@inheritDoc}
     */
    public int delete(String statement, Object parameter) {
        return sqlSessionProxy.delete(statement, parameter);
    }

    /**
     * {@inheritDoc}
     */
    public <T> T getMapper(Class<T> type) {
        return getConfiguration().getMapper(type, this);
    }

    /**
     * {@inheritDoc}
     */
    public void commit() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public void commit(boolean force) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public void rollback() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public void rollback(boolean force) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public void clearCache() {
        this.sqlSessionProxy.clearCache();
    }

    /**
     * {@inheritDoc}
     */
    public Configuration getConfiguration() {
        return sqlSessionFactory.getConfiguration();
    }

    /**
     * {@inheritDoc}
     */
    public Connection getConnection() {
        return sqlSessionProxy.getConnection();
    }

    /**
     * Proxy needed to route Mapper method calls to the proper SqlSession got
     * from String's Transaction Manager
     * 
     */
    private class SqlSessionInterceptor implements InvocationHandler {
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final SqlSession sqlSession = SqlSessionUtils.getSqlSession(
                    SqlSessionTemplate.this.sqlSessionFactory,
                    SqlSessionTemplate.this.executorType);
            try {
                return method.invoke(sqlSession, args);
            } catch (Throwable t) {
                throw translateException(t);
            } finally {
                SqlSessionUtils.closeSqlSession(sqlSession, SqlSessionTemplate.this.sqlSessionFactory);
            }
        }
    }

}
