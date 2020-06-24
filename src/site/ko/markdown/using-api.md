<a name="MyBatis_API_사용하기"></a>
# MyBatis API 사용하기

MyBatis-Spring 연동 모듈을 사용해도 계속해서 MyBatis API를 직접 사용할 수 있다. `SqlSessionFactoryBean`을 사용해서 스프링에서 `SqlSessionFactory`를 생성하고 팩토리를 사용하면 된다.

```java
public class UserDaoImpl implements UserDao {
  // SqlSessionFactory would normally be set by SqlSessionDaoSupport
  private final SqlSessionFactory sqlSessionFactory;

  public UserDaoImpl(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
  }

  public User getUser(String userId) {
    // note standard MyBatis API usage - opening and closing the session manually
    try (SqlSession session = sqlSessionFactory.openSession()) {
      return session.selectOne("org.mybatis.spring.sample.mapper.UserMapper.getUser", userId);
    }
  }
}
```

이 방법은 **신중히** 사용하자. 왜냐하면 잘못사용하면 런타임 에러나 데이터 문제등을 야기할수 있기 때문이다. API를 직접 사용할때는 다음의 규칙들에 유의해야 한다.

* 스프링 트랜잭션에 **속하지 않고** 별도의 트랜잭션에서 동작한다.
* `SqlSession`이 스프링 트랜잭션 관리자가 사용하는 `DataSource`를 사용하고 이미 트랜잭션이 동작하고 있다면 이 코드는 예외를 **발생시킬 것이다**.
* 마이바티스의 `DefaultSqlSession`은 쓰레드에 안전하지 않다. 빈에 이 객체를 주입하면 아마도 에러를 **발생시킬 수 있다**.
* `DefaultSqlSession`을 사용해서 생성한 매퍼 또한 쓰레드에 안전하지 않다. 이렇게 만든 매퍼를 빈에 주입하면 에러를 **발생시킬 수 있다**.
* `SqlSession`은 **항상** 마지막에 `close()` 메서드를 호출해야 한다.
