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
package org.mybatis.spring.sample.service;

import org.mybatis.spring.sample.User;
import org.springframework.transaction.annotation.Transactional;

/**
 * FooService acts as a business service. 
 * 
 * All calls to any method of FooService are transactional.
 *
 * @version $Id: FooService.java 2654 2010-10-09 17:34:50Z eduardo.macarron $
 */
@Transactional
public interface FooService {

    User doSomeBusinessStuff(String userId);

}
