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
package sample;

import org.mybatis.spring.annotation.Mapper;

/**
 * 
 * A sample mapper
 * @Mapper annotation is optional. It is just needed for mapper scanning
 * value "userMapper2" is also optional, if no value is setted, the mapper will be
 * registered in Spring with its classname "sample.UserMapper"
 *
 * @version $Id$
 */
@Mapper("userMapper2")
public interface UserMapper {

    User getUser(String userId);

}
