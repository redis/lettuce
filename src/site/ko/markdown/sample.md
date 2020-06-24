<a name="샘플_코드"></a>
# 샘플 코드

<span class="label important">중요</span>
전체 웹 애플리케이션 서버에서 Spring을 사용하는 방법을 알고 싶다면 [JPetstore 6 demo](https://github.com/mybatis/jpetstore-6) 를 참조하십시오.

MyBatis-Spring [repository](https://github.com/mybatis/spring/tree/master/src/test/java/org/mybatis/spring/sample) 에서 샘플 코드를 확인할 수 있다.

모든 샘플은 JUnit5 에서 실행할 수 있다.

샘플 코드는 트랜잭션 서비스가 data access layer 에서 도메인 개체를 가져 오는 일반적인 디자인을 보여준다.

다음 `FooService.java` 는 서비스처럼 작동한다.

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

이것은 트랜잭션 빈이다. 따라서 어떤 메서드든 실행이 되면 트랜잭션이 시작되고 예외가 발생하지 않았을 때 커밋된다. 트랜잭션은 `@Transactional` annotation 을 통해 설정할 수 있다.
이것은 필수가 아니다. Spring이 제공하는 다른 방법을 사용하여 트랜잭션을 구분할 수 있다.

이 서비스는 MyBatis로 이루어진 DAO layer 를 호출한다. 이 layer는 런타임시 MyBatis에 의해 작성되고 Spring에 의해 서비스에 주입되는 동적 프록시와 함께 사용되는 `UserMapper.java` 인터페이스로 구성된다.

```java
public interface UserMapper {

  User getUser(String userId);

}
```

단순함을 위해서 DAO가 인터페이스와 그 구현체로 만들어진 DAO 시나리오를 위해 <code>UserMapper.java</code> 인터페이스를 사용했지만, 이 경우 대신 `UserDao.java`라는 인터페이스를 사용하는 것이 더 적절하다.

매퍼 인터페이스를 찾고 Spring에 등록하고 서비스 빈에 주입하는 여러 가지 방법을 살펴본다.

## 시나리오

| 샘플 테스트 | 설명 |
| --- | --- |
| `SampleMapperTest.java` | `UserMapper` 구현체를 동적으로 빌드 할 `MapperFactoryBean`에 기반한 기본 구성을 보여준다. |
| `ampleScannerTest.java` | `MapperScannerConfigurer` 를 사용하여 어떻게 프로젝트의 모든 매퍼들을 자동으로 검색되도록 하는 방법을 보여준다. |
| `SampleSqlSessionTest.java` | Spring에서 관리하는 `SqlSession`을 사용하여 DAO를 코딩하고 자체적인 구현체인 `UserDaoImpl.java` 를 제공하는 방법을 보여준다. |
| `SampleEnableTest.java` | 스프링의 `@Configuration`과 `@MapperScann` annotation을 함께 사용하여 매퍼를 자동으로 검색하는 방법을 보여준다. |
| `SampleNamespaceTest.java` | 커스텀 MyBatis XML 네임스페이스를 사용하는 방법을 보여준다. |
| `SampleJavaConfigTest.java` | 스프링의 `@Configuration`을 사용하여 MyBatis 빈들을 수동적으로 생성하는 방법을 보여준다. |
| `SampleJobJavaConfigTest.java` | Java Configuration을 이용하여 Spring Batch에서 어떻게 `ItemReader`과 `ItemWriter`를 사용하는지 보여준다. |
| `SampleJobXmlConfigTest.java` | XML Configuration을 이용하여 Spring Batch에서 어떻게 `ItemReader`과 `ItemWriter`를 사용하는지 보여준다. |

MyBatis-Spring이 실제로 어떻게 다르게 작동하는지 보려면 `applicationContext.xml` 파일을 살펴보십시오.
