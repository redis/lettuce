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
  public void saveWithNoFailure(User user) {
    userMapperMaster.save(user);
    userMapperSlave.save(user);
  }
  
  @Transactional
  public void saveWithFailure(User user) {
    userMapperMaster.save(user);
    userMapperSlave.save(user);
    throw new RuntimeException("failed!");
  }

  public boolean checkUserExists(int id) {
    if (userMapperMaster.select(id) != null) return true;
    if (userMapperSlave.select(id) != null) return true;
    return false;
  }
}
