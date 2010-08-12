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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.NestedIOException;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that creates an iBatis
 * {@link org.apache.ibatis.session.SqlSessionFactory}. This is the usual way to set up a shared
 * iBatis SqlSessionFactory in a Spring application context; the SqlSessionFactory can then be
 * passed to iBatis-based DAOs via dependency injection.
 * 
 * <p>
 * Either {@link org.springframework.jdbc.datasource.DataSourceTransactionManager} or
 * {@link org.springframework.transaction.jta.JtaTransactionManager} can be used for transaction
 * demarcation in combination with a SqlSessionFactory, with JTA only necessary for transactions
 * which span multiple databases.
 * 
 * <p>
 * Allows for specifying a DataSource at the SqlSessionFactory level. This is preferable to per-DAO
 * DataSource references, as it allows for lazy loading and avoids repeated DataSource references in
 * every DAO.
 * 
 * @author Putthibong Boonbong
 * @see #setConfigLocation
 * @see #setDataSource
 * @see org.springframework.orm.ibatis3.SqlSessionTemplate#setSqlSessionFactory
 * @see org.springframework.orm.ibatis3.SqlSessionTemplate#setDataSource
 * @version $Id$
 */
public class SqlSessionFactoryBean implements FactoryBean<SqlSessionFactory>, InitializingBean {

    protected final Log logger = LogFactory.getLog(getClass());

    private Resource configLocation;

    private Resource[] mapperLocations;

    private DataSource dataSource;

    private Class<? extends TransactionFactory> transactionFactoryClass = SpringManagedTransactionFactory.class;

    private Properties transactionFactoryProperties;

    private Properties configurationProperties;

    private SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();

    private SqlSessionFactory sqlSessionFactory;

    private String environment = SqlSessionFactoryBean.class.getSimpleName();

    public SqlSessionFactoryBean() {}

    /**
     * Set the location of the iBatis SqlSessionFactory config file. A typical value is
     * "WEB-INF/ibatis-configuration.xml".
     */
    public void setConfigLocation(Resource configLocation) {
        this.configLocation = configLocation;
    }

    /**
     * Set locations of iBatis mapper files that are going to be merged into the SqlSessionFactory
     * configuration at runtime.
     * <p>
     * This is an alternative to specifying "&lt;sqlmapper&gt;" entries in an iBatis config file.
     * This property being based on Spring's resource abstraction also allows for specifying
     * resource patterns here: e.g. "/sqlmap/*-mapper.xml".
     * <p>
     */
    public void setMapperLocations(Resource[] mapperLocations) {
        this.mapperLocations = mapperLocations;
    }

    /**
     * Set optional properties to be passed into the SqlSession configuration, as alternative to a
     * <code>&lt;properties&gt;</code> tag in the configuration xml file. This will be used to
     * resolve placeholders in the config file.
     * 
     * @see org.apache.ibatis.session.Configuration#getVariables
     */
    public void setConfigurationProperties(Properties sqlSessionFactoryProperties) {
        this.configurationProperties = sqlSessionFactoryProperties;
    }

    /**
     * Set the JDBC DataSource that this instance should manage transactions for. The DataSource
     * should match the one used by the SqlSessionFactory: for example, you could specify the same
     * JNDI DataSource for both.
     * 
     * <p>
     * A transactional JDBC Connection for this DataSource will be provided to application code
     * accessing this DataSource directly via DataSourceUtils or DataSourceTransactionManager.
     * 
     * <p>
     * The DataSource specified here should be the target DataSource to manage transactions for, not
     * a TransactionAwareDataSourceProxy. Only data access code may work with
     * TransactionAwareDataSourceProxy, while the transaction manager needs to work on the
     * underlying target DataSource. If there's nevertheless a TransactionAwareDataSourceProxy
     * passed in, it will be unwrapped to extract its target DataSource.
     * 
     * @see org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
     * @see org.springframework.jdbc.datasource.DataSourceUtils
     * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
     */
    public void setDataSource(DataSource dataSource) {
        if (dataSource instanceof TransactionAwareDataSourceProxy) {
            // If we got a TransactionAwareDataSourceProxy, we need to perform
            // transactions for its underlying target DataSource, else data
            // access code won't see properly exposed transactions (i.e.
            // transactions for the target DataSource).
            this.dataSource = ((TransactionAwareDataSourceProxy) dataSource).getTargetDataSource();
        }
        else {
            this.dataSource = dataSource;
        }
    }

    /**
     * Sets the SqlSessionFactoryBuilder to use when creating the SqlSessionFactory.
     * <p>
     * This is mainly meant for testing so that mock SqlSessionFactory classes can be injected. By
     * default, SqlSessionFactoryBuilder creates <code>DefaultSqlSessionFactory<code> instances.
     * 
     * @see org.apache.ibatis.session.SqlSessionFactoryBuilder
     */
    public void setSqlSessionFactoryBuilder(SqlSessionFactoryBuilder sqlSessionFactoryBuilder) {
        this.sqlSessionFactoryBuilder = sqlSessionFactoryBuilder;
    }

    /**
     * Set the iBatis TransactionFactory class to use. Default is
     * <code>SpringManagedTransactionFactory</code> .
     * <p>
     * The default SpringManagedTransactionFactory should be appropriate for all cases: be it Spring
     * transaction management, EJB CMT or plain JTA. If there is no active transaction, SqlSession
     * operations will execute SQL statements non-transactionally.
     * <p>
     * <b>It is strongly recommended to use the default TransactionFactory.</b> If not used, any
     * attempt at getting an SqlSession through Spring's iBatis framework will throw an exception if
     * a transaction is active.
     * 
     * @see #setDataSource
     * @see #setTransactionFactoryProperties(java.util.Properties)
     * @see org.apache.ibatis.transaction.TransactionFactory
     * @see org.springframework.orm.ibatis3.transaction.SpringManagedTransactionFactory
     * @see org.apache.ibatis.transaction.Transaction
     * @see org.springframework.orm.ibatis3.SqlSessionUtils#getSqlSession(SqlSessionFactory,
     *      DataSource, org.apache.ibatis.session.ExecutorType)
     */
    public void setTransactionFactoryClass(Class<TransactionFactory> transactionFactoryClass) {
        this.transactionFactoryClass = transactionFactoryClass;
    }

    /**
     * Set properties to be passed to the TransactionFactory instance used by this
     * SqlSessionFactory.
     * <p/>
     * 
     * As of iBatis 3.0.0, these properties are <em>ignored</em> by the provided
     * <code>TransactionFactory</code> implementations.
     * 
     * @see org.apache.ibatis.transaction.TransactionFactory#setProperties(java.util.Properties)
     * @see org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory
     * @see org.apache.ibatis.transaction.managed.ManagedTransactionFactory
     */
    public void setTransactionFactoryProperties(Properties transactionFactoryProperties) {
        this.transactionFactoryProperties = transactionFactoryProperties;
    }

    /**
     * <b>NOTE:</b> This class <em>overrides</em> any Environment you have set in the iBatis config
     * file. This is used only as a placeholder name. The default value is
     * <code>SqlSessionFactoryBean.class.getSimpleName()</code> .
     * 
     * @param environment the environment name
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(dataSource, "Property 'dataSource' is required");
        Assert.notNull(sqlSessionFactoryBuilder, "Property 'sqlSessionFactoryBuilder' is required");
        Assert.notNull(transactionFactoryClass, "Property 'transactionFactoryClass' is required");

        sqlSessionFactory = buildSqlSessionFactory();
    }

    /**
     * Build a SqlSessionFactory instance.
     * <p>
     * The default implementation uses the standard iBatis {@link XMLConfigBuilder} API to build a
     * SqlSessionFactory instance based on an Reader.
     *
     * @see org.apache.ibatis.builder.xml.XMLConfigBuilder#parse()
     *
     * @return
     *
     * @throws IOException if loading the config file failed
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    protected SqlSessionFactory buildSqlSessionFactory() throws IOException, IllegalAccessException,
            InstantiationException {

        XMLConfigBuilder xmlConfigBuilder;
        Configuration configuration;

        if (configLocation != null) {
            try {
                Reader reader = new InputStreamReader(configLocation.getInputStream());
                // null environment causes the configuration to use the default
                // this will be overwritten below regardless
                xmlConfigBuilder = new XMLConfigBuilder(reader, null, configurationProperties);
                configuration = xmlConfigBuilder.parse();
            }
            catch (IOException ex) {
                throw new NestedIOException("Failed to parse config resource: " + configLocation, ex.getCause());
            }

            logger.debug("Parsed configuration file: '" + configLocation + "'");
        }
        else {
            logger.info("Property 'configLocation' not specified, using default iBatis Configuration");
            configuration = new Configuration();
        }

        TransactionFactory transactionFactory = this.transactionFactoryClass.newInstance(); // expose IllegalAccessException, InstantiationException

        transactionFactory.setProperties(transactionFactoryProperties);
        Environment environment = new Environment(this.environment, transactionFactory, this.dataSource);

        configuration.setEnvironment(environment);

        if (!ObjectUtils.isEmpty(mapperLocations)) {
            Map<String, XNode> sqlFragments = new HashMap<String, XNode>();

            for (Resource mapperLocation : mapperLocations) {
                try {
                    Reader reader = new InputStreamReader(mapperLocation.getInputStream());
                    XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(reader, configuration, mapperLocation.toString(), sqlFragments);
                    xmlMapperBuilder.parse();
                }
                catch (Exception e) {
                    throw new NestedIOException("Failed to parse mapping resource: '" + mapperLocation + "'", e);
                }

                logger.debug("Parsed mapper file: '" + mapperLocation + "'");
            }
        }
        else {
            logger.debug("Property 'mapperLocations' was not specified, only iBatis mapper files specified in the config xml were loaded");
        }

        return sqlSessionFactoryBuilder.build(configuration);
    }

    public SqlSessionFactory getObject() throws Exception {
        if (sqlSessionFactory == null) {
            afterPropertiesSet();
        }

        return sqlSessionFactory;
    }

    public Class<? extends SqlSessionFactory> getObjectType() {
        return sqlSessionFactory == null ? SqlSessionFactory.class : sqlSessionFactory.getClass();
    }

    public boolean isSingleton() {
        return true;
    }

}
