<a name="スタートガイド"></a>
# スタートガイド
 
この章では、MyBatis-Spring のインストール・設定手順と、トランザクション処理を含むシンプルなアプリケーションの構築する方法について説明します。

## インストール

MyBatis-Spring を使うためには、 `mybatis-spring-${project.version}.jar` と依存するライブラリをクラスパスに追加するだけで OK です。

Maven をお使いの場合は、 pom.xml に次の dependency を追加してください。

```xml
<dependency>
  <groupId>org.mybatis</groupId>
  <artifactId>mybatis-spring</artifactId>
  <version>${project.version}</version>
</dependency>
```

## クイックセットアップ

MyBatis と Spring を組み合わせて使う場合、Spring の Application Context 内に少なくとも `SqlSessionFactory` と一つ以上の Mapper インターフェイスを定義する必要があります。

MyBatis-Spring では `SqlSessionFactory` の生成に `SqlSessionFactoryBean` を使います。この Factory Bean を設定するため、Spring の 設定ファイルに次の Bean を追加してください。

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

`SqlSessionFactory` が `DataSource` を必要としている点に注意してください。どのような `DataSource` でも構いません。通常の手順で設定してください。

Mapper インターフェイスが次のように定義されている場合...

```java
public interface UserMapper {
  @Select("SELECT * FROM users WHERE id = #{userId}")
  User getUser(@Param("userId") String userId);
}
```

`MapperFactoryBean` を使ってこのインターフェイスを Spring に登録する場合、以下のように設定します。

```xml
<bean id="userMapper" class="org.mybatis.spring.mapper.MapperFactoryBean">
  <property name="mapperInterface" value="org.mybatis.spring.sample.mapper.UserMapper" />
  <property name="sqlSessionFactory" ref="sqlSessionFactory" />
</bean>
```

ここで指定した Mapper は、実装クラスではなく **インターフェイス** である必要がありますので注意してください。
この例では、アノテーションを使って SQL を指定していますが、Mapper XML ファイルを使うこともできます。

上記のように設定しておけば、あとは他の Spring Bean と同様にビジネス／サービス層のオブジェクトにインジェクト（注入）することができます。
`MapperFactoryBean` は `SqlSession` の生成とクローズを行います。
もし Spring のトランザクション内で実行された場合は、トランザクション終了時にセッションがコミットあるいはロールバックされます。
最後にもう一点、全ての例外は Spring の `DataAccessException` に変換されます。

Java Configurationを使用する場合:

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

MyBatis のデータメソッドは、一行だけで実行可能となります。

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
