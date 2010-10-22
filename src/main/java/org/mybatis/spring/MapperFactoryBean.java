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

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DaoSupport;
import org.springframework.util.Assert;

/**
 * BeanFactory that enables injection of MyBatis mapper interfaces.
 *
 * @see SqlSessionTemplate
 * @version $Id$
 */
public class MapperFactoryBean <T> extends DaoSupport implements FactoryBean<T>  {

    private Class<T> mapperInterface;

    private boolean addToConfig = true;

    private SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate();

    private boolean externalTemplate;

    public MapperFactoryBean() {}

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
            this.sqlSessionTemplate.setDataSource(dataSource);
        }
    }

    /**
     * Set the SqlSessionFactory to work with.
     * <p>
     * This is a no-op if an external SqlSessionTemplate has been set.
     * 
     * @see #setSqlSessionTemplate
     */
    @Autowired(required=false)
    public final void setSqlSessionFactory(SqlSessionFactory sessionFactory) {
        if (!this.externalTemplate) {
            this.sqlSessionTemplate.setSqlSessionFactory(sessionFactory);
        }
    }

    /**
     * Set the SqlSessionTemplate for this DAO explicitly, as an alternative to specifying a
     * SqlSessionFactory.
     * 
     * @see #setSqlSessionFactory
     */
    @Autowired(required=false)
    public final void setSqlSessionTemplate(SqlSessionTemplate sessionTemplate) {
        this.sqlSessionTemplate = sessionTemplate;
        this.externalTemplate = true;
    }

    public void setMapperInterface(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    public void setAddToConfig(boolean addToConfig) {
        this.addToConfig = addToConfig;
    }

    public void checkDaoConfig() {
        Assert.notNull(mapperInterface, "Property 'mapperInterface' is required");
        Assert.notNull(sqlSessionTemplate, "Property 'sqlSessionTemplate' is required");

        SqlSessionFactory sqlSessionFactory = sqlSessionTemplate.getSqlSessionFactory();
        if (addToConfig && !sqlSessionFactory.getConfiguration().hasMapper(mapperInterface)) {
            sqlSessionFactory.getConfiguration().addMapper(mapperInterface);
        }
        
        sqlSessionTemplate.afterPropertiesSet();
    }

    public T getObject() throws Exception {
        return sqlSessionTemplate.getMapper(mapperInterface);
    }

    public Class<T> getObjectType() {
        return mapperInterface;
    }

    public boolean isSingleton() {
        return true;
    }

}
