<a name="Injecting_Mappers"></a>
# Injecting Mappers

Rather than code data access objects (DAOs) manually using `SqlSessionDaoSupport` or `SqlSessionTemplate`, Mybatis-Spring can create a thread safe mapper that you can inject directly into other beans:

```xml
<bean id="fooService" class="org.mybatis.spring.sample.service.FooServiceImpl">
  <constructor-arg ref="userMapper" />
</bean>
```

Once injected, the mapper is ready to be used in application logic:

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
    
Notice that there are no `SqlSession` or MyBatis references in this code. Nor is there any need to create, open or close the session, MyBatis-Spring will take care of that.

<a name="register"></a>
## Registering a mapper

The way you register a mapper depends on whether you are using a classic XML configuration or the new 3.0+ Java Config (a.k.a. `@Configuration`).

### With XML Config

A mapper is registered to Spring by including a `MapperFactoryBean` in your XML config file like follows:

```xml
<bean id="userMapper" class="org.mybatis.spring.mapper.MapperFactoryBean">
  <property name="mapperInterface" value="org.mybatis.spring.sample.mapper.UserMapper" />
  <property name="sqlSessionFactory" ref="sqlSessionFactory" />
</bean>
```

If the UserMapper has a corresponding MyBatis XML mapper file in the same classpath location as the mapper interface, it will be parsed automatically by the `MapperFactoryBean`.
There is no need to specify the mapper in a MyBatis configuration file unless the mapper XML files are in a different classpath location. See the `SqlSessionFactoryBean`'s [`configLocation`](factorybean.html) property for more information.

Note that `MapperFactoryBean` requires either an `SqlSessionFactory` or an `SqlSessionTemplate`. These can be set through the respective `sqlSessionFactory` and `sqlSessionTemplate` properties.
If both properties are set, the `SqlSessionFactory` is ignored. Since the `SqlSessionTemplate` is required to have a session factory set, that factory will be used by `MapperFactoryBean`.

### With Java Config

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
## Scanning for mappers

There is no need to register all your mappers one by one. Instead, you can let MyBatis-Spring scan your classpath for them.

There are three different ways to do it:

* Using the `<mybatis:scan>` element.
* Using the annotation `@MapperScan`
* Using a classic Spring xml file and registering the `MapperScannerConfigurer`

Both `<mybatis:scan/>` and `@MapperScan` are features introduced in MyBatis-Spring 1.2.0. `@MapperScan` requires Spring 3.1+.

Since 2.0.2, mapper scanning feature support an option (`lazy-initialization`) that control lazy initialization enabled/disabled of mapper bean.
The motivation for adding this option is supporting a lazy initialization control feature supported by Spring Boot 2.2. The default of this option is `false` (= not use lazy initialization).
If developer want to use lazy initialization for mapper bean, it should be set to the `true` expressly.

<span class="label important">IMPORTANT</span>
If use the lazy initialization feature, the developer need to understand following limitations. If any of following conditions are matches, usually the lazy initialization feature cannot use on your application.

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

The `<mybatis:scan/>` XML element will search for mappers in a very similar way than the Spring built-in element `<context:component-scan/>` searches for beans.

Follows below a sample XML configuration:

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

The `base-package` attribute lets you set the base package for your mapper interface files. You can set more than one package by using a semicolon or comma as a separator. Mappers will be searched for recursively starting in the specified package(s).

Notice that there is no need to specify a `SqlSessionFactory` or `SqlSessionTemplate` as an attribute in the `<mybatis:scan/>` element because it will create `MapperFactoryBean`s that can be autowired.
But if you are using more than one `DataSource` autowire may not work for you. In this case you can use the `factory-ref` or `template-ref` attributes to set the right bean name to use.

`<mybatis:scan/>` supports filtering the mappers created by either specifying a marker interface or an annotation. The `annotation` property specifies an annotation to search for.
The `marker-interface` attribute specifies a parent interface to search for. If both properties are specified, mappers are added for interfaces that match **either** criteria.
By default, these two properties are null, so all interfaces in the given base package(s) will be loaded as mappers.

Discovered mappers will be named using Spring default naming strategy for autodetected components (see [the Spring reference document(Core Technologies -Naming autodetected components-)](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans-scanning-name-generator)).
That is, if no annotation is found, it will use the uncapitalized non-qualified class name of the mapper. But if either a `@Component` or a JSR-330 `@Named` annotation is found it will get the name from the annotation.
Notice that you can set the `annotation` attribute to `org.springframework.stereotype.Component`, `javax.inject.Named` (if you have JSE 6) or to your own annotation (that must be itself annotated) so the annotation will work both as a marker and as a name provider.

<span class="label important">NOTE</span>
`<context:component-scan/>` won't be able to scan and register mappers. Mappers are interfaces and, in order to register them to Spring, the scanner must know how to create a `MapperFactoryBean` for each interface it finds.

### @MapperScan

If you are using the Spring Java Configuration (a.k.a `@Configuration`) you would prefer to use the `@MapperScan` rather than the `<mybatis:scan/>`.

The `@MapperScan` annotation is used as follows:

```java
@Configuration
@MapperScan("org.mybatis.spring.sample.mapper")
public class AppConfig {
  // ...
}
```

The annotation works in the same exact way than `<mybatis:scan/>` we saw in the previous section. It also lets you specify a marker interface or an annotation class through its properties `markerInterface` and `annotationClass`.
You can also provide an specific `SqlSessionFactory` or `SqlSessionTemplate` by using its properties `sqlSessionFactory` and `sqlSessionTemplate`.

<span class="label important">NOTE</span>
Since 2.0.4, If `basePackageClasses` or `basePackages` are not defined, scanning will occur from the package of the class that declares this annotation.

### MapperScannerConfigurer

The `MapperScannerConfigurer` is a `BeanDefinitionRegistryPostProcessor` that can be included in a classic xml application context as a normal bean.
To set up a `MapperScannerConfigurer` add the following to the Spring configuration:

```xml
<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
  <property name="basePackage" value="org.mybatis.spring.sample.mapper" />
</bean>
```

If you need to specify an specific `sqlSessionFactory` or `sqlSessionTemplate` note that **bean names** are required, not bean references, thus the `value` attribute is used instead of the usual `ref`:

```xml
<property name="sqlSessionFactoryBeanName" value="sqlSessionFactory" />
```

<span class="label important">NOTE</span>
`sqlSessionFactoryBean` and `sqlSessionTemplateBean` properties were the only option available up to MyBatis-Spring 1.0.2 but given that the `MapperScannerConfigurer` runs earlier in the startup process that `PropertyPlaceholderConfigurer` there were frequent errors.
For that purpose that properties have been deprecated and the new properties `sqlSessionFactoryBeanName` and `sqlSessionTemplateBeanName` are recommended.
