<a name="소개"></a>
# 소개

## MyBatis-Spring 은 무엇일까?

마이바티스 스프링 연동모듈은 마이바티스와 스프링을 편하고 간단하게 연동한다. 이 모듈은 마이바티스로 하여금 스프링 트랜잭션에 쉽게 연동되도록 처리한다. 게다가 마이바티스 매퍼와 `SqlSession`을 다루고 다른 빈에 주입시켜준다.
마이바티스 예외를 스프링의 `DataAccessException`로 변환하기도 하고 마이바티스, 스프링 또는 마이바티스 스프링 연동모듈에 의존성을 없애기도 한다.

## 동기 부여

스프링 2.x은 아이바티스 2.x만을 지원한다. 스프링 3.x에서 마이바티스 3.x를 지원하기 위한 시도가 진행중이다. (스프링의 이슈관리 시스템인 [이슈](https://jira.springsource.org/browse/SPR-5991) 를 보라.)
불행하게도 스프링 3의 개발이 마이바티스 3.0의 정식릴리즈전에 개발이 완료되었다. 그래서 스프링팀은 릴리즈가 안된 마이바티스 코드를 함께 릴리즈하는 것을 원하지 않았고 실제적인 스프링 지원을 기다릴수밖에 없었다.
스프링의 마이바티스 지원에 대한 관심으로 인해, 마이바티스 커뮤니티는 재결합하는 형태로 결정을 내고 대신 마이바티스의 하위 프로젝트 형태로 스프링 연동 프로젝트를 추가한다.

## 필요 조건

마이바티스 스프링 연동을 시작하기 전에, 마이바티스와 스프링의 용어를 맞추는 일이 굉장히 중요했다. 이 문서는 배경지식이나 기본적인 셋업방법 그리고 마이바티스와 스프링의 설정에 대한 튜토리얼등은 제공하지 않는다.

MyBatis-Spring requires following versions:

| MyBatis-Spring | MyBatis | Spring Framework | Spring Batch | Java |
| --- | --- | --- | --- | --- |
| **2.0** | 3.5+ | 5.0+ | 4.0+ | Java 8+ |
| **1.3** | 3.4+ | 3.2.2+ | 2.1+ | Java 6+ |

## 감사 인사

이 프로젝트가 실제로 만들어지게 도와준 모든 특별한 분들에게 정말 감사한다.
알파벳 순서로 보면, 코딩및 테스트 그리고 문서화를 담당했던 Eduardo Macarron, Hunter Presnall, Putthiphong Boonphong;
그외 다양한 프로젝트 기여자인 Andrius Juozapaitis, Giovanni Cuccu, Mike Lanyon, Raj Nagappan, Tomas Pinos;
그리고 마이바티스에 하위 프로젝트로 가져올수 있도록 많은 것을 찾아준 Simone Tripodi 에게 감사한다. ;)
이들이 없었다면 이 프로젝트는 존재하지 않았을 것이다.

## 이 문서가 더 나아지도록 도와주세요…

만약에 어떤 방법으로든 이 문서의 취약점이 발견되거나 기능에 대한 문서화가 빠진 부분이 보인다면, 가장 좋은 방법은 먼저 공부해서 자신만의 문서를 작성하는 것이다.

이 문서의 원본은 markdown 포맷이며 [프로젝트의 Git](https://github.com/mybatis/spring/tree/master/src/site)에서 찾을 수 있다. repository 를 fork 하고, 업데이트하고 pull request 를 보내주십시오.

당신처럼 이 문서를 읽는 사람들에게 이 문서의 최고의 저자가 될수 있다!

## 번역

사용자들은 다음의 번역문서별로 마이바티스 스프링 연동모듈에 대해 알수 있다.

<ul class="i18n">
  <li class="en"><a href="./../index.html">English</a></li>
  <li class="es"><a href="./../es/index.html">Español</a></li>
  <li class="zh"><a href="./../zh/index.html">简体中文</a></li>
  <li class="ja"><a href="./../ja/index.html">日本語</a></li>
  <li class="ko"><a href="./getting-started.html">한국어</a></li>
</ul>

위 번역문서에는 없지만 자국의 언어로 문서로 보고 싶다면, 자국의 언어로 된 문서를 만들어서 우리에게 보내달라.
