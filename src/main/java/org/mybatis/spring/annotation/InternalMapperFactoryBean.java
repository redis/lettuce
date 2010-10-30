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
* {@link MapperScannerPostProcessor} registers new mappers creating new {@link MapperFactoryBean}s 
* but if there are other {@link MapperFactoryBean}s defined in the context their 
* bean definitions are created before the {@link MapperScannerPostProcessor} is executed. 
* It seems that the {@link MapperFactoryBean}'s bean definition created by the {@link MapperScannerPostProcessor}
* is not the same that the one created by Spring. That makes autowire fail and an
* exception will be thrown indicating that sqlSessionFactory property is null and its required.
* <p>
* Seems that the problem is solved if another factory bean class is registered
* because bean definitions will be different and will not collide.
* <p> 
* So this class just extends {@link MapperFactoryBean} and does nothing more.
* <p> 
* This class is only for the {@link MapperScannerPostProcessor} and should never be used by final users.
* 
* @see org.mybatis.spring.MapperFactoryBean
* @see org.mybatis.spring.annotation.MapperScannerPostProcessor
* @version $Id$
*/
public class InternalMapperFactoryBean<T> extends MapperFactoryBean<T> {

}
