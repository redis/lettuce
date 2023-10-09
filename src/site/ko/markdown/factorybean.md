<a name="SqlSessionFactoryBean"></a>
# SqlSessionFactoryBean

마이바티스만 사용하면, `SqlSessionFactory`는 `SqlSessionFactoryBuilder`를 사용해서 생성한다.
마이바티스 스프링 연동모듈에서는, `SqlSessionFactoryBean`가 대신 사용된다.

## 설정

팩토리 빈을 생성하기 위해, 스프링 XML설정파일에 다음설정을 추가하자.

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
  <property name="dataSource" ref="dataSource" />
</bean>
```

`SqlSessionFactoryBean` 은 스프링의 `FactoryBean` 인터페이스를 구현(see [the Spring documentation(Core Technologies -Customizing instantiation logic with a FactoryBean-)](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans-factory-extension-factorybean))한다는 점을 알아야 한다.
이 설정은 스프링이 `SqlSessionFactoryBean` 자체를 생성하는 것이 **아니라** 팩토리에서 `getObject()` 메서드를 호출한 결과를 리턴한다는 것을 의미한다.
이 경우, 스프링은 애플리케이션 시작 시점에 `SqlSessionFactory`를 빌드하고 `sqlSessionFactory` 라는 이름으로 저장한다. 자바에서 코드로 표현하면 아래와 같다.

```java
@Configuration
public class MyBatisConfig {
  @Bean
  public SqlSessionFactory sqlSessionFactory() {
    SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
    factoryBean.setDataSource(dataSource());
    return factoryBean.getObject();
  }
}
```

일반적인 마이바티스 스프링 사용법에서는, `SqlSessionFactoryBean`이나 관련된 `SqlSessionFactory`를 직접 사용할 필요가 없다.
대신 세션 팩토리가 `MapperFactoryBean`나 `SqlSessionDaoSupport`를 확장하는 다른 DAO에 주입될것이다.

## 속성

`SqlSessionFactory`는 JDBC `DataSource`의 필수 프로퍼티가 필요하다. 어떤 `DataSource`라도 상관없고 다른 스프링 데이터베이스 연결처럼 설정되어야만 한다.

하나의 공통적인 프로퍼티는 마이바티스 XML설정파일의 위치를 지정하기 위해 사용되는 `configLocation`이다. 이 프로퍼티를 설정하는 것은 디폴트 설정을 가진 마이바티스 설정을 변경해야 할 경우 뿐이다.
대개는 `<settings>`과 `<typeAliases>` 섹션을 변경하는 경우이다.

설정파일이 마이바티스 설정을 완전히 다룰 필요는 없다. 어떤 환경, 어떤 데이터소스 그리고 마이바티스 트랜잭션 관리자가 **무시**될수도 있다.
`SqlSessionFactoryBean` 는 필요에 따라 이 값들을 설정하여 자체적인 MyBatis `Environment` 를 만든다.

설정파일이 필요한 다른 이유는 마이바티스 XML파일이 매퍼 클래스와 동일한 클래스패스에 있지 않은 경우이다. 이 설정을 사용하면 두가지 옵션이 있다.
첫번째는 마이바티스 설정파일에 `<mappers>` 섹션을 사용해서 XML파일의 클래스패스를 지정하는 것이다. 두번째는 팩토리 빈의 `mapperLocations` 프로퍼티를 사용하는 것이다.

`mapperLocations` 프로퍼티는 매퍼에 관련된 자원의 위치를 나열한다. 이 프로퍼티는 마이바티스의 XML매퍼 파일들의 위치를 지정하기 위해 사용될 수 있다.
디렉터리 아래 모든 파일을 로드하기 위해 앤트(Ant) 스타일의 패턴을 사용할수도 있고 가장 상위 위치를 지정하는 것으로 재귀적으로 하위 경로를 찾도록 할수도 있다. 예를 들어보면 다음과 같다.

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
  <property name="dataSource" ref="dataSource" />
  <property name="mapperLocations" value="classpath*:sample/config/mappers/**/*.xml" />
</bean>
```

이 설정은 `sample.config.mappers` 패키지 아래와 그 하위 패키지를 모두 검색해서 마이바티스 매퍼 XML파일을 모두 로드할 것이다.

컨테이너 관리 트랜잭션을 사용하는 환경에서 필요한 하나의 프로퍼티는 `transactionFactoryClass` 이다. 이에 관련해서는 트랜잭션을 다루는 장에서 볼수 있다.

만약 multi-db 기능을 사용한다면 다음과 같이 `databaseIdProvider` 속성을 설정해야 한다.

```xml
<bean id="databaseIdProvider" class="org.apache.ibatis.mapping.VendorDatabaseIdProvider">
  <property name="properties">
    <props>
      <prop key="SQL Server">sqlserver</prop>
      <prop key="DB2">db2</prop>
      <prop key="Oracle">oracle</prop>
      <prop key="MySQL">mysql</prop>
    </props>
  </property>
</bean>
```
````xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
  <property name="dataSource" ref="dataSource" />
  <property name="mapperLocations" value="classpath*:sample/config/mappers/**/*.xml" />
  <property name="databaseIdProvider" ref="databaseIdProvider"/>
</bean>
````

<span class="label important">NOTE</span>
1.3.0 버전 부터 `configuration` 속성이 추가되었다. 다음과 같이 MyBatis XML 설정 파일없이 `Configuration` 인스턴스를 직접 지정할 수 있습니다.

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
  <property name="dataSource" ref="dataSource" />
  <property name="configuration">
    <bean class="org.apache.ibatis.session.Configuration">
      <property name="mapUnderscoreToCamelCase" value="true"/>
    </bean>
  </property>
</bean>
```
