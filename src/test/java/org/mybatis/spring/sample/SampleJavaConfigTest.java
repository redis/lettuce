/**
 * Copyright 2010-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * MyBatis @Configuration style sample
 */
package org.mybatis.spring.sample;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.sample.config.SampleConfig;
import org.mybatis.spring.sample.domain.User;
import org.mybatis.spring.sample.service.FooService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = SampleConfig.class)
class SampleJavaConfigTest {

  @Autowired
  private FooService fooService;

  @Autowired
  private FooService fooServiceWithMapperFactoryBean;

  @Test
  void test() {
    User user = fooService.doSomeBusinessStuff("u1");
    assertThat(user.getName()).isEqualTo("Pocoyo");
  }

  @Test
  void testWithMapperFactoryBean() {
    User user = fooServiceWithMapperFactoryBean.doSomeBusinessStuff("u1");
    assertThat(user.getName()).isEqualTo("Pocoyo");
  }

}
