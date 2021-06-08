<a name="Sample_Code"></a>
# Sample Code

<span class="label important">NOTE</span>
See [JPetstore 6 demo](https://github.com/mybatis/jpetstore-6) to know about how to use Spring with a full web application server.

You can check out sample code from the MyBatis-Spring [repo](https://github.com/mybatis/spring/tree/master/src/test/java/org/mybatis/spring/sample):

Any of the samples can be run with JUnit 5.

The sample code shows a typical design where a transactional service gets domain objects from a data access layer.

`FooService.java` acts as the service:

```java
@Transactional
public class FooService {

  private final UserMapper userMapper;

  public FooService(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  public User doSomeBusinessStuff(String userId) {
    return this.userMapper.getUser(userId);
  }

}
```

It is a transactional bean, so when the method is called, the transaction is started and the transaction is committed when the method ends without throwing an uncaught exception.
Notice that transactional behaviour is configured with the `@Transactional` attribute. This is not required; any other way provided by Spring can be used to demarcate your transactions.

This service calls a data access layer built with MyBatis.
This layer consists on a just an interface `UserMapper.java` that will be used with a dynamic proxy built by MyBatis at runtime and injected into the service by Spring.

```java
public interface UserMapper {

  User getUser(String userId);

}
```

Note that, for the sake of simplicity we used the interface `UserMapper.java` for the DAO scenario where a DAO is built with an interface and a implementation though in this case it would have been more adequate to use an interface called `UserDao.java` instead.

We will see different ways to find the mapper interface, register it to Spring and inject it into the service bean:

## Scenarios

| Sample test | Description |
| --- | --- |
| `SampleMapperTest.java` | Shows you the base configuration based on a `MapperFactoryBean` that will dynamically build an implementation for `UserMapper` |
| `SampleScannerTest.java` | Shows how to use the `MapperScannerConfigurer` so all the mappers in a project are autodiscovered. |
| `SampleSqlSessionTest.java` | Shows how to hand code a DAO using a Spring managed `SqlSession` and providing your own implementation `UserDaoImpl.java`. |
| `SampleEnableTest.java` | Shows how to use Spring's `@Configuration` with the `@MapperScann` annotation so mappers are autodiscovered. |
| `SampleNamespaceTest.java` | Shows how to use the custom MyBatis XML namespace. |
| `SampleJavaConfigTest.java` | Shows how to use Spring's `@Configuration` to create MyBatis beans manually. |
| `SampleJobJavaConfigTest.java` | Shows how to use `ItemReader` and `ItemWriter` on Spring Batch using Java Configuration. |
| `SampleJobXmlConfigTest.java` | Shows how to use `ItemReader` and `ItemWriter` on Spring Batch using XML Configuration. |

Please take a look at the different `applicationContext.xml` files to see MyBatis-Spring in action.
