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

import org.apache.ibatis.transaction.Transaction;

import org.apache.ibatis.logging.jdbc.ConnectionLogger;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.springframework.util.Assert;

import javax.sql.DataSource;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * TODO fill me
 *
 * @version $Id$
 */
public class SpringManagedTransaction implements Transaction {

    private final Connection connection;

    private final boolean shouldManageConnection;

    public SpringManagedTransaction(Connection connection) {
        Assert.notNull(connection, "Property 'connection' is required");

        this.connection = connection;

        Connection nonLoggingConnection;

        // Unwrap the connection if it is a ConnectionLogger for use with Spring.
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

        // This connection could have been created with any DataSource, not just the one that is
        // registered in the MyBatis Environment. So, rather than passing a DataSource into this
        // Transaction, just check the bound resources for a DataSource and use that in the call to
        // isConnectionTransactional. If there is no DataSource bound, there is no Spring
        // transaction; if this Connection does not match the one in the current transaction, this
        // is a different Spring transactional context. In either case, this Transaction should
        // manage the connection. If there is a Spring transaction in progress, this Transaction
        // will no-op all methods.
        //
        // Note: This also assumes that MyBatis does not allow changing the Transaction or the
        // Connection once an SqlSession has been created, which is consistent with the default
        // implementation.
        boolean manageConnection = true;

        for (Object o : TransactionSynchronizationManager.getResourceMap().keySet()) {
            if (o instanceof DataSource) {
                manageConnection = !DataSourceUtils.isConnectionTransactional(nonLoggingConnection, (DataSource) o);
            }
        }

        this.shouldManageConnection = manageConnection;
    }

    /**
     * {@inheritDoc}
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * {@inheritDoc}
     */
    public void commit() throws SQLException {
        if (shouldManageConnection) {
            connection.commit();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void rollback() throws SQLException {
        if (shouldManageConnection) {
            connection.rollback();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws SQLException {
        if (shouldManageConnection) {
            connection.close();
        }
    }

}
