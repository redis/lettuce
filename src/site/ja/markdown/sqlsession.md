<a name="SqlSession_の利用"></a>
# SqlSession の利用

MyBatis では `SqlSessionFactory` を使って `SqlSession` を生成しました。
そして取得したセッションを使って Mapped Statement を実行し、接続をコミットあるいはロールバックした後、最終的に不要となったセッションをクローズする、というのが一連の流れでした。
MyBatis-Spring では、Spring のトランザクション設定に基づいて自動的にコミット、ロールバック、クローズされるスレッドセーフな `SqlSession` が注入されるので、直接 `SqlSessionFactory` を使う必要はありません。

## SqlSessionTemplate

`SqlSessionTemplate` は MyBatis-Spring で最も重要なクラスです。このクラスが MyBatis の `SqlSession` を管理して、SQL メソッドの実行や例外の変換を行なっています。
このクラスは既存のコードで使用している `SqlSession` の代わりに使うことを前提に `SqlSession` インターフェイスを実装しています。`SqlSessionTemplate` はスレッドセーフで、複数の DAO, Mapper 間で共有することができます。

`getMapper()` から返された Mapper のメソッドも含めて、SQL メソッドを呼び出す場合、確実に現在の Spring トランザクションに付加された `SqlSession` が使われるようにするのも `SqlSessionTemplate` の仕事です。
それ以外にも、セッションのクローズや状況に応じたコミットあるいはロールバックといったライフサイクルの管理、更には MyBatis の例外を Spring の `DataAccessException` に変換する処理も行います。

`SqlSessionTemplate` は Spring が管理するトランザクション内で実行され、また Spring によってインジェクトされる複数の Mapper から呼び出すことができるようにスレッドセーフとなっているので、常にこのクラスを MyBatis のデフォルト実装である `DefaultSqlSession` の代わりに使用するべきです。
同一アプリケーション内でこれら２つのクラスを混在させて使用するとデータの不整合などの問題が発生する可能性があります。

`SqlSessionTemplate` を生成する際は、`SqlSessionFactory` をコンストラクタ引数として渡します。

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

この Bean は、直接あなたの DAO Bean にインジェクト（注入）することができます。注入対象の Bean には `SqlSession` プロパティを定義しておく必要があります。

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

そして、以下のようにして `SqlSessionTemplate` を注入します。

```xml
<bean id="userDao" class="org.mybatis.spring.sample.dao.UserDaoImpl">
  <property name="sqlSession" ref="sqlSession" />
</bean>
```

`SqlSessionTemplate` には、`ExecutorType.BATCH` を引数に取るコンストラクタも定義されています。
このコンストラクタを使うと、例えばバッチ処理を行う `SqlSession` を取得することができます。

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

これで実行されるステートメントは全てバッチ処理の対象となります。DAO クラス中では、例えば次のように書くことができます。

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

デフォルト以外の `ExecutorType` を使用する場合にのみ `SqlSessionFactory` の Bean を定義する際に２つの引数を指定する必要があります。

この初期化方式を使用する場合の注意点として、このメソッドが呼び出される時点で異なる `ExecutorType` で実行されているトランザクションが存在していてはならない、という制限があります。
そのためには、異なる `ExecutorType` が指定された `SqlSessionTemplate` への呼び出しを、それぞれが独立したトランザクション内で実行する（例えば `PROPAGATION_REQUIRES_NEW` を指定しておく）か、あるいは完全にトランザクションの外で実行するようにしてください。

## SqlSessionDaoSupport

`SqlSessionDaoSupport` は `SqlSession` を提供する抽象クラスです。`getSqlSession()` を呼び出すことで、SQL メソッドを実行するための `SqlSessionTemplate` を取得することができます。

```java
public class UserDaoImpl extends SqlSessionDaoSupport implements UserDao {
  public User getUser(String userId) {
    return getSqlSession().selectOne("org.mybatis.spring.sample.mapper.UserMapper.getUser", userId);
  }
}
```

普通は `MapperFactoryBean` を使った方がコード量が少なくて済みますが、DAO の中で MyBatis 以外の処理を行うため実装クラスが必要となる場合には便利なクラスです。

`SqlSessionDaoSupport` を使用する際は、`sqlSessionFactory` または `sqlSessionTemplate` をセットする必要があります。もし両方のプロパティがセットされた場合、`sqlSessionFactory` は無視されます。

`SqlSessionDaoSupport` のサブクラスである `UserDaoImpl` を Spring Bean として定義する例を挙げておきます。

```xml
<bean id="userDao" class="org.mybatis.spring.sample.dao.UserDaoImpl">
  <property name="sqlSessionFactory" ref="sqlSessionFactory" />
</bean>
```
