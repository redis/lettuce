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

import org.mybatis.spring.support.SqlSessionDaoSupport;

/**
 * This DAO extends SqlSessionDaoSupport and uses SqlSessionTemplate instead of MyBatis API
 * SqlSessions are handled by Spring.
 * MyBatis exceptions are translated to Spring Data Exceptions.
 *
 * @version $Id: UserMapperTemplateImpl.java 2444 2010-09-15 07:38:37Z simone.tripodi $
 */
public class UserMapperTemplateImpl extends SqlSessionDaoSupport implements UserMapper {

    public User getUser(String userId) {
        return (User) getSqlSessionTemplate().selectOne("org.mybatis.spring.sample.mapper.UserMapper.getUser", userId);
    }

}
