<a name="Spring_Batch"></a>
# Spring Batch

마이바티스 스프링 연동모듈의 1.1.0버전에서는 스프링 배치 애플리케이션을 만들기 위해 두개의 빈을 제공한다.
두개의 빈은 `MyBatisPagingItemReader` 와 `MyBatisCursorItemReader` 와 <code>MyBatisBatchItemWriter</code>이다.

또한 2.0.0 버전에서는 Java Configuration 을 지원하는 다음의 세 가지 Builder class 를 제공한다.
`MyBatisPagingItemReaderBuilder`, `MyBatisCursorItemReaderBuilder` 그리고 `MyBatisBatchItemWriterBuilder` 이다.

<span class="label important">중요</span>
이 문서는 [스프링 배치](http://static.springsource.org/spring-batch/) 에 대한 것으로 마이바티스 배치 `SqlSession` 을 다루지는 않는다.
배치 세션에 대해서는 [SqlSession 사용](sqlsession.html) 에서 좀더 다루었다.

# MyBatisPagingItemReader

이 빈은 마이바티스로 페이지를 처리하는 형태로 데이터베이스 데이터를 읽어오는 `ItemReader`이다.

요청된 데이터를 가져오기 위해 `setQueryId` 프로퍼티에 명시된 쿼리를 실행한다.
쿼리는 `setPageSize` 프로퍼티에 명시된 크기만큼 데이터를 가져오도록 실행된다.
`read()` 메서드를 사용하면 필요할 때 현재 위치에서 정해진 수 만큼 더 추가 데이터를 가져온다.
reader는 몇가지의 표준적인 쿼리 파라미터를 제공하고 명명된 쿼리의 SQL은 요청된 크기만큼의 데이터를 만들기 위해 파라미터의 일부 혹은 모두 사용한다.
여기서 사용가능한 파라미터이다.

* `_page`: 읽을 페이지 수(0부터 시작)
* `_pagesize`: 페이지의 크기, 이를테면 리턴하는 로우 수
* `_skiprows`: `_page` 와 `_pagesize`의 결과

각각의 파라미터는 selet구문에서 다음처럼 매핑될 수 있다.

```xml
<select id="getEmployee" resultMap="employeeBatchResult">
  SELECT id, name, job FROM employees ORDER BY id ASC LIMIT #{_skiprows}, #{_pagesize}
</select>
```

다음의 코드는 샘플 설정이다.

```xml
<bean id="reader" class="org.mybatis.spring.batch.MyBatisPagingItemReader">
  <property name="sqlSessionFactory" ref="sqlSessionFactory" />
  <property name="queryId" value="com.my.name.space.batch.EmployeeMapper.getEmployee" />
</bean>
```

```java
@Configuration
public class BatchAppConfig {
  @Bean
  public MyBatisPagingItemReader<Employee> reader() {
    return new MyBatisPagingItemReaderBuilder<Employee>()
        .sqlSessionFactory(sqlSessionFactory())
        .queryId("com.my.name.space.batch.EmployeeMapper.getEmployee")
        .build();
  }
}
```

**좀더 복잡한 예제를 보자.**

```xml
<bean id="dateBasedCriteriaReader"
  class="org.mybatis.spring.batch.MyBatisPagingItemReader"
  p:sqlSessionFactory-ref="batchReadingSessionFactory"
  p:parameterValues-ref="datesParameters"
  p:queryId="com.my.name.space.batch.ExampleMapper.queryUserInteractionsOnSpecificTimeSlot"
  p:pageSize="200"
  scope="step"/>
```
```xml
<util:map id="datesParameters" scope="step">
  <entry key="yesterday" value="#{jobExecutionContext['EXTRACTION_START_DATE']}"/>
  <entry key="today" value="#{jobExecutionContext['TODAY_DATE']}"/>
  <entry key="first_day_of_the_month" value="#{jobExecutionContext['FIRST_DAY_OF_THE_MONTH_DATE']}"/>
  <entry key="first_day_of_the_previous_month" value="#{jobExecutionContext['FIRST_DAY_OF_THE_PREVIOUS_MONTH_DATE']}"/>
</util:map>
```

```java
@Configuration
public class BatchAppConfig {
  @StepScope
  @Bean
  public MyBatisPagingItemReader<User> dateBasedCriteriaReader(
      @Value("#{@datesParameters}") Map<String, Object> datesParameters) throws Exception {
    return new MyBatisPagingItemReaderBuilder<User>()
        .sqlSessionFactory(batchReadingSessionFactory())
        .queryId("com.my.name.space.batch.ExampleMapper.queryUserInteractionsOnSpecificTimeSlot")
        .parameterValues(datesParameters)
        .pageSize(200)
        .build();
  }

  @StepScope
  @Bean
  public Map<String, Object> datesParameters(
      @Value("#{jobExecutionContext['EXTRACTION_START_DATE']}") LocalDate yesterday,
      @Value("#{jobExecutionContext['TODAY_DATE']}") LocalDate today,
      @Value("#{jobExecutionContext['FIRST_DAY_OF_THE_MONTH_DATE']}") LocalDate firstDayOfTheMonth,
      @Value("#{jobExecutionContext['FIRST_DAY_OF_THE_PREVIOUS_MONTH_DATE']}") LocalDate firstDayOfThePreviousMonth) {
    Map<String, Object> map = new HashMap<>();
    map.put("yesterday", yesterday);
    map.put("today", today);
    map.put("first_day_of_the_month", firstDayOfTheMonth);
    map.put("first_day_of_the_previous_month", firstDayOfThePreviousMonth);
    return map;
  }
}
```

앞의 예제와는 몇가지 차이점이 있다.

* `sqlSessionFactory`: reader에 별도로 구현한 sessionFactory를 지정할 수 있다. 이 옵션은 다양한 데이터베이스에서 데이터를 읽을때 유용하다.
* `queryId`: 여러개의 테이블에서 데이터를 읽어야 하고 서로 다른 쿼리를 사용한다면 서로다른 네임스페이스를 가진 매퍼 파일을 사용하는게 좋을수 있다. 쿼리를 알아볼때, 매퍼 파일의 네임스페이스를 잊지 말아야 한다.
* `parameterValues`: 이 맵을 사용해서 추가로 파라미터를 전달할 수 있다. 위 예제는 `jobExecutionContext`에서 값들을 가져오는 SpEL표현식을 사용하는 맵을 사용하고 있다.
  맵의 키는 매퍼파일에서 마이바티스가 사용할 것이다. (예: *yesterday* 는 `#{yesterday,jdbcType=TIMESTAMP}` 로 사용될수 있다.).
  맵과 reader 모두 `jobExecutionContext`에서 SpEL표현식을 사용하기 위해 `step` 스코프를 사용한다. 마이바티스의 타입핸들러가 제대로 설정이 되었다면 JodaTime날짜를 맵을 사용해서 파라미터로 넘길수 있다.
* `pageSize`: 배치가 청크크기가 지정된 형태로 처리가 되면 reader에 이 값을 전달하는게 적절하다.

## MyBatisCursorItemReader

이 빈은 cursor 를 사용하여 데이터베이스에서 레코드를 읽는 `ItemReader` 이다.

<span class="label important">중요</span>
이 빈을 사용하려면 최소한 MyBatis 3.4.0 이나 그 이상이어야 한다.

`setQueryId` 속성으로 지정된 쿼리를 실행하여 `selectCursor()` 메서드를 사용하여 요청 된 데이터를 검색한다. `read()` 메서드가 호출 될 때마다 요소가 더 이상 남아 있지 않을 때까지 cursor 의 다음 요소를 반환한다.

reader 는 별도의 connection 을 사용하므로 select 문은 step processing 일부로 생성된 트랜잭션에 속하지 않는다.

cursor 를 사용할 때 다음과 같이 일반 쿼리를 실행할 수 있다.

```xml
<select id="getEmployee" resultMap="employeeBatchResult">
  SELECT id, name, job FROM employees ORDER BY id ASC
</select>
```

아래는 샘플 설정이다.

```xml
<bean id="reader" class="org.mybatis.spring.batch.MyBatisCursorItemReader">
  <property name="sqlSessionFactory" ref="sqlSessionFactory" />
  <property name="queryId" value="com.my.name.space.batch.EmployeeMapper.getEmployee" />
</bean>
```

```java
@Configuration
public class BatchAppConfig {
  @Bean
  public MyBatisCursorItemReader<Employee> reader() {
    return new MyBatisCursorItemReaderBuilder<Employee>()
        .sqlSessionFactory(sqlSessionFactory())
        .queryId("com.my.name.space.batch.EmployeeMapper.getEmployee")
        .build();
  }
}
```

# MyBatisBatchItemWriter

모든 아이템을 배치로 구문을 일괄실행하기 위해 `SqlSessionTemplate`에서 배치로 작업을 처리하는 `ItemWriter`이다. `SqlSessionFactory`는 `BATCH` 실행자로 설정할 필요가 있다.

사용자는 `write()` 메서드가 호출될때 실행될 매핑구문 아이디를 제공해야 한다. `write()` 메서드는 트랜잭션내에서 호출되는 것으로 예상된다.

다음의 코드는 샘플설정이다.

```xml
<bean id="writer" class="org.mybatis.spring.batch.MyBatisBatchItemWriter">
  <property name="sqlSessionFactory" ref="sqlSessionFactory" />
  <property name="statementId" value="com.my.name.space.batch.EmployeeMapper.updateEmployee" />
</bean>
```
```java
@Configuration
public class BatchAppConfig {
  @Bean
  public MyBatisBatchItemWriter<User> writer() {
    return new MyBatisBatchItemWriterBuilder<User>()
        .sqlSessionFactory(sqlSessionFactory())
        .statementId("com.my.name.space.batch.EmployeeMapper.updateEmployee")
        .build();
  }
}
```


**Converting a item that read using ItemReader to an any parameter object:**

By default behavior, the `MyBatisBatchItemWriter` passes a item that read using `ItemReader` (or convert by `ItemProcessor`) to the MyBatis(`SqlSession#update()`) as the parameter object.
If you want to customize a parameter object that passes to the MyBatis, you can realize to use the `itemToParameterConverter` option.
For example using `itemToParameterConverter` option, you can passes any objects other than the item object to the MyBatis.
Follows below a sample:

At first, you create a custom converter class (or factory method). The following sample uses a factory method.

```java
public class ItemToParameterMapConverters {
  public static <T> Converter<T, Map<String, Object>> createItemToParameterMapConverter(String operationBy, LocalDateTime operationAt) {
    return item -> {
      Map<String, Object> parameter = new HashMap<>();
      parameter.put("item", item);
      parameter.put("operationBy", operationBy);
      parameter.put("operationAt", operationAt);
      return parameter;
    };
  }
}
```

At next, you write a sql mapping.

```xml
<select id="createPerson" resultType="org.mybatis.spring.sample.domain.Person">
    insert into persons (first_name, last_name, operation_by, operation_at)
           values(#{item.firstName}, #{item.lastName}, #{operationBy}, #{operationAt})
</select>
```

At last, you configure the `MyBatisBatchItemWriter`.

```java
@Configuration
public class BatchAppConfig {
  @Bean
  public MyBatisBatchItemWriter<Person> writer() throws Exception {
    return new MyBatisBatchItemWriterBuilder<Person>()
        .sqlSessionFactory(sqlSessionFactory())
        .statementId("org.mybatis.spring.sample.mapper.PersonMapper.createPerson")
        .itemToParameterConverter(createItemToParameterMapConverter("batch_java_config_user", LocalDateTime.now()))
        .build();
  }
}
```

```xml
<bean id="writer" class="org.mybatis.spring.batch.MyBatisBatchItemWriter">
  <property name="sqlSessionFactory" ref="sqlSessionFactory"/>
  <property name="statementId" value="org.mybatis.spring.sample.mapper.PersonMapper.createPerson"/>
  <property name="itemToParameterConverter">
    <bean class="org.mybatis.spring.sample.config.SampleJobConfig" factory-method="createItemToParameterMapConverter">
      <constructor-arg type="java.lang.String" value="batch_xml_config_user"/>
      <constructor-arg type="java.time.LocalDateTime" value="#{T(java.time.LocalDateTime).now()}"/>
    </bean>
  </property>
</bean>
```

**여러개의 테이블에 데이터를 쓰려면 한꺼번에 처리할 수 있도록 만든 writer(몇가지 규칙을 가지고)를 사용하자.**

이 기능은 마이바티스 3.2이상에서만 사용할 수 있다. 이전의 버전에서는 예상과 다르게 동작하는데 그 내용은 [이슈](http://code.google.com/p/mybatis/issues/detail?id=741)를 참고하면 된다.

배치가 관계를 가지는 데이터나 여러개의 데이터베이스를 다루는 것처럼 복잡한 데이터를 작성할때 필요하다면 insert구문이 한개에 테이블에만 데이터를 넣을수 있다는 사실만 피하면 가능하기도 하다.
이런 복잡한 데이터를 처리하기 위해 writer가 작성하는 <em>아이템(Item)</em>을 준비해야 한다. 다음의 기술을 사용하면 단순한 관계를 가진 데이터나 관계가 없는 테이블을 처리하는 아이템에서 사용할 수 있다.

이러한 방법으로 스프링 배치 아이템은 모든 레코드를 *다룰것이다*.
여기에는 1:1 *관계*를 가지는 *InteractionMetadata*, 관계가 없는 두개의 로우는 *VisitorInteraction* 와 *CustomerInteraction*이 있다.
이 각각의 객체는 다음과 같이 볼수 있다.

```java
public class InteractionRecordToWriteInMultipleTables {
  private final VisitorInteraction visitorInteraction;
  private final CustomerInteraction customerInteraction;
  private final Interaction interaction;
  // ...
}
```
```java
public class Interaction {
  private final InteractionMetadata interactionMetadata;
}
```

그리고 스프링 설정에는 각각의 레코드를 처리하기위해 특별히 설정된 전용(delegates) writer를 사용하는 `CompositeItemWriter`가 있다.

```xml
<bean id="interactionsItemWriter" class="org.springframework.batch.item.support.CompositeItemWriter">
  <property name="delegates">
    <list>
      <ref bean="visitorInteractionsWriter"/>
      <ref bean="customerInteractionsWriter"/>

      <!-- Order is important -->
      <ref bean="interactionMetadataWriter"/>
      <ref bean="interactionWriter"/>
    </list>
  </property>
</bean>
```
```java
@Bean
@Configuration
public class BatchAppConfig {
  public CompositeItemWriter<?> interactionsItemWriter() {
    CompositeItemWriter compositeItemWriter = new CompositeItemWriter();
    List<ItemWriter<?>> writers = new ArrayList<>(4);
    writers.add(visitorInteractionsWriter());
    writers.add(customerInteractionsWriter());
    writers.add(interactionMetadataWriter());
    writers.add(interactionWriter());
    compositeItemWriter.setDelegates(writers);
    return compositeItemWriter;
  }
}
```

각각의 전용(delegate) writer는 필요할 만큼 설정할 수 있다. 예를들면 *Interaction* 과 *InteractionMetadata*를 위한 writer가 있다.

```xml
<bean id="interactionMetadataWriter"
  class="org.mybatis.spring.batch.MyBatisBatchItemWriter"
  p:sqlSessionTemplate-ref="batchSessionTemplate"
  p:statementId="com.my.name.space.batch.InteractionRecordToWriteInMultipleTablesMapper.insertInteractionMetadata"/>
```
```xml
<bean id="interactionWriter"
  class="org.mybatis.spring.batch.MyBatisBatchItemWriter"
  p:sqlSessionTemplate-ref="batchSessionTemplate"
  p:statementId="com.my.name.space.batch.InteractionRecordToWriteInMultipleTablesMapper.insertInteraction"/>
```

reader와 동일하게 `statementId`는 네임스페이스를 가진 구문을 가리킬수 있다.

매퍼 파일에서 구문은 다음과 같은 방법으로 각각의 레코드를 위해 만들어져있다.

```xml
<insert id="insertInteractionMetadata"
  parameterType="com.my.batch.interactions.item.InteractionRecordToWriteInMultipleTables"
  useGeneratedKeys="true"
  keyProperty="interaction.interactionMetadata.id"
  keyColumn="id">
  <!-- the insert statement using #{interaction.interactionMetadata.property,jdbcType=...} -->
</insert>
```
```xml
<insert id="insertInteraction"
  parameterType="com.my.batch.interactions.item.InteractionRecordToWriteInMultipleTables"
  useGeneratedKeys="true"
  keyProperty="interaction.id"
  keyColumn="id">
  <!--
   the insert statement using #{interaction.property,jdbcType=...} for regular properties
   and #{interaction.interactionMetadata.property,jdbcType=...} for the InteractionMetadata property
  -->
</insert>
```

먼저 `insertInteractionMetadata`가 호출될것이고 update구문은 jdbc드라이버에 의해(`keyProperty` 와 `keyColumn`) 생성된 아이디들을 리턴하기 위해 설정되었다.
`InteractionMetadata` 객체가 이 쿼리에 의해 업데이트되면 다음의 쿼리는 `insertInteraction`를 통해 상위객체인 `Interaction`를 작성하기 위해 사용될수 있다.

***방금 언급한 내용에 관련하여 JDBC드라이버가 똑같이 동작하지 않을수 있다.
이 글을 쓰는 시점에 H2 드라이버 1.3.168버전(`org.h2.jdbc.JdbcStatement#getGeneratedKeys`를 보라)만 배치모드에서 마지막 인덱스를 리턴한다.
반면에 MySQL 드라이버는 기대한 것과 동일하게 동작하고 모든 아이디를 리턴한다.***
