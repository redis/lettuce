/*
 *    Copyright 2010-2013 The MyBatis Team
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
package org.mybatis.spring.submitted.autowire;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AutowireTest {
  private ClassPathXmlApplicationContext context;

  @Test
  public void shouldReturnMapper() {
    context = new ClassPathXmlApplicationContext("classpath:org/mybatis/spring/submitted/autowire/spring.xml");

    FooMapper fooMapper = (FooMapper) context.getBean("fooMapper");
    assertNotNull(fooMapper);
    fooMapper.executeFoo();

    BarMapper barMapper = (BarMapper) context.getBean("barMapper");
    assertNotNull(barMapper);
    barMapper.executeBar();

  }
}
