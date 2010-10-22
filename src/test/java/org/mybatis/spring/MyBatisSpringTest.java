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

import org.junit.Test;
import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;

import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import org.springframework.dao.TransientDataAccessResourceException;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.mockrunner.mock.jdbc.MockConnection;

/**
 * @version $Id$
 */
public final class MyBatisSpringTest extends AbstractMyBatisSpringTest {

    private SqlSession session;

    @After
    public void validateSessionClose() {
        // assume if the Executor is closed, the Session is too
        if ((session != null) && !executorInterceptor.isExecutorClosed()) {
            session = null;
            fail("SqlSession is not closed");
        } else {
            session = null;
        }
    }

    // ensure MyBatis API still works with SpringManagedTransaction
    @Test
    public void testMyBatisAPI() {
        session = sqlSessionFactory.openSession();
        session.getMapper(TestMapper.class).findTest();
        session.close();

        assertNoCommit();
        assertSingleConnection();
    }

    @Test
    public void testMyBatisAPIWithCommit() {
        session = sqlSessionFactory.openSession();
        session.getMapper(TestMapper.class).findTest();
        session.commit(true);
        session.close();

        assertCommit();
        assertSingleConnection();
    }

    @Test
    public void testMyBatisAPIWithRollback() {
        session = sqlSessionFactory.openSession();
        session.getMapper(TestMapper.class).findTest();
        session.rollback(true);
        session.close();

        assertRollback();
        assertSingleConnection();
    }

    // basic tests using SqlSessionUtils instead of using the MyBatis API directly
    @Test
    public void testSpringAPI() {
        session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
        session.getMapper(TestMapper.class).findTest();
        SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

        assertNoCommit();
        assertSingleConnection();
    }

    @Test
    public void testSpringAPIWithCommit() {
        session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
        session.getMapper(TestMapper.class).findTest();
        session.commit(true);
        SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

        assertCommit();
        assertSingleConnection();
    }

    @Test
    public void testSpringAPIWithRollback() {
        session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
        session.getMapper(TestMapper.class).findTest();
        session.rollback(true);
        SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

        assertRollback();
        assertSingleConnection();
    }

    @Test
    public void testSpringAPIWithMyBatisClose() {
        // This is a programming error and could lead to connection leak if there is a transaction
        // in progress. But, the API allows it, so make sure it at least works without a tx.
        session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
        session.getMapper(TestMapper.class).findTest();
        session.close();

        assertNoCommit();
        assertSingleConnection();
    }

    @Test
    public void testWithSameDataSource() {
        // use the same DataSource the SqlSession is configured with
        session = SqlSessionUtils.getSqlSession(sqlSessionFactory, dataSource);
        session.getMapper(TestMapper.class).findTest();
        SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

        assertNoCommit();
        assertSingleConnection();
    }

    @Test
    public void testWithDifferentDataSource() {
        try {
            CountingMockDataSource ds = new CountingMockDataSource();
            ds.setupConnection(createMockConnection());

            session = SqlSessionUtils.getSqlSession(sqlSessionFactory, ds);
            session.getMapper(TestMapper.class).findTest();
            SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

            assertNoCommit();
            assertEquals("should only call DataSource.getConnection() once", 1, ds.getConnectionCount());
            assertEquals("should not call DataSource.getConnection() on SqlSession DataSource", 0, dataSource
                    .getConnectionCount());
            assertConnectionClosed(ds.getMockConnection());
        } finally {
            // null the connection since it was not used
            // this avoids failing in validateConnectionClosed()
            connection = null;
        }
    }

    // Spring API should work with a MyBatis TransactionFactories, as long as there is not a Spring
    // TX is progress
    @Test
    public void testWithNonSpringTransactionFactory() {
        Environment original = sqlSessionFactory.getConfiguration().getEnvironment();
        Environment nonSpring = new Environment("non-spring", new JdbcTransactionFactory(), dataSource);
        sqlSessionFactory.getConfiguration().setEnvironment(nonSpring);

        try {
            session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
            session.getMapper(TestMapper.class).findTest();
            SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

            // users need to manually call commit, rollback and close, just like with normal MyBatis
            // API usage
            assertNoCommit();
            assertSingleConnection();
        } finally {
            sqlSessionFactory.getConfiguration().setEnvironment(original);
        }
    }

    @Test(expected = TransientDataAccessResourceException.class)
    public void testNonSpringTxFactoryWithTx() throws Exception {
        Environment original = sqlSessionFactory.getConfiguration().getEnvironment();
        Environment nonSpring = new Environment("non-spring", new JdbcTransactionFactory(), dataSource);
        sqlSessionFactory.getConfiguration().setEnvironment(nonSpring);

        TransactionStatus status = null;

        try {
            status = txManager.getTransaction(new DefaultTransactionDefinition());

            session = SqlSessionUtils.getSqlSession(sqlSessionFactory);

            fail("should not be able to get an SqlSession using non-Spring tx manager when there is an active Spring tx");
        } finally {
            // rollback required to close connection
            txManager.rollback(status);

            sqlSessionFactory.getConfiguration().setEnvironment(original);
        }
    }

    // TODO should this pass?
    /*
     * this is an edge case - completly separate DataSource, non-Spring TXManager but with an
     * existing Spring TX. Technically, this could be allowed, but the current implementation fails
     * because DataSourceUtils.getConnection(DataSource) pulls _any_ Connection into the current tx.
     * To fix, however, SqlSessionTemplate.execute() would need to run more checks
     * (DataSourceUtils.isConnectionTransactional() and unwrapping TransactionAwareDataSourceProxy)
     * which would increase the code path for all transactions.
     */
    @Test(expected = TransientDataAccessResourceException.class)
    public void testNonSpringTxFactoryNonSpringDSWithTx() {
        Environment original = sqlSessionFactory.getConfiguration().getEnvironment();

        CountingMockDataSource mockDataSource = new CountingMockDataSource();
        mockDataSource.setupConnection(createMockConnection());

        Environment nonSpring = new Environment("non-spring", new JdbcTransactionFactory(), mockDataSource);
        sqlSessionFactory.getConfiguration().setEnvironment(nonSpring);

        TransactionStatus status = null;

        try {
            status = txManager.getTransaction(new DefaultTransactionDefinition());

            session = SqlSessionUtils.getSqlSession(sqlSessionFactory);

            fail("should not be able to get an SqlSession using non-Spring tx manager when there is an active Spring tx");
        } finally {
            // rollback required to close connection
            txManager.rollback(status);

            sqlSessionFactory.getConfiguration().setEnvironment(original);
        }
    }

    @Test
    public void testWithTxSupports() {
        DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
        txDef.setPropagationBehaviorName("PROPAGATION_SUPPORTS");

        TransactionStatus status = txManager.getTransaction(txDef);

        session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
        session.getMapper(TestMapper.class).findTest();
        SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

        txManager.commit(status);

        // SUPPORTS should just activate tx synchronization but not commits
        assertNoCommit();
        assertSingleConnection();
    }

    @Test
    public void testWithTxRequired() {
        DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
        txDef.setPropagationBehaviorName("PROPAGATION_REQUIRED");

        TransactionStatus status = txManager.getTransaction(txDef);

        session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
        session.getMapper(TestMapper.class).findTest();
        SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

        txManager.commit(status);

        assertCommit();
        assertSingleConnection();
    }

    @Test
    public void testSqlSessionCommitWithTx() {
        DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
        txDef.setPropagationBehaviorName("PROPAGATION_REQUIRED");

        TransactionStatus status = txManager.getTransaction(txDef);

        session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
        session.getMapper(TestMapper.class).findTest();
        // commit should no-op since there is an active transaction
        session.commit(true);
        SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

        txManager.commit(status);

        // Connection should be commited once, but we explicity called commit on the SqlSession, so
        // it should be commited twice
        assertEquals("should call commit on Connection", 1, connection.getNumberCommits());
        assertEquals("should not call rollback on Connection", 0, connection.getNumberRollbacks());
        assertEquals("should call commit on SqlSession", 2, executorInterceptor.getCommitCount());
        assertEquals("should not call rollback on SqlSession", 0, executorInterceptor.getRollbackCount());

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
        session = SqlSessionUtils.getSqlSession(sqlSessionFactory, ds);

        try {
            // this transaction should use another Connection
            txManager.setDataSource(ds);
            TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());

            // all MyBatis work happens during the tx, but should not be participating
            session.getMapper(TestMapper.class).findTest();
            session.commit(true);
            SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

            // this should succeed
            // SpringManagedTransaction (from SqlSession.commit()) should not interfere with tx
            txManager.commit(status);

            // two transactions should have completed, each using their own Connection
            assertEquals("should call DataSource.getConnection() twice", 2, ds.getConnectionCount());

            // both connections and should be commited
            assertEquals("should call commit on Connection 1", 1, connection1.getNumberCommits());
            assertEquals("should not call rollback on Connection 1", 0, connection1.getNumberRollbacks());

            assertEquals("should call commit on Connection 2", 1, connection2.getNumberCommits());
            assertEquals("should not call rollback on Connection 2", 0, connection2.getNumberRollbacks());

            // the SqlSession should have also commmited
            assertEquals("should call commit on SqlSession", 1, executorInterceptor.getCommitCount());
            assertEquals("should call rollback on SqlSession", 0, executorInterceptor.getRollbackCount());

            assertConnectionClosed(connection1);
            assertConnectionClosed(connection2);
        } finally {
            // reset the txManager; keep other tests from potentially failing
            txManager.setDataSource(dataSource);

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
            txManager.setDataSource(ds);
            TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());

            session = SqlSessionUtils.getSqlSession(sqlSessionFactory, ds);

            // start a new tx while the other is in progress
            DefaultTransactionDefinition txRequiresNew = new DefaultTransactionDefinition();
            txRequiresNew.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
            TransactionStatus status2 = txManager.getTransaction(txRequiresNew);

            SqlSession session2 = SqlSessionUtils.getSqlSession(sqlSessionFactory, ds);

            assertNotSame("getSqlSession() should not return suspended SqlSession", session, session2);

            SqlSessionUtils.closeSqlSession(session2, sqlSessionFactory);
            txManager.commit(status2);

            // first tx should be resumed now and this should succeed
            session.getMapper(TestMapper.class).findTest();
            SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);
            txManager.commit(status);

            // two transactions should have completed, each using their own Connection
            assertEquals("should call DataSource.getConnection() twice", 2, ds.getConnectionCount());

            // both connections and should be commited
            assertEquals("should call commit on Connection 1", 1, connection1.getNumberCommits());
            assertEquals("should not call rollback on Connection 1", 0, connection1.getNumberRollbacks());

            assertEquals("should call commit on Connection 2", 1, connection2.getNumberCommits());
            assertEquals("should not call rollback on Connection 2", 0, connection2.getNumberRollbacks());

            // the SqlSession should have also commmited twice
            assertEquals("should call commit on SqlSession", 2, executorInterceptor.getCommitCount());
            assertEquals("should call rollback on SqlSession", 0, executorInterceptor.getRollbackCount());

            assertConnectionClosed(connection1);
            assertConnectionClosed(connection2);
        } finally {
            // reset the txManager; keep other tests from potentially failing
            txManager.setDataSource(dataSource);

            // null the connection since it was not used
            // this avoids failing in validateConnectionClosed()
            connection = null;
        }
    }
}
