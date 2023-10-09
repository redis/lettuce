<a name="Getting_Started"></a>
# Getting Started

This chapter will show you in a few steps how to install and setup MyBatis-Spring and how to build a simple transactional application.

## Installation

To use the MyBatis-Spring module, you just need to include the `mybatis-spring-${project.version}.jar` file and its dependencies in the classpath.

If you are using Maven just add the following dependency to your pom.xml:

```xml
<dependency>
  <groupId>org.mybatis</groupId>
  <artifactId>mybatis-spring</artifactId>
  <version>${project.version}</version>
</dependency>
```

## Quick Setup

To use MyBatis with Spring you need at least two things defined in the Spring application context:
an `SqlSessionFactory` and at least one mapper interface.

In MyBatis-Spring, an `SqlSessionFactoryBean` is used to create an `SqlSessionFactory`. To configure the factory bean, put the following in the Spring configuration file:

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
  <property name="dataSource" ref="dataSource" />
</bean>
```

```java
@Configuration
public class MyBatisConfig {
  @Bean
  public SqlSessionFactory sqlSessionFactory() throws Exception {
    SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
    factoryBean.setDataSource(dataSource());
    return factoryBean.getObject();
  }
}
```

Notice that the `SqlSessionFactory` requires a `DataSource`.  This can be any `DataSource` and should be configured just like any other Spring database connection.

Assume you have a mapper interface defined like the following:

```java
public interface UserMapper {
  @Select("SELECT * FROM users WHERE id = #{userId}")
  User getUser(@Param("userId") String userId);
}
```

This interface is added to Spring using a `MapperFactoryBean` like the following:

```xml
<bean id="userMapper" class="org.mybatis.spring.mapper.MapperFactoryBean">
  <property name="mapperInterface" value="org.mybatis.spring.sample.mapper.UserMapper" />
  <property name="sqlSessionFactory" ref="sqlSessionFactory" />
</bean>
```

Note that the mapper class specified **must** be an interface, not an actual implementation class. In this example, annotations are used to specify the SQL, but a MyBatis mapper XML file could also be used.

Once configured, you can inject mappers directly into your business/service objects in the same way you inject any other Spring bean.
The `MapperFactoryBean` handles creating an `SqlSession` as well as closing it.
If there is a Spring transaction in progress, the session will also be committed or rolled back when the transaction completes.
Finally, any exceptions will be translated into Spring `DataAccessException`s.

If you use the Java Configuration:

```java
@Configuration
public class MyBatisConfig {
  @Bean
  public UserMapper userMapper() throws Exception {
    SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory());
    return sqlSessionTemplate.getMapper(UserMapper.class);
  }
}
```

Calling MyBatis data methods is now only one line of code:

```java
public class FooServiceImpl implements FooService {

  private final UserMapper userMapper;

  public FooServiceImpl(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  public User doSomeBusinessStuff(String userId) {
    return this.userMapper.getUser(userId);
  }
}
```
