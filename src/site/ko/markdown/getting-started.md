<a name="시작하기"></a>
# 시작하기

이 장은 마이바티스 스프링 연동모듈을 설치하고 셋팅하는 방법에 대해 간단히 보여준다. 그리고 트랜잭션을 사용하는 간단한 애플리케이션을 만드는 방법까지 다룰 것이다.

## 설치

마이바티스 스프링 연동모듈을 사용하기 위해서, 클래스패스에 `mybatis-spring-${project.version}.jar`를 포함시켜야 한다.

메이븐을 사용하고 있다면 pom.xml에 다음처럼 의존성을 추가하면 된다.

```xml
<dependency>
  <groupId>org.mybatis</groupId>
  <artifactId>mybatis-spring</artifactId>
  <version>${project.version}</version>
</dependency>
```

## 빠른 설정

마이바티스를 스프링과 함께 사용하려면 스프링의 애플리케이션 컨텍스트에 적어도 두개를 정의해줄 필요가 있다. 
두가지는 `SqlSessionFactory`와 한개 이상의 매퍼 인터페이스이다.

마이바티스 스프링 연동모듈에서, `SqlSessionFactoryBean`은 `SqlSessionFactory`를 만들기 위해 사용된다. 팩토리 빈을 설정하기 위해, 스프링 설정파일에 다음 설정을 추가하자.

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

`SqlSessionFactory`는 `DataSource`를 필요로 하는 것을 알아둘 필요가 있다. 어떤 `DataSource`도 상관없지만 다른 스프링의 데이터베이스 연결과 동일하게 설정되어야 한다.

매퍼 인터페이스가 다음처럼 정의되었다고 가정해보자.

```java
public interface UserMapper {
  @Select("SELECT * FROM users WHERE id = #{userId}")
  User getUser(@Param("userId") String userId);
}
```

UserMapper인터페이스는 다음처럼 `MapperFactoryBean`을 사용해서 스프링에 추가된다.

```xml
<bean id="userMapper" class="org.mybatis.spring.mapper.MapperFactoryBean">
  <property name="mapperInterface" value="org.mybatis.spring.sample.mapper.UserMapper" />
  <property name="sqlSessionFactory" ref="sqlSessionFactory" />
</bean>
```

매퍼는 **반드시** 구현체 클래스가 아닌 인터페이스로 정의되어야 한다. 예를들어, 애노테이션이 SQL을 명시하기 위해 사용되지만 마이바티스 매퍼 XML파일 또한 사용될 수 있다.

한번만 설정하면, 다른 스프링 빈에 주입하는 같은 방법으로 비즈니스/서비스 객체에 매퍼를 직접 주입할 수 있다. `MapperFactoryBean`은 `SqlSession`을 생성하고 닫는 작업을 잘 다룬다.
실행중인 스프링 트랜잭션이 있다면, 트랜잭션이 완료되는 시점에 커밋이나 롤백이 될 것이다. 마지막으로 예외가 발생하면 스프링의 `DataAccessException`예외가 발생한다.

자바로 설정하면 다음과 같다.

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

마이바티스의 데이터 관련 메서드는 호출하는 것은 한줄이면 된다.

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
