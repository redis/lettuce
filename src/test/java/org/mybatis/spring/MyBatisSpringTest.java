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
package org.mybatis.spring;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.mockrunner.mock.ejb.MockUserTransaction;
import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockDataSource;
import com.mockrunner.mock.jdbc.MockPreparedStatement;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

class MyBatisSpringTest extends AbstractMyBatisSpringTest {

  private SqlSession session;

  @AfterEach
  void validateSessionClose() {
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
  void testMyBatisAPI() {
    session = sqlSessionFactory.openSession();
    session.getMapper(TestMapper.class).findTest();
    session.close();

    assertNoCommit();
    assertSingleConnection();
    assertExecuteCount(1);
  }

  @Test
  void testMyBatisAPIWithCommit() {
    session = sqlSessionFactory.openSession();
    session.getMapper(TestMapper.class).findTest();
    session.commit(true);
    session.close();

    assertCommit();
    assertSingleConnection();
    assertExecuteCount(1);
  }

  @Test
  void testMyBatisAPIWithRollback() {
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
  void testSpringAPI() {
    session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
    session.getMapper(TestMapper.class).findTest();
    SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

    assertNoCommit();
    assertSingleConnection();
    assertExecuteCount(1);
  }

  @Test
  void testSpringAPIWithCommit() {
    session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
    session.getMapper(TestMapper.class).findTest();
    session.commit(true);
    SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

    assertCommit();
    assertSingleConnection();
  }

  @Test
  void testSpringAPIWithRollback() {
    session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
    session.getMapper(TestMapper.class).findTest();
    session.rollback(true);
    SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

    assertRollback();
    assertSingleConnection();
  }

  @Test
  void testSpringAPIWithMyBatisClose() {
    // This is a programming error and could lead to connection leak if there is a transaction
    // in progress. But, the API allows it, so make sure it at least works without a tx.
    session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
    session.getMapper(TestMapper.class).findTest();
    session.close();

    assertNoCommit();
    assertSingleConnection();
  }

  // Spring API should work with a MyBatis TransactionFactories
  @Test
  void testWithNonSpringTransactionFactory() {
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

  // Spring TX, non-Spring TransactionFactory, Spring managed DataSource
  // this should not work since the DS will be out of sync with MyBatis
  @Test
  void testNonSpringTxFactoryWithTx() throws Exception {
    Environment original = sqlSessionFactory.getConfiguration().getEnvironment();
    Environment nonSpring = new Environment("non-spring", new JdbcTransactionFactory(), dataSource);
    sqlSessionFactory.getConfiguration().setEnvironment(nonSpring);

    TransactionStatus status = null;

    try {
      status = txManager.getTransaction(new DefaultTransactionDefinition());

      assertThrows(TransientDataAccessResourceException.class,
          () -> session = SqlSessionUtils.getSqlSession(sqlSessionFactory));
      // fail("should not be able to get an SqlSession using non-Spring tx manager when there is an active Spring tx");
    } finally {
      // rollback required to close connection
      txManager.rollback(status);

      sqlSessionFactory.getConfiguration().setEnvironment(original);
    }
  }

  // Spring TX, non-Spring TransactionFactory, MyBatis managed DataSource
  // this should work since the DS is managed MyBatis
  @Test
  void testNonSpringTxFactoryNonSpringDSWithTx() throws java.sql.SQLException {
    Environment original = sqlSessionFactory.getConfiguration().getEnvironment();

    MockDataSource mockDataSource = new MockDataSource();
    mockDataSource.setupConnection(createMockConnection());

    Environment nonSpring = new Environment("non-spring", new JdbcTransactionFactory(), mockDataSource);
    sqlSessionFactory.getConfiguration().setEnvironment(nonSpring);

    TransactionStatus status;

    try {
      status = txManager.getTransaction(new DefaultTransactionDefinition());

      session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
      session.commit();
      session.close();

      txManager.commit(status);

      // txManager still uses original connection
      assertCommit();
      assertSingleConnection();

      // SqlSession uses its own connection
      // that connection will not have committed since no SQL was executed by the session
      MockConnection mockConnection = (MockConnection) mockDataSource.getConnection();
      assertThat(mockConnection.getNumberCommits()).as("should call commit on Connection").isEqualTo(0);
      assertThat(mockConnection.getNumberRollbacks()).as("should not call rollback on Connection").isEqualTo(0);
      assertCommitSession();
    } finally {
      SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

      sqlSessionFactory.getConfiguration().setEnvironment(original);
    }
  }

  @Test
  void testChangeExecutorTypeInTx() throws Exception {
    TransactionStatus status = null;

    try {
      status = txManager.getTransaction(new DefaultTransactionDefinition());

      session = SqlSessionUtils.getSqlSession(sqlSessionFactory);

      assertThrows(TransientDataAccessResourceException.class,
          () -> session = SqlSessionUtils.getSqlSession(sqlSessionFactory, ExecutorType.BATCH, exceptionTranslator));

      // fail("should not be able to change the Executor type during an existing transaction");
    } finally {
      SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

      // rollback required to close connection
      txManager.rollback(status);
    }
  }

  @Test
  void testChangeExecutorTypeInTxRequiresNew() throws Exception {

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
  void testWithJtaTxManager() {
    JtaTransactionManager jtaManager = new JtaTransactionManager(new MockUserTransaction());

    DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
    txDef.setPropagationBehaviorName("PROPAGATION_REQUIRED");

    TransactionStatus status = jtaManager.getTransaction(txDef);

    session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
    session.getMapper(TestMapper.class).findTest();
    SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

    jtaManager.commit(status);

    // assume a real JTA tx would enlist and commit the JDBC connection
    assertNoCommitJdbc();
    assertCommitSession();
    assertSingleConnection();
  }

  @Test
  void testWithJtaTxManagerAndNonSpringTxManager() throws java.sql.SQLException {
    Environment original = sqlSessionFactory.getConfiguration().getEnvironment();

    MockDataSource mockDataSource = new MockDataSource();
    mockDataSource.setupConnection(createMockConnection());

    Environment nonSpring = new Environment("non-spring", new ManagedTransactionFactory(), mockDataSource);
    sqlSessionFactory.getConfiguration().setEnvironment(nonSpring);

    JtaTransactionManager jtaManager = new JtaTransactionManager(new MockUserTransaction());

    DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
    txDef.setPropagationBehaviorName("PROPAGATION_REQUIRED");

    TransactionStatus status = jtaManager.getTransaction(txDef);

    try {
      session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
      session.getMapper(TestMapper.class).findTest();
      // Spring is not managing SqlSession, so commit is needed
      session.commit(true);
      SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

      jtaManager.commit(status);

      // assume a real JTA tx would enlist and commit the JDBC connection
      assertNoCommitJdbc();
      assertCommitSession();

      MockConnection mockConnection = (MockConnection) mockDataSource.getConnection();
      assertThat(mockConnection.getNumberCommits()).as("should call commit on Connection").isEqualTo(0);
      assertThat(mockConnection.getNumberRollbacks()).as("should not call rollback on Connection").isEqualTo(0);

      assertThat(dataSource.getConnectionCount()).as("should not call DataSource.getConnection()").isEqualTo(0);

    } finally {
      SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

      sqlSessionFactory.getConfiguration().setEnvironment(original);

      // null the connection since it was not used
      // this avoids failing in validateConnectionClosed()
      connection = null;
    }
  }

  @Test
  void testWithTxSupports() {
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
  void testRollbackWithTxSupports() {
    DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
    txDef.setPropagationBehaviorName("PROPAGATION_SUPPORTS");

    TransactionStatus status = txManager.getTransaction(txDef);

    session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
    session.getMapper(TestMapper.class).findTest();
    SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

    txManager.rollback(status);

    // SUPPORTS should just activate tx synchronization but not commits
    assertNoCommit();
    assertSingleConnection();
  }

  @Test
  void testWithTxRequired() {
    DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
    txDef.setPropagationBehaviorName("PROPAGATION_REQUIRED");

    TransactionStatus status = txManager.getTransaction(txDef);

    session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
    session.getMapper(TestMapper.class).findTest();
    SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

    txManager.commit(status);

    assertCommit();
    assertCommitSession();
    assertSingleConnection();
  }

  @Test
  void testSqlSessionCommitWithTx() {
    DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
    txDef.setPropagationBehaviorName("PROPAGATION_REQUIRED");

    TransactionStatus status = txManager.getTransaction(txDef);

    session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
    session.getMapper(TestMapper.class).findTest();
    // commit should no-op since there is an active transaction
    session.commit(true);
    SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

    txManager.commit(status);

    // Connection should be committed once, but we explicitly called commit on the SqlSession,
    // so it should be committed twice
    assertThat(connection.getNumberCommits()).as("should call commit on Connection").isEqualTo(1);
    assertThat(connection.getNumberRollbacks()).as("should not call rollback on Connection").isEqualTo(0);
    assertThat(executorInterceptor.getCommitCount()).as("should call commit on SqlSession").isEqualTo(2);
    assertThat(executorInterceptor.getRollbackCount()).as("should not call rollback on SqlSession").isEqualTo(0);

    assertSingleConnection();
  }

  @Test
  void testWithInterleavedTx() {
    // this session will use one Connection
    session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
    session.getMapper(TestMapper.class).findTest();

    // this transaction should use another Connection
    TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());

    // session continues using original connection
    session.getMapper(TestMapper.class).insertTest("test2");
    session.commit(true);
    SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

    // this should succeed
    // SpringManagedTransaction (from SqlSession.commit()) should not interfere with tx
    txManager.commit(status);

    // two transactions should have completed, each using their own Connection
    assertThat(dataSource.getConnectionCount()).as("should call DataSource.getConnection() twice").isEqualTo(2);

    // both connections should be committed
    assertThat(connection.getNumberCommits()).as("should call commit on Connection 1").isEqualTo(1);
    assertThat(connection.getNumberRollbacks()).as("should not call rollback on Connection 1").isEqualTo(0);

    assertThat(connectionTwo.getNumberCommits()).as("should call commit on Connection 2").isEqualTo(1);
    assertThat(connectionTwo.getNumberRollbacks()).as("should not call rollback on Connection 2").isEqualTo(0);

    // the SqlSession should have also committed and executed twice
    assertCommitSession();
    assertExecuteCount(2);

    assertConnectionClosed(connection);
    assertConnectionClosed(connectionTwo);
  }

  @Test
  void testSuspendAndResume() {

    try {
      txManager.setDataSource(dataSource);
      TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());

      session = SqlSessionUtils.getSqlSession(sqlSessionFactory);

      // start a new tx while the other is in progress
      DefaultTransactionDefinition txRequiresNew = new DefaultTransactionDefinition();
      txRequiresNew.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
      TransactionStatus status2 = txManager.getTransaction(txRequiresNew);

      SqlSession session2 = SqlSessionUtils.getSqlSession(sqlSessionFactory);

      assertThat(session).as("getSqlSession() should not return suspended SqlSession").isNotSameAs(session2);

      SqlSessionUtils.closeSqlSession(session2, sqlSessionFactory);
      txManager.commit(status2);

      // first tx should be resumed now and this should succeed
      session.getMapper(TestMapper.class).findTest();
      SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);
      txManager.commit(status);

      // two transactions should have completed, each using their own Connection
      assertThat(dataSource.getConnectionCount()).as("should call DataSource.getConnection() twice").isEqualTo(2);

      // both connections and should be committed
      assertThat(connection.getNumberCommits()).as("should call commit on Connection 1").isEqualTo(1);
      assertThat(connection.getNumberRollbacks()).as("should not call rollback on Connection 1").isEqualTo(0);

      assertThat(connectionTwo.getNumberCommits()).as("should call commit on Connection 2").isEqualTo(1);
      assertThat(connectionTwo.getNumberRollbacks()).as("should not call rollback on Connection 2").isEqualTo(0);

      // the SqlSession should have also committed twice
      assertThat(executorInterceptor.getCommitCount()).as("should call commit on SqlSession").isEqualTo(2);
      assertThat(executorInterceptor.getRollbackCount()).as("should call rollback on SqlSession").isEqualTo(0);

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
  void testBatch() {
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
  void testBatchInTx() {
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

  @Test
  void testBatchWithError() {
    try {
      setupBatchStatements();

      session = SqlSessionUtils.getSqlSession(sqlSessionFactory, ExecutorType.BATCH, exceptionTranslator);

      session.getMapper(TestMapper.class).insertTest("test1");
      session.getMapper(TestMapper.class).insertTest("test2");
      session.update("org.mybatis.spring.TestMapper.insertFail");
      session.getMapper(TestMapper.class).insertTest("test3");

      assertThrows(PersistenceException.class, () -> session.commit(true));
    } finally {
      SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);
    }
  }

  @Test
  void testBatchInTxWithError() {
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

    assertThrows(DataAccessException.class, () -> txManager.commit(status));
  }

  private void setupBatchStatements() {
    // these queries must be the same as the query in TestMapper.xml
    connection.getPreparedStatementResultSetHandler()
        .addPreparedStatement(new MockPreparedStatement(connection, "INSERT ? INTO test"));

    connection.getPreparedStatementResultSetHandler().prepareThrowsSQLException("INSERT fail");
  }
}
