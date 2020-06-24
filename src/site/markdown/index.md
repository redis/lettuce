<a name="Introduction"></a>
# Introduction

## What is MyBatis-Spring?

MyBatis-Spring integrates MyBatis seamlessly with Spring.
This library allows MyBatis to participate in Spring transactions, takes care of building MyBatis mappers and `SqlSession`s and inject them into other beans, translates MyBatis exceptions into Spring `DataAccessException`s, and finally, it lets you build your application code free of dependencies on MyBatis, Spring or MyBatis-Spring.

## Motivation

Spring version 2 only supports iBATIS version 2. An attempt was made to add MyBatis 3 support into Spring 3 (see the Spring Jira [issue](https://jira.springsource.org/browse/SPR-5991).
Unfortunately, Spring 3 development ended before MyBatis 3 was officially released. Because the Spring team did not want to release with code based on a non-released version of MyBatis, official Spring support would have to wait.
Given the interest in Spring support for MyBatis, the MyBatis community decided it was time to reunite the interested contributors and add Spring integration as a community sub-project of MyBatis instead.

## Requirements

Before starting with MyBatis-Spring integration, it is very important that you are familiar with both MyBatis and Spring terminology.
This document does not attempt to provide background information or basic setup and configuration tutorials for either MyBatis or Spring.

MyBatis-Spring requires following versions:

| MyBatis-Spring | MyBatis | Spring Framework | Spring Batch | Java |
| --- | --- | --- | --- | --- |
| **2.0** | 3.5+ | 5.0+ | 4.0+ | Java 8+ |
| **1.3** | 3.4+ | 3.2.2+ | 2.1+ | Java 6+ |

## Acknowledgements

A special thanks goes to all the special people who made this project a reality (in alphabetical order): Eduardo Macarron, Hunter Presnall and Putthiphong Boonphong for the coding,
testing and documentation; Andrius Juozapaitis, Giovanni Cuccu, Mike Lanyon, Raj Nagappan and Tomas Pinos for their contributions; and Simone Tripodi for finding everyone and bringing them all back to the project under MyBatis ;)
Without them, this project wouldn't exist.

## Help make this documentation better…

If you find this documentation lacking in any way, or missing documentation for a feature, then the best thing to do is learn about it and then write the documentation yourself!

Sources of this manual are available in markdown format at [project's Git](https://github.com/mybatis/spring/tree/master/src/site) Fork the repository, update them and send a pull request.

You’re the best author of this documentation, people like you have to read it!

## Translations

Users can read about MyBatis-Spring in the following translations:

<ul class="i18n">
  <li class="en"><a href="./getting-started.html">English</a></li>
  <li class="es"><a href="./es/index.html">Español</a></li>
  <li class="ja"><a href="./ja/index.html">日本語</a></li>
  <li class="ko"><a href="./ko/index.html">한국어</a></li>
  <li class="zh"><a href="./zh/index.html">简体中文</a></li>
</ul>

Do you want to read about MyBatis in your own native language? Fill an issue providing patches with your mother tongue documentation!
        
