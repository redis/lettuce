/**
 * Copyright 2010-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mybatis.spring;

import java.sql.SQLException;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.transaction.TransactionException;

/**
 * Default exception translator.
 *
 * Translates MyBatis SqlSession returned exception into a Spring {@code DataAccessException} using Spring's
 * {@code SQLExceptionTranslator} Can load {@code SQLExceptionTranslator} eagerly or when the first exception is
 * translated.
 *
 * @author Eduardo Macarron
 */
public class MyBatisExceptionTranslator implements PersistenceExceptionTranslator {

  private final Supplier<SQLExceptionTranslator> exceptionTranslatorSupplier;
  private SQLExceptionTranslator exceptionTranslator;

  /**
   * Creates a new {@code PersistenceExceptionTranslator} instance with {@code SQLErrorCodeSQLExceptionTranslator}.
   *
   * @param dataSource
   *          DataSource to use to find metadata and establish which error codes are usable.
   * @param exceptionTranslatorLazyInit
   *          if true, the translator instantiates internal stuff only the first time will have the need to translate
   *          exceptions.
   */
  public MyBatisExceptionTranslator(DataSource dataSource, boolean exceptionTranslatorLazyInit) {
    this(() -> new SQLErrorCodeSQLExceptionTranslator(dataSource), exceptionTranslatorLazyInit);
  }

  /**
   * Creates a new {@code PersistenceExceptionTranslator} instance with specified {@code SQLExceptionTranslator}.
   *
   * @param exceptionTranslatorSupplier
   *          Supplier for creating a {@code SQLExceptionTranslator} instance
   * @param exceptionTranslatorLazyInit
   *          if true, the translator instantiates internal stuff only the first time will have the need to translate
   *          exceptions.
   * @since 2.0.3
   */
  public MyBatisExceptionTranslator(Supplier<SQLExceptionTranslator> exceptionTranslatorSupplier,
      boolean exceptionTranslatorLazyInit) {
    this.exceptionTranslatorSupplier = exceptionTranslatorSupplier;
    if (!exceptionTranslatorLazyInit) {
      this.initExceptionTranslator();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DataAccessException translateExceptionIfPossible(RuntimeException e) {
    if (e instanceof PersistenceException) {
      // Batch exceptions come inside another PersistenceException
      // recursion has a risk of infinite loop so better make another if
      if (e.getCause() instanceof PersistenceException) {
        e = (PersistenceException) e.getCause();
      }
      if (e.getCause() instanceof SQLException) {
        this.initExceptionTranslator();
        String task = e.getMessage() + "\n";
        SQLException se = (SQLException) e.getCause();
        DataAccessException dae = this.exceptionTranslator.translate(task, null, se);
        return dae != null ? dae : new UncategorizedSQLException(task, null, se);
      } else if (e.getCause() instanceof TransactionException) {
        throw (TransactionException) e.getCause();
      }
      return new MyBatisSystemException(e);
    }
    return null;
  }

  /**
   * Initializes the internal translator reference.
   */
  private synchronized void initExceptionTranslator() {
    if (this.exceptionTranslator == null) {
      this.exceptionTranslator = exceptionTranslatorSupplier.get();
    }
  }

}
