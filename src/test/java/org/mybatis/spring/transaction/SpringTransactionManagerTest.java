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

import static org.junit.Assert.assertEquals;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.junit.Test;
import org.mybatis.spring.AbstractMyBatisSpringTest;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * @version $Id: MapperFactoryBeanTest.java 3294 2010-11-28 04:01:46Z hpresnall $
 */
public final class SpringTransactionManagerTest extends AbstractMyBatisSpringTest {


    @Test
    public void testWithTx() throws Exception {
        DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
        txDef.setPropagationBehaviorName("PROPAGATION_REQUIRED");

        TransactionStatus status = txManager.getTransaction(txDef);

        SqlSession session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
        
        TransactionFactory transactionFactory = sqlSessionFactory.getConfiguration().getEnvironment().getTransactionFactory();
        Transaction transaction = transactionFactory.newTransaction(session.getConnection(), false);
        transaction.commit();
        
        assertEquals("should not call commit on Connection", 0, connection.getNumberCommits());
        
        SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);

        txManager.commit(status);
    }
    
    @Test
    public void testWithNoTx() throws Exception {

        SqlSession session = SqlSessionUtils.getSqlSession(sqlSessionFactory);
        
        TransactionFactory transactionFactory = sqlSessionFactory.getConfiguration().getEnvironment().getTransactionFactory();
        Transaction transaction = transactionFactory.newTransaction(session.getConnection(), false);
        transaction.commit();
        
        assertEquals("should call commit on Connection", 1, connection.getNumberCommits());
        
        SqlSessionUtils.closeSqlSession(session, sqlSessionFactory);
    }
    
}
