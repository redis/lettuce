/*
 *    Copyright 2010-2012 The myBatis Team
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

import org.springframework.test.context.ContextConfiguration;

/**
 * Example of MyBatis-Spring batch integration usage.
 *
 * @version $Id$
 */
@ContextConfiguration(locations = { "classpath:org/mybatis/spring/sample/applicationContext-batch.xml" })
public class SampleBatchTest extends AbstractSampleTest {
    // Note this does not actually test batch functionality since FooService
    // only calls one DAO method. This class and associated Spring context
    // simply show that no implementation changes are needed to enable
    // different MyBatis configurations.
}
