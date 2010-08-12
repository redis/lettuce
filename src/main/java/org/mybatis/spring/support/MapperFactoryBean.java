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
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * BeanFactory that enables injection of iBatis Mapper Interfaces.
 * 
 * @author Eduardo Macarrón
 * @since 3.0
 * @see org.springframework.orm.ibatis3.SqlSessionTemplate
 * @version $Id$
 */
public class MapperFactoryBean <T> implements FactoryBean<T>, InitializingBean {

    private DataSource dataSource;

    private Class<T> mapperInterface;

    private SqlSessionFactory sqlSessionFactory;

    private SqlSessionTemplate sqlSessionTemplate;

    public MapperFactoryBean() {
        super();
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setMapperInterface(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(mapperInterface, "Property 'MapperInterface' is required");

        if (sqlSessionFactory == null && sqlSessionTemplate == null) {
            throw new IllegalArgumentException("Property 'sqlSessionFactory' is required");

        } else if (sqlSessionTemplate == null) {
            sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
        }

        if (dataSource != null) {
            sqlSessionTemplate.setDataSource(dataSource);
        }
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
