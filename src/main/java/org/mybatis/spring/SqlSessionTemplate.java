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
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcAccessor;
import org.springframework.util.Assert;

/**
 * Helper class that simplifies data access via the MyBatis
 * {@link org.apache.ibatis.session.SqlSession} API, converting checked SQLExceptions into unchecked
 * DataAccessExceptions, following the <code>org.springframework.dao</code> exception hierarchy.
 * Uses the same {@link org.springframework.jdbc.support.SQLExceptionTranslator} mechanism as
 * {@link org.springframework.jdbc.core.JdbcTemplate}.
 * <p>
 * The main method of this class executes a callback that implements a data access action.
 * Furthermore, this class provides numerous convenience methods that mirror
 * {@link org.apache.ibatis.session.SqlSession}'s execution methods.
 * <p>
 * It is generally recommended to use the convenience methods on this template for plain
 * query/insert/update/delete operations. However, for more complex operations like batch updates, a
 * custom SqlSessionCallback must be implemented, usually as anonymous inner class. For example:
 *
 * <pre class="code">
 * {@code
 * getSqlSessionTemplate().execute(new SqlSessionCallback&lt;Object&gt;() {
 *     public Object doInSqlSession(SqlSession sqlSession) throws SQLException {
 *         sqlSession.getMapper(MyMapper.class).update(parameterObject);
 *         sqlSession.update(&quot;MyMapper.update&quot;, otherParameterObject);
 *         return null;
 *     }
 * }, ExecutorType.BATCH);
 * }
 * </pre>
 *
 * The template needs a SqlSessionFactory to create SqlSessions, passed in via the
 * "sqlSessionFactory" property or as a constructor argument.
 * <p>
 * SqlSessionTemplate is thread safe, so a single instance can be shared by all DAOs; there
 * should also be a small memory savings by doing this. To support a shared template, this class has
 * a constructor that accepts an SqlSessionTemplate. This pattern can be used in Spring
 * configuration files as follows:
 *
 * <pre class="code">
 * {@code
 *   <bean id="sqlSessionTemplate" class="org.mybatis.spring.SqlSessionTemplate">
 *     <property name="sqlSessionFactory" ref="sqlSessionFactory" />
 *   </bean>
 * }
 * </pre>
 *
 * @see #execute
 * @see #setSqlSessionFactory(org.apache.ibatis.session.SqlSessionFactory)
 * @see SqlSessionFactoryBean#setDataSource
 * @see org.apache.ibatis.session.SqlSessionFactory#getConfiguration()
 * @see org.apache.ibatis.session.SqlSession
 * @see org.mybatis.spring.SqlSessionOperations
 * @version $Id$
 */
public class SqlSessionTemplate extends JdbcAccessor implements SqlSessionOperations {

    private SqlSessionFactory sqlSessionFactory;

    /**
     * This constructor is left here to enable the creation of the SqlSessionTemplate
     * using this xml in the applicationContext.xml. Otherwise constructor should be used
     * and that will not match how other mybatis-spring beans are created.
     * 
     * <pre class="code">
     * {@code
     * <bean id="sqlSessionTemplate" class="org.mybatis.spring.SqlSessionTemplate">
     *   <property name="sqlSessionFactory" ref="sqlSessionFactory" />
     * </bean>
     * }
     * </pre>
     */
    public SqlSessionTemplate() {
    }

    public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        setSqlSessionFactory(sqlSessionFactory);
        afterPropertiesSet();
    }

    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDataSource(DataSource dataSource) {
        throw new UnsupportedOperationException("Datasource change is not allowed. SqlSessionFactory datasource must be used");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSource getDataSource() {
        return this.sqlSessionFactory.getConfiguration().getEnvironment().getDataSource();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() {
        Assert.notNull(this.sqlSessionFactory, "Property 'sqlSessionFactory' is required");
        super.afterPropertiesSet();
    }

    /**
     * Execute the given data access action with the proper SqlSession (got from current transaction or 
     * a new one)
     *
     * @param <T>
     * @param action
     * @return
     * @throws DataAccessException
     */
    public <T> T execute(SqlSessionCallback<T> action) throws DataAccessException {
        return execute(action, this.sqlSessionFactory.getConfiguration().getDefaultExecutorType());
    }

    /**
     * Execute the given data access action on a Executor.
     *
     * @param action callback object that specifies the data access action
     * @param executorType SIMPLE, REUSE, BATCH
     * @return a result object returned by the action, or <code>null</code>
     * @throws DataAccessException in case of errors
     */
    public <T> T execute(SqlSessionCallback<T> action, ExecutorType executorType) throws DataAccessException {
        Assert.notNull(action, "Callback object must not be null");
        Assert.notNull(this.sqlSessionFactory, "No SqlSessionFactory specified");

        SqlSession sqlSession = SqlSessionUtils.getSqlSession(this.sqlSessionFactory, getDataSource(), executorType);

        try {
            return action.doInSqlSession(sqlSession);
        } catch (Throwable t) {
            throw wrapException(t);
        } finally {
            SqlSessionUtils.closeSqlSession(sqlSession, this.sqlSessionFactory);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object selectOne(String statement) {
        return selectOne(statement, null);
    }

    /**
     * {@inheritDoc}
     */
    public Object selectOne(final String statement, final Object parameter) {
        return execute(new SqlSessionCallback<Object>() {
            public Object doInSqlSession(SqlSession sqlSession) {
                return sqlSession.selectOne(statement, parameter);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public <T> List<T> selectList(String statement) {
        return selectList(statement, null);
    }

    /**
     * {@inheritDoc}
     */
    public <T> List<T> selectList(String statement, Object parameter) {
        return selectList(statement, parameter, RowBounds.DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    public <T> List<T> selectList(final String statement, final Object parameter, final RowBounds rowBounds) {
        return execute(new SqlSessionCallback<List<T>>() {
            @SuppressWarnings({ "unchecked" })
            public List<T> doInSqlSession(SqlSession sqlSession) {
                return sqlSession.selectList(statement, parameter, rowBounds);
            }
        });
    }

//    /**
//     * {@inheritDoc}
//     */
//    public <K, T> Map<K, T> selectMap(final String statement, final String mapKey) {
//        return selectMap(statement, null, mapKey);
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    public <K, T> Map<K, T> selectMap(final String statement, final Object parameter, final String mapKey) {
//        return selectMap(statement, null, mapKey, RowBounds.DEFAULT);
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    public <K, T> Map<K, T> selectMap(final String statement, final Object parameter, final String mapKey, final RowBounds rowBounds) {
//        return execute(new SqlSessionCallback<Map<K, T>>() {
//            @SuppressWarnings("unchecked")
//            public Map<K, T> doInSqlSession(SqlSession sqlSession) {
//                return sqlSession.selectMap(statement, parameter, mapKey, rowBounds);
//            }
//        });
//    }
    
    /**
     * {@inheritDoc}
     */
    public void select(String statement, Object parameter, ResultHandler handler) {
        select(statement, parameter, RowBounds.DEFAULT, handler);
    }

    /**
     * {@inheritDoc}
     */
    public void select(String statement, ResultHandler handler) {
        select(statement, null, RowBounds.DEFAULT, handler);
    }

    /**
     * {@inheritDoc}
     */
    public void select(final String statement, final Object parameter, final RowBounds rowBounds,
                       final ResultHandler handler) {
        execute(new SqlSessionCallback<Object>() {
            public Object doInSqlSession(SqlSession sqlSession) {
                sqlSession.select(statement, parameter, rowBounds, handler);
                return null;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public int insert(String statement) {
        return insert(statement, null);
    }

    /**
     * {@inheritDoc}
     */
    public int insert(final String statement, final Object parameter) {
        return execute(new SqlSessionCallback<Integer>() {
            public Integer doInSqlSession(SqlSession sqlSession) {
                return sqlSession.insert(statement, parameter);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public int update(String statement) {
        return update(statement, null);
    }

    /**
     * {@inheritDoc}
     */
    public int update(final String statement, final Object parameter) {
        return execute(new SqlSessionCallback<Integer>() {
            public Integer doInSqlSession(SqlSession sqlSession) {
                return sqlSession.update(statement, parameter);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public int delete(String statement) {
        return delete(statement, null);
    }

    /**
     * {@inheritDoc}
     */
    public int delete(final String statement, final Object parameter) {
        return execute(new SqlSessionCallback<Integer>() {
            public Integer doInSqlSession(SqlSession sqlSession) {
                return sqlSession.delete(statement, parameter);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T getMapper(final Class<T> type) {
        return (T) java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, new InvocationHandler() {
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                return execute(new SqlSessionCallback<Object>() {
                    public Object doInSqlSession(SqlSession sqlSession) {
                        try {
                            return method.invoke(sqlSession.getMapper(type), args);
                        } catch (InvocationTargetException e) {
                            throw wrapException(e.getCause());
                        } catch (Exception e) {
                            throw new MyBatisSystemException("SqlSession operation", e);
                        }
                    }
                });
            }
        });
    }

    /**
     * Translates MyBatis exceptions into Spring DataAccessExceptions.
     * It uses {@link JdbcTemplate#getExceptionTranslator} for the SqlException translation
     * 
     * @param t the exception has to be converted to DataAccessException.
     * @return a Spring DataAccessException
     */
    protected DataAccessException wrapException(Throwable t) {
        if (t instanceof PersistenceException) {
            if (t.getCause() instanceof SQLException) {
                return getExceptionTranslator().translate("SqlSession operation", null, (SQLException) t.getCause());
            } else {
                return new MyBatisSystemException("SqlSession operation", t);
            }
        } else if (t instanceof DataAccessException) {
            return (DataAccessException) t;
        } else {
            return new MyBatisSystemException("SqlSession operation", t);
        }
    }

}
