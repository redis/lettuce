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
package org.mybatis.spring.transaction;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.logging.jdbc.ConnectionLogger;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.jdbc.JdbcTransaction;
import org.apache.ibatis.transaction.managed.ManagedTransaction;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.util.Assert;

/**
 * MyBatis has two TransactionManagers out of the box: The {@link JdbcTransaction} and the
 * {@link ManagedTransaction}. When MyBatis runs under a Spring transaction none of them
 * will work well because {@link JdbcTransaction} would commit/rollback/close and it should not.
 * And {@link ManagedTransaction} would close the connection and it should not.
 * {@link SpringManagedTransaction} looks if the current connection is been managed by Spring. In that case
 * it will not commit/rollback/close. Otherwise it will behave like {@link JdbcTransaction}.
 *
 * @version $Id$
 */
public class SpringManagedTransaction implements Transaction {

    private static final Log logger = LogFactory.getLog(SpringManagedTransaction.class);

    private final Connection connection;

    private final boolean shouldManageConnection;

    public SpringManagedTransaction(Connection connection, DataSource dataSource) {
        Assert.notNull(connection, "No Connection specified");
        Assert.notNull(dataSource, "No DataSource specified");
        
        this.connection = connection;

        // Unwrap the connection if it is a ConnectionLogger for use with Spring.
        Connection nonLoggingConnection;
        if (Proxy.isProxyClass(connection.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(connection);

            if (handler instanceof ConnectionLogger) {
                nonLoggingConnection = ((ConnectionLogger) handler).getConnection();
            } else {
                nonLoggingConnection = connection;
            }
        } else {
            nonLoggingConnection = connection;
        }

        this.shouldManageConnection = !DataSourceUtils.isConnectionTransactional(nonLoggingConnection, dataSource);

        if (logger.isDebugEnabled()) {
            if (this.shouldManageConnection) {
                logger.debug("JDBC Connection [" + this.connection + "] will be managed by SpringManagedTransaction");
            } else {
                logger.debug("JDBC Connection [" + this.connection + "] will be managed by Spring");                
            }
        }   
    }

    /**
     * {@inheritDoc}
     */
    public Connection getConnection() {
        return this.connection;
    }

    /**
     * {@inheritDoc}
     */
    public void commit() throws SQLException {
        if (this.shouldManageConnection) {
            if (logger.isDebugEnabled()) {
                logger.debug("Committing JDBC Connection [" + this.connection + "]");
            }
            this.connection.commit();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void rollback() throws SQLException {
        if (this.shouldManageConnection) {
            if (logger.isDebugEnabled()) {
                logger.debug("Rolling back JDBC Connection [" + this.connection + "]");
            }
            this.connection.rollback();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws SQLException {
        if (this.shouldManageConnection) {
            if (logger.isDebugEnabled()) {
                logger.debug("Closing JDBC Connection [" + this.connection + "]");
            }
            this.connection.close();
        }
    }

}
