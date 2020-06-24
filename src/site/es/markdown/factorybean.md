<a name="SqlSessionFactoryBean"></a>
# SqlSessionFactoryBean

En MyBatis una `SqlSessionFactory` se crea mediante la clase `SqlSessionFactoryBuilder`. En MyBatis-Spring se usa la clase `SqlSessionFactoryBean` en su lugar.

## Configuración

Para crear un factory bean, pon lo siguiente en el fichero XML de configuración de Spring:

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
  <property name="dataSource" ref="dataSource" />
</bean>
```

La clase `SqlSessionFactoryBean` implementa el interfaz `FactoryBean` (see [the Spring documentation(Core Technologies -Customizing instantiation logic with a FactoryBean-](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans-factory-extension-factorybean)).
Lo cual significa que el bean que crea Spring en última instancia **no** es un `SqlSessionFactoryBean` en si mismo, sino el objeto que la factoria devuelve como resultado de la llamada al método `getObject()`.
En este caso, Spring creará un bean `SqlSessionFactory` durante el arranque de la aplicación y lo guardará bajo el nombre `sqlSessionFactory`. En Java, el código equivalente sería:

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

Normalmente no necesitarás utilizar directamente un `SqlSessionFactoryBean` o su correspondiente `SqlSessionFactory` directly.
En su lugar, la factoría se utilizará para ser inyectada en `MapperFactoryBean`s o DAOs que extiendan de `SqlSessionDaoSupport`.

## Properties

La clase `SqlSessionFactory` solo tiene una propiedad obligatoria, un `DataSource`.
Puede ser cualquier `DataSource` y se puede configurar como cualquier otra conexión a base de daots de Spring.

Una propiedad muy común es la `configLocation` que se utiliza para indicar la localización del fichero de configuración XML de MyBatis.
Normalmente solo es necesario dicho fichero si se requiere cambiar los valores por defecto de las secciones `<settings>` o `<typeAliases>`.

Es importante saber que este fichero de configuración **no** tiene por qué ser un fichero de configuración de MyBatis completo.
Concretamente, los environments, dataSources y transactionManagers serán **ignorados**.
`SqlSessionFactoryBean` crea su propio `Environment` de MyBatis con los valores configurados tal y como se requieren.

Otro motivo para necesitar un fichero de configuración es que los ficheros de mapeo XML no estén en el mismo lugar del classpath que los mapper interfaces.
En este caso hay dos opciones. La primera es especificar manualmente el classpath de los ficheros XML usando la sección `<mappers>` del fichero de configuración de MyBatis.
La segunda opción es usar la propiedad `mapperLocations` del factory bean.

La propiedad `mapperLocations` recibe una lista de localizaciones de recursos. Se utiliza para indicar la ubicación de los ficheros de mapeo XML de MyBatis.
El valor puede contener un patron tipo Ant para cargar todos los ficheros de un directorio o buscar de forma recursiva en todos los paths desde una localización base. Por ejemplo:

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
  <property name="dataSource" ref="dataSource" />
  <property name="mapperLocations" value="classpath*:sample/config/mappers/**/*.xml" />
</bean>
```

Esto cargaría todos los ficheros de mapeo XML en el paquete sample.config.mappers y sus subpaquetes.

Otra propiedad que puede ser necesaria en un entorno con transacciones gestionadas por contenedor es la `transactionFactoryClass`. Lee la sección de transacciones para obtener más detalles.

En caso de usar la característica multi-db necesitarás informar la propiedad `databaseIdProvider` de la siguiente forma:

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
