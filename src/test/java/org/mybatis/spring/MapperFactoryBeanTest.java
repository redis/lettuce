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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockResultSet;

/**
 * 
 *
 * @version $Id$
 */
public class MapperFactoryBeanTest {

    private static ApplicationContext CTX;

    private static CountingMockDataSource DATA_SOURCE;

    private static ExecutorInterceptor EXECUTOR_INTERCEPTOR = new ExecutorInterceptor();

    private MockConnection connection;

    private SqlSession session;

    @BeforeClass
    public static void setup() throws Exception {
        CTX = new ClassPathXmlApplicationContext("org/mybatis/spring/application-context-test-MapperFactoryBean.xml");

        DATA_SOURCE = CTX.getBean("dataSource", CountingMockDataSource.class);

        SqlSessionFactory sqlSessionFactory = CTX.getBean("sqlSessionFactory", SqlSessionFactory.class);
        sqlSessionFactory.getConfiguration().addInterceptor(EXECUTOR_INTERCEPTOR);
    }

    // these mapper tests should also cover SqlSessionTemplate since MapperFactoryBean uses it
    @Test
    public void testMapperFactoryBean() throws Exception {
        TestDao testDao = CTX.getBean("testDao", TestDao.class);
        Integer result = testDao.findTest();
        assertEquals(Integer.valueOf(1), result);

        assertNoCommit();
        assertSingleConnection();
    }

    private void assertNoCommit() {
        assertEquals("should not call commit on Connection", 0, connection.getNumberCommits());
        assertEquals("should not call rollback on Connection", 0, connection.getNumberRollbacks());
        assertEquals("should not call commit on SqlSession", 0, EXECUTOR_INTERCEPTOR.getCommitCount());
        assertEquals("should not call rollback on SqlSession", 0, EXECUTOR_INTERCEPTOR.getRollbackCount());
    }

    private void assertSingleConnection() {
        assertEquals("should only call DataSource.getConnection() once", 1, DATA_SOURCE.getConnectionCount());
    }

    private MockConnection createMockConnection() {
        // this query must be the same as the query in TestDao.xml
        MockResultSet rs = new MockResultSet("SELECT 1");
        rs.addRow(new Object[]{1});

        MockConnection con = new MockConnection();
        con.getPreparedStatementResultSetHandler().prepareResultSet("SELECT 1", rs);

        return con;
    }

    /*
     * Setup a new Connection before each test since its closed state will need to be checked
     * afterwards and there is no Connection.open().
     */
    @Before
    public void setupConnection() {
        connection = createMockConnection();
        DATA_SOURCE.setupConnection(connection);
    }

    @Before
    public void resetExecutorInterceptor() {
        EXECUTOR_INTERCEPTOR.reset();
    }

    @Before
    public void resetDataSource() {
        DATA_SOURCE.reset();
    }

    @After
    public void validateSessionClose() {
        // assume if the Executor is closed, the Session is too
        if ((this.session != null) && !EXECUTOR_INTERCEPTOR.isExecutorClosed()) {
            fail("SqlSession is not closed");
        }

        this.session = null;
    }

    @After
    public void validateConnectionClosed() throws SQLException {
        if ((this.connection != null) && !this.connection.isClosed()) {
            fail("Connection is not closed");
        }

        this.connection = null;
    }

}
