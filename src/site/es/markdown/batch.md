<a name="Spring_Batch"></a>
# Spring Batch

Desde la versión 1.1.0 MyBatis-Spring proporciona dos beans para construir aplicaciones Spring Batch: `MyBatisPagingItemReader` y `MyBatisCursorItemReader` y `MyBatisBatchItemWriter`.
Also, As of version 2.0.0 provides three builder classes for supporting the Java Configuration: the `MyBatisPagingItemReaderBuilder`, the `MyBatisCursorItemReaderBuilder` and the `MyBatisBatchItemWriterBuilder`.

<span class="label important">NOTA</span>
Esta sección se refiere a [Spring Batch](http://static.springsource.org/spring-batch/) y no a sesiones batch de MyBatis. Para obtener información sobre las sesiones batch ve a la sección [Usnado un SqlSession](sqlsession.html).

## MyBatisPagingItemReader

Este bean es un `ItemReader` que lee registros de una base de datos usando paginación.

Ejecuta la sentencia especificada mediante la propiedad `setQueryId` para obtener los datos. La sentencia se ejecuta usando peticiones paginadas del tamaño indicando en la propiedad `setPageSize`.
Al llamar al método `read()` éste devuelve el objeto que corresponde a la posición actual y solicita más páginas si es necesario.

El reader recibe algunos parametros estándar y la SQL deberá hacer uso de algunos de ellos para construir un resultset del tamaño requerido. Los parametros son:

* `_page`: el número de página a leer (comenzando en 0)
* `_pagesize`: el tamaño de la página, es decir, el número de filas a devolver
* `_skiprows`: el producto de `_page` por `_pagesize`

Se pueden mapear en un statement de tipo select de la siguiente forma:

```xml
<select id="getEmployee" resultMap="employeeBatchResult">
  SELECT id, name, job FROM employees ORDER BY id ASC LIMIT #{_skiprows}, #{_pagesize}
</select>
```

A continuación se muestra un ejemplo de configuración:

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

**Veamos un ejemplo más complejo:**

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

El ejemplo anterior hace uso de tres cosas distintas:

* `sqlSessionFactory`: Puedes tu propio sessionFactory, podría ser útil si quires leer de varias bases de datos.
* `queryId`: Si el código accede a varias tablas, y tienes distintas sentencias de consulta, puede ser interesante usar ficheros de mapeo distintos con namespaces distintos.
  En este caso, al referirte a la query, no olvides incluir el namespace correspondiente.
* `parameterValues`: Puedes pasar parametros adicionales en este mapa, el ejemplo de arriba usa un mapa que se construye usando una expresion SpEL y obteniendo valores del <code>jobExecutionContext</code>.
  Las claves del mapa puede usarse en el fichero mapper de MyBatis (por ejemplo: *yesterday* se puede usar como `#{yesterday,jdbcType=TIMESTAMP}`).
  Observa que el mapa y el reader se consutruyen en un solo `step` para que sea posible usar la expresión SpEL con el `jobExecutionContext`.
  Adicionalmente si los type handlers de MyBatis están configurados correctamente puedes pasar instancias personalizadas como los parametros del ejemplo que son fechas JodaTime.
* `pageSize`: Si le flujo batch está configurado con un tamaño de bloque (chunk size), es importante pasar esta información al reader, y eso se hace mediante esta propiedad.

## MyBatisCursorItemReader

Este bean es un `ItemReader` que lee registros de la base de datos usando un cursor.

<span class="label important">NOTA</span>
Para usar este bean necesitas al menos MyBatis 3.4.0 o superior.

Ejecuta la sentencia especificada mediante la propiedad `setQueryId` para obtener los datos usando el método `selectCursor()`.
Al llamar al método `read()` se devolverá el siguiente elemento del cursor hasta que no quede ninguno por devolver.

El reader usa una conexión separada para que la sentencia no participe en ninguna transacción creada como parte del proceso del step.

Cuando se usar un cursor puedes usar una sentencia convencional:

```xml
<select id="getEmployee" resultMap="employeeBatchResult">
  SELECT id, name, job FROM employees ORDER BY id ASC
</select>
```

A continuación se muestra un ejemplo de configuración:

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

Es un `ItemWriter` que usa las capacidades de batch de `SqlSessionTemplate` para ejecutar sentencias batch para todos los elementos (items) proporcionados.
El `SqlSessionFactory` debe configurarse con un executor de tipo `BATCH`.

Ejecuta la sentencia indicada en la propiedad `statementId` cuando se invoca a `write()`. Se supone que `write()` se invoca dentro de una transacción.

A continuación se muestra un ejemplo de configuración:

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
If you want to customize a parameter object that passes to the MyBatis, you can realize to use the `itemToParameterConverter` option. For example using `itemToParameterConverter` option, you can passes any objects other than the item object to the MyBatis.
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

**Escribiendo en distintas tablas usando composite writers (con algunos condicionantes):**

Esta técnica sólo puede usarse con MyBatis 3.2+, por que había un [error](http://code.google.com/p/mybatis/issues/detail?id=741) en las versiones anteriores que hacían que el writer funcionara de forma incorrecta.

Si el batch necesita escribir datos complejos, como registros con asociaciones, o en distintas bases de datos, entonces es necesario sortear el problema de que los insert statements solo pueden escribir en una tabla.
Para conseguir esto debes preparar un <em>Item</em> para que sea escrito por el writer. Sin embargo, dependiendo de las circunstancias puede ser interesante usar la siguiente técnica.
El truco siguiente funciona con items con asociaciones simples o con tablas no relacionadas.

Elabora el `item` de forma que *contenta* todos los resgistros distintos. Supon que para cada `item` hay una *Interaction* que tiene una asociación *InteractionMetadata* y dos filas no asociadas *VisitorInteraction* and *CustomerInteraction*.
El objeto contenedor será de la siguiente forma:

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

Entonces en la configuración de spring habrá un `CompositeItemWriter` que usará writers delegados configurados especificamente para cada tipo de registro.
Fijate que el *InteractionMetadata* es una asociacióin en el ejemplo por lo que debe ser escrita antes para que la Interaction pueda recibir la clave generada.

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

Cada writer delegados se configura como sea necesario, por ejemplo para *Interaction* y *InteractionMetadata*:

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

Al igual que con el reader el `statementId` puede hacer referencia al statement con un namespace como prefijo.

Ahora es debe elaborarse el fichero de mapeo para cada tipo de registro, de la siguiente forma:

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

Lo que sucede es que primeramente se llamará a `insertInteractionMetadata`, y la sentencia de update está configurada para devolver las claves autogeneradas (`keyProperty` y `keyColumn`).
Una vez que el `InteractionMetadata` se ha almacenado por esta sentencia se puede ejecutar la siguiente para escribir el objeto padre `Interaction` mediante `insertInteraction`.

***Sin embargo, ten en cuenta que los drivers JDBC se comportan distinto en este aspecto. A la fecha en la que se escribe esto
el driver H2 1.3.168 solo devuelve el último ID incluso en modo BATCH (see `org.h2.jdbc.JdbcStatement#getGeneratedKeys`),
mientras que el driver JDBC de MySQL se comporta como es de esperar y devuelve todos los IDs.***
