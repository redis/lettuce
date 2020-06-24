<a name="SqlSessionFactoryBean"></a>
# SqlSessionFactoryBean

In base MyBatis, the `SqlSessionFactory` is built using `SqlSessionFactoryBuilder`. In MyBatis-Spring, `SqlSessionFactoryBean` is used instead.

## Setup

To create the factory bean, put the following in the Spring XML configuration file:

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
  <property name="dataSource" ref="dataSource" />
</bean>
```
Note that `SqlSessionFactoryBean` implements Spring's `FactoryBean` interface see [the Spring documentation(Core Technologies -Customizing instantiation logic with a FactoryBean-)](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans-factory-extension-factorybean)).
This means that the bean Spring ultimately creates is **not** the `SqlSessionFactoryBean` itself, but what the factory returns as a result of the `getObject()` call on the factory. 
In this case, Spring will build an `SqlSessionFactory` for you at application startup and store it with the name `sqlSessionFactory`.
In Java, the equivalent code would be:

```java
@Configuration
public class MyBatisConfig {
  @Bean
  public SqlSessionFactory sqlSessionFactory() {
    SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
    factoryBean.setDataSource(dataSource());
    return factoryBean.getObject();
  }
}
```

In normal MyBatis-Spring usage, you will not need to use `SqlSessionFactoryBean` or the corresponding `SqlSessionFactory` directly.
Instead, the session factory will be injected into `MapperFactoryBean`s or other DAOs that extend `SqlSessionDaoSupport`.

## Properties

`SqlSessionFactory` has a single required property, the JDBC `DataSource`. This can be any `DataSource` and should be configured just like any other Spring database connection.

One common property is `configLocation` which is used to specify the location of the MyBatis XML configuration file.
One case where this is needed is if the base MyBatis configuration needs to be changed. Usually this will be `<settings>` or `<typeAliases>` sections.

Note that this config file does **not** need to be a complete MyBatis config. Specifically, any environments, data sources and MyBatis transaction managers will be **ignored**.
`SqlSessionFactoryBean` creates its own, custom MyBatis `Environment` with these values set as required.

Another reason to require a config file is if the MyBatis mapper XML files are not in the same classpath location as the mapper classes. With this configuration, there are two options.
This first is to manually specify the classpath of the XML files using a `<mappers>` section in the MyBatis config file. A second option is to use the `mapperLocations` property of the factory bean.

The `mapperLocations` property takes a list of resource locations. This property can be used to specify the location of MyBatis XML mapper files.
The value can contain Ant-style patterns to load all files in a directory or to recursively search all paths from a base location.

For example:

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
  <property name="dataSource" ref="dataSource" />
  <property name="mapperLocations" value="classpath*:sample/config/mappers/**/*.xml" />
</bean>
```

This will load all the MyBatis mapper XML files in the `sample.config.mappers` package and its sub-packages from the classpath.

One property that may be required in an environment with container managed transactions is `transactionFactoryClass`. Please see the relevant section in the Transactions chapter.

In case you are using the multi-db feature you will need to set the `databaseIdProvider` property:

```xml
<bean id="databaseIdProvider" class="org.apache.ibatis.mapping.VendorDatabaseIdProvider">
  <property name="properties">
    <props>
      <prop key="SQL Server">sqlserver</prop>
      <prop key="DB2">db2</prop>
      <prop key="Oracle">oracle</prop>
      <prop key="MySQL">mysql</prop>
    </props>
  </property>
</bean>
```
```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
  <property name="dataSource" ref="dataSource" />
  <property name="mapperLocations" value="classpath*:sample/config/mappers/**/*.xml" />
  <property name="databaseIdProvider" ref="databaseIdProvider"/>
</bean>
```

<span class="label important">NOTE</span>
Since 1.3.0, `configuration` property has been added. It can be specified a `Configuration` instance directly without MyBatis XML configuration file.

For example:

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
  <property name="dataSource" ref="dataSource" />
  <property name="configuration">
    <bean class="org.apache.ibatis.session.Configuration">
      <property name="mapUnderscoreToCamelCase" value="true"/>
    </bean>
  </property>
</bean>
```
