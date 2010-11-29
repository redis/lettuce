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

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Handles MyBatis SqlSession life cycle. It can register and get SqlSessions from 
 * Spring {@link TransactionSynchronizationManager}. Also works if no transaction is active.
 *
 * @version $Id$
 * @see DataSourceUtils
 * @see TransactionSynchronizationManager
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
     * @param sessionFactory a MyBatis {@literal SqlSessionFactory} to create new sessions
     * @return a MyBatis {@literal SqlSession}
     * @throws TransientDataAccessResourceException if a transaction is active and the
     *             {@literal SqlSessionFactory} is not using a {@literal SpringManagedTransactionFactory}
     */
    public static SqlSession getSqlSession(SqlSessionFactory sessionFactory) {
        ExecutorType executorType = sessionFactory.getConfiguration().getDefaultExecutorType();
        return getSqlSession(sessionFactory, executorType, null);
    }

    /**
     * If a Spring transaction is active it uses {@link DataSourceUtils} to get a 
     * Spring managed {@link Connection}, then creates a new {@link SqlSession}
     * with this connection and synchronizes it with the transaction.
     * If there is not an active transaction it gets a connection directly from 
     * the {@link DataSource} and creates a {@link SqlSession} with it. 
     *
     * @param sessionFactory a MyBatis {@literal SqlSessionFactory} to create new sessions
     * @param executorType The executor type of the SqlSession to create
     * @param exceptionTranslator Optional. Translates SqlSession.commit() exceptions to Spring exceptions.
     * @throws TransientDataAccessResourceException if a transaction is active and the
     *             {@link SqlSessionFactory} is not using a {@link SpringManagedTransactionFactory}
     * @see org.mybatis.spring.transaction.SpringManagedTransactionFactory
     */
    public static SqlSession getSqlSession(
            SqlSessionFactory sessionFactory, 
            ExecutorType executorType, 
            PersistenceExceptionTranslator exceptionTranslator) {
        
        Assert.notNull(sessionFactory, "No SqlSessionFactory specified");
        Assert.notNull(executorType, "No ExecutorType specified");

        SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);

        if (holder != null && holder.isSynchronizedWithTransaction()) {
            if (holder.getExecutorType() != executorType) {
                throw new TransientDataAccessResourceException(
                        "Cannot change the ExecutorType when there is an existing transaction");
            }

            holder.requested();
            
            if (logger.isDebugEnabled()) {
                logger.debug("Fetched SqlSession [" + holder.getSqlSession() + "] from current transaction");
            }

            return holder.getSqlSession();
        }

        DataSource dataSource = sessionFactory.getConfiguration().getEnvironment().getDataSource();
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
                logger.debug("Registering transaction synchronization for SqlSession [" + session + "]");
            }
            holder = new SqlSessionHolder(session, executorType, exceptionTranslator);
            TransactionSynchronizationManager.bindResource(sessionFactory, holder);
            TransactionSynchronizationManager.registerSynchronization(new SqlSessionSynchronization(holder, sessionFactory));
            holder.setSynchronizedWithTransaction(true);
            holder.requested();
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("SqlSession [" + session + "] was not registered for synchronization because synchronization is not active");
            }            
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
        closeSqlSession(session, sessionFactory, null);
    }
    
    /**
     * Checks if {@link SqlSession} passed as an argument is managed by Spring {@link TransactionSynchronizationManager}
     * If it is not, it closes it, otherwise it just updates the reference counter and 
     * lets Spring call the close callback when the managed transaction ends
     *
     * @param session
     * @param sessionFactory
     * @param exceptionTranslator
     */
    public static void closeSqlSession(SqlSession session, 
            SqlSessionFactory sessionFactory,
            PersistenceExceptionTranslator exceptionTranslator) {
        
        Assert.notNull(session, "No SqlSession specified");
        Assert.notNull(sessionFactory, "No SqlSessionFactory specified");
        
        SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);

        if ((holder != null) && (holder.getSqlSession() == session)) {
            holder.released();
        } else {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Closing no transactional SqlSession [" + session + "]");
                }
                session.close();
            } catch (PersistenceException p) {
                if (exceptionTranslator != null) {
                    throw exceptionTranslator.translateExceptionIfPossible(p);
                }
                throw p;
            }
        }
    }

    /**
     * Returns if the {@link SqlSession} passed as an argument is being managed by Spring
     *
     * @param session a MyBatis SqlSession to check
     * @param sessionFactory the SqlSessionFactory which the SqlSession was built with
     * @return true if session is transactional, otherwise false
     */
    public static boolean isSqlSessionTransactional(SqlSession session, SqlSessionFactory sessionFactory) {
        Assert.notNull(session, "No SqlSession specified");
        Assert.notNull(sessionFactory, "No SqlSessionFactory specified");

        SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);

        return (holder != null) && (holder.getSqlSession() == session);
    }

    /**
     * Callback for cleaning up resouces. It cleans TransactionSynchronizationManager and
     * also commits and closes the {@link SqlSession}.
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
            // order right before any Connection synchronization
            return DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 1;
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
        public void beforeCommit(boolean readOnly) {
            // Connection commit or rollback will be handled by ConnectionSynchronization or
            // DataSourceTransactionManager.
            // But, do cleanup the SqlSession / Executor, including flushing BATCH statements so
            // they are actually executed.
            // SpringManagedTransaction will no-op the commit over the jdbc connection
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Transaction synchronization committing SqlSession [" + this.holder.getSqlSession() + "]");
                    }
                    this.holder.getSqlSession().commit(false);
                } catch (PersistenceException p) {
                    if (this.holder.getPersistenceExceptionTranslator() != null) {
                        throw this.holder.getPersistenceExceptionTranslator().translateExceptionIfPossible(p);
                    }
                    throw p;
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void afterCompletion(int status) {
            // unbind the SqlSession from tx synchronization
            // Note, assuming DefaultSqlSession, rollback is not needed because rollback on
            // SpringManagedTransaction will no-op anyway. In addition, closing the session cleans
            // up the same internal resources as rollback.
            if (!this.holder.isOpen()) {
                TransactionSynchronizationManager.unbindResource(this.sessionFactory);
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Transaction synchronization closing SqlSession [" + this.holder.getSqlSession() + "]");
                    }
                    this.holder.getSqlSession().close();
                } finally {
                    this.holder.reset();
                }
            }
        }
    }

}
