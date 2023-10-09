<a name="Primeros_pasos"></a>
# Primeros pasos

Este capítulo te mostrará en pocos pasos cómo instalar y configurar MyBatis-Spring y cómo construir
una pequeña aplicación transaccional.

## Instalación

Para usar el módulo MyBatis-Spring, debes incluir el fichero `mybatis-spring-${project.version}.jar` y sus dependencias en el classpath.

Si usas Maven simplemente añade la siguiente dependencia a tu pom.xml:

```xml
<dependency>
  <groupId>org.mybatis</groupId>
  <artifactId>mybatis-spring</artifactId>
  <version>${project.version}</version>
</dependency>
```

## Configuración rápida

Para usar MyBatis con Spring necesitas definir al menos dos cosas en tu contexto Spring: una `SqlSessionFactory` y al menos un mapper interface.

En MyBatis-Spring se usa un `SqlSessionFactoryBean` para crear una `SqlSessionFactory`. Para configurar la factory bean pon lo siguiente en tu fichero de configuración de Spring:

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

Observa que la `SqlSessionFactory` requiere un `DataSource`. Éste puede ser cualquier `DataSource` y debe configurarse como cualquier otra conexión a base de datos de Spring.

Asumamos que tienes un mapper interface definido de la siguiente forma:

```java
public interface UserMapper {
  @Select("SELECT * FROM users WHERE id = #{userId}")
  User getUser(@Param("userId") String userId);
}
```

Este interface se añade a Spring usando un `MapperFactoryBean` de la siguiente forma:

```xml
<bean id="userMapper" class="org.mybatis.spring.mapper.MapperFactoryBean">
  <property name="mapperInterface" value="org.mybatis.spring.sample.mapper.UserMapper" />
  <property name="sqlSessionFactory" ref="sqlSessionFactory" />
</bean>
```

Observa que la clase del mapper indicada **debe** ser un interface, no una implementación. En este ejemplo se usan anotaciones para especificar la SQL, pero también es posible usar un fichero de mapeo XML.

Una vez configurado, puedes inyectar mappers directamente en tus beans de servicio/negocio de la misma forma que inyectarías cualquier otro bean en Spring.
La clase `MapperFactoryBean` se encargará de obtener una `SqlSession` y de cerrarla. Si hay una transación Spring en curso, la sesión se comitará o se hará rollback cuando la transacción finalice.
Finalmente, cualquier excepción será traducida a una excepión `DataAccessException`s de Spring.

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

Invocar a MyBatis es sólo una línea de código:

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
