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

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DaoSupport;
import org.springframework.util.Assert;

/**
 * Convenient super class for iBATIS SqlSession data access objects. In the usual case, all that a
 * DAO needs is an SqlSessionFactory. This class also supports passing in an SqlSessionTemplate if a
 * custom DataSource or ExceptionTranslator is needed for a specific DAO.
 * <p>
 *
 * By default, each DAO gets its own SqlSessionTemplate which holds the SqlSessionFactory.
 * SqlSessionTemplate is thread safe, so a single instance cannot be shared by all DAOs; there
 * should also be a small memory savings by doing this. To support a shared template, this class has
 * a constructor that accepts an SqlSessionTemplate. This pattern can be used in Spring
 * configuration files as follows:
 *
 * <pre class="code">
 *   <bean id="baseDAO" abstract="true" lazy-init="true">
 *     <constructor-arg>
 *       <bean class="org.springframework.orm.ibatis3.SqlSessionTemplate">
 *         <constructor-arg ref="sqlSessionFactory" />
 *       </bean>
 *     </constructor-arg>
 *   </bean>
 * 
 *   <bean id="testDao" parent="baseDAO" class="org.springframework.orm.ibatis3.support.SqlSessionDaoSupport" />
 * </pre>
 *
 * @author Putthibong Boonbong
 * @see #setSqlSessionFactory
 * @see #setSqlSessionTemplate
 * @see org.springframework.orm.ibatis3.SqlSessionTemplate
 * @see org.springframework.orm.ibatis3.SqlSessionTemplate#setExceptionTranslator
 * @version $Id$
 */
public abstract class SqlSessionDaoSupport extends DaoSupport {

    private SqlSessionTemplate sessionTemplate;

    private boolean externalTemplate;

    public SqlSessionDaoSupport() {
        sessionTemplate = new SqlSessionTemplate();
        externalTemplate = false;
    }

    public SqlSessionDaoSupport(SqlSessionTemplate sessionTemplate) {
        this.sessionTemplate = sessionTemplate;
        externalTemplate = true;
    }

    /**
     * Set the JDBC DataSource to be used by this DAO. Not required: The SqlSessionFactory defines a
     * shared DataSource.
     * <p>
     * This is a no-op if an external SqlSessionTemplate has been set.
     * 
     * @see #setSqlSessionFactory
     */
    public final void setDataSource(DataSource dataSource) {
        if (!this.externalTemplate) {
            this.sessionTemplate.setDataSource(dataSource);
        }
    }

    /**
     * Return the JDBC DataSource used by this DAO.
     */
    public final DataSource getDataSource() {
        return this.sessionTemplate.getDataSource();
    }

    /**
     * Set the SqlSessionFactory to work with.
     * <p>
     * This is a no-op if an external SqlSessionTemplate has been set.
     * 
     * @see #setSqlSessionTemplate
     */
    @Autowired
    public final void setSqlSessionFactory(SqlSessionFactory sessionFactory) {
        if (!this.externalTemplate) {
            this.sessionTemplate.setSqlSessionFactory(sessionFactory);
        }
    }

    /**
     * Return the SqlSessionFactory that this DAO uses.
     */
    public final SqlSessionFactory getSqlSessionFactory() {
        return this.sessionTemplate.getSqlSessionFactory();
    }

    /**
     * Set the SqlSessionTemplate for this DAO explicitly, as an alternative to specifying a
     * SqlSessionFactory.
     * 
     * @see #setSqlSessionFactory
     */
    public final void setSqlSessionTemplate(SqlSessionTemplate sessionTemplate) {
        this.sessionTemplate = sessionTemplate;
        this.externalTemplate = true;
    }

    /**
     * Return the SqlSessionTemplate for this DAO, pre-initialized with the SqlSessionFactory or set
     * explicitly.
     */
    public final SqlSessionTemplate getSqlSessionTemplate() {
        return this.sessionTemplate;
    }

    protected void checkDaoConfig() {
        Assert.notNull(sessionTemplate, "Property 'SqlSessionTemplate' is required");
        this.sessionTemplate.afterPropertiesSet();
    }

}
