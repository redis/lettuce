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
package org.mybatis.spring.sample.mapper;

import org.mybatis.spring.annotation.Mapper;

/**
 * 
 * A org.mybatis.spring.sample mapper
 * @Mapper annotation is optional. It is just needed for mapper scanning
 * value "userMapper" is also optional, if no value is setted, the mapper will be
 * registered in Spring with its classname "org.mybatis.spring.sample.mapper.UserMapper"
 *
 * @version $Id: UserMapper.java 2697 2010-10-14 13:04:41Z eduardo.macarron $
 */
@Mapper("userMapper")
public interface UserMapper {

    User getUser(String userId);

}
