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

import static org.junit.Assert.fail;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * @version $Id$
 */
public final class MapperFactoryBeanTest extends AbstractMyBatisSpringTest {

    private static SqlSessionTemplate sqlSessionTemplate;

    @BeforeClass
    public static void setupSqlTemplate() {
        sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
    }

    // test normal MapperFactoryBean usage
    @Test
    public void testBasicUsage() throws Exception {
        find();

        assertNoCommit();
        assertSingleConnection();
    }

    @Test
    public void testAddToConfigTrue() throws Exception {
        // the default SqlSessionFactory in BaseMyBatis SpringTest is created with an explicitly set
        // MapperLocations list, so create a new factory here that tests auto-loading the config
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        // mapperLocations properties defaults to null
        factoryBean.setDataSource(dataSource);

        SqlSessionFactory sqlSessionFactory = factoryBean.getObject();

        find(new SqlSessionTemplate(sqlSessionFactory), true);
        assertNoCommit();
        assertSingleConnection();
    }

    // will fail because TestDao's mapper config is never loaded
    @Test(expected = org.apache.ibatis.binding.BindingException.class)
    public void testAddToConfigFalse() throws Throwable {
        try {
            // the default SqlSessionFactory in BaseMyBatis SpringTest is created with an explicitly
            // set
            // MapperLocations list, so create a new factory here that tests auto-loading the config
            SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
            // mapperLocations properties defaults to null
            factoryBean.setDataSource(dataSource);

            SqlSessionFactory sqlSessionFactory = factoryBean.getObject();

            find(new SqlSessionTemplate(sqlSessionFactory), false);
            fail("TestDao's mapper xml should not be loaded");
        } catch (MyBatisSystemException mbse) {
            // unwrap exception so the exact MyBatis exception can be tested
            throw mbse.getCause();
        }
    }

    @Test
    public void testWithTx() throws Exception {
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());

        find();

        txManager.commit(status);

        assertCommit();
        assertSingleConnection();
    }

    // SqlSessionTemplate should use explicitly set DataSource, if there is one
//    @Test(expected = UnsupportedOperationException.class)
//    public void testWithDifferentDataSource() throws Exception {
//        try {
//            CountingMockDataSource mockDataSource = new CountingMockDataSource();
//            mockDataSource.setupConnection(createMockConnection());
//
//            SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
//            sqlSessionTemplate.setDataSource(mockDataSource);
//
//            fail("should not be able to change the datasource");
//
//        } finally {
//            // null the connection since it was not used
//            // this avoids failing in validateConnectionClosed()
//            connection = null;
//        }
//    }

    // MapperFactoryBeans should be usable outside of Spring TX, as long as a there is no active
    // transaction
    @Test
    public void testWithNonSpringTransactionFactory() throws Exception {
        Environment original = sqlSessionFactory.getConfiguration().getEnvironment();
        Environment nonSpring = new Environment("non-spring", new JdbcTransactionFactory(), dataSource);
        sqlSessionFactory.getConfiguration().setEnvironment(nonSpring);

        try {
            find(new SqlSessionTemplate(sqlSessionFactory));

            assertNoCommit();
            assertSingleConnection();
        } finally {
            sqlSessionFactory.getConfiguration().setEnvironment(original);
        }
    }

    // active transaction using the DataSource, but without a SpringTransactionFactory
    // this should error
    @Test(expected = TransientDataAccessResourceException.class)
    public void testNonSpringTxMgrWithTx() throws Exception {
        Environment original = sqlSessionFactory.getConfiguration().getEnvironment();
        Environment nonSpring = new Environment("non-spring", new JdbcTransactionFactory(), dataSource);
        sqlSessionFactory.getConfiguration().setEnvironment(nonSpring);

        TransactionStatus status = null;

        try {
            status = txManager.getTransaction(new DefaultTransactionDefinition());

            find();

            fail("should not be able to get an SqlSession using non-Spring tx manager when there is an active Spring tx");
        } finally {
            // rollback required to close connection
            txManager.rollback(status);

            sqlSessionFactory.getConfiguration().setEnvironment(original);
        }
    }

    // TODO should this pass?
    // similar to testNonSpringTxFactoryNonSpringDSWithTx() in MyBatisSpringTest
    @Test(expected = TransientDataAccessResourceException.class)
    public void testNonSpringWithTx() throws Exception {
        Environment original = sqlSessionFactory.getConfiguration().getEnvironment();

        CountingMockDataSource mockDataSource = new CountingMockDataSource();
        mockDataSource.setupConnection(createMockConnection());

        Environment nonSpring = new Environment("non-spring", new JdbcTransactionFactory(), mockDataSource);
        sqlSessionFactory.getConfiguration().setEnvironment(nonSpring);

        SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);

        TransactionStatus status = null;

        try {
            status = txManager.getTransaction(new DefaultTransactionDefinition());

            find(sqlSessionTemplate);

            fail("should not be able to get an SqlSession using non-Spring tx manager when there is an active Spring tx");
        } finally {
            // rollback required to close connection
            txManager.rollback(status);

            sqlSessionFactory.getConfiguration().setEnvironment(original);
        }
    }

    private void find() throws Exception {
        find(MapperFactoryBeanTest.sqlSessionTemplate, true);
    }

    private void find(SqlSessionTemplate sqlSessionTemplate) throws Exception {
        find(sqlSessionTemplate, true);
    }

    private void find(SqlSessionTemplate sqlSessionTemplate, boolean addToConfig) throws Exception {
        // recreate the mapper for each test since sqlSessionTemplate or the underlying
        // SqlSessionFactory could change for each test
        MapperFactoryBean<TestMapper> mapper = new MapperFactoryBean<TestMapper>();
        mapper.setMapperInterface(TestMapper.class);
        mapper.setSqlSession(sqlSessionTemplate);
        mapper.setAddToConfig(addToConfig);
        mapper.afterPropertiesSet();

        mapper.getObject().findTest();
    }
}
