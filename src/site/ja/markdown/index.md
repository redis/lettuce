<a name="イントロダクション"></a>
# イントロダクション

## MyBatis-Spring とは？

MyBatis-Spring によって MyBatis と Spring をシームレスに連携させることができます。このライブラリを使えば、MyBatis のステートメントを Spring のトランザクション内で実行することもできますし、Mapper や `SqlSession` の生成、他の Bean への注入、MyBatis の例外から Spring の `DataAccessException` への変換、さらには MyBatis や Spring, MyBatis-Spring に依存しないコードでアプリケーションを構築することも可能になります。

## 動機

Spring バージョン 2 は iBATIS バージョン 2 しかサポートしていません。Spring 3 の開発時に MyBatis 3 への対応が検討されました（こちらの [チケット](https://jira.springsource.org/browse/SPR-5991) 参照）が、Spring 3 が MyBatis 3 よりも前に正式リリースを迎えたため、残念ながら実装は見送られました。Spring 開発陣としては、未リリースの MyBatis 3 に合わせたコードをリリースしたくなかったという事情があり、Spring 側での正式対応は保留となっていました。MyBatis コミュニティの中で Spring 対応への要望が強かったため、有志によって Spring との連携を行う MyBatis のサブプロジェクトが立ち上げられました。

## 動作条件

MyBatis-Spring を利用するためには、MyBatis と Spring の用語について理解しておくことが重要です。このドキュメントには MyBatis や Spring についての説明や基本設定といった情報は含まれていません。

MyBatis-Spring は以下のバージョンを必要とします。

| MyBatis-Spring | MyBatis | Spring Framework | Spring Batch | Java |
| --- | --- | --- | --- | --- |
| **2.0** | 3.5+ | 5.0+ | 4.0+ | Java 8+ |
| **1.3** | 3.4+ | 3.2.2+ | 2.1+ | Java 6+ |

## 謝辞

このプロジェクトの実現にご協力頂いた次の方々に感謝します（アルファベット順）:
Eduardo Macarron, Hunter Presnall, Putthiphong Boonphong（コーディング、テスト、ドキュメント作成）;
Andrius Juozapaitis, Giovanni Cuccu, Mike Lanyon, Raj Nagappan, Tomas Pinos（コントリビューション）;
Simone Tripodi（メンバーを集め、MyBatis のサブプロジェクトとしてまとめてくれました）
このプロジェクトは彼らの協力なしには実現できなかったでしょう。

## このドキュメントの改善にご協力ください...

このドキュメントの中で誤りや特定の機能に関する記述が抜けていることに気づいたら、詳しく調べてドキュメントを更新して頂けると助かります。

このマニュアルのソースは markdown 形式で、[プロジェクトの Git リポジトリ](https://github.com/mybatis/spring/tree/master/src/site) で配布されています。
リポジトリをフォーク、それらを更新します、とプルリクエストを送信します。

このドキュメントを必要としている人、つまりあなたこそが最高の著者なのです！

## Translations

MyBatis-Spring は以下の言語の翻訳を用意しています。

<ul class="i18n">
  <li class="en"><a href="./../index.html">English</a></li>
  <li class="es"><a href="./../es/index.html">Español</a></li>
  <li class="ja"><a href="./getting-started.html">日本語</a></li>
  <li class="ko"><a href="./../ko/index.html">한국어</a></li>
  <li class="zh"><a href="./../zh/index.html">简体中文</a></li>
</ul>

母国語でMyBatis Springのリファレンスを読んでみませんか？ ぜひドキュメントを母国語へ翻訳するためのIssue(パッチ)を作成してください！
