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
package org.mybatis.spring.submitted.xa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

  @Autowired
  private UserMapper userMapperMaster;
  @Autowired
  private UserMapper userMapperSlave;

  @Override
  @Transactional
  public void saveWithNoFailure(User user) {
    userMapperMaster.save(user);
    userMapperSlave.save(user);
  }

  @Override
  @Transactional
  public void saveWithFailure(User user) {
    userMapperMaster.save(user);
    userMapperSlave.save(user);
    throw new RuntimeException("failed!");
  }

  @Override
  public boolean checkUserExists(int id) {
    if (userMapperMaster.select(id) != null)
      return true;
    if (userMapperSlave.select(id) != null)
      return true;
    return false;
  }
}
