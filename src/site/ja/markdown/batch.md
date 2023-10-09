<a name="Spring_Batch"></a>
# Spring Batch

MyBatis-Spring 1.1.0 以降では、 Spring Batch を構築するための Bean として `MyBatisPagingItemReader` 、 `MyBatisCursorItemReader` 、 `MyBatisBatchItemWriter` が用意されています。
また、2.0.0 以降では、Java Configuration をサポートするための Builder クラスとして `MyBatisPagingItemReaderBuilder` 、 `MyBatisCursorItemReaderBuilder` 、 `MyBatisBatchItemWriterBuilder` が用意されています。

<span class="label important">NOTE</span>
ここで扱うのは [Spring Batch](http://static.springsource.org/spring-batch/) を使ったバッチ処理で、MyBatis の [`SqlSession`](sqlsession.html) を利用したバッチ処理ではありません。

## MyBatisPagingItemReader

この Bean は、MyBatis を利用してデータベースからページ単位でレコードを読み出す `ItemReader` です。

結果を取得する際は `setQueryId` プロパティで指定したクエリが実行されます。1ページあたりの件数は <code>setPageSize</code> プロパティで指定することができます。
`<code>read()` メソッドが呼び出されると、必要に応じて追加のページを取得するクエリが実行されます。
実行されるクエリでは、Reader によって提供されるページング処理を行う際に必要となるパラメーターを使って期待される結果を返す SQL 文を記述することになります（実際の SQL 文は方言依存です）。
提供されるパラメーターは次の通りです。

* `_page`: 取得対象のページ番号（最初のページは０
* `_pagesize`: １ページあたりの件数
* `_skiprows`: `_page` と `_pagesize` の積

これらのパラメーターは、例えば次のように SELECT 文中で指定することができます。

```xml
<select id="getEmployee" resultMap="employeeBatchResult">
  SELECT id, name, job FROM employees ORDER BY id ASC LIMIT #{_skiprows}, #{_pagesize}
</select>
```

設定例：

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

**さらに複雑な例：**

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

The previous example makes use of a few different things:

* `sqlSessionFactory`: あなた自身の sessionFactory を reader に指定することができます。複数のデータベースから読み取る場合は有用かも知れません。
* `queryId`: レコード取得時に実行されるクエリの ID を指定します。異なるネームスペースに属するクエリを指定する場合はネームスペースの指定を忘れないようにしてください。
* `parameterValues`: クエリ実行時に使用する追加のパラメーターを Map 形式で渡すことができます。上の例では Spring が `jobExecutionContext` から SpEL 式を使って取得した値をもとに構築した `Map` を指定しています。
  MyBatis の Mapper ファイルでは `Map` のキーをパラメーター名として指定します（例： *yesterday* を指定する場合は `#{yesterday,jdbcType=TIMESTAMP}` のように指定します）。
  `jobExecutionContext` と Spring EL 式を利用するため、`Map` および `Reader` はどちらも `step` スコープ内で構築されているという点に注意してください。
  また、MyBatis の `TypeHandler` が正しく設定されていれば、この例のように JodaTime のようなカスタムのインスタンスを引数として渡すこともできます。
* `pageSize`: バッチ処理のチャンクサイズを指定します。

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

`SqlSessionTemplate` のバッチ機能を使って渡された一連のアイテムを処理する `ItemWriter` です。 `SqlSessionFactory` は `ExecutorType.BATCH` を使って設定する必要があります。

`write()` の呼び出し時に実行するステートメントの ID を指定しておく必要があります。 `write()` はトランザクション内で呼び出されることを前提としています。

設定例：

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

**`ItemReader`を使用して読み込んだアイテムを任意のパラメータオブジェクトへ変換する**

デフォルトの動作では、`MyBatisBatchItemWriter` は `ItemReader` を使用して読みこんだアイテム (または `ItemProcessor` によって変換したアイテム)を、そのままMyBatis(`SqlSession` の `update` メソッド)のパラメーターオブジェクトとして渡します。
もしMyBatisへ渡すパラメーターオブジェクトをカスタマイズしたい場合は、`itemToParameterConverter` オプションを利用することで実現するすることができます。
たとえば、`itemToParameterConverter` オプションを使用すると、 アイテムオブジェクト以外のオブジェクトをMyBatisへ渡すことができます。
以下にサンプルを示します。

まず、任意のパラメータオブジェクトに変換するためのコンバータクラス(またはファクトリメソッド)を作成します。以下のサンプルではファクトリメソッドを使用します。

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

つぎに, SQLマッピングを書きます。

```xml
<select id="createPerson" resultType="org.mybatis.spring.sample.domain.Person">
    insert into persons (first_name, last_name, operation_by, operation_at)
           values(#{item.firstName}, #{item.lastName}, #{operationBy}, #{operationAt})
</select>
```

さいごに, `MyBatisBatchItemWriter` の設定を行います。

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

**Composite Writer を使って複数のテーブルに書き込む（注意事項あり）**

このテクニックを使うには MyBatis 3.2 以降が必要です。それ以前のバージョンには [問題](http://code.google.com/p/mybatis/issues/detail?id=741) があるため、Writer が期待通りに動作しません。

まず、*Interaction* と１対１の関係にある *InteractionMetadata* と、これらとは独立した *VisitorInteraction* および *CustomerInteraction* を保持する Item クラスを用意します。

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

`CompositeItemWriter` の設定では、それぞれのオブジェクトの writer を順番に呼び出すように設定します。
この例では *Interaction* をアップデートするためのキーを取得するため、*InteractionMetadata* を先に書き込む必要があります。

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

それぞれの writer を必要に応じて設定します。例えば *Interaction* と *InteractionMetadata* の設定は次のようになります。

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

reader の場合と同様、 `statementId` はネームスペースを含むステートメント ID です。

Mapper ファイルにステートメントを定義します。

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

はじめに `insertInteractionMetadata` が呼ばれ、その際に取得した主キーを使って親となる `Interaction` を `insertInteraction` を使って書き込むことができます。

***JDBC ドライバによって動作が異なるので注意が必要です。例えば MySQL の JDBC ドライバは作成された全ての行の ID を返しますが、H2 バージョン 1.3.168 ではバッチモードでも最後に作成された行の ID のみが返されます。***
