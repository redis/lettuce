<a name="Spring_Batch"></a>
# Spring Batch

As of version 1.1.0 MyBatis-Spring provides three beans for building Spring Batch applications: the `MyBatisPagingItemReader`, the `MyBatisCursorItemReader` and the `MyBatisBatchItemWriter`.
Also, As of version 2.0.0 provides three builder classes for supporting the Java Configuration: the `MyBatisPagingItemReaderBuilder`, the `MyBatisCursorItemReaderBuilder` and the `MyBatisBatchItemWriterBuilder`.

<span class="label important">NOTE</span>
This is about [Spring Batch](http://static.springsource.org/spring-batch/) and not about MyBatis batch SqlSessions. For information about batch sessions go to section [Using an SqlSession](sqlsession.html).

## MyBatisPagingItemReader

This bean is an `ItemReader` that reads records from a database in a paging fashion.

It executes the query specified as the `setQueryId` property to retrieve requested data.
The query is executed using paged requests of a size specified in `setPageSize` property.
Additional pages are requested when needed as `read()` method is called, returning an object corresponding to current position.

Some standard query parameters are provided by the reader and the SQL in the named query must use some or all of these parameters (depending on the SQL variant) to construct a result set of the required size.
The parameters are:

* `_page`: the page number to be read (starting at 0)
* `_pagesize`: the size of the pages, i.e. the number of rows to return
* `_skiprows`: the product of `_page` and `_pagesize`

And they could be mapped as the follow in a select statement:

```xml
<select id="getEmployee" resultMap="employeeBatchResult">
  SELECT id, name, job FROM employees ORDER BY id ASC LIMIT #{_skiprows}, #{_pagesize}
</select>
```

Follows below a sample configuration snippet:

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

**Explaining a more complex example:**

```xml
<bean id="dateBasedCriteriaReader" class="org.mybatis.spring.batch.MyBatisPagingItemReader"
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

The previous example makes use of a few different things:

* `sqlSessionFactory`: You can specify your own sessionFactory to the reader, it might be useful if you want to read from several databases.

* `queryId`: If the base code have several tables or databases to read from, and that you have different queries, it might be interesting to use different mapper files with different namespaces.
  so when referring to the query, don't forget about the namespace of the mapper file.

* `parameterValues`: You can pass additional parameters via this map, the example above uses a map that is build by spring using a SpEL expression taking values from the `jobExecutionContext`.
  The keys of the map will be used by MyBatis in the mapper file (ex: *yesterday* could be used as `#{yesterday,jdbcType=TIMESTAMP}`).
  Note that the map and the reader are both built in the `step` scope in order to be able to use the Spring EL expression with the `jobExecutionContext`.
  Also if MyBatis type handlers are correctly configured you can pass custom instances like the parameters of this map that are JodaTime dates.

* `pageSize`: If the batch flow is configured with chunk size, it is relevant to pass this information to the reader as well, which is done via this property.

## MyBatisCursorItemReader

This bean is an `ItemReader` that reads records from a database using a cursor.

<span class="label important">NOTE</span>
To use this bean you need at least MyBatis 3.4.0 or a newer version.

It executes the query specified as the `setQueryId` property to retrieve requested data by using the method `selectCursor()`.
Each time a `read()` method is called it will return the next element of the cursor until no more elements are left.

The reader will use a separate connection so the select statement does no participate in any transactions created as part of the step processing.

When using the cursor you can just execute a regular query:

```xml
<select id="getEmployee" resultMap="employeeBatchResult">
  SELECT id, name, job FROM employees ORDER BY id ASC
</select>
```

Follows below a sample configuration snippet:

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

## MyBatisBatchItemWriter

It is an `ItemWriter` that uses the batching features from `SqlSessionTemplate` to execute a batch of statements for all items provided.
The `SqlSessionFactory` needs to be configured with a `BATCH` executor.

When `write()` is called it executes the mapped statement indicated in the property `statementId`. It is expected that `write()` is called inside a transaction.

Follows below a sample configuration snippet:

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

By default behavior, the `MyBatisBatchItemWriter` passes a item that read using `ItemReader`  (or convert by `ItemProcessor`) to the MyBatis(`SqlSession#update()`) as the parameter object.
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

At last, you configure the <code>MyBatisBatchItemWriter</code>.

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

**Writing to different tables using composite writers (with some caveats):**

This technique can only be used with MyBatis 3.2+, as there was an [issue](http://code.google.com/p/mybatis/issues/detail?id=741) in previous versions that made the writer misbehave.

If the batch needs to write complex data, like records with associations, or even to different databases, then it is possible to work around the fact that insert statements only insert in one table.
In order to make it happen the batch have to prepare the *Item* to be written by the writer.
However depending on the constraints, opportunities or insight on the processed data it might be interesting to use the following technique.
The following trick can work on items with simple associations or just with unrelated tables.

In a processor craft the Spring Batch Item in such way it will *hold* all the different records.
Suppose for each Item there is an *Interaction* that have one association *InteractionMetadata*,
and two non associated rows *VisitorInteraction* and *CustomerInteraction*, the holder object will look like:

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

Then in the spring configuration there will be a `CompositeItemWriter` that will use delegate writers specifically configured for each kind of records.
Note that as the *InteractionMetadata* is an association in the example it will need to be written first so that Interaction can have the updated key.

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
@Configuration
public class BatchAppConfig {
  @Bean
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

Then each delegate writer will be configured as needed; for example for *Interaction* and *InteractionMetadata*:

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

Same as the reader the `statementId` can refer to the statement with the prefixed namespace.

Now in the mapper file the statement have to be crafted for each kind of records in the following way:

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

What's happening is that first the `insertInteractionMetadata` will be called, and the update statement is configured to return the ids created by the jdbc driver (`keyProperty` and `keyColumn`).
As the `InteractionMetadata` object were updated by this query the next query can be used to write the parent object `Interaction` via `insertInteraction`.

***However note that JDBC drivers don't behave the same in this regard. At the time of this writing the H2 driver 1.3.168 will only return the latest index even in BATCH mode (see `org.h2.jdbc.JdbcStatement#getGeneratedKeys`),
while the MySQL JDBC driver will behave as expected and return all the IDs.***
