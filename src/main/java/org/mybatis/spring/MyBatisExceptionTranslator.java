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

import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

/**
 * Default exception translator.
 *
 * Translates MyBatis SqlSession returned exception into a Spring
 * {@code DataAccessException} using Spring's {@code SQLExceptionTranslator}
 * Can load {@code SQLExceptionTranslator} eagerly of when the
 * first exception is translated.
 *
 * @version $Id$
 */
public class MyBatisExceptionTranslator implements PersistenceExceptionTranslator {

  private final DataSource dataSource;

  private SQLExceptionTranslator exceptionTranslator;

  /**
   * Creates a new {@code DataAccessExceptionTranslator} instance.
   *
   * @param dataSource DataSource to use to find metadata and establish which error codes are usable.
   * @param exceptionTranslatorLazyInit if true, the translator instantiates internal stuff only the first time will
   *        have the need to translate exceptions.
   */
  public MyBatisExceptionTranslator(DataSource dataSource, boolean exceptionTranslatorLazyInit) {
    this.dataSource = dataSource;

    if (!exceptionTranslatorLazyInit) {
      this.initExceptionTranslator();
    }
  }

  /**
   * {@inheritDoc}
   */
  public DataAccessException translateExceptionIfPossible(RuntimeException e) {
    if (e instanceof PersistenceException) {
      // Batch exceptions come inside another PersistenceException
      // recursion has a risk of infinite loop so better make another if
      if (e.getCause() instanceof PersistenceException) {
        e = (PersistenceException) e.getCause();
      }
      if (e.getCause() instanceof SQLException) {
        this.initExceptionTranslator();
        return this.exceptionTranslator.translate(e.getMessage() + "\n", null, (SQLException) e.getCause());
      }
      return new MyBatisSystemException(e);
    } else {
      return null;
    }
  }

  /**
   * Initializes the internal translator reference.
   */
  private synchronized void initExceptionTranslator() {
    if (this.exceptionTranslator == null) {
      this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(this.dataSource);
    }
  }

}
