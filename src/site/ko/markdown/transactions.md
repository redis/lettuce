<a name="Transactions"></a>
# Transactions

마이바티스 스프링 연동모듈을 사용하는 중요한 이유중 하나는 마이바티스가 스프링 트랜잭션에 자연스럽게 연동될수 있다는 것이다.
마이바티스에 종속되는 새로운 트랜잭션 관리를 만드는 것보다는 마이바티스 스프링 연동모듈이 스프링의 `DataSourceTransactionManager`과 융합되는 것이 좋다.

스프링 트랜잭션 관리자를 한번 설정하면, 대개의 경우처럼 스프링에서 트랜잭션을 설정할 수 있다. `@Transactional` 애노테이션과 AOP스타일의 설정 모두 지원한다.
하나의 `SqlSession`객체가 생성되고 트랜잭션이 동작하는 동안 지속적으로 사용될것이다. 세션은 트랜잭션이 완료되면 적절히 커밋이 되거나 롤백될것이다.

마이바티스 스프링 연동모듈은 한번 셋업되면 트랜잭션을 투명하게 관리한다. DAO클래스에 어떠한 추가적인 코드를 넣을 필요가 없다.

<a name="configuration"></a>
## 표준 설정

스프링 트랜잭션을 가능하게 하려면, 스프링 설정파일에 `DataSourceTransactionManager`를 생성하자.

```xml
<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
  <constructor-arg ref="dataSource" />
</bean>
```

```java
@Configuration
public class DataSourceConfig {
  @Bean
  public DataSourceTransactionManager transactionManager() {
    return new DataSourceTransactionManager(dataSource());
  }
}
```

명시된 `DataSource`는 스프링을 사용할때 일반적으로 사용한다면 어떠한 JDBC `DataSource`도 될수 있다. JNDI룩업을 통해 얻어진 `DataSource`뿐 아니라 커넥션 풀링 기능도 포함한다.

트랜잭션 관리자에 명시된 `DataSource`가 `SqlSessionFactoryBean`을 생성할때 사용된 것과 **반드시** 동일한 것이어야 하는 것만 꼭 기억하자. 그렇지 않으면 트랜잭션 관리가 제대로 되지 않을것이다.

<a name="container"></a>
## Container Managed Transactions

만약에 JEE컨테이너를 사용하고 스프링을 컨테이너 관리 트랜잭션(container managed transactions, CMT)에 두려한다면, 스프링은 `JtaTransactionManager`나 그 하위 클래스로 설정되어야 한다.
이러한 설정을 가장 쉽게 하는 방법은 스프링의 트랜잭션 네임스페이스 or `JtaTransactionManagerFactoryBean` 를 사용하는 것이다.

```xml
<tx:jta-transaction-manager />
```
```java
@Configuration
public class DataSourceConfig {
  @Bean
  public JtaTransactionManager transactionManager() {
    return new JtaTransactionManagerFactoryBean().getObject();
  }
}
```

이 설정에서, 마이바티스는 CMT와 함께 설정된 스프링 트랜잭션 리소스처럼 동작할 것이다. 스프링은 이미 설정된 트랜잭션을 사용해서 `SqlSession`을 이미 동작중인 트랜잭션에 넣을 것이다.
시작된 트랜잭션이 없고 트랜잭션이 필요한 경우라면 스프링은 새로운 컨테이너 관리 트랜잭션을 시작할 것이다.

CMT는 사용하지만 스프링 트랜잭션 관리를 원하지 **않는다면** 어떠한 스프링 트랜잭션 관리자를 설정해서도 **안되고** 마이바티스 `ManagedTransactionFactory`를 사용하기 위해 `SqlSessionFactoryBean`를 설정**해야만** 한다.

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
  <property name="dataSource" ref="dataSource" />
  <property name="transactionFactory">
    <bean class="org.apache.ibatis.transaction.managed.ManagedTransactionFactory" />
  </property>
</bean>
```

```java
@Configuration
public class MyBatisConfig {
  @Bean
  public SqlSessionFactory sqlSessionFactory() {
    SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
    factoryBean.setDataSource(dataSource());
    factoryBean.setTransactionFactory(new ManagedTransactionFactory());
    return factoryBean.getObject();
  }
}
```

<a name="programmatic"></a>
## Programmatic Transaction Management

마이바티스 `SqlSession`은 트랜잭션을 제어하는 메서드를 제공한다. 하지만 마이바티스 스프링 연동모듈은 빈을 스프링이 관리하는 `SqlSession`이나 스프링이 관리하는 매퍼에 주입한다.
이 말은 스프링이 **항상** 트랜잭션을 관리한다는 뜻이다.

스프링이 관리하는 `SqlSession`에서는 `SqlSession.commit()`, `SqlSession.rollback()` 또는 `SqlSession.close()` 메서드를 호출할수가 없다.
그럼에도 불구하고 이 메서드들을 사용하면 `UnsupportedOperationException` 예외가 발생한다. 이러한 메서드는 주입된 매퍼 클래스에서는 사용할 수 없다.

JDBC연결의 자동커밋 설정을 어떻게 하더라도 스프링 트랜잭션 밖의 `SqlSession` 데이터 메서드나 매퍼 메서드의 실행은 자동으로 커밋된다.

트래잭션을 수동으로 제어하고자 한다면 [the Spring reference document(Data Access -Programmatic transaction management-)](https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#transaction-programmatic) 을 참고하자.
다음의 코드는 스프링 레퍼런스 에서 언급된 내용으로 `PlatformTransactionManager`를 사용해서 수동으로 트랜잭션을 다루는 방법을 보여준다.

```java
public class UserService {
  private final PlatformTransactionManager transactionManager;
  public UserService(PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }
  public void createUser() {
    TransactionStatus txStatus =
        transactionManager.getTransaction(new DefaultTransactionDefinition());
    try {
      userMapper.insertUser(user);
    } catch (Exception e) {
      transactionManager.rollback(txStatus);
      throw e;
    }
    transactionManager.commit(txStatus);
  }
}
```
You can omit to call the `commit` and `rollback` method using the `TransactionTemplate`.

```java
public class UserService {
  private final PlatformTransactionManager transactionManager;
  public UserService(PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }
  public void createUser() {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.execute(txStatus -> {
      userMapper.insertUser(user);
      return null;
    });
  }
}
```

이 코드는 매퍼를 사용하지만 `SqlSession`를 사용해도 잘 동작한다.
