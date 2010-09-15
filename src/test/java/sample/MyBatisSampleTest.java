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
package sample;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Example of MyBatis-Spring integration usage
 *
 * @version $Id$
 */
@ContextConfiguration(locations = {"classpath:sample/context.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class MyBatisSampleTest {

    @Autowired
    private FooService fooService1;
    @Autowired
    private FooService fooService2;
    @Autowired
    private FooService fooService3;

    public void setFooService1(FooService fooService1) {
        this.fooService1 = fooService1;
    }

    public void setFooService2(FooService fooService2) {
        this.fooService2 = fooService2;
    }

    public void setFooService3(FooService fooService3) {
        this.fooService3 = fooService3;
    }

    @Test
    public void testFooService1() throws Exception {
        User user = this.fooService1.doSomeBusinessStuff("u1");
        Assert.assertNotNull(user);
        Assert.assertEquals("Pocoyo",user.getName());
    }

    @Test
    public void testFooService2() throws Exception {
        User user = this.fooService2.doSomeBusinessStuff("u1");
        Assert.assertNotNull(user);
        Assert.assertEquals("Pocoyo",user.getName());
    }
    
    @Test
    public void testFooService3() throws Exception {
        User user = this.fooService3.doSomeBusinessStuff("u1");
        Assert.assertNotNull(user);
        Assert.assertEquals("Pocoyo",user.getName());
    }
}
