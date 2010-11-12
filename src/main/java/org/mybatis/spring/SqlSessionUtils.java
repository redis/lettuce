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

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Note: This class does not translate PersistenceException to DataSourceException
 * since MyBatis now uses runtime exceptions.
 *
 * @version $Id$
 */
public final class SqlSessionUtils {

    private static final Log logger = LogFactory.getLog(SqlSessionUtils.class);

    /**
     * This class can't be instantiated, exposes static utility methods only.
     */
    private SqlSessionUtils() {
        // do nothing
    }

    /**
     * Creates a new MyBatis {@link SqlSession} from the {@link SqlSessionFactory}
     * provided as a parameter and using its {@link DataSource} and {@link ExecutorType}
     *
     * @param sessionFactory
     * @return
     */
    public static SqlSession getSqlSession(SqlSessionFactory sessionFactory) {
        DataSource dataSource = sessionFactory.getConfiguration().getEnvironment().getDataSource();
        ExecutorType executorType = sessionFactory.getConfiguration().getDefaultExecutorType();
        return getSqlSession(sessionFactory, dataSource, executorType);
    }

    /**
     * Creates a new MyBatis {@link SqlSession} with a different {@link DataSource} than
     * the one holden into {@link SqlSessionFactory}. 
     * It is just for testing purposes. Using this can cause runtime errors.
     *
     * @param sessionFactory
     * @param dataSource
     * @return
     */
    public static SqlSession getSqlSession(SqlSessionFactory sessionFactory, DataSource dataSource) {
        ExecutorType executorType = sessionFactory.getConfiguration().getDefaultExecutorType();
        return getSqlSession(sessionFactory, dataSource, executorType);
    }

    /**
     * Creates a new MyBatis {@link SqlSession} from the provided {@link SqlSessionFactory}
     * using the {@link ExecutorType} provided as an argument and the {@link DataSource}
     * configured in the {@link SqlSessionFactory}
     *
     * @param sessionFactory
     * @param executorType
     * @return
     */
    public static SqlSession getSqlSession(SqlSessionFactory sessionFactory, ExecutorType executorType) {
        DataSource dataSource = sessionFactory.getConfiguration().getEnvironment().getDataSource();
        return getSqlSession(sessionFactory, dataSource, executorType);
    }

    /**
     * If a Spring transaction is active it uses {@link DataSourceUtils} to get a 
     * Spring managed {@link Connection}, then creates a new {@link SqlSession}
     * with this connection and synchronizes it with the transaction.
     * If there is not an active transaction it gets a connection directly from 
     * the {@link DataSource} and creates a {@link SqlSession} with it. 
     *
     * @throws TransientDataAccessResourceException if a transaction is active and the
     *             SqlSessionFactory is not using a SpringManagedTransactionFactory
     * @see org.mybatis.spring.transaction.SpringManagedTransactionFactory
     */
    public static SqlSession getSqlSession(SqlSessionFactory sessionFactory, DataSource dataSource, ExecutorType executorType) {
        // either return the existing SqlSession or create a new one
        SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);

        if (holder != null && holder.isSynchronizedWithTransaction()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Fetching SqlSession from current transaction");
            }

            if (!holder.getExecutorType().equals(executorType)) {
                throw new TransientDataAccessResourceException(
                        "cannot change the ExecutorType when there is an existing transaction");
            }

            holder.requested();
            return holder.getSqlSession();
        }

        boolean transactionAware = (dataSource instanceof TransactionAwareDataSourceProxy);
        Connection conn;

        try {
            conn = transactionAware ? dataSource.getConnection() : DataSourceUtils.getConnection(dataSource);
        } catch (SQLException e) {
            throw new CannotGetJdbcConnectionException("Could not get JDBC Connection for SqlSession", e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Creating SqlSession from SqlSessionFactory");
        }

        // Assume either DataSourceTransactionManager or the underlying
        // connection pool already dealt with enabling auto commit.
        // This may not be a good assumption, but the overhead of checking
        // connection.getAutoCommit() again may be expensive (?) in some drivers
        // (see DataSourceTransactionManager.doBegin()). One option would be to
        // only check for auto commit if this function is being called outside
        // of DSTxMgr, but to do that we would need to be able to call
        // ConnectionHolder.isTransactionActive(), which is protected and not
        // visible to this class.
        SqlSession session = sessionFactory.openSession(executorType, conn);

        // Register session holder and bind it to enable synchronization.
        //
        // Note: The DataSource should be synchronized with the transaction
        // either through DataSourceTxMgr or another tx synchronization.
        // Further assume that if an exception is thrown, whatever started the transaction will
        // handle closing / rolling back the Connection associated with the SqlSession.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            if (!(sessionFactory.getConfiguration().getEnvironment().getTransactionFactory() instanceof SpringManagedTransactionFactory)
                    && DataSourceUtils.isConnectionTransactional(conn, dataSource)) {
                throw new TransientDataAccessResourceException(
                        "SqlSessionFactory must be using a SpringManagedTransactionFactory in order to use Spring transaction synchronization");
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Registering transaction synchronization for SqlSession");
            }
            holder = new SqlSessionHolder(session, executorType);
            TransactionSynchronizationManager.bindResource(sessionFactory, holder);
            TransactionSynchronizationManager.registerSynchronization(new SqlSessionSynchronization(holder, sessionFactory));
            holder.setSynchronizedWithTransaction(true);
            holder.requested();
        }

        return session;
    }

    /**
     * Checks if {@link SqlSession} passed as an argument is managed by Spring {@link TransactionSynchronizationManager}
     * If it is not, it closes it, otherwise it just updates the reference counter and 
     * lets Spring call the close callback when the managed transaction ends
     *
     * @param session
     * @param sessionFactory
     */
    public static void closeSqlSession(SqlSession session, SqlSessionFactory sessionFactory) {
        SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);

        if ((holder == null) || (session != holder.getSqlSession())) {
            if (session != null) {
                session.close();
            }
        } else {
            holder.released();
            // Assume transaction synchronization will actually close session.
        }
    }

    /**
     * Returns if the {@link SqlSession} passed as an argument is being managed by Spring
     *
     * @param session
     * @param sessionFactory
     * @return
     */
    public static boolean isSqlSessionTransactional(SqlSession session, SqlSessionFactory sessionFactory) {
        if (sessionFactory == null) {
            return false;
        }

        SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);

        return (holder != null) && (holder.getSqlSession() == session);
    }

    /**
     * Callback for cleaning up resouces. It cleans TransactionSynchronizationManager and
     * also commits, rollbacks and closes the {@link SqlSession}
     * It assumes that {@link Connection} life cycle will be managed by 
     * {@link DataSourceTransactionManager} or {@link JtaTransactionManager} 
     */
    private static final class SqlSessionSynchronization extends TransactionSynchronizationAdapter {

        private final SqlSessionHolder holder;

        private final SqlSessionFactory sessionFactory;

        public SqlSessionSynchronization(SqlSessionHolder holder, SqlSessionFactory sessionFactory) {
            Assert.notNull(holder, "Parameter 'holder' must be not null");
            Assert.notNull(sessionFactory, "Parameter 'sessionFactory' must be not null");

            this.holder = holder;
            this.sessionFactory = sessionFactory;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getOrder() {
            // order right after any Connection synchronization
            return DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER + 1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void suspend() {
            TransactionSynchronizationManager.unbindResource(this.sessionFactory);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void resume() {
            TransactionSynchronizationManager.bindResource(this.sessionFactory, this.holder);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void afterCompletion(int status) {
            // Connection commit or rollback will be handled by ConnectionSynchronization or
            // DataSourceTransactionManager.
            try {
                // Do not call commit unless there is really a transaction; no need to commit if
                // just tx synchronization is active.
                if ((STATUS_COMMITTED == status)) {
                    if (TransactionSynchronizationManager.isActualTransactionActive()) {
                        // False here on commit or rollback prevents a call to Transaction.commit()
                        // in BaseExecutor which will be redundant with SpringManagedTransaction
                        // since we already know that commit on the Connection is being handled.
                        this.holder.getSqlSession().commit(false);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Transaction synchronization committed SqlSession");
                        }
                    }
                } else {
                    this.holder.getSqlSession().rollback(false);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Transaction synchronization rolled back SqlSession");
                    }
                }
            } finally {
                if (!this.holder.isOpen()) {
                    TransactionSynchronizationManager.unbindResource(this.sessionFactory);
                    this.holder.getSqlSession().close();
                    this.holder.reset();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Transaction synchronization closed SqlSession");
                    }
                }
            }
        }
    }

}
