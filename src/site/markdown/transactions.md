<a name="Transactions"></a>
# Transactions

One of the primary reasons for using MyBatis-Spring is that it allows MyBatis to participate in Spring transactions.
Rather than create a new transaction manager specific to MyBatis,  MyBatis-Spring leverages the existing `DataSourceTransactionManager` in Spring.
      
Once a Spring transaction manager is configured, you can configure transactions in Spring as you normally would. Both `@Transactional` annotations and AOP style configurations are supported.
A single `SqlSession` object will be created and used for the duration of the transaction. This session will be committed or rolled back as appropriate when then transaction completes.

MyBatis-Spring will transparently manage transactions once they are set up. There is no need for additional code in your DAO classes.

<a name="configuration"></a>
## Standard Configuration

To enable Spring transaction processing, simply create a `DataSourceTransactionManager` in your Spring configuration file:

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

The `DataSource` specified can be any JDBC `DataSource` you would normally use with Spring. This includes connection pools as well as `DataSource`s obtained through JNDI lookups.

Note that the `DataSource` specified for the transaction manager **must** be the same one that is used to create the `SqlSessionFactoryBean` or transaction management will not work.

<a name="container"></a>
## Container Managed Transactions

If you are using a JEE container and would like Spring to participate in container managed transactions (CMT), then Spring should be configured with a `JtaTransactionManager` or one of its container specific subclasses.
The easiest way to do this is to use the Spring transaction namespace or the `JtaTransactionManagerFactoryBean`:

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

In this configuration, MyBatis will behave like any other Spring transactional resource configured with CMT. Spring will automatically use any existing container transaction and attach an `SqlSession` to it.
If no transaction is started and one is needed based on the transaction configuration, Spring will start a new container managed transaction.

Note that if you want to use CMT and do **not** want to use Spring transaction management, you **must not** configure any Spring transaction manager and you **must** also configure the `SqlSessionFactoryBean` to use the base MyBatis `ManagedTransactionFactory`:

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

MyBatis `SqlSession` provides you with specific methods to handle transactions programmatically.
But when using MyBatis-Spring your beans will be injected with a Spring managed `SqlSession` or a Spring managed mapper. That means that Spring will **always** handle your transactions.

You cannot call `SqlSession.commit()`, `SqlSession.rollback()` or `SqlSession.close()` over a Spring managed `SqlSession`.
If you try to do so, a `UnsupportedOperationException` exception will be thrown. Note these methods are not exposed in injected mapper classes.

Regardless of your JDBC connection's autocommit setting, any execution of a `SqlSession` data method or any call to a mapper method outside a Spring transaction will be automatically committed.

If you want to control your transactions programmatically please refer to [the Spring reference document(Data Access -Programmatic transaction management-)](https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#transaction-programmatic). This code shows how to handle a transaction manually using the `PlatformTransactionManager`.

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

Notice that this code uses a mapper, but it will also work with a `SqlSession`.
