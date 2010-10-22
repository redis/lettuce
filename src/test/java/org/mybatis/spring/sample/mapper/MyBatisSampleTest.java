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
package org.mybatis.spring.sample.mapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Example of MyBatis-Spring integration usage
 *
 * @version $Id: MyBatisSampleTest.java 2697 2010-10-14 13:04:41Z eduardo.macarron $
 */
@ContextConfiguration(locations = {"classpath:org/mybatis/spring/sample/mapper/applicationContext.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class MyBatisSampleTest {

    @Autowired
    private FooService fooServiceWithMapperFactoryBean;
    @Autowired
    private FooService fooServiceWithMapperAnnotation;
    @Autowired
    private FooService fooServiceWithDaoSupport;
    @Autowired
    private FooService fooServiceWithSqlSession;

    public void setFooServiceWithMapperFactoryBean(FooService fooServiceWithMapperFactoryBean) {
        this.fooServiceWithMapperFactoryBean = fooServiceWithMapperFactoryBean;
    }

    public void setFooServiceWithMapperAnnotation(FooService fooServiceWithMapperAnnotation) {
        this.fooServiceWithMapperAnnotation = fooServiceWithMapperAnnotation;
    }

    public void setFooServiceWithDaoSupport(FooService fooServiceWithDaoSupport) {
        this.fooServiceWithDaoSupport = fooServiceWithDaoSupport;
    }

    public void setFooServiceWithSqlSession(FooService fooServiceWithSqlSession) {
        this.fooServiceWithSqlSession = fooServiceWithSqlSession;
    }
    
    @Test
    public void testFooServiceWithMapperFactoryBean() throws Exception {
        User user = this.fooServiceWithMapperFactoryBean.doSomeBusinessStuff("u1");
        Assert.assertNotNull(user);
        Assert.assertEquals("Pocoyo",user.getName());
    }

    @Test
    public void testFooServiceWithMapperAnnotation() throws Exception {
        User user = this.fooServiceWithMapperAnnotation.doSomeBusinessStuff("u1");
        Assert.assertNotNull(user);
        Assert.assertEquals("Pocoyo",user.getName());
    }
    
    @Test
    public void testFooServiceWithDaoSupport() throws Exception {
        User user = this.fooServiceWithDaoSupport.doSomeBusinessStuff("u1");
        Assert.assertNotNull(user);
        Assert.assertEquals("Pocoyo",user.getName());
    }
    
    @Test
    public void testFooServiceWithSqlSession() throws Exception {
        User user = this.fooServiceWithSqlSession.doSomeBusinessStuff("u1");
        Assert.assertNotNull(user);
        Assert.assertEquals("Pocoyo",user.getName());
    }    
}
