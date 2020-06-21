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
package org.mybatis.spring.sample;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

abstract class AbstractSampleJobTest {

  @Autowired
  private JobLauncherTestUtils jobLauncherTestUtils;

  @Autowired
  private NamedParameterJdbcTemplate jdbcTemplate;

  @Test
  void testJob() throws Exception {

    JobExecution jobExecution = jobLauncherTestUtils.launchJob();

    Assertions.assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode());

    List<Map<String, Object>> persons = jdbcTemplate.queryForList("SELECT * FROM persons ORDER BY person_id",
        EmptySqlParameterSource.INSTANCE);
    Assertions.assertEquals(5, persons.size());
    Object operationBy = persons.get(0).get("OPERATION_BY");
    Object operationAt = persons.get(0).get("OPERATION_AT");
    {
      Map<String, Object> person = persons.get(0);
      Assertions.assertEquals(0, person.get("PERSON_ID"));
      Assertions.assertEquals("Pocoyo", person.get("FIRST_NAME"));
      Assertions.assertNull(person.get("LAST_NAME"));
      Assertions.assertEquals(getExpectedOperationBy(), operationBy);
      Assertions.assertNotNull(operationAt);
    }
    {
      Map<String, Object> person = persons.get(1);
      Assertions.assertEquals(1, person.get("PERSON_ID"));
      Assertions.assertEquals("Pato", person.get("FIRST_NAME"));
      Assertions.assertNull(person.get("LAST_NAME"));
      Assertions.assertEquals(operationBy, person.get("OPERATION_BY"));
      Assertions.assertEquals(operationAt, person.get("OPERATION_AT"));
    }
    {
      Map<String, Object> person = persons.get(2);
      Assertions.assertEquals(2, person.get("PERSON_ID"));
      Assertions.assertEquals("Eli", person.get("FIRST_NAME"));
      Assertions.assertNull(person.get("LAST_NAME"));
      Assertions.assertEquals(operationBy, person.get("OPERATION_BY"));
      Assertions.assertEquals(operationAt, person.get("OPERATION_AT"));
    }
    {
      Map<String, Object> person = persons.get(3);
      Assertions.assertEquals(3, person.get("PERSON_ID"));
      Assertions.assertEquals("Valentina", person.get("FIRST_NAME"));
      Assertions.assertNull(person.get("LAST_NAME"));
      Assertions.assertEquals(operationBy, person.get("OPERATION_BY"));
      Assertions.assertEquals(operationAt, person.get("OPERATION_AT"));
    }
    {
      Map<String, Object> person = persons.get(4);
      Assertions.assertEquals(4, person.get("PERSON_ID"));
      Assertions.assertEquals("Taro", person.get("FIRST_NAME"));
      Assertions.assertEquals("Yamada", person.get("LAST_NAME"));
      Assertions.assertEquals(operationBy, person.get("OPERATION_BY"));
      Assertions.assertEquals(operationAt, person.get("OPERATION_AT"));
    }
  }

  protected abstract String getExpectedOperationBy();

  @Configuration
  static class LocalContext {
    @Bean
    JobLauncherTestUtils jobLauncherTestUtils() {
      return new JobLauncherTestUtils();
    }

    @Bean
    NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
      return new NamedParameterJdbcTemplate(dataSource);
    }
  }

}
