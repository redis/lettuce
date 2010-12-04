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
package org.mybatis.spring.support;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mybatis.spring.AbstractMyBatisSpringTest;
import org.mybatis.spring.SqlSessionTemplate;

/**
 * @version $Id$
 */
public final class SqlSessionDaoSupportTest extends AbstractMyBatisSpringTest {

    @Test
    public void shouldStoreTheTemplate() throws Exception {

        SqlSessionTemplate sessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
        SqlSessionDaoSupport sqlSessionDaoSupport = new SqlSessionDaoSupport() {};
        sqlSessionDaoSupport.setSqlSessionTemplate(sessionTemplate);
        connection.close();
        
        assertEquals("should store the Template", sessionTemplate, sqlSessionDaoSupport.getSqlSession());
    }

    @Test
    public void shouldStoreTheFactory() throws Exception {
        SqlSessionDaoSupport sqlSessionDaoSupport = new SqlSessionDaoSupport() {};
        sqlSessionDaoSupport.setSqlSessionFactory(sqlSessionFactory);
        connection.close();

        assertEquals("should store the Template", sqlSessionFactory, ((SqlSessionTemplate) sqlSessionDaoSupport.getSqlSession()).getSqlSessionFactory());
    }
    
    @Test
    public void shouldIgnoreTheFactory() throws Exception {
        SqlSessionTemplate sessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
        SqlSessionDaoSupport sqlSessionDaoSupport = new SqlSessionDaoSupport() {};
        sqlSessionDaoSupport.setSqlSessionTemplate(sessionTemplate);
        sqlSessionDaoSupport.setSqlSessionFactory(sqlSessionFactory);
        connection.close();

        assertEquals("should ignore the Factory", sessionTemplate, sqlSessionDaoSupport.getSqlSession());
    }
    
}
