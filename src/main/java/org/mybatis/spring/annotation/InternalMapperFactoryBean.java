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
* MapperScanner registers new mappers creating new MapperFactoryBeans 
* but if there are other MapperFactoryBeans defined in the context their 
* BeanDefinitions are created before the MapperScanner is executed. 
* It seems that the MapperFactoryBean's bean definition created by the MapperScanner 
* is not the same * that the one created by Spring. That makes autowire fail and Spring 
* will throw an Exception indicating that a either sqlSessionFactory or sqlSessionTemplate 
* is null and its required.
* <p>
* Seems that the problem is solved if another factory bean class is registered
* because bean definitions will not collide.
* <p> 
* So this class just extends MapperFactoryBean and does nothing more.
* <p> 
* This class is only for the MapperScanner and should never be used by final users.
* 
* @see org.mybatis.spring.MapperFactoryBean
* @version $Id$
*/

public class InternalMapperFactoryBean<T> extends MapperFactoryBean<T> {

}
