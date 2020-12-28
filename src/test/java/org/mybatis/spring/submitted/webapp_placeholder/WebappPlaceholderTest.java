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
package org.mybatis.spring.submitted.webapp_placeholder;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@SpringJUnitConfig(locations = "file:src/test/java/org/mybatis/spring/submitted/webapp_placeholder/spring.xml")
class WebappPlaceholderTest {

  @Autowired
  private SqlSessionFactory sqlSessionFactory;

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void testName() {
    Assertions.assertEquals(0, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());
    Mapper mapper = applicationContext.getBean(Mapper.class);
    assertThat(mapper).isNotNull();
    Assertions.assertEquals(1, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());
  }
}
