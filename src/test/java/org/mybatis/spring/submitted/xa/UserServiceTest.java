package org.mybatis.spring.submitted.xa;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(value = SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:org/mybatis/spring/submitted/xa/applicationContext.xml")
public class UserServiceTest {
  
  @Autowired
  private UserService userService;

  @Test
  public void testSave() {
    User user = new User(1, "Pocoyo");
    userService.save(user);
  }
}
