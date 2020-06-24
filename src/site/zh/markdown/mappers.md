<a name="注入映射器"></a>
# 注入映射器

与其在数据访问对象（DAO）中手工编写使用 `SqlSessionDaoSupport` 或 `SqlSessionTemplate` 的代码，还不如让 Mybatis-Spring 为你创建一个线程安全的映射器，这样你就可以直接注入到其它的 bean 中了：

```xml
<bean id="fooService" class="org.mybatis.spring.sample.service.FooServiceImpl">
  <constructor-arg ref="userMapper" />
</bean>
```

注入完毕后，映射器就可以在你的应用逻辑代码中使用了：

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

注意代码中并没有任何的对 `SqlSession` 或 MyBatis 的引用。你也不需要担心创建、打开、关闭 session，MyBatis-Spring 将为你打理好一切。

<a name="register"></a>
## 注册映射器

注册映射器的方法根据你的配置方法，即经典的 XML 配置或新的 3.0 以上版本的 Java 配置（也就是常说的 `@Configuration`），而有所不同。

### XML 配置

在你的 XML 中加入 `MapperFactoryBean` 以便将映射器注册到 Spring 中。就像下面一样：

```xml
<bean id="userMapper" class="org.mybatis.spring.mapper.MapperFactoryBean">
  <property name="mapperInterface" value="org.mybatis.spring.sample.mapper.UserMapper" />
  <property name="sqlSessionFactory" ref="sqlSessionFactory" />
</bean>
```

如果映射器接口 UserMapper 在相同的类路径下有对应的 MyBatis XML 映射器配置文件，将会被 `MapperFactoryBean` 自动解析。不需要在 MyBatis 配置文件中显式配置映射器，除非映射器配置文件与接口类不在同一个类路径下。
参考 `SqlSessionFactoryBean` 的 [`configLocation`](factorybean.html) 属性以获取更多信息。

注意 `MapperFactoryBean` 需要配置一个 `SqlSessionFactory` 或 `SqlSessionTemplate`。它们可以分别通过 `sqlSessionFactory` 和 `sqlSessionTemplate` 属性来进行设置。
如果两者都被设置，`SqlSessionFactory` 将被忽略。由于 `SqlSessionTemplate` 已经设置了一个 session 工厂，`MapperFactoryBean` 将使用那个工厂。

### Java 配置

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
## 发现映射器

不需要一个个地注册你的所有映射器。你可以让 MyBatis-Spring 对类路径进行扫描来发现它们。


有几种办法来发现映射器：

* 使用 `<mybatis:scan/>` 元素
* 使用 `@MapperScan` 注解
* 在经典 Spring XML 配置文件中注册一个 `MapperScannerConfigurer`

`<mybatis:scan/>` 和 `@MapperScan` 都在 MyBatis-Spring 1.2.0 中被引入。`@MapperScan` 需要你使用 Spring 3.1+。

Since 2.0.2, mapper scanning feature support a option (`lazy-initialization`) that control lazy initialization enabled/disabled of mapper bean.
The motivation for adding this option is supporting a lazy initialization control feature supported by Spring Boot 2.2. The default of this option is `false` (= not use lazy initialization).
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

`<mybatis:scan/>` 元素会发现映射器，它发现映射器的方法与 Spring 内建的 `<context:component-scan/>` 发现 bean 的方法非常类似。

下面是一个 XML 配置样例：

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

`base-package` 属性允许你设置映射器接口文件的基础包。通过使用逗号或分号分隔，你可以设置多个包。并且会在你所指定的包中递归搜索映射器。

注意，不需要为 `<mybatis:scan/>` 指定 `SqlSessionFactory` 或 `SqlSessionTemplate`，这是因为它将使用能够被自动注入的 `MapperFactoryBean`。但如果你正在使用多个数据源（`DataSource`），自动注入可能不适合你。
在这种情况下，你可以使用 `factory-ref` 或 `template-ref` 属性指定你想使用的 bean 名称。

`<mybatis:scan/>` 支持基于标记接口或注解的过滤操作。在 `annotation` 属性中，可以指定映射器应该具有的特定注解。而在 `marker-interface` 属性中，可以指定映射器应该继承的父接口。当这两个属性都被设置的时候，被发现的映射器会满足这两个条件。
默认情况下，这两个属性为空，因此在基础包中的所有接口都会被作为映射器被发现。


被发现的映射器会按照 Spring 对自动发现组件的默认命名策略进行命名（参考 [the Spring reference document(Core Technologies -Naming autodetected components-)](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans-scanning-name-generator) ）。
也就是说，如果没有使用注解显式指定名称，将会使用映射器的首字母小写非全限定类名作为名称。但如果发现映射器具有 `@Component` 或 JSR-330 标准中 `@Named` 注解，会使用注解中的名称作为名称。
提醒一下，你可以设置 `annotation` 属性为你自定义的注解，然后在你的注解上设置 `org.springframework.stereotype.Component` 或 `javax.inject.Named`（需要使用 Java SE 6 以上）注解，这样你的注解既可以作为标记，也可以作为一个名字提供器来使用了。

<span class="label important">提示</span>
`<context:component-scan/>` 无法发现并注册映射器。映射器的本质是接口，为了将它们注册到 Spring 中，发现器必须知道如何为找到的每个接口创建一个 `MapperFactoryBean`。

### @MapperScan

当你正在使用 Spring 的基于 Java 的配置时（也就是 `@Configuration`），相比于使用 `<mybatis:scan/>`，你会更喜欢用 `@MapperScan`。

`@MapperScan` 注解的使用方法如下：

```java
@Configuration
@MapperScan("org.mybatis.spring.sample.mapper")
public class AppConfig {
  // ...
}
```

这个注解具有与之前见过的 `<mybatis:scan/>` 元素一样的工作方式。它也可以通过 `markerInterface` 和 `annotationClass` 属性设置标记接口或注解类。
通过配置 `sqlSessionFactory` 和 `sqlSessionTemplate` 属性，你还能指定一个 `SqlSessionFactory` 或 `SqlSessionTemplate`。

<span class="label important">NOTE</span>
Since 2.0.4, If `basePackageClasses` or `basePackages` are not defined, scanning will occur from the package of the class that declares this annotation.

### MapperScannerConfigurer

`MapperScannerConfigurer` 是一个 `BeanDefinitionRegistryPostProcessor`，这样就可以作为一个 bean，包含在经典的 XML 应用上下文中。为了配置 `MapperScannerConfigurer`，使用下面的 Spring 配置：

```xml
<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
  <property name="basePackage" value="org.mybatis.spring.sample.mapper" />
</bean>
```

如果你需要指定 `sqlSessionFactory` 或 `sqlSessionTemplate`，那你应该要指定的是 **bean 名**而不是 bean 的引用，因此要使用 `value` 属性而不是通常的 `ref` 属性：

```xml
<property name="sqlSessionFactoryBeanName" value="sqlSessionFactory" />
```

<span class="label important">提示</span>
在 MyBatis-Spring 1.0.2 之前，`sqlSessionFactoryBean` 和 `sqlSessionTemplateBean` 属性是唯一可用的属性。
但由于 `MapperScannerConfigurer` 在启动过程中比 `PropertyPlaceholderConfigurer` 运行得更早，经常会产生错误。基于这个原因，上述的属性已被废弃，现在建议使用 `sqlSessionFactoryBeanName` 和 `sqlSessionTemplateBeanName` 属性。
