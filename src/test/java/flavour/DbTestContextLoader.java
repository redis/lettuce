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
package flavour;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.jdbc.ScriptRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.ContextLoader;

/**
 * 
 * @version $Id$
 */
public final class DbTestContextLoader implements ContextLoader {

    private final ApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);

    public String[] processLocations(Class<?> clazz, String... locations) {
        return locations;
    }

    public ApplicationContext loadContext(String... locations) throws Exception {
        final ApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);

        DataSource dataSource = this.context.getBean(DataSource.class);
        Connection connection = null;
        ScriptRunner scriptRunner = null;
        Reader scriptReader = null;
        try {
            connection = dataSource.getConnection();
            scriptRunner = new ScriptRunner(connection);
            scriptRunner.setAutoCommit(true);
            scriptRunner.setStopOnError(true);

            scriptReader = new InputStreamReader(this.getClass().getResourceAsStream("create_hsqldb_database.sql"));
            scriptRunner.runScript(scriptReader);
        } catch (Exception e) {
            throw e;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    // close quietly
                }
            }
            if (scriptReader != null) {
                try {
                    scriptReader.close();
                } catch (IOException e) {
                    // close quietly
                }
            }
        }

        return context;
    }

}
