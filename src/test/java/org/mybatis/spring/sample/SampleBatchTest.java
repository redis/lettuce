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
package org.mybatis.spring.sample;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.sample.dao.UserDaoImpl;
import org.mybatis.spring.sample.domain.User;
import org.mybatis.spring.sample.service.FooService;
import org.apache.ibatis.session.ExecutorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Example of MyBatis-Spring batch integration usage 
 *
 * @version $Id: MyBatisSampleTest.java 2697 2010-10-14 13:04:41Z eduardo.macarron $
 */
@ContextConfiguration(locations = {"classpath:org/mybatis/spring/sample/applicationContext-batch.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class SampleBatchTest {

    @Autowired
    private FooService fooService;

    // needed only to verify that the ExecutorType is BATCH
    @Autowired
    private UserDaoImpl userMapperImpl;
    
    public void setFooService(FooService fooService) {
        this.fooService = fooService;
    }

    @Test
    public void testFooService() throws Exception {
        User user = this.fooService.doSomeBusinessStuff("u1");
        assertNotNull(user);
        assertEquals("Pocoyo", user.getName());
        assertEquals(ExecutorType.BATCH,
                ((SqlSessionTemplate) userMapperImpl.getSqlSession())
                .getExecutorType());
    }
}
