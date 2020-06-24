<a name="トランザクション"></a>
# トランザクション

これは MyBatis-Spring を使う主な理由の一つでもありますが、MyBatis-Spring を使うと MyBatis の処理を Spring が管理するトランザクションの一部として実行できるようになります。
MyBatis-Spring は、MyBatis のために新しいトランザクションマネージャーを生成するのではなく、Spring が生成した `DataSourceTransactionManager` を利用します。

Spring のトランザクションマネージャーが定義されていれば、通常の手順で Spring のトランザクションを利用することができます。`@Transactional` アノテーションと AOP 形式での指定、どちらも利用可能です。
トランザクション内では `SqlSession` が一つ生成され、トランザクションの生存期間中はこの `SqlSession` が使用されます。このセッションは、トランザクション完了時にコミットあるいはロールバックされます。

MyBatis-Spring ではトランザクションは透過的に管理されるので、あなたの DAO クラスにコードを追加する必要はありません。

<a name="configuration"></a>
## 標準的な設定

Spring の 設定ファイルで `DataSourceTransactionManager` を生成するだけで、Spring のトランザクション処理が有効となります。

```xml
<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
  <property name="dataSource" ref="dataSource" />
</bean>
```

```java
@Configuration
public class DataSourceConfig {
  @Bean
  public DataSourceTransactionManager transactionManager() {
    return new DataSourceTransactionManager(dataSource());
  }
}
```

ここで指定する `DataSource` は、通常 Spring で利用される JDBC `DataSource` であればどのようなデータソースでも構いません。
例えば、コネクションプールや JNDI 経由で取得した `DataSource` などです。

ただし、トランザクションマネージャーに対して指定する `DataSource` は、`SqlSessionFactoryBean` に対して指定したものと同じでなくてはなりません。もし別のデータソースを指定した場合、トランザクション機能は正しく動作しません。

<a name="container"></a>
# Container Managed Transactions

JEEコンテナを利用していて、Spring の処理を CMT (Container Managed Transaction) の一部として利用したい場合、`JtaTransactionManager` あるいはそのコンテナ固有のサブクラスを使って Spring を設定する必要があります。
最も簡単なのは、Spring のトランザクション名前空間 又は `JtaTransactionManagerFactoryBean` を使う方法です。

```xml
<tx:jta-transaction-manager />
```
```java
@Configuration
public class DataSourceConfig {
  @Bean
  public JtaTransactionManager transactionManager() {
    return new JtaTransactionManagerFactoryBean().getObject();
  }
}
```

このように設定しておくと MyBatis は、CMT を使うように設定された他の Spring リソースと同じように動作します。
Spring は、既存のコンテナ管理されたトランザクションがあれば、そのトランザクションに `SqlSession` を付加して利用します。
もしトランザクションを要求する処理が呼び出された時点で開始されたトランザクションがなければ、Spring が新しいコンテナ管理されたトランザクションを開始します。

CMT は使いたいが、Spring のトランザクション管理は利用したくないという場合、Spring のトランザクションマネージャーを定義してはいけません。
またこの場合、MyBatis 側で生成された `ManagedTransactionFactory` を使うように `SqlSessionFactoryBean` を設定する必要があります。

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
  <property name="dataSource" ref="dataSource" />
  <property name="transactionFactory">
    <bean class="org.apache.ibatis.transaction.managed.ManagedTransactionFactory" />
  </property>
</bean>
```

```java
@Configuration
public class MyBatisConfig {
  @Bean
  public SqlSessionFactory sqlSessionFactory() {
    SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
    factoryBean.setDataSource(dataSource());
    factoryBean.setTransactionFactory(new ManagedTransactionFactory());
    return factoryBean.getObject();
  }
}
```

<a name="programmatic"></a>
## トランザクションをプログラム的に制御する

MyBatis の `SqlSession` では、トランザクションをプログラム的に制御するためのメソッドが用意されています。
しかし、MyBatis-Spring では、あなたの Bean にインジェクト（注入）されるのは Spring が管理する `SqlSession` あるいは Mapper です。つまり、トランザクションを制御するのは常に Spring でなくてはなりません。

Spring が管理している `SqlSession` に対して `SqlSession.commit()`, `SqlSession.rollback()`, `SqlSession.close()` を呼び出すことはできません。
もしこれらのメソッドを呼び出した場合、`UnsupportedOperationException` がスローされます。あなたの Bean に注入される Mapper クラスでは、これらのメソッドは隠蔽されています。

Spring が管理するトランザクションの外側で `SqlSession` のデータメソッドあるいは Mapper メソッドを呼び出した場合、JDBC 接続に対する auto-commit の設定に関わらず、変更は直ちにコミットされます。

もしあなたがトランザクションをプログラム的に制御したいのであれば、[the Spring reference document(Data Access -Programmatic transaction management-](https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#transaction-programmatic) を参照してください。
以下のコードは、`PlatformTransactionManager` を使ってトランザクションを手動で制御する例です。

```java
public class UserService {
  private final PlatformTransactionManager transactionManager;
  public UserService(PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }
  public void createUser() {
    TransactionStatus txStatus =
        transactionManager.getTransaction(new DefaultTransactionDefinition());
    try {
      userMapper.insertUser(user);
    } catch (Exception e) {
      transactionManager.rollback(txStatus);
      throw e;
    }
    transactionManager.commit(txStatus);
  }
}
```

`TransactionTemplate` を使用して `commit` と `rollback` メソッドを省略することもできます。

```java
public class UserService {
  private final PlatformTransactionManager transactionManager;
  public UserService(PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }
  public void createUser() {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.execute(txStatus -> {
      userMapper.insertUser(user);
      return null;
    });
  }
}
```
ここでは Mapper を使っていますが、`SqlSession` を使うこともできます。
