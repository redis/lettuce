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

import java.util.List;

import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * Interface that specifies a basic set of iBatis SqlSession operations,
 * implemented by {@link SqlSessionTemplate}. Not often used, but a useful
 * option to enhance testability, as it can easily be mocked or stubbed.
 *
 * <p>Defines SqlSessionTemplate's convenience methods that mirror
 * the iBatis {@link org.apache.ibatis.session.SqlSession}'s execution
 * methods. Users are strongly encouraged to read the iBatis javadocs
 * for details on the semantics of those methods.
 *
 * @author Putthibong Boonbong
 * @see org.springframework.orm.ibatis3.SqlSessionTemplate
 * @see org.apache.ibatis.session.SqlSession
 * @version $Id$
 */
public interface SqlSessionOperations {

    /**
     * @see org.apache.ibatis.session.SqlSession#selectOne(String)
     * @throws org.springframework.dao.DataAccessException in case of errors
     */
    Object selectOne(String statement);

    /**
     * @see org.apache.ibatis.session.SqlSession#selectOne(String, Object)
     * @throws org.springframework.dao.DataAccessException in case of errors
     */
    Object selectOne(String statement, Object parameter);

    /**
     * @see org.apache.ibatis.session.SqlSession#selectList(String, Object)
     * @throws org.springframework.dao.DataAccessException in case of errors
     */
    <T> List<T> selectList(String statement);

    /**
     * @see org.apache.ibatis.session.SqlSession#selectList(String, Object)
     * @throws org.springframework.dao.DataAccessException in case of errors
     */
    <T> List<T> selectList(String statement, Object parameter);

    /**
     * @see org.apache.ibatis.session.SqlSession#selectList(String, Object, org.apache.ibatis.session.RowBounds)
     * @throws org.springframework.dao.DataAccessException in case of errors
     */
    <T> List<T> selectList(String statement, Object parameter, RowBounds rowBounds);

    /**
     * @see org.apache.ibatis.session.SqlSession#select(String, Object, org.apache.ibatis.session.ResultHandler)
     * @throws org.springframework.dao.DataAccessException in case of errors
     */
    void select(String statement, Object parameter, ResultHandler handler);

    /**
     * @see org.apache.ibatis.session.SqlSession#select(String, Object, org.apache.ibatis.session.RowBounds, org.apache.ibatis.session.ResultHandler)
     * @throws org.springframework.dao.DataAccessException in case of errors
     */
    void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler);

    /**
     * @see org.apache.ibatis.session.SqlSession#insert(String)
     * @throws org.springframework.dao.DataAccessException in case of errors
     */
    int insert(String statement);

    /**
     * @see org.apache.ibatis.session.SqlSession#insert(String, Object)
     * @throws org.springframework.dao.DataAccessException in case of errors
     */
    int insert(String statement, Object parameter);

    /**
     * @see org.apache.ibatis.session.SqlSession#update(String)
     * @throws org.springframework.dao.DataAccessException in case of errors
     */
    int update(String statement);

    /**
     * @see org.apache.ibatis.session.SqlSession#update(String, Object)
     * @throws org.springframework.dao.DataAccessException in case of errors
     */
    int update(String statement, Object parameter);

    /**
     * @see org.apache.ibatis.session.SqlSession#delete(String)
     * @throws org.springframework.dao.DataAccessException in case of errors
     */
    int delete(String statement);

    /**
     * @see org.apache.ibatis.session.SqlSession#delete(String, Object)
     * @throws org.springframework.dao.DataAccessException in case of errors
     */
    int delete(String statement, Object parameter);

    /**
     * @see org.apache.ibatis.session.SqlSession#getMapper(Class) 
     * @throws org.springframework.dao.DataAccessException in case of errors
     */
    <T> T getMapper(Class<T> type);

}
