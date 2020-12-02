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

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig({ AbstractSampleJobTest.LocalContext.class, SampleJobXmlConfigTest.LocalContext.class })
class SampleJobXmlConfigTest extends AbstractSampleJobTest {

  @Override
  protected String getExpectedOperationBy() {
    return "batch_xml_config_user";
  }

  @ImportResource("classpath:org/mybatis/spring/sample/config/applicationContext-job.xml")
  @Configuration
  static class LocalContext {
  }

}
