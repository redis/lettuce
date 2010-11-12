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
 *
 * Translates MyBatis SqlSession returned exception into a Spring
 * {@link DataAccessException} using Spring's {@link SQLExceptionTranslator}
 * Can load {@link SQLExceptionTranslator} eagerly of when the
 * first exception is translated.
 *
 * @see DataAccessException
 * @see SqlSession
 * @version $Id$
 */
public class DataAccessExceptionTranslator implements SqlSessionExceptionTranslator {

    private static final Object $LOCK = new Object();

    private final DataSource dataSource;

    private SQLExceptionTranslator exceptionTranslator;

    /**
     * Creates a new {@literal DataAccessExceptionTranslator} instance.
     *
     * @param dataSource DataSource to use to find metadata and establish which error codes are usable.
     * @param exceptionTranslatorLazyInit if true, the translator instantiates internal stuff only the first time will
     *        have the need to translate exceptions.
     */
    public DataAccessExceptionTranslator(DataSource dataSource, boolean exceptionTranslatorLazyInit) {
        this.dataSource = dataSource;

        if (!exceptionTranslatorLazyInit) {
            initExceptionTranslator();
        }
    }

    /**
     * {@inheritDoc}
     */
   public RuntimeException translateException(PersistenceException e, String statement) {
        if (e.getCause() instanceof SQLException) {
            if (this.exceptionTranslator == null) {
                initExceptionTranslator();
            }
            return this.exceptionTranslator.translate("SqlSession operation", statement, (SQLException) e.getCause());
        }
        return new MyBatisSystemException("SqlSession operation", e);
    }

   /**
    * Initializes the internal translator reference.
    */
   private void initExceptionTranslator() {
       synchronized ($LOCK) {
           // check again because more than one thread may be waiting for lock release
           if (this.exceptionTranslator == null) {
               this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(this.dataSource);
           }
       }
   }

}
