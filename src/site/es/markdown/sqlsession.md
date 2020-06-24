<a name="Uso_de_SqlSession"></a>
# Uso de SqlSession

En MyBatis usas un `SqlSessionFactory` para crear un `SqlSession`.
Una vez tienes una sesión, la usas para ejecutar tus mapped statements, hacer commit o rollback y finalmente cuando no la necesitas la cierras.
Con MyBatis-Spring no es necesario que utilices la `SqlSessionFactory` directamente porque puedes inyectar en tus beans una `SqlSession` thread safe (reentrante) que hará de forma automática el commit,
rollback y se cerrará conforme a la configuración de la transacción en Spring.

## SqlSessionTemplate

El `SqlSessionTemplate` is el corazón de MyBatis-Spring.
Implementa `SqlSession` y está pensado para que sea directamente reemplazable por cualquier código que actualmente use `SqlSession`.
`SqlSessionTemplate` es thread safe y se puede compartir por más de un DAO.

Cuando se invoca a cualquier método SQL, incluyendo cualquier método de un mapper devuelto por `getMapper()`, el `SqlSessionTemplate` se asegurará de que la `SqlSession` utilizada es la asociada con la transacción Spring en curso.
Adicionalmente, se encarga de gestionar su ciclo de vida, incluyendo su cierre, commit o rollback de la sesión si fuera necesario.

Se debe usar <strong>siempre</strong> un <code>SqlSessionTemplate</code> en lugar de la implementación de sesión por default MyBatis:
`DefaultSqlSession` porque el template puede participar en transacciones Spring y es thread safe con lo que puede ser inyectado en múltiples mappers (proxies).
Cambiar de uno a otro puede crear problemas de integridad de datos.

El `SqlSessionTemplate` puede construirse usando un `SqlSessionFactory` como argumento de su constructor.

```xml
<bean id="sqlSession" class="org.mybatis.spring.SqlSessionTemplate">
  <constructor-arg index="0" ref="sqlSessionFactory" />
</bean>
```

```java
@Configuration
public class MyBatisConfig {
  @Bean
  public SqlSessionTemplate sqlSession() throws Exception {
    return new SqlSessionTemplate(sqlSessionFactory());
  }
}
```

Este bean puede ser inyectado directamente en tus DAOs. Necesitarás una propiedad `SqlSession` en tu bean como se muestra a continuación:

```java
public class UserDaoImpl implements UserDao {

  private SqlSession sqlSession;

  public void setSqlSession(SqlSession sqlSession) {
    this.sqlSession = sqlSession;
  }

  public User getUser(String userId) {
    return sqlSession.selectOne("org.mybatis.spring.sample.mapper.UserMapper.getUser", userId);
  }
}
```

E inyectar el `SqlSessionTemplate` de la siguiente forma:

```xml
<bean id="userDao" class="org.mybatis.spring.sample.dao.UserDaoImpl">
  <property name="sqlSession" ref="sqlSession" />
</bean>
```

El `SqlSessionTemplate` tiene también un constructor que recibe como parámetro un `ExecutorType`.
Esto permite construir, por ejemplo, una `SqlSession` batch utilizando la siguiente configuracíon de Spring:

```xml
<bean id="sqlSession" class="org.mybatis.spring.SqlSessionTemplate">
  <constructor-arg index="0" ref="sqlSessionFactory" />
  <constructor-arg index="1" value="BATCH" />
</bean>
```

```java
@Configuration
public class MyBatisConfig {
  @Bean
  public SqlSessionTemplate sqlSession() throws Exception {
    return new SqlSessionTemplate(sqlSessionFactory(), ExecutorType.BATCH);
  }
}
```

Ahora todos tus statements se ejecutarán en batch de forma que puedes programar lo siguiente:

```java
public class UserService {
  private final SqlSession sqlSession; 
  public UserService(SqlSession sqlSession) {
    this.sqlSession = sqlSession;
  }
  public void insertUsers(List<User> users) {
    for (User user : users) {
      sqlSession.insert("org.mybatis.spring.sample.mapper.UserMapper.insertUser", user);
    }
  }
}
```

Fijate que esta configuración si el método de ejecución deseado es distinto del establecido por defecto en el `SqlSessionFactory`.

La contrapartida de esto es que **no es posible** cambiar el méodo de ejecución dentro de una transacción.
Por tanto asegurate que o bien todas las llamadas a un `SqlSessionTemplate`s con distinto método de ejecución se ejecutan en una transacción diferente (p.ej: with `PROPAGATION_REQUIRES_NEW`) o completamente fuera de la transacción.

## SqlSessionDaoSupport

`SqlSessionDaoSupport` es una clase de soporte abstracta que proporciona un `SqlSession`.
Llamando a `getSqlSession()` obtiene un `SqlSessionTemplate` que puedes utilizar para ejecutar métodos SQL, como sigue:

```java
public class UserDaoImpl extends SqlSessionDaoSupport implements UserDao {
  public User getUser(String userId) {
    return getSqlSession().selectOne("org.mybatis.spring.sample.mapper.UserMapper.getUser", userId);
  }
}
```

Normalmente es preferible usar un `MapperFactoryBean` a esta clase dao que no requeire código extra.
Pero esta clase es de utilidad cuando es necesario hacer algún otro tipo de trabajo no-MyBatis en el DAO y se necesitan clases concretas.

`SqlSessionDaoSupport` que se informe la propiedad `sqlSessionFactory` o la `sqlSessionTemplate`. Si se informan ambas propiedades, la `sqlSessionFactory` se ignora.

Asumiendo una clase `UserDaoImpl` que extiende `SqlSessionDaoSupport`, se puede configurar Spring de la siguiente forma:

```xml
<bean id="userDao" class="org.mybatis.spring.sample.dao.UserDaoImpl">
  <property name="sqlSessionFactory" ref="sqlSessionFactory" />
</bean>
```
