<a name="Inyectar_mappers"></a>
# Inyectar mappers

En lugar de codificar DAOs (data access objects) manualmente usando la clase `SqlSessionDaoSupport` o `SqlSessionTemplate`, Mybatis-Spring puede crear un mapper thread-safe que puedes inyectar directamente en otros beans.

```xml
<bean id="fooService" class="org.mybatis.spring.sample.service.FooServiceImpl">
  <constructor-arg ref="userMapper" />
</bean>
```

 Una vez inyectado, el mapper está listo para se usado en la lógica de aplicación:

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

Observa que no se usa la `SqlSession` ni ninguna otra referencia a MyBatis en este código. No es necesario ni siquiera crear o cerrar la sesión, MyBatis-Spring se encarga de ello.

<a name="register"></a>
## Registrar un mapper

La forma de registrar un mapper varía según si quieres usar la configuración XML clásica o la nueva Java Config de Spring 3.0+ (También conocida como `@Configuration`).

### Con confiugración XML

Un mapper se registra en Spring incluyendo un `MapperFactoryBean` en tu fichero de configuración XML, de la siguiente forma:

```xml
<bean id="userMapper" class="org.mybatis.spring.mapper.MapperFactoryBean">
  <property name="mapperInterface" value="org.mybatis.spring.sample.mapper.UserMapper" />
  <property name="sqlSessionFactory" ref="sqlSessionFactory" />
</bean>
```

Si el mapper UserMapper tiene un fichero XML de mapeo asociado el `MapperFactoryBean` lo cargará automáticamente.
Por lo tanto no es necesario especificar dicho mapper en el fichero de configuración de MyBatis a no ser que los ficheros XML estén en una lugar distinto del classpath.
Ver la sección de `SqlSessionFactoryBean` y la propiedad [`configLocation`](factorybean.html) para más información.

El `MapperFactoryBean` requiere o un `SqlSessionFactory` o un `SqlSessionTemplate`.
Ambos se pueden informar usando sendas propiedades `sqlSessionFactory` y `sqlSessionTemplate`.
Si ambas propiedades han sdo informadas la `SqlSessionFactory` se ignora.
Dado que un `SqlSessionTemplate` debe tener un session factory dicho factory se usará por el `MapperFactoryBean`.

### Con Java Config

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
## Escanear mappers

No es necesario registrar los mappers uno por uno en el fichero XML de Spring. En lugar de esto, puede dejar que MyBatis-Spring los busque en tu classpath.

Hay tres formas distintas de hacerlo:

* Usando el elemneto `<mybatis:scan/>`.
* Usando la anotación `@MapperScan`
* Usando un fichero clásico XML de configuración de Spring y añadiendo el bean `MapperScannerConfigurer`

Tango `<mybatis:scan/>` como `@MapperScan` son características añadidas en MyBatis-Spring 1.2.0. `@MapperScan` requiere Spring 3.1+.

Since 2.0.2, mapper scanning feature support a option (`lazy-initialization`) that control lazy initialization enabled/disabled of mapper bean.
The motivation for adding this option is supporting a lazy initialization control feature supported by Spring Boot 2.2.
The default of this option is `false` (= not use lazy initialization).
If developer want to use lazy initialization for mapper bean, it should be set to the `true` expressly.

<span class="label important">IMPORTANT</span> 
If use the lazy initialization feature, the developer need to understand following limitations.
If any of following conditions are matches, usually the lazy initialization feature cannot use on your application.

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

El elemento XML `<mybatis:scan/>` busca mappers de una forma muy similar a cómo `<context:component-scan/>` busca beans.

A continuación se muestra un fichero XML de configuración:

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

La propiedad <code>basePackage</code> te permite indicar el paquete base donde residen tus mappers.
Puedes indicar más de un paquete usando un punto y coma o una coma como separador. Los mappers serán buscados de forma recursiva comenzando en el/los paquetes especificados.

Fíjate que no es necesario indicar una `SqlSessionFactory` o `SqlSessionTemplate` porque el `<mybatis:scan/>` creará `MapperFactoryBean`s que pueden ser autowired.
Pero si usas más de un `DataSource` el autowire puede que no te funcione. En este caso puedes usar las propiedades `factory-ref` or `template-ref` para indicar los beans correctos a utilizar.

`<mybatis:scan/>` soporta el filtrado de mappers mediante una interfaz marcador o una anotación.
La propiedad `annotation` especifica la anotación que se debe buscar. La propiedad `marker-interface` especifica la interfaz a buscar.
Si se indican ambas se añadirán todos los mappers que cumplan **cualquier** criterio. Por defecto ambas propiedades son null asi que todos los interfaces de los paquetes base serán cargados como mappers.

Los mappers descubiertos serán nombrados usando la estratégia de nombres por defecto de Spring para los componentes autodetectados (see [the Spring reference document(Core Technologies -Naming autodetected components-](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans-scanning-name-generator)).
Es decir, si no se encuentra ninguna anotación, se usará el nombre no cualificado sin capitalizar del mapper. Pero si se encuentra una anotación `@Component` o JSR-330 `@Named` se obtendrá el nombre de dicha anotación.
Fíjate que puedes usar como valor de la `annotation` el valor `org.springframework.stereotype.Component`, `javax.inject.Named` (if you have JSE 6) o una anotación propia (que debe ser a su vez anotada) de forma que la anotación hará las veces de localizador y de proveedor de nombre.

<span class="label important">NOTE</span>
`<context:component-scan/>` no puede encontrar y registrar mappers. Los mappers son interfaces y, para poderlos registrar en Spring, el scanner deben conocer cómo crear un `MapperFactoryBean` para cada interfaz encontrado.

### @MapperScan

Si usas la Java Configuration de Spring (`@Configuration`) posiblemente prefieras usar `@MapperScan` en lugar de `<mybatis:scan/>`.

La anotación `@MapperScan` se usa de la siguiente forma:

```java
@Configuration
@MapperScan("org.mybatis.spring.sample.mapper")
public class AppConfig {
  // ...
}
```

La anotación fucniona exactamente igual que `<mybatis:scan/>` que hemos visto en la sección anterior.
También te permite especificar un interfaz marcador o una anotación mediante sus propiedades `markerInterface` y `annotationClass`.
Tambien puedes indicar una `SqlSessionFactory` o un `SqlSessionTemplate` específicos mediante las propiedades `sqlSessionFactory` y `sqlSessionTemplate`.

<span class="label important">NOTE</span>
Since 2.0.4, If `basePackageClasses` or `basePackages` are not defined, scanning will occur from the package of the class that declares this annotation.

### MapperScannerConfigurer

`MapperScannerConfigurer` es un `BeanDefinitionRegistryPostProcessor` que se puede incluir como un bean normal en el fichero clásico XML de configuración de Spring.
Para configurar un `MapperScannerConfigurer` añade lo siguiente al fichero de configuración de Spring:

```xml
<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
  <property name="basePackage" value="org.mybatis.spring.sample.mapper" />
</bean>
```

Si quieres indicar un `sqlSessionFactory` o un `sqlSessionTemplate` observa que se requeiren **los nombres de los beans** y no sus referencias por ello se usa el atributo `value` en lugar del habitual `ref`:

```xml
<property name="sqlSessionFactoryBeanName" value="sqlSessionFactory" />
```
