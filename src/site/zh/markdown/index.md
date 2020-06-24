<a name="简介"></a>
# 简介

## 什么是 MyBatis-Spring？

MyBatis-Spring 会帮助你将 MyBatis 代码无缝地整合到 Spring 中。它将允许 MyBatis 参与到 Spring 的事务管理之中，创建映射器 mapper 和 `SqlSession` 并注入到 bean 中，以及将 Mybatis 的异常转换为 Spring 的 `DataAccessException`。
最终，可以做到应用代码不依赖于 MyBatis，Spring 或 MyBatis-Spring。

## 动机

Spring 2.0 只支持 iBatis 2.0。那么，我们就想将 MyBatis3 的支持添加到 Spring 3.0 中（参见 Spring Jira 中的 [问题](https://jira.springsource.org/browse/SPR-5991) ）。不幸的是，Spring 3.0 的开发在 MyBatis 3.0 官方发布前就结束了。
由于 Spring 开发团队不想发布一个基于未发布版的 MyBatis 的整合支持，如果要获得 Spring 官方的支持，只能等待下一次的发布了。基于在 Spring 中对 MyBatis 提供支持的兴趣，MyBatis 社区认为，应该开始召集有兴趣参与其中的贡献者们，将对 Spring 的集成作为 MyBatis 的一个社区子项目。

## 知识基础

在开始使用 MyBatis-Spring 之前，你需要先熟悉 Spring 和 MyBatis 这两个框架和有关它们的术语。这很重要——因为本手册中不会提供二者的基本内容，安装和配置教程。

MyBatis-Spring 需要以下版本：

| MyBatis-Spring | MyBatis | Spring Framework | Spring Batch | Java |
| --- | --- | --- | --- | --- |
| **2.0** | 3.5+ | 5.0+ | 4.0+ | Java 8+ |
| **1.3** | 3.4+ | 3.2.2+ | 2.1+ | Java 6+ |

## 致谢

特别感谢那些使本项目变为现实的人们（按字母顺序排序）： Eduardo Macarron, Hunter Presnall 和 Putthiphong Boonphong 负责本项目的代码实现，测试和编写文档工作；
Andrius Juozapaitis, Giovanni Cuccu, Raj Nagappan 和 Tomas Pinos 的贡献；
而 Simone Tripodi 发现了这些人并邀请他们参与到这一个 MyBatis 子项目之中。没有他们的努力，这个项目只能沦为空谈。

## 帮助改进文档...

如果你发现文档有任何的缺失，或者缺少某一个功能点的说明，最好的解决办法是先自己学习，并且为缺失的部份补上相应的文档。

手册的 markdown 出自 [项目的 Git 仓库](https://github.com/mybatis/spring/tree/master/src/site) 。Fork 仓库，更新它并提交 Pull Request 吧。

还有其它像你一样的人都需要阅读这份文档，而你，就是这份文档最好的作者。

## 文档的翻译版本

可以阅读以下 MyBatis-Spring 文档的翻译版本：

 <ul class="i18n">
   <li class="en"><a href="./../index.html">English</a></li>
   <li class="es"><a href="./../es/index.html">Español</a></li>
   <li class="ja"><a href="./../ja/index.html">日本語</a></li>
   <li class="ko"><a href="./../ko/index.html">한국어</a></li>
   <li class="zh"><a href="./../zh/getting-started.html">简体中文</a></li>
 </ul>

想用自己的母语阅读这篇文档吗？那就用你的母语翻译它吧！
