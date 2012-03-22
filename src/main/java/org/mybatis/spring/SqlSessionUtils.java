/*
 *    Copyright 2010-2012 The MyBatis Team
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

import static org.springframework.transaction.support.TransactionSynchronizationManager.bindResource;
import static org.springframework.transaction.support.TransactionSynchronizationManager.getResource;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive;
import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;
import static org.springframework.transaction.support.TransactionSynchronizationManager.unbindResource;
import static org.springframework.util.Assert.notNull;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

/**
 * Handles MyBatis SqlSession life cycle. It can register and get SqlSessions from
 * Spring {@code TransactionSynchronizationManager}. Also works if no transaction is active.
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
   * Creates a new MyBatis {@code SqlSession} from the {@code SqlSessionFactory}
   * provided as a parameter and using its {@code DataSource} and {@code ExecutorType}
   *
   * @param sessionFactory a MyBatis {@code SqlSessionFactory} to create new sessions
   * @return a MyBatis {@code SqlSession}
   * @throws TransientDataAccessResourceException if a transaction is active and the
   *             {@code SqlSessionFactory} is not using a {@code SpringManagedTransactionFactory}
   */
  public static SqlSession getSqlSession(SqlSessionFactory sessionFactory) {
    ExecutorType executorType = sessionFactory.getConfiguration().getDefaultExecutorType();
    return getSqlSession(sessionFactory, executorType, null);
  }

  /**
   * Gets an SqlSession from Spring Transaction Manager or creates a new one if needed.
   * Tries to get a SqlSession out of current transaction. If there is not any, it creates a new one.
   * Then, it synchronizes the SqlSession with the transaction if Spring TX is active and
   * <code>SpringManagedTransactionFactory</code> is configured as a transaction manager.
   *
   * @param sessionFactory a MyBatis {@code SqlSessionFactory} to create new sessions
   * @param executorType The executor type of the SqlSession to create
   * @param exceptionTranslator Optional. Translates SqlSession.commit() exceptions to Spring exceptions.
   * @throws TransientDataAccessResourceException if a transaction is active and the
   *             {@code SqlSessionFactory} is not using a {@code SpringManagedTransactionFactory}
   * @see SpringManagedTransactionFactory
   */
  public static SqlSession getSqlSession(SqlSessionFactory sessionFactory, ExecutorType executorType, PersistenceExceptionTranslator exceptionTranslator) {

    notNull(sessionFactory, "No SqlSessionFactory specified");
    notNull(executorType, "No ExecutorType specified");

    SqlSessionHolder holder = (SqlSessionHolder) getResource(sessionFactory);

    if (holder != null && holder.isSynchronizedWithTransaction()) {
      if (holder.getExecutorType() != executorType) {
        throw new TransientDataAccessResourceException("Cannot change the ExecutorType when there is an existing transaction");
      }

      holder.requested();

      if (logger.isDebugEnabled()) {
        logger.debug("Fetched SqlSession [" + holder.getSqlSession() + "] from current transaction");
      }

      return holder.getSqlSession();
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Creating a new SqlSession");
    }

    SqlSession session = sessionFactory.openSession(executorType);

    // Register session holder if synchronization is active (i.e. a Spring TX is active)
    //
    // Note: The DataSource used by the Environment should be synchronized with the
    // transaction either through DataSourceTxMgr or another tx synchronization.
    // Further assume that if an exception is thrown, whatever started the transaction will
    // handle closing / rolling back the Connection associated with the SqlSession.
    if (isSynchronizationActive()) {
      Environment environment = sessionFactory.getConfiguration().getEnvironment();

      if (environment.getTransactionFactory() instanceof SpringManagedTransactionFactory) {
        if (logger.isDebugEnabled()) {
          logger.debug("Registering transaction synchronization for SqlSession [" + session + "]");
        }

        holder = new SqlSessionHolder(session, executorType, exceptionTranslator);
        bindResource(sessionFactory, holder);
        registerSynchronization(new SqlSessionSynchronization(holder, sessionFactory));
        holder.setSynchronizedWithTransaction(true);
        holder.requested();
      } else {
        if (getResource(environment.getDataSource()) == null) {
          if (logger.isDebugEnabled()) {
            logger.debug("SqlSession [" + session + "] was not registered for synchronization because DataSource is not transactional");
          }
        } else {
          throw new TransientDataAccessResourceException(
              "SqlSessionFactory must be using a SpringManagedTransactionFactory in order to use Spring transaction synchronization");
        }
      }
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("SqlSession [" + session + "] was not registered for synchronization because synchronization is not active");
      }
    }

    return session;
  }

  /**
   * Checks if {@code SqlSession} passed as an argument is managed by Spring {@code TransactionSynchronizationManager}
   * If it is not, it closes it, otherwise it just updates the reference counter and
   * lets Spring call the close callback when the managed transaction ends
   *
   * @param session
   * @param sessionFactory
   */
  public static void closeSqlSession(SqlSession session, SqlSessionFactory sessionFactory) {

    notNull(session, "No SqlSession specified");
    notNull(sessionFactory, "No SqlSessionFactory specified");

    SqlSessionHolder holder = (SqlSessionHolder) getResource(sessionFactory);
    if ((holder != null) && (holder.getSqlSession() == session)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Releasing transactional SqlSession [" + session + "]");
      }
      holder.released();
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Closing non transactional SqlSession [" + session + "]");
      }
      session.close();
    }
  }

  /**
   * Returns if the {@code SqlSession} passed as an argument is being managed by Spring
   *
   * @param session a MyBatis SqlSession to check
   * @param sessionFactory the SqlSessionFactory which the SqlSession was built with
   * @return true if session is transactional, otherwise false
   */
  public static boolean isSqlSessionTransactional(SqlSession session, SqlSessionFactory sessionFactory) {
    notNull(session, "No SqlSession specified");
    notNull(sessionFactory, "No SqlSessionFactory specified");

    SqlSessionHolder holder = (SqlSessionHolder) getResource(sessionFactory);

    return (holder != null) && (holder.getSqlSession() == session);
  }

  /**
   * Callback for cleaning up resources. It cleans TransactionSynchronizationManager and
   * also commits and closes the {@code SqlSession}.
   * It assumes that {@code Connection} life cycle will be managed by
   * {@code DataSourceTransactionManager} or {@code JtaTransactionManager}
   */
  private static final class SqlSessionSynchronization extends TransactionSynchronizationAdapter {

    private final SqlSessionHolder holder;

    private final SqlSessionFactory sessionFactory;

    public SqlSessionSynchronization(SqlSessionHolder holder, SqlSessionFactory sessionFactory) {
      notNull(holder, "Parameter 'holder' must be not null");
      notNull(sessionFactory, "Parameter 'sessionFactory' must be not null");

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
      unbindResource(this.sessionFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resume() {
      bindResource(this.sessionFactory, this.holder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeCommit(boolean readOnly) {
      // Flush BATCH statements so they are actually executed before the connection is committed.
      // If there is no tx active data will be rolled back so there is no need to flush batches
      if (this.holder.getExecutorType() == ExecutorType.BATCH && isActualTransactionActive()) {
        try {
          if (logger.isDebugEnabled()) {
            logger.debug("Transaction synchronization flushing SqlSession [" + this.holder.getSqlSession() + "]");
          }
          this.holder.getSqlSession().flushStatements();
        } catch (PersistenceException p) {
          if (this.holder.getPersistenceExceptionTranslator() != null) {
            DataAccessException translated = this.holder.getPersistenceExceptionTranslator().translateExceptionIfPossible(p);
            if (translated != null) {
              throw translated;
            }
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
      // Unbind the SqlSession from tx synchronization
      // Note, commit/rollback is needed to ensure 2nd level cache is properly updated
      // SpringTransaction will no-op the connection commit/rollback
      try {
        // Do not call commit unless there is really a transaction; 
        // no need to commit if just tx synchronization is active but no transaction was started
        if (isActualTransactionActive()) {
          switch (status) {
          case STATUS_COMMITTED:
            if (logger.isDebugEnabled()) {
              logger.debug("Transaction synchronization committing SqlSession [" + this.holder.getSqlSession() + "]");
            }
            holder.getSqlSession().commit();
            break;
          case STATUS_ROLLED_BACK:
            if (logger.isDebugEnabled()) {
              logger.debug("Transaction synchronization rolling back SqlSession [" + this.holder.getSqlSession() + "]");
            }
            holder.getSqlSession().rollback();
            break;
          default:
            if (logger.isDebugEnabled()) {
              logger.debug("Transaction synchronization ended with unknown status for SqlSession [" + this.holder.getSqlSession() + "]");
            }
          }
        }
      } finally {
        if (!holder.isOpen()) {
          unbindResource(sessionFactory);
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

}
