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

import java.sql.SQLException;

import org.apache.ibatis.session.SqlSessionFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockResultSet;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

/**
 * 
 *
 * @version $Id$
 */
public abstract class AbstractMyBatisSpringTest {

    protected static PooledMockDataSource dataSource = new PooledMockDataSource();

    protected static SqlSessionFactory sqlSessionFactory;

    protected static ExecutorInterceptor executorInterceptor = new ExecutorInterceptor();

    protected static DataSourceTransactionManager txManager;

    protected MockConnection connection;

    protected MockConnection connectionTwo;

    @BeforeClass
    public static void setupBase() throws Exception {
        // create an SqlSessionFactory that will use SpringManagedTransactions
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setMapperLocations(new Resource[] { new ClassPathResource("org/mybatis/spring/TestMapper.xml") });
        // note running without SqlSessionFactoryBean.configLocation set => default configuration
        factoryBean.setDataSource(dataSource);

        sqlSessionFactory = factoryBean.getObject();
        sqlSessionFactory.getConfiguration().addInterceptor(executorInterceptor);

        txManager = new DataSourceTransactionManager(dataSource);
    }

    protected void assertNoCommit() {
        assertEquals("should not call commit on Connection", 0, connection.getNumberCommits());
        assertEquals("should not call rollback on Connection", 0, connection.getNumberRollbacks());
        assertEquals("should not call commit on SqlSession", 0, executorInterceptor.getCommitCount());
        assertEquals("should not call rollback on SqlSession", 0, executorInterceptor.getRollbackCount());
    }

    protected void assertCommit() {
        assertEquals("should call commit on Connection", 1, connection.getNumberCommits());
        assertEquals("should not call rollback on Connection", 0, connection.getNumberRollbacks());
        assertEquals("should call commit on SqlSession", 1, executorInterceptor.getCommitCount());
        assertEquals("should not call rollback on SqlSession", 0, executorInterceptor.getRollbackCount());
    }

    protected void assertRollback() {
        assertEquals("should not call commit on Connection", 0, connection.getNumberCommits());
        assertEquals("should call rollback on Connection", 1, connection.getNumberRollbacks());
        assertEquals("should not call commit on SqlSession", 0, executorInterceptor.getCommitCount());
        assertEquals("should call rollback on SqlSession", 1, executorInterceptor.getRollbackCount());
    }

    protected void assertSingleConnection() {
        assertEquals("should only call DataSource.getConnection() once", 1, dataSource.getConnectionCount());
    }

    protected void assertExecuteCount(int count) {
        assertEquals("should have executed " + count + " SQL statements", count, connection
                .getPreparedStatementResultSetHandler().getExecutedStatements().size());
    }

    protected void assertConnectionClosed(MockConnection connection) {
        try {
            if ((connection != null) && !connection.isClosed()) {
                fail("Connection is not closed");
            }
        } catch (SQLException sqle) {
            fail("cannot call Connection.isClosed() " + sqle.getMessage());
        }
    }

    protected MockConnection createMockConnection() {
        // this query must be the same as the query in TestMapper.xml
        MockResultSet rs = new MockResultSet("SELECT 1");
        rs.addRow(new Object[] { 1 });

        MockConnection con = new MockConnection();
        con.getPreparedStatementResultSetHandler().prepareResultSet("SELECT 1", rs);

        return con;
    }

    /*
     * Setup a new Connection before each test since its closed state will need to be checked
     * afterwards and there is no Connection.open().
     */
    @Before
    public void setupConnection() throws SQLException {
        connection = createMockConnection();
        connectionTwo = createMockConnection();
        dataSource.addConnection(connectionTwo);
        dataSource.addConnection(connection);
    }

    @Before
    public void resetExecutorInterceptor() {
        executorInterceptor.reset();
    }

    @Before
    public void resetDataSource() {
        dataSource.reset();
    }

    @After
    public void validateConnectionClosed() {
        assertConnectionClosed(connection);

        connection = null;
    }

}
