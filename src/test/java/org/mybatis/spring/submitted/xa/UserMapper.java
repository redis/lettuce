package org.mybatis.spring.submitted.xa;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

public interface UserMapper {
  
    @Insert("INSERT INTO USERS VALUES(#{id}, #{name})")
    void save(User user);

    @Select("SELECT * FROM USERS WHERE ID=#{id}")
    User select(int id);

}
