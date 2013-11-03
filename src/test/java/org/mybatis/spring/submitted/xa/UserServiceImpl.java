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

  @Transactional
  public void save(User user) {
    userMapperMaster.save(user);
    user.setId(user.getId() + 1);
    userMapperSlave.save(user);
  }
}
