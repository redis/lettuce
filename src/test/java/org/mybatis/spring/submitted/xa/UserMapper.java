package org.mybatis.spring.submitted.xa;

import org.apache.ibatis.annotations.Insert;

public interface UserMapper {
    @Insert("INSERT INTO USERS VALUES(#{id}, #{name})")
    void save(User user);
}
