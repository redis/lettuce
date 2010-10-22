package org.mybatis.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.Test;

import com.mockrunner.mock.jdbc.MockDataSource;

/**
 * 
 * @version $Id$
 */
public final class SqlSessionFactoryBeanTest {

    private static MockDataSource dataSource = new MockDataSource();

    private SqlSessionFactoryBean factoryBean;

    public void setupFactoryBean() {
        factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
    }

    @Test
    public void testDefaults() throws Exception {
        setupFactoryBean();

        assertDefaultConfig(factoryBean.getObject());
    }

    // DataSource is the only required property that does not have a default value, so test for both
    // not setting it at all and setting it to null
    @Test(expected = IllegalArgumentException.class)
    public void testNullDataSource() throws Exception {
        factoryBean = new SqlSessionFactoryBean();
        factoryBean.getObject();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetNullDataSource() throws Exception {
        factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(null);
        factoryBean.getObject();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullSqlSessionFactoryBuilder() throws Exception {
        setupFactoryBean();
        factoryBean.setSqlSessionFactoryBuilder(null);
        factoryBean.getObject();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullTransactionFactoryClass() throws Exception {
        setupFactoryBean();
        factoryBean.setTransactionFactoryClass(null);
        factoryBean.getObject();
    }

    @Test
    public void testOtherTransactionFactoryClass() throws Exception {
        setupFactoryBean();
        factoryBean.setTransactionFactoryClass(JdbcTransactionFactory.class);

        assertConfig(factoryBean.getObject(), JdbcTransactionFactory.class);
    }

    @Test
    public void testNullTransactionFactoryProperties() throws Exception {
        setupFactoryBean();
        // default should also be null, but test explicitly setting to null
        factoryBean.setTransactionFactoryProperties(null);

        assertDefaultConfig(factoryBean.getObject());
    }

    // the MyBatis API allows null and empty environment names, so we should allow them too
    @Test
    public void testNullEnvironment() throws Exception {
        setupFactoryBean();

        factoryBean.setEnvironment(null);

        assertConfig(factoryBean.getObject(), null,
                org.mybatis.spring.transaction.SpringManagedTransactionFactory.class);
    }

    @Test
    public void testEmptyStringEnvironment() throws Exception {
        setupFactoryBean();

        factoryBean.setEnvironment("");

        assertConfig(factoryBean.getObject(), "", org.mybatis.spring.transaction.SpringManagedTransactionFactory.class);
    }

    @Test
    public void testNullConfigLocation() throws Exception {
        setupFactoryBean();
        // default should also be null, but test explicitly setting to null
        factoryBean.setConfigLocation(null);

        assertDefaultConfig(factoryBean.getObject());
    }

    @Test
    public void testSetConfigLocation() throws Exception {
        setupFactoryBean();

        factoryBean.setConfigLocation(new org.springframework.core.io.ClassPathResource(
                "org/mybatis/spring/mybatis-config.xml"));

        SqlSessionFactory factory = factoryBean.getObject();

        assertEquals(factory.getConfiguration().getEnvironment().getId(), SqlSessionFactoryBean.class.getSimpleName());
        assertSame(factory.getConfiguration().getEnvironment().getDataSource(), dataSource);
        assertSame(factory.getConfiguration().getEnvironment().getTransactionFactory().getClass(),
                org.mybatis.spring.transaction.SpringManagedTransactionFactory.class);

        // properties explicitly set differently than the defaults in the config xml

        assertFalse(factory.getConfiguration().isCacheEnabled());
        assertTrue(factory.getConfiguration().isUseGeneratedKeys());
        assertSame(factory.getConfiguration().getDefaultExecutorType(), org.apache.ibatis.session.ExecutorType.REUSE);

        // [org.mybatis.spring.TestMapper.findTest, findTest]
        assertEquals(factory.getConfiguration().getMappedStatementNames().size(), 2);

        // [findTest-void, org.mybatis.spring.TestMapper.findTest-void]
        assertEquals(factory.getConfiguration().getResultMapNames().size(), 2);
        assertEquals(factory.getConfiguration().getParameterMapNames().size(), 0);
    }

    @Test
    public void testNullMapperLocations() throws Exception {
        setupFactoryBean();
        // default should also be null, but test explicitly setting to null
        factoryBean.setMapperLocations(null);

        assertDefaultConfig(factoryBean.getObject());
    }

    @Test
    public void testEmptyMapperLocations() throws Exception {
        setupFactoryBean();
        factoryBean.setMapperLocations(new org.springframework.core.io.Resource[0]);

        assertDefaultConfig(factoryBean.getObject());
    }

    @Test
    public void testMapperLocationsWithNullEntry() throws Exception {
        setupFactoryBean();
        factoryBean.setMapperLocations(new org.springframework.core.io.Resource[] { null });

        assertDefaultConfig(factoryBean.getObject());
    }

    private void assertDefaultConfig(SqlSessionFactory factory) {
        assertConfig(factory, SqlSessionFactoryBean.class.getSimpleName(),
                org.mybatis.spring.transaction.SpringManagedTransactionFactory.class);
    }

    private void assertConfig(SqlSessionFactory factory, Class<? extends TransactionFactory> transactionFactoryClass) {
        assertConfig(factory, SqlSessionFactoryBean.class.getSimpleName(), transactionFactoryClass);
    }

    private void assertConfig(SqlSessionFactory factory, String environment,
            Class<? extends TransactionFactory> transactionFactoryClass) {
        assertEquals(factory.getConfiguration().getEnvironment().getId(), environment);
        assertSame(factory.getConfiguration().getEnvironment().getDataSource(), dataSource);
        assertSame(factory.getConfiguration().getEnvironment().getTransactionFactory().getClass(),
                transactionFactoryClass);

        // no mappers configured => no mapped statements
        assertEquals(factory.getConfiguration().getMappedStatementNames().size(), 0);
        assertEquals(factory.getConfiguration().getResultMapNames().size(), 0);
        assertEquals(factory.getConfiguration().getParameterMapNames().size(), 0);
    }
}
