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

import javax.sql.DataSource;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

/**
 * Default exception translator. 
 * Translates MyBatis SqlSession returned exception into a Spring 
 * {@link DataAccessException} using Spring's {@link SQLExceptionTranslator}
 * Can load {@link SQLExceptionTranslator} eagerly of when the 
 * first exception is translated
 *
 * @see DataAccessException
 * @see SqlSession
 */
public class DataAccessExceptionTranslator implements SqlSessionExceptionTranslator {

    private boolean exceptionTranslatorLazyInit;
    private DataSource dataSource;
    private SQLExceptionTranslator exceptionTranslator;

    public DataAccessExceptionTranslator(DataSource dataSource, boolean exceptionTranslatorLazyInit) {
        this.exceptionTranslatorLazyInit = exceptionTranslatorLazyInit;
        this.dataSource = dataSource;

        if (!this.exceptionTranslatorLazyInit) {
            getExceptionTranslator();
        }
    }

    private synchronized SQLExceptionTranslator getExceptionTranslator() {
        if (this.exceptionTranslator == null) {
            this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
        }
        return this.exceptionTranslator;
    }

    /**
     * {@inheritDoc}
     */
   public RuntimeException translateException(Throwable t) {

        if (t instanceof PersistenceException && t.getCause() instanceof SQLException) {
            return getExceptionTranslator().translate("SqlSession operation", null, (SQLException) t.getCause());
        } 

        return new MyBatisSystemException("SqlSession operation", t);
    }

}
