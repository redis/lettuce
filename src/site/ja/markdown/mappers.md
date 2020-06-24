<a name="Mapper_の注入"></a>
# Mapper の注入

MyBatis-Spring がスレッドセーフな Mapper を生成してくれるので、`SqlSessionDaoSupport` や `SqlSessionTemplate` を使って手動で DAO オブジェクトを生成するコードは不要となります。
生成された Mapper は他の Bean に注入することができます。

```xml
<bean id="fooService" class="org.mybatis.spring.sample.service.FooServiceImpl">
  <constructor-arg ref="userMapper" />
</bean>
```

アプリケーション側の処理では、注入された Mapper のメソッドを呼び出すだけです。

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

このコードには `SqlSession` や MyBatis への参照が含まれていない点に注目してください。また、セッションの生成やオープン、クローズも MyBatis-Spring が処理してくれるため不要となります。

<a name="register"></a>
## Mapper の登録

Mapper を Bean として登録する方法は、Spring の設定を XML ファイルを使って行う場合と Spring 3.0 以降で導入された Java Config (= `@Configuration`) を使う場合で異なります。

### XML で設定する場合

XML ファイルを使って Spring を設定する場合、次のように `MapperFactoryBean` のエントリーを追加することで Mapper を Spring Bean として登録することができます。

```xml
<bean id="userMapper" class="org.mybatis.spring.mapper.MapperFactoryBean">
  <property name="mapperInterface" value="org.mybatis.spring.sample.mapper.UserMapper" />
  <property name="sqlSessionFactory" ref="sqlSessionFactory" />
</bean>
```

ここで指定した UserMapper のインターフェイスと同じクラスパスに MyBatis の XML Mapper ファイルが配置されている場合は自動的にロードされます。
XML Mapper が異なるクラスパスに配置されている場合を除けば、MyBatis の設定ファイルでこの Mapper を指定する必要はありません。
詳しくは `SqlSessionFactoryBean` の [`configLocation`](factorybean.html) プロパティの説明を参照してください。

`MapperFactoryBean` を登録する際は `SqlSessionFactory` あるいは `SqlSessionTemplate` のどちらかを指定する必要があります。
指定対象のプロパティは、それぞれ `sqlSessionFactory` と `sqlSessionTemplate` です。
両方が指定された場合、 `SqlSessionFactory` の指定は無視され、`SqlSessionTemplate` の登録時に指定した Session Factory が使われます。

### Java Config で設定する場合

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
## Mapper の自動検出

上で説明した方法では Mapper を個別に指定していましたが、MyBatis-Spring では特定のクラスパスに含まれる Mapper を自動検出させることもできます。

これには３通りの方法があります。

* `<mybatis:scan/>` 要素を使う。
* `@MapperScan` アノテーションを使う。
* Spring の XML 設定ファイルに `MapperScannerConfigurer` のエントリーを追加する。

`<mybatis:scan/>` または `@MapperScan` を使う場合は MyBatis-Spring 1.2.0 以降が必要です。また `@MapperScan` を使う場合は Spring 3.1 以降が必要となります。

2.0.2以降では、Mapperの自動検出機能は、Mapper Beanの遅延初期化の有効/無効を制御するオプション（`lazy-initialization`）をサポートします。
このオプションを追加する動機は、Spring Boot 2.2でサポートされた遅延初期化を制御する機能をサポートすることです。このオプションのデフォルトは`false`です（遅延初期化を使用しません）。
開発者がMapper Beanを遅延初期化したい場合は、明示的にこのオプションを`true`に設定する必要があります。

<span class="label important">重要</span>
遅延初期化機能を使用する場合は、開発者は以下の制限を理解しておく必要があります。以下の条件のいずれかに一致する場合、通常あなたのアプリケーションで遅延初期化機能を使用することはできません。

* `<association>`(`@One`) and `<collection>`(`@Many`)を利用して、**他のMapperの**ステートメントを参照している場合
* `<include>`を利用して、**他のMapperの**フラグメントをインクルードしている場合
* `<cache-ref>`(`@CacheNamespaceRef`)を利用して、**他のMapperの**キャッシュを参照している場合
* `<select resultMap="...">`(`@ResultMap`)を利用して、**他のMapperの**結果マッピングを参照している場合

<span class="label important">NOTE</span>
しかしながら、以下のように`@DependsOn`(Springの機能)を利用して、依存するMapper Beanも同時に初期化すると遅延初期化機能を利用することができるようになります。

```java
@DependsOn("vendorMapper")
public interface GoodsMapper {
  // ...
}
```

2.0.6以降では、開発者はMapper BeanのスコープをMapperの自動検出機能のオプション(`default-scope`)とスコープを指定するアノテーション(`@Scope` や `@RefreshScope` など)を使用して指定することができるようになります。
このオプションを追加する動機は、Spring Cloudから提供されている `refresh` スコープをサポートすることです。このオプションのデフォルトは空（`singleton`スコープを指定するのと同等）です。
`default-scope` オプションで指定した値は、スキャンしたMapperのBean定義に指定されているスコープが `singleton` の際に適用され、最終的なMapperのスコープが `singleton` でない場合はScoped ProxyのBeanを作成します。

### \<mybatis:scan\>

`<mybatis:scan/>` は、Spring の `<context:component-scan/>` が Bean を検索するのと良く似た方法で Mapper を検出します。

XML 設定の例：

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

`base-package` 属性で Mapper ファイルを含むパッケージを指定します。セミコロンまたはカンマ区切りで複数のパッケージを指定することもできます。
また、指定されたパッケージが内包するパッケージも検索対象となります。

ここでは `<mybatis:scan/>` に `SqlSessionFactory` や `SqlSessionTemplate` を指定していませんが、この場合は Autowired 可能な `MapperFactoryBean` が自動的に生成されます。
ただし、複数の `DataSource` を利用する場合は Autowired に頼ることができないので、 `factory-ref` または `template-ref` 属性を使って適切な Bean を指定する必要があります。

`<mybatis:scan/>` を使う場合、マーカーインターフェイスまたはアノテーションを指定して Mapper をフィルタリングすることができます。検出対象のアノテーションを指定するには `annotation` 属性を使います。
検出対象の Mapper が実装するインターフェイスを指定する場合は `marker-interface` 属性を使います。両方の属性が指定された場合、どちらかの条件を満たすインターフェイスが Mapper として登録されます。
デフォルトではどちらも `null` となっており、`base-package` で指定したパッケージに含まれるすべてのインターフェイスが Mapper としてロードされます。

検出された Mapper は、Spring の自動検出コンポーネントに対するデフォルト命名規則によって Bean 名が決められます[the Spring reference document(Core Technologies -Naming autodetected components-](https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans-scanning-name-generator) を参照してください。
アノテーションによる指定がない場合はクラス名の先頭を小文字にした文字列が Bean 名となりますが、`@Component` あるいは JSR-330 の `@Named` アノテーションを使って Bean 名を明示的に指定することもできます。
先に説明した `annotation` 属性で `org.springframework.stereotype.Component` や `javax.inject.Named` （Java 6 以降を利用している場合のみ）を指定すれば、検出時のマーカーと Bean 名の指定を１つのアノテーションで兼ねることができます。
同じ目的で独自に定義したアノテーションを使うこともできますが、このアノテーション自体に `@Component` か `@Named` を付加しておく必要があります。

<span class="label important">NOTE</span>
Spring 標準の `<context:component-scan/>` を使って Mapper を検出することはできません。
Mapper はインターフェイスなので、各 Mapper に対する `MapperFactoryBean` の生成方法が分かっていないと Spring Bean として登録することができないのです。

### @MapperScan

Java Config を使って Spring を設定しているのなら、`<mybatis:scan/>` よりも `@MapperScan` を使う方が気に入ると思います。

`@MapperScan` アノテーションは次のように使用します。


```java
@Configuration
@MapperScan("org.mybatis.spring.sample.mapper")
public class AppConfig {
  // ...
}
```

このアノテーションは前章で説明した `<mybatis:scan/>` と全く同じ要領で Mapper の検出を行います。
引数 `markerInterface`, `annotationClass` を使えば検出対象のマーカーインターフェイスとアノテーションを指定することもできますし、`sqlSessionFactory`, `sqlSessionTemplate` で `SqlSessionFactory` や `SqlSessionTemplate` を指定することができます。

<span class="label important">NOTE</span>
2.0.4以降では、`basePackageClasses` もしくは `basePackages` が指定されていない場合、このアノテーションが定義されているクラスのパッケージを基準にスキャンします。

### MapperScannerConfigurer

`MapperScannerConfigurer` は `BeanDefinitionRegistryPostProcessor` として定義されているので、従来の XML による設定で通常の Bean として登録することができます。`MapperScannerConfigurer` の登録は次のように行います。

```xml
<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
  <property name="basePackage" value="org.mybatis.spring.sample.mapper" />
</bean>
```

特定の `sqlSessionFactory` または `sqlSessionTemplate` を指定する場合は、 Bean を参照ではなく **名前で** 指定する必要があるので、`ref` ではなく `value` を使います。

```xml
<property name="sqlSessionFactoryBeanName" value="sqlSessionFactory" />
```

<span class="label important">NOTE</span>
MyBatis-Spring 1.0.2 までは有効なプロパティは `sqlSessionFactoryBean` と `sqlSessionTemplateBean` のみでしたが、 `MapperScannerConfigurer` が `PropertyPlaceholderConfigurer` よりも先に読み込まれるためエラーの原因となっていました。
この問題を回避するため、これらのプロパティの使用は非推奨となり、新たに追加された `sqlSessionFactoryBeanName` と `sqlSessionTemplateBeanName` を使うことが推奨されています。
