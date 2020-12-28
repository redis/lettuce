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
package org.mybatis.spring.sample.service;

import org.mybatis.spring.sample.dao.UserDao;
import org.mybatis.spring.sample.domain.User;
import org.springframework.transaction.annotation.Transactional;

/**
 * BarService simply receives a userId and uses a dao to get a record from the database.
 */
@Transactional
public class BarService {

  private final UserDao userDao;

  public BarService(UserDao userDao) {
    this.userDao = userDao;
  }

  public User doSomeBusinessStuff(String userId) {
    return this.userDao.getUser(userId);
  }

}
