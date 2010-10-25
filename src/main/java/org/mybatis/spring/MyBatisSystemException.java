/*
 *    Copyright 2010 The MyBatis Team
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

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * MyBatis specific subclass of UncategorizedDataAccessException, for MyBatis system errors that do
 * not match any concrete <code>org.springframework.dao</code> exceptions.
 *
 * In MyBatis 3 <code>org.apache.ibatis.exceptions.PersistenceException</code> is a <code>RuntimeException</code>,
 * but using this wrapper class to bring everything under a single hierarchy will be easier for client code to
 * handle.
 *
 * @version $Id$
 */
public class MyBatisSystemException extends UncategorizedDataAccessException {

    private static final long serialVersionUID = -5284728621670758939L;

    public MyBatisSystemException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
