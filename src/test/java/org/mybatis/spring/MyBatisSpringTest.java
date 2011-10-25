/*
 *    Copyright 2010-2011 The myBatis Team
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

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.After;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.mockrunner.mock.jdbc.MockDataSource;
import com.mockrunner.mock.jdbc.MockPreparedStatement;

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
        assertExecuteCount(1);
    }

    @Test
    public void testMyBatisAPIWithCommit() {
        session = sqlSessionFactory.openSession();
        session.getMapper(TestMapper.class).findTest();
        session.commit(true);
        session.close();

        assertCommit();
        assertSingleConnection();
        assertExecuteCount(1);
    }

    @Test
    public void testMyBatisAPIWithRollback() {
        session = sqlSessionFactory.openSession();
        session.getMapper(TestMapper.class).findTest();
        session.rollback(true);
        session.close();

        assertRollback();
        assertSingleConnection();
        assertExecuteCount(1);
    }

    // basic tests using SqlSessionUtils instead of using the MyBatis API directly
    @Test
    public void testSpringAPI() {
        session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
        session.getMapper(TestMapper.class).findTest();
        SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

        assertNoCommit();
        assertSingleConnection();
        assertExecuteCount(1);
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
     * this is an edge case - completely separate DataSource, non-Spring TXManager but with an
     * existing Spring TX. Technically, this could be allowed, but the current implementation fails
     * because DataSourceUtils.getConnection(DataSource) pulls _any_ Connection into the current tx.
     * To fix, however, SqlSessionTemplate.execute() would need to run more checks
     * (DataSourceUtils.isConnectionTransactional() and unwrapping TransactionAwareDataSourceProxy)
     * which would increase the code path for all transactions.
     */
    @Test(expected = TransientDataAccessResourceException.class)
    public void testNonSpringTxFactoryNonSpringDSWithTx() {
        Environment original = sqlSessionFactory.getConfiguration().getEnvironment();

        MockDataSource mockDataSource = new MockDataSource();
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

    @Test(expected = TransientDataAccessResourceException.class)
    public void testChangeExecutorTypeInTx() throws Exception {
        TransactionStatus status = null;

        try {
            status = txManager.getTransaction(new DefaultTransactionDefinition());

            session = SqlSessionUtils.getSqlSession(sqlSessionFactory);

            session = SqlSessionUtils.getSqlSession(sqlSessionFactory, ExecutorType.BATCH, exceptionTranslator);

            fail("should not be able to change the Executor type during an existing transaction");
        } finally {
            SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

            // rollback required to close connection
            txManager.rollback(status);
        }
    }

    @Test
    public void testChangeExecutorTypeInTxRequiresNew() throws Exception {

        try {
            txManager.setDataSource(dataSource);
            TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());

            session = SqlSessionUtils.getSqlSession(sqlSessionFactory);

            // start a new tx while the other is in progress
            DefaultTransactionDefinition txRequiresNew = new DefaultTransactionDefinition();
            txRequiresNew.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
            TransactionStatus status2 = txManager.getTransaction(txRequiresNew);

            SqlSession session2 = SqlSessionUtils.getSqlSession(sqlSessionFactory, ExecutorType.BATCH, exceptionTranslator);

            SqlSessionUtils.closeSqlSession(session2, sqlSessionFactory);
            txManager.rollback(status2);

            SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);
            txManager.rollback(status);

        } finally {
            // reset the txManager; keep other tests from potentially failing
            txManager.setDataSource(dataSource);

            // null the connection since it was not used
            // this avoids failing in validateConnectionClosed()
            connection = null;
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

        // Connection should be committed once, but we explicitly called commit on the SqlSession, so
        // it should be committed twice
        assertEquals("should call commit on Connection", 1, connection.getNumberCommits());
        assertEquals("should not call rollback on Connection", 0, connection.getNumberRollbacks());
        assertEquals("should call commit on SqlSession", 2, executorInterceptor.getCommitCount());
        assertEquals("should not call rollback on SqlSession", 0, executorInterceptor.getRollbackCount());

        assertSingleConnection();
    }

    @Test
    public void testWithOtherTx() throws Exception {

        session = SqlSessionUtils.getSqlSession(sqlSessionFactory);

        try {
            // this transaction should use another Connection
            txManager.setDataSource(dataSource);
            TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());

            // all MyBatis work happens during the tx, but should not be participating
            session.getMapper(TestMapper.class).findTest();
            session.commit(true);
            SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

            // this should succeed
            // SpringManagedTransaction (from SqlSession.commit()) should not interfere with tx
            txManager.commit(status);

            // two transactions should have completed, each using their own Connection
            assertEquals("should call DataSource.getConnection() twice", 2, dataSource.getConnectionCount());

            // both connections and should be committed
            assertEquals("should call commit on Connection 1", 1, connection.getNumberCommits());
            assertEquals("should not call rollback on Connection 1", 0, connection.getNumberRollbacks());

            assertEquals("should call commit on Connection 2", 1, connectionTwo.getNumberCommits());
            assertEquals("should not call rollback on Connection 2", 0, connectionTwo.getNumberRollbacks());

            // the SqlSession should have also committed
            assertEquals("should call commit on SqlSession", 1, executorInterceptor.getCommitCount());
            assertEquals("should call rollback on SqlSession", 0, executorInterceptor.getRollbackCount());

            assertConnectionClosed(connection);
            assertConnectionClosed(connectionTwo);
        } finally {
            // reset the txManager; keep other tests from potentially failing
            txManager.setDataSource(dataSource);

            // null the connection since it was not used
            // this avoids failing in validateConnectionClosed()
            connection = null;
        }
    }

    @Test
    public void testSuspendAndResume() throws Exception {

        try {
            txManager.setDataSource(dataSource);
            TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());

            session = SqlSessionUtils.getSqlSession(sqlSessionFactory);

            // start a new tx while the other is in progress
            DefaultTransactionDefinition txRequiresNew = new DefaultTransactionDefinition();
            txRequiresNew.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
            TransactionStatus status2 = txManager.getTransaction(txRequiresNew);

            SqlSession session2 = SqlSessionUtils.getSqlSession(sqlSessionFactory);

            assertNotSame("getSqlSession() should not return suspended SqlSession", session, session2);

            SqlSessionUtils.closeSqlSession(session2, sqlSessionFactory);
            txManager.commit(status2);

            // first tx should be resumed now and this should succeed
            session.getMapper(TestMapper.class).findTest();
            SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);
            txManager.commit(status);

            // two transactions should have completed, each using their own Connection
            assertEquals("should call DataSource.getConnection() twice", 2, dataSource.getConnectionCount());

            // both connections and should be committed
            assertEquals("should call commit on Connection 1", 1, connection.getNumberCommits());
            assertEquals("should not call rollback on Connection 1", 0, connection.getNumberRollbacks());

            assertEquals("should call commit on Connection 2", 1, connectionTwo.getNumberCommits());
            assertEquals("should not call rollback on Connection 2", 0, connectionTwo.getNumberRollbacks());

            // the SqlSession should have also committed twice
            assertEquals("should call commit on SqlSession", 2, executorInterceptor.getCommitCount());
            assertEquals("should call rollback on SqlSession", 0, executorInterceptor.getRollbackCount());

            assertConnectionClosed(connection);
            assertConnectionClosed(connectionTwo);
        } finally {
            // reset the txManager; keep other tests from potentially failing
            txManager.setDataSource(dataSource);

            // null the connection since it was not used
            // this avoids failing in validateConnectionClosed()
            connection = null;
        }
    }

    @Test
    public void testBatch() {
        setupBatchStatements();

        session = SqlSessionUtils.getSqlSession(sqlSessionFactory, ExecutorType.BATCH, exceptionTranslator);

        session.getMapper(TestMapper.class).insertTest("test1");
        session.getMapper(TestMapper.class).insertTest("test2");
        session.getMapper(TestMapper.class).insertTest("test3");

        // nothing should execute until commit
        assertExecuteCount(0);

        session.commit(true);
        SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

        assertCommit();
        assertSingleConnection();
        assertExecuteCount(3);
    }

    @Test
    public void testBatchInTx() {
        setupBatchStatements();

        DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
        txDef.setPropagationBehaviorName("PROPAGATION_REQUIRED");

        TransactionStatus status = txManager.getTransaction(txDef);

        session = SqlSessionUtils.getSqlSession(sqlSessionFactory, ExecutorType.BATCH, exceptionTranslator);

        session.getMapper(TestMapper.class).insertTest("test1");
        session.getMapper(TestMapper.class).insertTest("test2");
        session.getMapper(TestMapper.class).insertTest("test3");

        SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

        txManager.commit(status);

        assertCommit();
        assertSingleConnection();
        assertExecuteCount(3);
    }

    @Test(expected = PersistenceException.class)
    public void testBatchWithError() {
        try {
            setupBatchStatements();

            session = SqlSessionUtils.getSqlSession(sqlSessionFactory, ExecutorType.BATCH, exceptionTranslator);

            session.getMapper(TestMapper.class).insertTest("test1");
            session.getMapper(TestMapper.class).insertTest("test2");
            session.update("org.mybatis.spring.TestMapper.insertFail");
            session.getMapper(TestMapper.class).insertTest("test3");

            session.commit(true);
            SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

            assertCommit();
            assertSingleConnection();
            assertExecuteCount(4);
        } finally {
            SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

        }
    }

    @Test(expected = DataAccessException.class)
    public void testBatchInTxWithError() {
        setupBatchStatements();

        DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
        txDef.setPropagationBehaviorName("PROPAGATION_REQUIRED");

        TransactionStatus status = txManager.getTransaction(txDef);

        session = SqlSessionUtils.getSqlSession(sqlSessionFactory, ExecutorType.BATCH, exceptionTranslator);

        session.getMapper(TestMapper.class).insertTest("test1");
        session.getMapper(TestMapper.class).insertTest("test2");
        session.update("org.mybatis.spring.TestMapper.insertFail");
        session.getMapper(TestMapper.class).insertTest("test3");

        SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

        txManager.commit(status);
    }

    private void setupBatchStatements() {
        // these queries must be the same as the query in TestMapper.xml
        connection.getPreparedStatementResultSetHandler().addPreparedStatement(
                new MockPreparedStatement(connection, "INSERT ? INTO test"));

        connection.getPreparedStatementResultSetHandler().prepareThrowsSQLException("INSERT fail");

    }
}
