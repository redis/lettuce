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

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.apache.ibatis.session.ExecutorType;

// tests basic usage and implementation only
// MapperFactoryBeanTest handles testing the transactional functions in SqlSessionTemplate
public final class SqlSessionTemplateTest extends AbstractMyBatisSpringTest {

    private static SqlSessionTemplate sqlSessionTemplate;

    @BeforeClass
    public static void setupSqlTemplate() {
        sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
    }

    @Test
    public void testGetConnection() throws java.sql.SQLException {
        java.sql.Connection con = sqlSessionTemplate.getConnection();

        assertTrue(con.isClosed());
    }

    @Test
    public void testGetConnectionInTx() throws java.sql.SQLException {
        TransactionStatus status = null;

        try {
            status = txManager.getTransaction(new DefaultTransactionDefinition());

            java.sql.Connection con = sqlSessionTemplate.getConnection();

            assertFalse(con.isClosed());

        } finally {
            // rollback required to close connection
            txManager.rollback(status);
        }
    }

    // commit should be a no-op
    @Test
    public void testCommit() throws SQLException {
        sqlSessionTemplate.commit();
        assertNoCommit();

        sqlSessionTemplate.commit(true);
        assertNoCommit();

        sqlSessionTemplate.commit(false);
        assertNoCommit();

        connection.close();
    }

    // close should be a no-op
    @Test
    public void testClose() throws SQLException {
        sqlSessionTemplate.close();
        assertFalse(connection.isClosed());

        connection.close();
    }

    // rollback should be a no-op
    @Test
    public void testRollback() throws SQLException {
        sqlSessionTemplate.rollback();
        assertNoRollback();

        sqlSessionTemplate.rollback(true);
        assertNoRollback();

        sqlSessionTemplate.rollback(false);
        assertNoRollback();

        connection.close();
    }

    @Test
    public void testExecutorType() {
        SqlSessionTemplate template = new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
        assertEquals(ExecutorType.BATCH, template.getExecutorType());

        DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource);

        TransactionStatus status = null;

        try {
            status = manager.getTransaction(new DefaultTransactionDefinition());

            // will synchronize the template with the current tx
            template.getConnection();

            SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager
                    .getResource(sqlSessionFactory);

            assertEquals(ExecutorType.BATCH, holder.getExecutorType());
        } finally {
            // rollback required to close connection
            txManager.rollback(status);
        }
    }

    @Test
    public void testExceptionTranslation() {
        try {
            sqlSessionTemplate.selectOne("undefined");
            fail("exception not thrown when expected");
        }
       catch (MyBatisSystemException mbse) {
           // success
       }
       catch (Throwable t) {
           fail("SqlSessionTemplate should translate MyBatis PersistenceExceptions");
       }

       // this query must be the same as the query in TestMapper.xml
       connection.getPreparedStatementResultSetHandler().prepareThrowsSQLException("SELECT 'fail'");

       try {
           sqlSessionTemplate.selectOne("org.mybatis.spring.TestMapper.findFail");
           fail("exception not thrown when expected");
       }
       catch (MyBatisSystemException mbse) {
           fail("SqlSessionTemplate should translate SQLExceptions into DataAccessExceptions");
       }
       catch (DataAccessException dae) {
           // success
       }
       catch (Throwable t) {
           fail("SqlSessionTemplate should translate MyBatis PersistenceExceptions");
       }
    }

    private void assertNoRollback() {
        assertEquals("should not call commit on Connection", 0, connection.getNumberCommits());
        assertEquals("should call rollback on Connection", 0, connection.getNumberRollbacks());
        assertEquals("should not call commit on SqlSession", 0, executorInterceptor.getCommitCount());
        assertEquals("should call rollback on SqlSession", 0, executorInterceptor.getRollbackCount());
    }

}
