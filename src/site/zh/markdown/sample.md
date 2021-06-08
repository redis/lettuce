<a name="示例代码"></a>
# 示例代码

<span class="label important">提示</span>
查看 [JPetstore 6 demo](https://github.com/mybatis/jpetstore-6) 了解如何在完整的 Web 应用服务器上使用 Spring。

您可以在 MyBatis-Spring 的 [代码仓库](https://github.com/mybatis/spring/tree/master/src/test/java/org/mybatis/spring/sample) 中查看示例代码：

所有示例都能在 JUnit 5 下运行。

示例代码演示了事务服务从数据访问层获取域对象的典型设计。

`FooService.java` 作为服务:

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

它是一个事务 bean，所以当调用它的任何方法时，事务被启动，在方法结束且没有抛出任何未经检查的异常的时候事务将会被提交。注意，事务的行为可以通过 `@Transactional` 的属性进行配置。这不是必需的；你可以使用 Spring 提供的任何其他方式来划分你的事务范围。

此服务调用使用 MyBatis 构建的数据访问层.。该层只包含一个接口，`UserMapper.java`，这将被 MyBatis 构建的动态代理使用，在运行时通过 Spring 注入到服务之中。

```java
public interface UserMapper {

  User getUser(String userId);

}
```

注意，为了简单起见，我们使用了接口 `UserMapper.java`。在使用 DAO 的场景中，一个 DAO 类应该分为一个接口和一个实现类。回到这个例子里，准确来说，这个接口应该叫 `UserDao.java` 。

我们将看到不同的方法来发现映射器接口，将其注册到 Spring 并将其注入到服务 bean 中：

## 测试场景

| 样例测试 | 描述 |
| --- | --- |
| `SampleMapperTest.java` | 演示基于 `MapperFactoryBean` 的基本配置，这将动态构建 `UserMapper` 的一个实现。 |
| `SampleScannerTest.java` | 演示如何使用 `MapperScannerConfigurer` 来自动发现项目中所有的映射器。 |
| `SampleSqlSessionTest.java` | 演示如何基于 Spring 管理的 `SqlSession` 手动编写 DAO，并在 `UserDaoImpl.java` 中提供你自己的实现。 |
| `SampleEnableTest.java` | 演示如何使用 Spring 的 `@Configuration` 和 `@MapperScann` 注解来自动发现 mappers. |
| `SampleNamespaceTest.java` | 演示如何使用自定义 MyBatis XML 命名空间. |
| `SampleJavaConfigTest.java` | 演示如何基于 Spring 的 `@Configuration` 来手工创建 MyBatis 的 bean。 |
| `SampleJobJavaConfigTest.java` | 演示如何在 Java 配置中使用 Spring Batch 中的 `ItemReader` 和 `ItemWriter`。 |
| `SampleJobXmlConfigTest.java` | 演示如何在 XML 配置中使用 Spring Batch 中的 `ItemReader` 和 `ItemWriter`。 |

查看不同的 `applicationContext.xml` 文件以了解 MyBatis-Spring 在实践中是如何运用的。

