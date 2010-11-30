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

import org.junit.Test;
import org.mybatis.spring.AbstractMyBatisSpringTest;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * @version $Id: MapperFactoryBeanTest.java 3294 2010-11-28 04:01:46Z hpresnall $
 */
public final class SpringTransactionManagerTest extends AbstractMyBatisSpringTest {


    @Test
    public void testShouldNoOpWithTx() throws Exception {
        DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
        txDef.setPropagationBehaviorName("PROPAGATION_REQUIRED");

        TransactionStatus status = txManager.getTransaction(txDef);
        SpringManagedTransactionFactory transactionFactory = new SpringManagedTransactionFactory(dataSource);
        SpringManagedTransaction transaction = (SpringManagedTransaction) transactionFactory.newTransaction(connection, false);
        transaction.commit();        
        assertEquals("should not call commit on Connection", 0, connection.getNumberCommits());

        txManager.commit(status);
    }
    
    @Test
    public void testShouldCommitWithNoTx() throws Exception {

        SpringManagedTransactionFactory transactionFactory = new SpringManagedTransactionFactory(dataSource);
        SpringManagedTransaction transaction = (SpringManagedTransaction) transactionFactory.newTransaction(connection, false);
        transaction.commit();
        
        assertEquals("should call commit on Connection", 1, connection.getNumberCommits());
        
        connection.close();
    }

    @Test
    public void testShouldIgnoreAutocommit() throws Exception {

        SpringManagedTransactionFactory transactionFactory = new SpringManagedTransactionFactory(dataSource);
        SpringManagedTransaction transaction = (SpringManagedTransaction) transactionFactory.newTransaction(connection, true);
        transaction.commit();
        
        assertEquals("should call commit on Connection", 1, connection.getNumberCommits());
        
        connection.close();
    }
    
}
