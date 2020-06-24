<a name="Introduction"></a>
# Introduction

## ¿Qué es MyBatis-Spring?

MyBatis-Spring permite integrar MyBatis con Spring. Esta librería permite que MyBatis participe en trasacciones Spring,
se encarga de constuir mappers y `SqlSession`s e inyectarlos en otros beans, traduce excepciones de MyBatis en excepcines `DataAccessException`s de Spring y finalmente, permite construir aplicaciones libres de dependencias de MyBatis, Spring y MyBatis-Spring.

## Motivación

Spring version 2 sólo soporta iBATIS version 2. See hizo un intento de incluir el soporte de MyBatis 3 en Spring 3 (ver el [issue Jira](https://jira.springsource.org/browse/SPR-5991)).
Pero desafortunadamente, el desarrollo de Spring 3 finalizó antes de que MyBatis 3 fuera liberado oficialmente.
Dado que el equipo de Spring no quería liberar una versión basada en un producto no terminado el soporte de oficial tenía que esperar.
Dado el interés de la comunidad en el soporte de MyBatis, la comunidad de MyBatis decidió que era el momento de unificar a los colaboradores interesados y proporcionar la integración con Spring como un sub-projecto de MyBatis en su lugar.

## Requisitos

Antes de comenzar con MyBatis-Spring, es muy importante que estés familiarizado con la terminología tanto de MyBatis como de Spring.
Este documento no pretende proporcionar información de configuración básica de MyBatis o Spring.

MyBatis-Spring requires following versions:

| MyBatis-Spring | MyBatis | Spring Framework | Spring Batch | Java |
| --- | --- | --- | --- | --- |
| **2.0** | 3.5+ | 5.0+ | 4.0+ | Java 8+ |
| **1.3** | 3.4+ | 3.2.2+ | 2.1+ | Java 6+ |

## Agradecimientos

Queremos agradecer a la todos los que han hecho de este proyecto una realidad (en orden alfabético):
Eduardo Macarron, Hunter Presnall y Putthiphong Boonphong por la codificación, pruebas y documentación;
a Andrius Juozapaitis, Giovanni Cuccu, Mike Lanyon, Raj Nagappan y Tomas Pinos por sus contribuciones;
y a Simone Tripodi por encontrarlos a todos y traerlos al proyecto MyBatis ;) Sin ellos este proyecto no existiría.

## Colabora en mejorar esta documentación...

Si ves que hay alguna carencia en esta documentación, o que falta alguna característica por documentar, te animamos a que lo investigues y la documentes tu	mismo!

Las fuentes de este manual están disponibles en formato markdown en el [Git del proyecto](https://github.com/mybatis/mybatis-3/tree/master/src/site). Haz un fork, cambialas y envía un pull request.

Eres el mejor candidato para documentar porque los lectores de esta	documentación son gente como tú!

## Translations

Users can read about MyBatis-Spring in the following translations:

<ul class="i18n">
  <li class="en"><a href="./../index.html">English</a></li>
  <li class="es"><a href="./getting-started.html">Español</a></li>
  <li class="ja"><a href="./../ja/index.html">日本語</a></li>
  <li class="ko"><a href="./../ko/index.html">한국어</a></li>
  <li class="zh"><a href="./../zh/index.html">简体中文</a></li>
</ul>

Do you want to read about MyBatis in your own native language? Fill an issue providing patches with your mother tongue documentation!
