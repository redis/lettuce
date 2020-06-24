<a name="Transacciones"></a>
# Transacciones

Uno de los principales motivos para usar MyBatis-Spring es que permite que MyBatis participe en transacciones de Spring.
En lugar de haber creado un TransactionManager especifico para MyBatis, MyBatis-Spring aprovecha el existente `DataSourceTransactionManager` de Spring.

Una vez que has configurado un TransactionManager in Spring puedes configurar tus transacciones en Spring como siempre.
Tanto las anotaciones `@Transactional` como las configuraciones de tipo AOP se soportan. Se creará una sola instancia de `SqlSession` para toda la transacción.
Se hará commit o rollback de esta sesión cuando la transacción finalice.

MyBatis-Spring se encargará de gestionar las transacciones de forma transparente una vez se hayan configurado. No es necesario incluir código adicional en tus clases.

<a name="configuration"></a>
## Configuration Estándar

Para habilitar las transacciones Spring, simplemente crea un `DataSourceTransactionManager` en tu fichero de configuración:

```xml
<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
  <constructor-arg ref="dataSource" />
</bean>
```

```java
@Configuration
public class DataSourceConfig {
  @Bean
  public DataSourceTransactionManager transactionManager() {
    return new DataSourceTransactionManager(dataSource());
  }
}
```

El `DataSource` especificado puede ser cualquier `DataSource` JDBC que usarías normalmente con Spring.
Esto incluye connection pools y `DataSource`s obtenidos mediante JNDI.

Fijate que el `DataSource` especificado en el transaction manager **debe** ser el mismo que el que se use para crear el `SqlSessionFactoryBean` o la gestión de transacciones no funcionará.

<a name="container"></a>
## Container Managed Transactions

Si estás usando un contenedor JEE y quiere que spring participe en las transacciones gestionadas por contenedor (CMT), entonces debes configurar un `JtaTransactionManager` en Spring o alguna de sus clases específicas de cada contenedor.
Lo más sencillo es utilizar el namespace tx de Spring or the `JtaTransactionManagerFactoryBean`:

```xml
<tx:jta-transaction-manager />
```

```java
@Configuration
public class DataSourceConfig {
  @Bean
  public JtaTransactionManager transactionManager() {
    return new JtaTransactionManagerFactoryBean().getObject();
  }
}
```

Con esta configuración, MyBatis se comportará como cualquier otro recurso configurado con CMT.
Spring utilizará cualquier transacción CMT existente y asociará la `SqlSession` a ella.
Si no no hay ninguna transacción previa pero se necesita una en base a la configuración de la transacción, Spring creará una transacción gestionada por contenedor nueva.

Fijate que si quieres usar transacciones CMT pero **no** quieres utilizar la gestión de transacciones de Spring **no debes** configurar ningun transaction manager en Spring y **debes** configurar el `SqlSessionFactoryBean` para que use la clase `ManagedTransactionFactory` de MyBatis de la siguiente forma:

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
  <property name="dataSource" ref="dataSource" />
  <property name="transactionFactory">
    <bean class="org.apache.ibatis.transaction.managed.ManagedTransactionFactory" />
  </property>
</bean>
```

```java
@Configuration
public class MyBatisConfig { 
  @Bean
  public SqlSessionFactory sqlSessionFactory() {
    SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
    factoryBean.setDataSource(dataSource());
    factoryBean.setTransactionFactory(new ManagedTransactionFactory());
    return factoryBean.getObject();
  }
}
```

<a name="programmatic"></a>
## Ǵestión programática de transacción

La interfaz `SqlSession` proporciona métodos especificos para gestionar la transacción.
Pero al usar MyBatis-Spring en tus beans se inyectará una `SqlSession` o un mapper gestionados por Spring.
Esto significa que Spring **siempre** gestionará tus transacciones.

No puedes llamar a los métodos `SqlSession.commit()`, `SqlSession.rollback()` o `SqlSession.close()` en una `SqlSession` gestionada por Spring.
Si lo intentas obtendrás una excepción `UnsupportedOperationException`. Además, estos métodos no se exponen en los mapper interfaces.

Independientemente de el estado del autocommit de la conexión JDBC cualquier llamada
a un metodo SQL de `SqlSession` fuera de una transacción Spring será automaticamente commitada.

Si quieres controlar las transacciones programaticamente consulta el [the Spring reference document(Data Access -Programmatic transaction management-)](https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#transaction-programmatic).
Este código muetra como controlar una transacción manualmente usando el `PlatformTransactionManager`.

```java
public class UserService {
  private final PlatformTransactionManager transactionManager;
  public UserService(PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }
  public void createUser() {
    TransactionStatus txStatus =
        transactionManager.getTransaction(new DefaultTransactionDefinition());
    try {
      userMapper.insertUser(user);
    } catch (Exception e) {
      transactionManager.rollback(txStatus);
      throw e;
    }
    transactionManager.commit(txStatus);
  }
}
```

You can omit to call the `commit` and `rollback` method using the `TransactionTemplate`.

```java
public class UserService {
  private final PlatformTransactionManager transactionManager;
  public UserService(PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }
  public void createUser() {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.execute(txStatus -> {
      userMapper.insertUser(user);
      return null;
    });
  }
}
```

Este código usa un mapper pero también funcionaría con `SqlSession.
