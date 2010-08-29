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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockResultSet;

/**
 * 
 *
 * @version $Id$
 */
public class MyBatisSpringTest {

    private static CountingMockDataSource DATA_SOURCE;

    private static SqlSessionFactory SQL_SESSION_FACTORY;

    private static SqlSessionTemplate SQL_SESSION_TEMPLATE;

    private static ExecutorInterceptor EXECUTOR_INTERCEPTOR = new ExecutorInterceptor();

    private static DataSourceTransactionManager TX_MANAGER;

    private MockConnection connection;

    private SqlSession session;

    @BeforeClass
    public static void setup() throws Exception {
        DATA_SOURCE = new CountingMockDataSource();

        // create an SqlSessionFactory that will use SpringManagedTransactions
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setMapperLocations(new Resource[]{new ClassPathResource(
                "org/mybatis/spring/TestDao.xml")});
        factoryBean.setDataSource(DATA_SOURCE);

        SQL_SESSION_FACTORY = factoryBean.getObject();
        SQL_SESSION_FACTORY.getConfiguration().addInterceptor(EXECUTOR_INTERCEPTOR);

        SQL_SESSION_TEMPLATE = new SqlSessionTemplate(SQL_SESSION_FACTORY);

        TX_MANAGER = new DataSourceTransactionManager(DATA_SOURCE);
    }

    // iBatis tests to make sure the iBatis API still works with SpringManagedTransaction
    @Test
    public void testIbatisAPI() {
        this.session = SQL_SESSION_FACTORY.openSession();
        this.session.getMapper(TestDao.class).findTest();
        this.session.close();

        assertNoCommit();
        assertSingleConnection();
    }

    @Test
    public void testIbatisAPIWithCommit() {
        this.session = SQL_SESSION_FACTORY.openSession();
        this.session.getMapper(TestDao.class).findTest();
        this.session.commit(true);
        this.session.close();

        assertCommit();
        assertSingleConnection();
    }

    @Test
    public void testIbatisAPIWithRollback() {
        this.session = SQL_SESSION_FACTORY.openSession();
        this.session.getMapper(TestDao.class).findTest();
        this.session.rollback(true);
        this.session.close();

        assertRollback();
        assertSingleConnection();
    }

    // basic tests using SqlSessionUtils instead of using the iBatis API directly
    @Test
    public void testSpringAPI() {
        this.session = SqlSessionUtils.getSqlSession(SQL_SESSION_FACTORY);
        this.session.getMapper(TestDao.class).findTest();
        SqlSessionUtils.closeSqlSession(session, SQL_SESSION_FACTORY);

        assertNoCommit();
        assertSingleConnection();
    }

    @Test
    public void testSpringAPIWithCommit() {
        this.session = SqlSessionUtils.getSqlSession(SQL_SESSION_FACTORY);
        this.session.getMapper(TestDao.class).findTest();
        this.session.commit(true);
        SqlSessionUtils.closeSqlSession(session, SQL_SESSION_FACTORY);

        assertCommit();
        assertSingleConnection();
    }

    @Test
    public void testSpringAPIWithRollback() {
        this.session = SqlSessionUtils.getSqlSession(SQL_SESSION_FACTORY);
        this.session.getMapper(TestDao.class).findTest();
        this.session.rollback(true);
        SqlSessionUtils.closeSqlSession(session, SQL_SESSION_FACTORY);

        assertRollback();
        assertSingleConnection();
    }

    @Test
    public void testSpringAPIWithIbatisClose() {
        // This is a programming error and could lead to connection leak if there is a transaction
        // in progress. But, the API allows it, so make sure it at least works without a tx.
        this.session = SqlSessionUtils.getSqlSession(SQL_SESSION_FACTORY);
        this.session.getMapper(TestDao.class).findTest();
        this.session.close();

        assertNoCommit();
        assertSingleConnection();
    }

    @Test
    public void testWithSameDataSource() {
        // use the same DataSource the SqlSession is configured with
        this.session = SqlSessionUtils.getSqlSession(SQL_SESSION_FACTORY, DATA_SOURCE);
        this.session.getMapper(TestDao.class).findTest();
        SqlSessionUtils.closeSqlSession(this.session, SQL_SESSION_FACTORY);

        assertNoCommit();
        assertSingleConnection();
    }

    @Test
    public void testWithDifferentDataSource() {
        CountingMockDataSource ds = new CountingMockDataSource();
        ds.setupConnection(connection);

        this.session = SqlSessionUtils.getSqlSession(SQL_SESSION_FACTORY, ds);
        this.session.getMapper(TestDao.class).findTest();
        SqlSessionUtils.closeSqlSession(session, SQL_SESSION_FACTORY);

        assertNoCommit();
        assertEquals("should only call DataSource.getConnection() once", 1, ds.getConnectionCount());
        assertEquals("should not call DataSource.getConnection() on SqlSession DataSource", 0, DATA_SOURCE.getConnectionCount());
    }

    @Test
    public void testWithTxSupports() {
        DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
        txDef.setPropagationBehaviorName("PROPAGATION_SUPPORTS");

        TransactionStatus status = TX_MANAGER.getTransaction(txDef);

        this.session = SqlSessionUtils.getSqlSession(SQL_SESSION_FACTORY, DATA_SOURCE);
        this.session.getMapper(TestDao.class).findTest();
        SqlSessionUtils.closeSqlSession(session, SQL_SESSION_FACTORY);

        TX_MANAGER.commit(status);

        // SUPPORTS should just activate tx synchronization but not commits
        assertNoCommit();
        assertSingleConnection();
    }

    @Test
    public void testWithTxRequired() {
        DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
        txDef.setPropagationBehaviorName("PROPAGATION_REQUIRED");

        TransactionStatus status = TX_MANAGER.getTransaction(txDef);

        this.session = SqlSessionUtils.getSqlSession(SQL_SESSION_FACTORY, DATA_SOURCE);
        this.session.getMapper(TestDao.class).findTest();
        SqlSessionUtils.closeSqlSession(session, SQL_SESSION_FACTORY);

        TX_MANAGER.commit(status);

        assertCommit();
        assertSingleConnection();
    }

    @Test
    public void testSqlSessionCommitWithTx() {
        DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
        txDef.setPropagationBehaviorName("PROPAGATION_REQUIRED");

        TransactionStatus status = TX_MANAGER.getTransaction(txDef);

        this.session = SqlSessionUtils.getSqlSession(SQL_SESSION_FACTORY, DATA_SOURCE);
        this.session.getMapper(TestDao.class).findTest();
        // commit should no-op since there is an active transaction
        // commit should only be called once in total
        this.session.commit(true);
        SqlSessionUtils.closeSqlSession(session, SQL_SESSION_FACTORY);

        TX_MANAGER.commit(status);

        // Connection should be commited once, but we explicity called commit on the SqlSession, so
        // it should be commited twice
        assertEquals("should call commit on Connection", 1, connection.getNumberCommits());
        assertEquals("should not call rollback on Connection", 0, connection.getNumberRollbacks());
        assertEquals("should call commit on SqlSession", 2, EXECUTOR_INTERCEPTOR.getCommitCount());
        assertEquals("should not call rollback on SqlSession", 0, EXECUTOR_INTERCEPTOR.getRollbackCount());

        assertSingleConnection();
    }

    @Test
    public void testWithOtherTx() {
        MockConnection connection1 = createMockConnection();
        MockConnection connection2 = createMockConnection();

        final PooledMockDataSource ds = new PooledMockDataSource();
        ds.addConnection(connection1);
        ds.addConnection(connection2);

        // session not in existing tx, should use first connection
        this.session = SqlSessionUtils.getSqlSession(SQL_SESSION_FACTORY, ds);

        try {
            // this transaction should use another Connection
            TX_MANAGER.setDataSource(ds);
            TransactionStatus status = TX_MANAGER.getTransaction(new DefaultTransactionDefinition());

            // all iBatis work happens during the tx, but should not be participating
            this.session.getMapper(TestDao.class).findTest();
            this.session.commit(true);
            SqlSessionUtils.closeSqlSession(session, SQL_SESSION_FACTORY);

            // this should succeed
            // SpringManagedTransaction (from SqlSession.commit()) should not interfere with tx
            TX_MANAGER.commit(status);

            // two transactions should have completed, each using their own Connection
            assertEquals("should call DataSource.getConnection() twice", 2, ds.getConnectionCount());

            // both connections and should be commited
            assertEquals("should call commit on Connection 1", 1, connection1.getNumberCommits());
            assertEquals("should not call rollback on Connection 1", 0, connection1.getNumberRollbacks());

            assertEquals("should call commit on Connection 2", 1, connection2.getNumberCommits());
            assertEquals("should not call rollback on Connection 2", 0, connection2.getNumberRollbacks());

            // the SqlSession should have also commmited
            assertEquals("should call commit on SqlSession", 1, EXECUTOR_INTERCEPTOR.getCommitCount());
            assertEquals("should call rollback on SqlSession", 0, EXECUTOR_INTERCEPTOR.getRollbackCount());
        }
        finally {
            // reset the txManager; keep other tests from potentially failing
            TX_MANAGER.setDataSource(DATA_SOURCE);

            // null the connection since it was not used
            // this avoids failing in validateConnectionClosed()
            connection = null;
        }
    }

    @Test
    public void testSuspendAndResume() {
        MockConnection connection1 = createMockConnection();
        MockConnection connection2 = createMockConnection();

        final PooledMockDataSource ds = new PooledMockDataSource();
        ds.addConnection(connection1);
        ds.addConnection(connection2);

        try {
            TX_MANAGER.setDataSource(ds);
            TransactionStatus status = TX_MANAGER.getTransaction(new DefaultTransactionDefinition());

            this.session = SqlSessionUtils.getSqlSession(SQL_SESSION_FACTORY, ds);

            // start a new tx while the other is in progress
            DefaultTransactionDefinition txRequiresNew = new DefaultTransactionDefinition();
            txRequiresNew.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
            TransactionStatus status2 = TX_MANAGER.getTransaction(txRequiresNew);

            SqlSession session2 = SqlSessionUtils.getSqlSession(SQL_SESSION_FACTORY, ds);

            assertNotSame("getSqlSession() should not return suspended SqlSession", session, session2);

            SqlSessionUtils.closeSqlSession(session2, SQL_SESSION_FACTORY);
            TX_MANAGER.commit(status2);

            // first tx should be resumed now and this should succeed
            session.getMapper(TestDao.class).findTest();
            SqlSessionUtils.closeSqlSession(session, SQL_SESSION_FACTORY);
            TX_MANAGER.commit(status);

            // two transactions should have completed, each using their own Connection
            assertEquals("should call DataSource.getConnection() twice", 2, ds.getConnectionCount());

            // both connections and should be commited
            assertEquals("should call commit on Connection 1", 1, connection1.getNumberCommits());
            assertEquals("should not call rollback on Connection 1", 0, connection1.getNumberRollbacks());

            assertEquals("should call commit on Connection 2", 1, connection2.getNumberCommits());
            assertEquals("should not call rollback on Connection 2", 0, connection2.getNumberRollbacks());

            // the SqlSession should have also commmited twice
            assertEquals("should call commit on SqlSession", 2, EXECUTOR_INTERCEPTOR.getCommitCount());
            assertEquals("should call rollback on SqlSession", 0, EXECUTOR_INTERCEPTOR.getRollbackCount());
        } finally {
            // reset the txManager; keep other tests from potentially failing
            TX_MANAGER.setDataSource(DATA_SOURCE);

            // null the connection since it was not used
            // this avoids failing in validateConnectionClosed()
            this.connection = null;
        }
    }

    // these mapper tests should also cover SqlSessionTemplate since MapperFactoryBean uses it
    @Test
    public void testMapperFactoryBean() throws Exception {
        MapperFactoryBean<TestDao> mapper = new MapperFactoryBean<TestDao>();
        mapper.setMapperInterface(TestDao.class);
        mapper.setSqlSessionTemplate(SQL_SESSION_TEMPLATE);
        mapper.afterPropertiesSet();

        TestDao finder = mapper.getObject();
        finder.findTest();

        assertNoCommit();
        assertSingleConnection();
    }

    @Test
    public void testMapperFactoryBeanWithDifferentDataSource() throws Exception {
        CountingMockDataSource mockDataSource = new CountingMockDataSource();
        mockDataSource.setupConnection(this.connection);

        SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(SQL_SESSION_FACTORY);
        sqlSessionTemplate.setDataSource(mockDataSource);

        MapperFactoryBean<TestDao> mapper = new MapperFactoryBean<TestDao>();
        mapper.setMapperInterface(TestDao.class);
        mapper.setSqlSessionTemplate(sqlSessionTemplate);
        mapper.afterPropertiesSet();

        TestDao finder = mapper.getObject();
        finder.findTest();

        assertNoCommit();
        assertEquals("should only call DataSource.getConnection() once", 1, mockDataSource.getConnectionCount());
        assertEquals("should not call DataSource.getConnection() on SqlSession DataSource", 0, DATA_SOURCE
                .getConnectionCount());
    }

    @Test
    public void testMapperFactoryBeanWithTx() throws Exception {
        MapperFactoryBean<TestDao> mapper = new MapperFactoryBean<TestDao>();
        mapper.setMapperInterface(TestDao.class);
        mapper.setSqlSessionTemplate(SQL_SESSION_TEMPLATE);
        mapper.afterPropertiesSet();

        TransactionStatus status = TX_MANAGER.getTransaction(new DefaultTransactionDefinition());

        TestDao finder = mapper.getObject();
        finder.findTest();

        TX_MANAGER.commit(status);

        assertCommit();
        assertSingleConnection();
    }

    @Test
    public void testMapperFactoryBeanWithNonSpringTransaction() throws Exception {
        Environment original = SQL_SESSION_FACTORY.getConfiguration().getEnvironment();
        Environment nonSpring = new Environment("non-spring", new JdbcTransactionFactory(), DATA_SOURCE);
        SQL_SESSION_FACTORY.getConfiguration().setEnvironment(nonSpring);

        try {
            MapperFactoryBean<TestDao> mapper = new MapperFactoryBean<TestDao>();
            mapper.setMapperInterface(TestDao.class);
            mapper.setSqlSessionTemplate(SQL_SESSION_TEMPLATE);
            mapper.afterPropertiesSet();

            TestDao finder = mapper.getObject();
            finder.findTest();

            assertNoCommit();
            assertSingleConnection();
        } finally {
            SQL_SESSION_FACTORY.getConfiguration().setEnvironment(original);
        }
    }

    @Test(expected = TransientDataAccessResourceException.class)
    public void testMapperFactoryBeanWithTxAndNonSpringTransaction() throws Exception {
        Environment original = SQL_SESSION_FACTORY.getConfiguration().getEnvironment();
        Environment nonSpring = new Environment("non-spring", new JdbcTransactionFactory(), DATA_SOURCE);
        SQL_SESSION_FACTORY.getConfiguration().setEnvironment(nonSpring);

        TransactionStatus status = null;

        try {
            SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(SQL_SESSION_FACTORY);
            sqlSessionTemplate.setDataSource(DATA_SOURCE);

            MapperFactoryBean<TestDao> mapper = new MapperFactoryBean<TestDao>();
            mapper.setMapperInterface(TestDao.class);
            mapper.setSqlSessionTemplate(sqlSessionTemplate);
            mapper.afterPropertiesSet();

            status = TX_MANAGER.getTransaction(new DefaultTransactionDefinition());

            TestDao finder = mapper.getObject();
            finder.findTest();
        } finally {
            // rollback required to close connection
            TX_MANAGER.rollback(status);
            SQL_SESSION_FACTORY.getConfiguration().setEnvironment(original);
        }
    }

    private void assertNoCommit() {
        assertEquals("should not call commit on Connection", 0, connection.getNumberCommits());
        assertEquals("should not call rollback on Connection", 0, connection.getNumberRollbacks());
        assertEquals("should not call commit on SqlSession", 0, EXECUTOR_INTERCEPTOR.getCommitCount());
        assertEquals("should not call rollback on SqlSession", 0, EXECUTOR_INTERCEPTOR.getRollbackCount());
    }

    private void assertCommit() {
        assertEquals("should call commit on Connection", 1, connection.getNumberCommits());
        assertEquals("should not call rollback on Connection", 0, connection.getNumberRollbacks());
        assertEquals("should call commit on SqlSession", 1, EXECUTOR_INTERCEPTOR.getCommitCount());
        assertEquals("should not call rollback on SqlSession", 0, EXECUTOR_INTERCEPTOR.getRollbackCount());
    }

    private void assertRollback() {
        assertEquals("should not call commit on Connection", 0, connection.getNumberCommits());
        assertEquals("should call rollback on Connection", 1, connection.getNumberRollbacks());
        assertEquals("should not call commit on SqlSession", 0, EXECUTOR_INTERCEPTOR.getCommitCount());
        assertEquals("should call rollback on SqlSession", 1, EXECUTOR_INTERCEPTOR.getRollbackCount());
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
        this.connection = createMockConnection();
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
