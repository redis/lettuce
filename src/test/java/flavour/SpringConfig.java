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
package flavour;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.MapperFactoryBean;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * 
 * @version $Id$
 */
@Configuration
public class SpringConfig {

    private final PooledDataSource dataSource;

    private final SqlSessionFactory sessionFactory;

    public SpringConfig() throws Exception {
        this.dataSource = new PooledDataSource("org.hsqldb.jdbcDriver", "jdbc:hsqldb:file:mybatisspring", "sa", "");
        this.dataSource.setDefaultAutoCommit(true);

        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setConfigLocation(new ClassPathResource("flavour/SpringMybatis.xml"));
        factoryBean.setDataSource(this.dataSource);
        this.sessionFactory = factoryBean.getObject();
    }

    protected <T> T getMapper(Class<T> mapperType) throws Exception {
        MapperFactoryBean<T> factory = new MapperFactoryBean<T>();
        factory.setMapperInterface(mapperType);
        factory.setSqlSessionFactory(this.sessionFactory);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Bean
    public WaterMapper waterMapper() throws Exception {
        return getMapper(WaterMapper.class);
    }

    @Bean
    public FlavourMapper flavourMapper() throws Exception {
        return getMapper(FlavourMapper.class);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        return this.sessionFactory;
    }

    /*
     * Use this datasource for an in-memory HSQL DB
     */
    @Bean
    public DataSource dataSource() throws Exception {
        return this.dataSource;
    }

}
