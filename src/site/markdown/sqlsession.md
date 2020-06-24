<a name="Using_an_SqlSession"></a>
# Using an SqlSession

In MyBatis you use the `SqlSessionFactory` to create an `SqlSession`.
Once you have a session, you use it to execute your mapped statements, commit or rollback connections and finally, when it is no longer needed, you close the session.
With MyBatis-Spring you don't need to use `SqlSessionFactory` directly because your beans can be injected with a thread safe `SqlSession` that automatically commits, rollbacks and closes the session based on Spring's transaction configuration.

## SqlSessionTemplate

`SqlSessionTemplate` is the heart of MyBatis-Spring. It implements `SqlSession` and is meant to be a drop-in replacement for any existing use of `SqlSession` in your code.
`SqlSessionTemplate` is thread safe and can be shared by multiple DAOs or mappers.

When calling SQL methods, including any method from Mappers returned by `getMapper()`, `SqlSessionTemplate` will ensure that the `SqlSession` used is the one associated with the current Spring transaction.
In addition, it manages the session life-cycle, including closing, committing or rolling back the session as necessary. It will also translate MyBatis exceptions into Spring `DataAccessException`s.

`SqlSessionTemplate` should **always** be used instead of default MyBatis implementation `DefaultSqlSession` because the template can participate in Spring transactions and is thread safe for use by multiple injected mapper classes.
Switching between the two classes in the same application can cause data integrity issues.

A `SqlSessionTemplate` can be constructed using an `SqlSessionFactory` as a constructor argument.

```xml
<bean id="sqlSession" class="org.mybatis.spring.SqlSessionTemplate">
  <constructor-arg index="0" ref="sqlSessionFactory" />
</bean>
```

```java
@Configuration
public class MyBatisConfig {
  @Bean
  public SqlSessionTemplate sqlSession() throws Exception {
    return new SqlSessionTemplate(sqlSessionFactory());
  }
}
```

This bean can now be injected directly in your DAO beans. You need a `SqlSession` property in your bean like the following

```java
public class UserDaoImpl implements UserDao {

  private SqlSession sqlSession;

  public void setSqlSession(SqlSession sqlSession) {
    this.sqlSession = sqlSession;
  }

  public User getUser(String userId) {
    return sqlSession.selectOne("org.mybatis.spring.sample.mapper.UserMapper.getUser", userId);
  }
}
```

And inject the `SqlSessionTemplate` as follows

```xml
<bean id="userDao" class="org.mybatis.spring.sample.dao.UserDaoImpl">
  <property name="sqlSession" ref="sqlSession" />
</bean>
```

`SqlSessionTemplate` has also a constructor that takes an `ExecutorType` as an argument. This allows you to construct, for example, a batch `SqlSession` by using the following in Spring's configuration file:

```xml
<bean id="sqlSession" class="org.mybatis.spring.SqlSessionTemplate">
  <constructor-arg index="0" ref="sqlSessionFactory" />
  <constructor-arg index="1" value="BATCH" />
</bean>
```

```java
@Configuration
public class MyBatisConfig {
  @Bean
  public SqlSessionTemplate sqlSession() throws Exception {
    return new SqlSessionTemplate(sqlSessionFactory(), ExecutorType.BATCH);
  }
}
```

Now all your statements will be batched so the following could be coded in a DAO

```java
public class UserService {
  private final SqlSession sqlSession; 
  public UserService(SqlSession sqlSession) {
    this.sqlSession = sqlSession;
  }
  public void insertUsers(List<User> users) {
    for (User user : users) {
      sqlSession.insert("org.mybatis.spring.sample.mapper.UserMapper.insertUser", user);
    }
  }
}
```

Note that this configuration style only needs to be used if the desired execution method differs from the default set for the `SqlSessionFactory`.

The caveat to this form is that there **cannot** be an existing transaction running with a different ExecutorType when this method is called.
Either ensure that calls to `SqlSessionTemplate`s with different executor types run in a separate transaction (e.g. with `PROPAGATION_REQUIRES_NEW`) or completely outside of a transaction.

## SqlSessionDaoSupport

`SqlSessionDaoSupport` is an abstract support class that provides you with a `SqlSession`. Calling `getSqlSession()` you will get a `SqlSessionTemplate` which can then be used to execute SQL methods, like the following:

```java
public class UserDaoImpl extends SqlSessionDaoSupport implements UserDao {
  public User getUser(String userId) {
    return getSqlSession().selectOne("org.mybatis.spring.sample.mapper.UserMapper.getUser", userId);
  }
}
```

Usually `MapperFactoryBean` is preferred to this class, since it requires no extra code. But, this class is useful if you need to do other non-MyBatis work in your DAO and concrete classes are required.

`SqlSessionDaoSupport` requires either an `sqlSessionFactory` or an `sqlSessionTemplate` property to be set. If both properties are set, the `sqlSessionFactory` is ignored.

Assuming a class `UserDaoImpl` that subclasses `SqlSessionDaoSupport`, it can be configured in Spring like the following:

```xml
<bean id="userDao" class="org.mybatis.spring.sample.dao.UserDaoImpl">
  <property name="sqlSessionFactory" ref="sqlSessionFactory" />
</bean>
```
