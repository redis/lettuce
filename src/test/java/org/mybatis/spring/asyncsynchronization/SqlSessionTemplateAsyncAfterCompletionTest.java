/**
 *    Copyright 2010-2017 the original author or authors.
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
package org.mybatis.spring.asyncsynchronization;

import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mybatis.spring.SqlSessionTemplateTest;


/**
 * 
 * The same test as original but afterCompletion is being called on a separate thread
 * 
 * @author Alex Rykov
 *
 */
@Ignore // FIXME: Enable after migrate BMUnitRunner to BMUnitExtension
@RunWith(BMUnitRunner.class)
@BMRule(name = "proxy synchronizations", targetClass = "TransactionSynchronizationManager", targetMethod = "registerSynchronization(TransactionSynchronization)", helper = "org.mybatis.spring.asyncsynchronization.AsyncAfterCompletionHelper", action = "$1=createSynchronizationWithAsyncAfterComplete($1)")
public class SqlSessionTemplateAsyncAfterCompletionTest extends SqlSessionTemplateTest {
}
