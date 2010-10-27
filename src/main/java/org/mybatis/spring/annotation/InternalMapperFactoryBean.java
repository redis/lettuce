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

package org.mybatis.spring.annotation;

import org.mybatis.spring.MapperFactoryBean;

/**
* MapperScanner does not work if there is also a MapperFactoryBean on the same context
* the problem is that autowiring does not work and a Exception is thrown indicating that a either 
* sqlSessionFactory or sqlSessionTemplate is null and its required
* Seems that the problem is solved if another different factory bean class is registered
* So this class just extends MapperFactoryBean and does nothing more
* 
* @see org.mybatis.spring.MapperFactoryBean
* @version $Id$
*/

public class InternalMapperFactoryBean<T> extends MapperFactoryBean<T> {

}
