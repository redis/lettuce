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
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.NestedIOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link FactoryBean} that creates an MyBatis {@link SqlSessionFactory}. 
 * This is the usual way to set up a shared MyBatis {@link SqlSessionFactory} in a Spring application context; 
 * the SqlSessionFactory can then be passed to MyBatis-based DAOs via dependency injection.
 *
 * Either {@link DataSourceTransactionManager} or {@link JtaTransactionManager} can be used for transaction
 * demarcation in combination with a {@link SqlSessionFactory}. JTA should be used for transactions
 * which span multiple databases or when container managed transactions (CMT) are being used.
 *
 * @see #setConfigLocation
 * @see #setDataSource
 * @version $Id$
 */
public class SqlSessionFactoryBean implements FactoryBean<SqlSessionFactory>, InitializingBean {

    private final Log logger = LogFactory.getLog(getClass());

    private Resource configLocation;

    private Resource[] mapperLocations;

    private DataSource dataSource;

    private TransactionFactory transactionFactory;

    private Properties configurationProperties;

    private SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();

    private SqlSessionFactory sqlSessionFactory;

    private String environment = SqlSessionFactoryBean.class.getSimpleName();

    /**
     * Set the location of the MyBatis {@link SqlSessionFactory} config file. A typical value is
     * "WEB-INF/mybatis-configuration.xml".
     */
    public void setConfigLocation(Resource configLocation) {
        this.configLocation = configLocation;
    }

    /**
     * Set locations of MyBatis mapper files that are going to be merged into the {@link SqlSessionFactory}
     * configuration at runtime.
     *
     * This is an alternative to specifying "&lt;sqlmapper&gt;" entries in an MyBatis config file.
     * This property being based on Spring's resource abstraction also allows for specifying
     * resource patterns here: e.g. "classpath*:sqlmap/*-mapper.xml".
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
     * Set the JDBC {@link DataSource} that this instance should manage transactions for. The {@link DataSource}
     * should match the one used by the {@link SqlSessionFactory}: for example, you could specify the same
     * JNDI DataSource for both.
     *
     * A transactional JDBC {@link Connection} for this {@link DataSource} will be provided to application code
     * accessing this {@link DataSource} directly via {@link DataSourceUtils} or {@link DataSourceTransactionManager}.
     *
     * The {@link DataSource} specified here should be the target {@link DataSource} to manage transactions for, not
     * a {@link TransactionAwareDataSourceProxy}. Only data access code may work with
     * {@link TransactionAwareDataSourceProxy}, while the transaction manager needs to work on the
     * underlying target {@link DataSource}. If there's nevertheless a {@link TransactionAwareDataSourceProxy}
     * passed in, it will be unwrapped to extract its target {@link DataSource}.
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
        } else {
            this.dataSource = dataSource;
        }
    }

    /**
     * Sets the {@link SqlSessionFactoryBuilder} to use when creating the {@link SqlSessionFactory}.
     *
     * This is mainly meant for testing so that mock SqlSessionFactory classes can be injected. By
     * default, {@link SqlSessionFactoryBuilder} creates {@link DefaultSqlSessionFactory} instances.
     *
     * @see org.apache.ibatis.session.SqlSessionFactoryBuilder
     */
    public void setSqlSessionFactoryBuilder(SqlSessionFactoryBuilder sqlSessionFactoryBuilder) {
        this.sqlSessionFactoryBuilder = sqlSessionFactoryBuilder;
    }

    /**
     * Set the MyBatis TransactionFactory to use. Default is {@link SpringManagedTransactionFactory}
     *
     * The default {@link SpringManagedTransactionFactory} should be appropriate for all cases: 
     * be it Spring transaction management, EJB CMT or plain JTA. If there is no active transaction, 
     * SqlSession operations will execute SQL statements non-transactionally.
     *
     * <b>It is strongly recommended to use the default {@link TransactionFactory}.</b> If not used, any
     * attempt at getting an SqlSession through Spring's MyBatis framework will throw an exception if
     * a transaction is active.
     *
     * @see org.apache.ibatis.transaction.TransactionFactory
     * @see org.mybatis.spring.transaction.SpringManagedTransactionFactory
     * @see org.apache.ibatis.transaction.Transaction
     * @param transactionFactory the MyBatis TransactionFactory
     */
    public void setTransactionFactory(TransactionFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
    }

    /**
     * <b>NOTE:</b> This class <em>overrides</em> any {@link Environment} you have set in the MyBatis 
     * config file. This is used only as a placeholder name. The default value is
     * <code>SqlSessionFactoryBean.class.getSimpleName()</code>.
     *
     * @param environment the environment name
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    /**
     * {@inheritDoc}
     */
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(dataSource, "Property 'dataSource' is required");
        Assert.notNull(sqlSessionFactoryBuilder, "Property 'sqlSessionFactoryBuilder' is required");

        this.sqlSessionFactory = buildSqlSessionFactory();
    }

    /**
     * Build a {@link SqlSessionFactory} instance.
     *
     * The default implementation uses the standard MyBatis {@link XMLConfigBuilder} API to build a
     * {@link SqlSessionFactory} instance based on an Reader.
     *
     * @return SqlSessionFactory
     * @throws IOException if loading the config file failed
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    protected SqlSessionFactory buildSqlSessionFactory() throws IOException, IllegalAccessException,
            InstantiationException {

        XMLConfigBuilder xmlConfigBuilder;
        Configuration configuration;

        if (this.configLocation != null) {
            Reader reader = null;
            try {
                reader = new InputStreamReader(this.configLocation.getInputStream());
                // Null environment causes the configuration to use the default.
                // This will be overwritten below regardless.
                xmlConfigBuilder = new XMLConfigBuilder(reader, null, this.configurationProperties);
                configuration = xmlConfigBuilder.parse();
            } catch (IOException ex) {
                throw new NestedIOException("Failed to parse config resource: "
                        + this.configLocation, ex);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                        // close quietly
                    }
                }
            }

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Parsed configuration file: '" + this.configLocation + "'");
            }
        } else {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Property 'configLocation' not specified, using default MyBatis Configuration");
            }
            configuration = new Configuration();
        }

        if (this.transactionFactory == null) {
            this.transactionFactory = new SpringManagedTransactionFactory(this.dataSource);
        }
        
        Environment environment = new Environment(this.environment, this.transactionFactory, this.dataSource);

        configuration.setEnvironment(environment);

        if (!ObjectUtils.isEmpty(this.mapperLocations)) {
            Map<String, XNode> sqlFragments = new HashMap<String, XNode>();

            for (Resource mapperLocation : this.mapperLocations) {
                if (mapperLocation == null) {
                    continue;
                }

                // MyBatis holds a Map using "resource" name as a key.
                // If a mapper file is loaded, it searches for a mapper interface type.
                // If the type is found then it tries to load the mapper file again looking for this:
                //
                //   String xmlResource = type.getName().replace('.', '/') + ".xml";
                //
                // So if a mapper interface exists, resource cannot be an absolute path.
                // Otherwise MyBatis will throw an exception because
                // it will load both a mapper interface and the mapper xml file,
                // and throw an exception telling that a mapperStatement cannot be loaded twice.
                String path;
                if (mapperLocation instanceof ClassPathResource) {
                    path = ((ClassPathResource) mapperLocation).getPath();
                } else {
                    // this won't work if there is also a mapper interface in classpath
                    path = mapperLocation.toString();
                }

                Reader reader = null;
                try {
                    reader = new InputStreamReader(mapperLocation.getInputStream());
                    XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(reader, configuration, path, sqlFragments);
                    xmlMapperBuilder.parse();
                } catch (Exception e) {
                    throw new NestedIOException("Failed to parse mapping resource: '" + mapperLocation + "'", e);
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException ignored) {
                        }
                    }
                }

                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Parsed mapper file: '" + mapperLocation + "'");
                }
            }
        } else {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Property 'mapperLocations' was not specified, only MyBatis mapper files specified in the config xml were loaded");
            }
        }

        return this.sqlSessionFactoryBuilder.build(configuration);
    }

    /**
     * {@inheritDoc}
     */
    public SqlSessionFactory getObject() throws Exception {
        if (this.sqlSessionFactory == null) {
            afterPropertiesSet();
        }

        return this.sqlSessionFactory;
    }

    /**
     * {@inheritDoc}
     */
    public Class<? extends SqlSessionFactory> getObjectType() {
        return this.sqlSessionFactory == null ? SqlSessionFactory.class : this.sqlSessionFactory.getClass();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSingleton() {
        return true;
    }

}
