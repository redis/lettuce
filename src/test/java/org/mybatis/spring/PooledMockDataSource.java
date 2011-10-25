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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;

import com.mockrunner.mock.jdbc.MockDataSource;

/**
 * 
 *
 * @version $Id$
 */
final class PooledMockDataSource extends MockDataSource {

    private int connectionCount = 0;

    private LinkedList<Connection> connections = new LinkedList<Connection>();

    @Override
    public Connection getConnection() throws SQLException {
        if (connections.isEmpty()) {
            throw new SQLException("Sorry, I ran out of connections");
        }
        ++this.connectionCount;
        return this.connections.removeLast();
    }

    int getConnectionCount() {
        return this.connectionCount;
    }

    void reset() {
        this.connectionCount = 0;
        this.connections.clear();
    }

    @Override
    public void setupConnection(Connection connection) {
        throw new UnsupportedOperationException("used addConnection() instead");
    }

    public void addConnection(Connection c) {
        this.connections.add(c);
    }

}
