<a name="MyBatis_API_の使用"></a>
# MyBatis API の使用

MyBatis-Spring を使っている場合でも、直接 MyBatis API を呼び出すことができます。
Spring の設定で `SqlSessionFactoryBean` を使って `SqlSessionFactory` を生成すれば、コード内で使用することができます。

```java
public class UserDaoImpl implements UserDao {
  // SqlSessionFactory would normally be set by SqlSessionDaoSupport
  private final SqlSessionFactory sqlSessionFactory;

  public UserDaoImpl(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
  }

  public User getUser(String userId) {
    // note standard MyBatis API usage - opening and closing the session manually
    try (SqlSession session = sqlSessionFactory.openSession()) {
      return session.selectOne("org.mybatis.spring.sample.mapper.UserMapper.getUser", userId);
    }
  }
}
```

この方法を使う場合は注意が必要です。なぜなら、誤った使い方をすると実行時エラーや、最悪の場合データの不整合といった問題を生じる可能性があるからです。
MyBatis API を直接使用する場合、次のような点に注意してください。

* API の呼び出しは Spring で管理されているトランザクション内では実行されません。
* `SqlSession` が Spring のトランザクションマネージャーが使っているのと同じ `DataSource` を使っていて、既に進行中のトランザクションが存在している場合、このコードは例外を投げます。
* MyBatis の `DefaultSqlSession` はスレッドセーフではありません。もしあなたの Bean に注入した場合、エラーが発生します。
* `DefaultSqlSession` を使って生成した Mapper もスレッドセーフとはなりません。もしあなたの Bean に注入した場合、エラーが発生します。
* `SqlSession` は常に finally ブロックでクローズする必要があります。
