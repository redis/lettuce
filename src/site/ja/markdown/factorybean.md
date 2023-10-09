<a name="SqlSessionFactoryBean"></a>
# SqlSessionFactoryBean

基となる MyBatis では、`SqlSessionFactory` をビルドする際 `SqlSessionFactoryBuilder` を使いましたが、MyBatis-Spring では、`SqlSessionFactoryBean` を使います。

## 設定

Spring の XML 設定ファイルに次の Bean を定義することで Factory Bean を生成することができます。

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
  <property name="dataSource" ref="dataSource" />
</bean>
```

`SqlSessionFactoryBean` は Spring の `FactoryBean` インターフェイス（[the Spring documentation(Core Technologies -Customizing instantiation logic with a FactoryBean-](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans-factory-extension-factorybean) を参照してください）を実装しています。
これはつまり、最終的に Spring が生成するのは `SqlSessionFactoryBean` ではなく、Factory の `getObject()` メソッドによって返されるオブジェクトであるということです。
上記の設定では、Spring は `SqlSessionFactory` を生成し、`sqlSessionFactory` という名前の Bean として登録します。
これに相当する Java のコードは下記のようになるでしょう。

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

通常 MyBatis-Spring を使う場合、`SqlSessionFactoryBean` や対応する `SqlSessionFactory` を直接利用する必要はありません。
`SqlSessionFactory` は `MapperFactoryBean` や `SqlSessionDaoSupport` を継承した他の DAO にインジェクト（注入）されます。

## プロパティ

`SqlSessionFactory` で必須のプロパティは JDBC の `DataSource` のみです。 どのような `DataSource` でも構いません。Spring でデータベース接続を定義する通常の手順で定義してください。

`configLocation` は、MyBatis の XML 設定ファイルの場所を指定する際に使用します。これは、例えば基になる MyBatis の設定の一部を変更したい場合などに必要となります。
よくあるのは `<settings>` や `<typeAliases>` などの設定です。

ここで指定する設定ファイルは、完全な MyBatis 設定ファイルである必要はありません。 環境、データソース、MyBatis のトランザクションマネージャーに関する設定は**無視されます**。
`SqlSessionFactoryBean` は、独自にカスタマイズした MyBatis `Environment` を生成し、必要に応じてこれらの値を設定するようになっています。

設定ファイルの指定が必要とされるもう一つの例は、MyBatis の Mapper XML ファイルが Mapper クラスとは別のクラスパスに存在する場合です。
このような構成にする場合、次のどちらかの方法で設定することができます。最初の方法は、MyBatis の設定ファイルの `<mappers>` で各 XML ファイルのクラスパスを指定する方法です。
そしてもう一つは、Factory Bean の `mapperLocations` を使った方法です。

`mapperLocations` プロパティは Resource Location のリストを取り、ここで MyBatis の XML Mapper ファイルの場所を指定することができます。
Ant スタイルのパターン文字列を使って特定のディレクトリ内の全ファイルを指定したり、内包するディレクトリを再帰的に検索対象にすることもできます。次の例を見てください。

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
  <property name="dataSource" ref="dataSource" />
  <property name="mapperLocations" value="classpath*:sample/config/mappers/**/*.xml" />
</bean>
```

このように指定すると、クラスパス内の `sample.config.mappers` パッケージと、そのサブパッケージに含まれる全ての MyBatis Mapper XML ファイルがロードされます。

Container-Managed トランザクションを利用する環境では、`transactionFactoryClass` プロパティが必須となります。「トランザクション」章の該当する節を参照してください。

複数のデータベースを使用する場合は、`databaseIdProvider` プロパティを設定する必要があります。

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
1.3.0より、`configuration` プロパティが追加されました。このプロパティには、MyBatisのXML設定ファイルを使わずに`Configuration`インスタンスを直接指定することができます。
次の例を見てください。

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
