<a name="매퍼_주입"></a>
# 매퍼 주입

`SqlSessionDaoSupport` 나 `SqlSessionTemplate` 를 직접적으로 사용하는 데이터 접근 객체(DAO)를 생성하기 보다, 마이바티스 스프링 연동모듈은 다른 빈에 직접 주입할 수 있는 쓰레드에 안전한 매퍼를 생성할 수 있다.

```xml
<bean id="fooService" class="org.mybatis.spring.sample.service.FooServiceImpl">
  <constructor-arg ref="userMapper" />
</bean>
```

한번 주입하고나면 매퍼는 애플리케이션 로직에서 사용할수 있는 준비가 된다.

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

이 코드를 보면 `SqlSession`이나 마이바티스 객체가 보이지 않는다. 게다가 세션을 생성하거나 열고 닫을필요도 없어보인다. 마이바티스 스프링 연동모듈이 알아서 처리할 것이다.

<a name="register"></a>
## 매퍼 등록하기

매퍼를 등록하는 방법은 기존의 전통적인 XML설정법을 사용하거나 새로운 3.0 이후의 자바설정(일명 `@Configuration`)을 사용하느냐에 따라 다르다.

### XML설정 사용

매퍼는 다음처럼 XML설정파일에 `MapperFactoryBean`을 두는 것으로 스프링에 등록된다.

```xml
<bean id="userMapper" class="org.mybatis.spring.mapper.MapperFactoryBean">
  <property name="mapperInterface" value="org.mybatis.spring.sample.mapper.UserMapper" />
  <property name="sqlSessionFactory" ref="sqlSessionFactory" />
</bean>
```

UserMapper가 매퍼 인터페이스와 같은 경로의 클래스패스에 마이바티스 XML매퍼 파일을 가지고 있다면 `MapperFactoryBean`이 자동으로 파싱할것이다.
매퍼 XML파일을 다른 클래스패스에 두는게 아니라면 마이바티스 설정파일에 매퍼를 지정할 필요가 없다. 좀더 세부적인 정보는 `SqlSessionFactoryBean`의 [`configLocation`](factorybean.html) 프로퍼티를 살펴보자.

`MapperFactoryBean`은 `SqlSessionFactory` 나 `SqlSessionTemplate`가 필요하다. `sqlSessionFactory` 와 `sqlSessionTemplate` 프로퍼티를 셋팅하면 된다.
둘다 셋팅하면 `SqlSessionFactory`가 무시된다. 세션 팩토리 셋은 `SqlSessionTemplate`이 필요하고 `MapperFactoryBean`는 팩토리를 사용할것이다.

### 자바설정 사용

```java
@Configuration
public class MyBatisConfig {
  @Bean
  public MapperFactoryBean<UserMapper> userMapper() throws Exception {
    MapperFactoryBean<UserMapper> factoryBean = new MapperFactoryBean<>(UserMapper.class);
    factoryBean.setSqlSessionFactory(sqlSessionFactory());
    return factoryBean;
  }
}
```

<a name="scan"></a>
## 매퍼 스캔

하나씩 매퍼를 모두 등록할 필요가 없다. 대신 클래스패스를 지정해서 마이바티스 스프링 연동모듈의 자동스캔기능을 사용할 수 있다.

자동스캔을 사용하는데는 3가지 방법이 있다.

* `<mybatis:scan/>` 엘리먼트 사용
* `@MapperScan` 애노테이션 사용
* 스프링 XML파일을 사용해서 `MapperScannerConfigurer`를 등록

`<mybatis:scan/>` 와 `@MapperScan` 모두 마이바티스 스프링 연동모듈 1.2.0에서 추가된 기능이다. `@MapperScan` 은 스프링 버전이 3.1이상이어야 한다.

Since 2.0.2, mapper scanning feature support a option (`lazy-initialization`) that control lazy initialization enabled/disabled of mapper bean.
The motivation for adding this option is supporting a lazy initialization control feature supported by Spring Boot 2.2.
The default of this option is `false` (= not use lazy initialization). If developer want to use lazy initialization for mapper bean, it should be set to the `true` expressly.

<span class="label important">IMPORTANT</span>
If use the lazy initialization feature, the developer need to understand following limitations. If any of following conditions are matches, usually　the lazy initialization feature cannot use on your application.

* When refers to the statement of **other mapper** using `<association>`(`@One`) and `<collection>`(`@Many`)
* When includes to the fragment of **other mapper** using `<include>`
* When refers to the cache of **other mapper** using `<cache-ref>`(`@CacheNamespaceRef`)
* When refers to the result mapping of **other mapper** using `<select resultMap="...">`(`@ResultMap`)

<span class="label important">NOTE</span>
However, It become possible to use it by simultaneously initializing dependent beans using `@DependsOn`(Spring's feature) as follow:

```java
@DependsOn("vendorMapper")
public interface GoodsMapper {
  // ...
}
```

Since 2.0.6, the develop become can specified scope of mapper using mapper scanning feature option(`default-scope`) and scope annotation(`@Scope`, `@RefreshScope`, etc ...).
The motivation for adding this option is supporting the `refresh` scope provided by the Spring Cloud. The default of this option is empty (= equiv to specify the `singleton` scope).
The `default-scope` apply to the mapper bean(`MapperFactoryBean`) when scope of scanned bean definition is `singleton`(default scope) and create a scoped proxy bean for scanned mapper when final scope is not `singleton`.

### \<mybatis:scan\>

`<mybatis:scan/>` XML엘리먼트는 스프링에서 제공하는 `<context:component-scan/>` 엘리먼트와 매우 유사한 방법으로 매퍼를 검색할 것이다.

샘플 XML설정을 아래에서 볼수 있다.

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:mybatis="http://mybatis.org/schema/mybatis-spring"
  xsi:schemaLocation="
  http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
  http://mybatis.org/schema/mybatis-spring http://mybatis.org/schema/mybatis-spring.xsd">

  <mybatis:scan base-package="org.mybatis.spring.sample.mapper" />

  <!-- ... -->

</beans>
```

`base-package` 속성은 매퍼 인터페이스 파일이 있는 가장 상위 패키지를 지정하면 된다. 세미콜론이나 콤마를 구분자로 사용해서 한개 이상의 패키지를 셋팅할 수 있다.
매퍼는 지정된 패키지에서 재귀적으로 하위 패키지를 모두 검색할 것이다.

`<mybatis:scan/>`이 자동으로 주입할 수 있는 `MapperFactoryBean`를 생성하기 때문에 `SqlSessionFactory` 나 `SqlSessionTemplate` 를 명시할 필요가 없다.
하지만 한개 이상의 `DataSource`를 사용한다면 자동주입이 생각한데로 동작하지 않을수도 있다. 이 경우 사용할 빈 이름을 지정하기 위해 `factory-ref` 나 `template-ref` 속성을 사용할수 있다.

`<mybatis:scan/>`은 마커(marker) 인터페이스나 애노테이션을 명시해서 생성되는 매퍼를 필터링할수도 있다. `annotation` 프로퍼티는 검색할 애노테이션을 지정한다.
`marker-interface` 프로퍼티는 검색할 상위 인터페이스를 지정한다. 이 두개의 프로퍼티를 모두 지정하면, 매퍼는 두 조건을 모두 만족하는 인터페이스만을 추가한다.
디폴트로 이 두가지 프로퍼티는 모두 null이다. 그래서 base-package프로퍼티에 설정된 패키지 아래 모든 인터페이스가 매퍼로 로드될 것이다.

발견된 매퍼는 자동검색된 컴포넌트를 위한 스프링의 디폴트 명명규칙 전략(see [the Spring reference document(Core Technologies -Naming autodetected components-)](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans-scanning-name-generator) 을 사용해서 빈이름이 명명된다.
빈 이름을 정하는 애노테이션이 없다면 매퍼의 이름에서 첫글자를 소문자로 변환한 형태로 빈 이름을 사용할 것이다. `@Component` 나 JSR-330의 `@Named` 애노테이션이 있다면 애노테이션에 정의한 이름을 그대로 사용할 것이다.
`annotation` 프로퍼티를 `org.springframework.stereotype.Component`, `javax.inject.Named`(자바SE 1.6을 사용한다면) 또는 개발자가 스스로 작성한 애노테이션으로 셋팅할 수 있다.
그러면 애노테이션은 마커와 이름을 제공하는 역할로 동작할 것이다.

<span class="label important">중요</span>
`<context:component-scan/>` 가 매퍼를 검색해서 등록을 하지 못할수도 있다. 매퍼는 인터페이스고 스프링에 빈으로 등록하기 위해서는 각각의 인터페이스를 찾기 위해 스캐너가 `MapperFactoryBean` 를 생성하는 방법을 알아야만 한다.

### @MapperScan

`@Configuration` 라고 불리는 스프링의 자바설정을 사용한다면 `<mybatis:scan/>`보다는 `@MapperScan`를 사용하길 선호할것이다.

`@MapperScan` 애노테이션은 다음처럼 사용된다.

```java
@Configuration
@MapperScan("org.mybatis.spring.sample.mapper")
public class AppConfig {
  // ...
}
```

애노테이션은 앞서 본 `<mybatis:scan/>` 에서 설명하는 것과 동일하게 동작한다. `markerInterface` 와 `annotationClass` 프로퍼티를 사용해서 마커 인터페이스와 애노테이션 클래스를 명시하게 한다.
`sqlSessionFactory` 와 `sqlSessionTemplate` 프로퍼티를 사용해서 `SqlSessionFactory` 나 `SqlSessionTemplate`을 제공할 수도 있다.

<span class="label important">NOTE</span>
Since 2.0.4, If `basePackageClasses` or `basePackages` are not defined, scanning will occur from the package of the class that declares this annotation.

### MapperScannerConfigurer

`MapperScannerConfigurer`는 평범한 빈처럼 XML애플리케이션 컨텍스트에 포함된 `BeanDefinitionRegistryPostProcessor` 이다. `MapperScannerConfigurer`를 셋업하기 위해 다음의 스프링 설정을 추가하자.

```xml
<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
  <property name="basePackage" value="org.mybatis.spring.sample.mapper" />
</bean>
```

`sqlSessionFactory` 나 `sqlSessionTemplate`를 지정할 필요가 있다면 빈참조가 아닌 **빈이름**이 필요하다. `value` 프로퍼티는 빈 이름을 지정하고 `ref` 는 빈 참조를 지정하기 때문에 `value` 프로퍼티를 사용하자.

```xml
<property name="sqlSessionFactoryBeanName" value="sqlSessionFactory" />
```

<span class="label important">중요</span>
`sqlSessionFactoryBean` 과 `sqlSessionTemplateBean` 프로퍼티는 마이바티스 스프링 연동모듈 1.0.2 버전 이상에서만 사용이 가능하다.
하지만 `MapperScannerConfigurer`는 잦은 에러를 발생시키는 `PropertyPlaceholderConfigurer`보다 앞서 실행되기 때문에 이 프로퍼티들은 사용하지 말길 바란다(deprecated).
대신 새롭게 추가된 프로퍼티인 `sqlSessionFactoryBeanName` 과 `sqlSessionTemplateBeanName` 를 사용하도록 권한다.
