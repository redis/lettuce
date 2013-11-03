package org.mybatis.spring.submitted.xa;

public interface UserService {
  
  void saveWithNoFailure(User user);

  void saveWithFailure(User user);

  boolean checkUserExists(int id);

}
