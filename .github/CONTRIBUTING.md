# Contributing to Lettuce

Lettuce is released under the Apache 2.0 license. If you would like to contribute something, or simply want to hack on the code this document should help you get started.

## Code of Conduct

This project adheres to the Contributor Covenant [code of
conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to lettuce-redis-client-users@googlegroups.com.

## Using GitHub Issues

We use GitHub issues to track bugs and enhancements. If you have a general usage question please ask on [Stack Overflow](https://stackoverflow.com). 
The Lettuce team and the broader community monitor the [`lettuce`](https://stackoverflow.com/tags/lettuce) tag.

If you are reporting a bug, please help to speed up problem diagnosis by providing as much information as possible. 
Ideally, that would include a small sample project that reproduces the problem.

## Quickstart
 
For the impatient, if you want to submit a quick pull request:

* Don't create a pull request upfront. Create a feature request ticket first, so we can discuss your idea. API changes require discussion, use cases, etc. Code comes later.
* Pull request needs to be approved and merged by maintainers into the master branch.
* Upon agreeing the feature is a good fit for Lettuce, please:
  * Make sure there is a ticket in GitHub issues.
  * Make sure you use the code formatters provided here and have them applied to your changes. Don’t submit any formatting related changes.
  * Make sure you submit test cases (unit or integration tests) that back your changes.
  * Try to reuse existing test sample code (domain classes). Try not to amend existing test cases but create new ones dedicated to the changes you’re making to the codebase. Try to test as locally as possible but potentially also add integration tests.

When submitting code, please make every effort to follow existing conventions and style in order to keep the code as readable as possible. Formatting changes create a lot of noise and reduce the likelyhood of merging the pull request.
Formatting settings are provided for Eclipse in https://github.com/lettuce-io/lettuce-core/blob/main/formatting.xml

## Bugreports

If you report a bug, please ensure to specify the following:

* Use a concise subject.
* Check GitHub issues whether someone else already filed a ticket. If not, then feel free to create another one.
* Comments on closed tickets are typically a bad idea as there's little attention on closed tickets. Please create a new one.
* Lettuce version (e.g. 3.2.Final).
* Contextual information (what were you trying to do using Lettuce).
* Simplest possible steps to reproduce:
   * Ideally, a [Minimal, Complete, and Verifiable example](https://stackoverflow.com/help/mcve).
   * JUnit tests to reproduce are great but not obligatory.

## Features

If you want to request a feature, please ensure to specify the following:

* What do you want to achieve?
* Contextual information (what were you trying to do using Lettuce).
* Ideally, but not required: Describe your idea how you would implement it.

## Questions

If you have a question, then check one of the following places first as GitHub issues are for bugs and feature requests. Typically, forums, chats, and mailing lists are the best place to ask your question as you can expect to get an answer faster there:

**Checkout the docs**

* [Reference documentation](https://lettuce.io/docs/)
* [Wiki](https://github.com/lettuce-io/lettuce-core/wiki)
* [Javadoc](https://lettuce.io/core/release/api/)

**Communication**

* Google Group/Mailing List (General discussion, announcements and releases): [lettuce-redis-client-users](https://groups.google.com/g/lettuce-redis-client-users) or [lettuce-redis-client-users@googlegroups.com](mailto:lettuce-redis-client-users@googlegroups.com)
* Stack Overflow (Questions): [Questions about Lettuce](https://stackoverflow.com/questions/tagged/lettuce)
* Gitter (General discussion): [![Join the chat at https://gitter.im/lettuce-io/Lobby](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/lettuce-io/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

### Building from Source

Lettuce source can be built from the command line using Maven on JDK 1.8 or above.

The project can be built from the root directory using the standard Maven command:

```bash
	$ mvn clean test
```

You can run a full build including integration tests using the `make` command:

```bash
	$ make test
```

## Conventions

* New public API must be annotated with `@since` and the version it was introduced with (e.g. `@since 6.0`, `@since 5.3.2`)
* Add yourself to each file you touch as `@author`
* Split feature/bugfix commits from polishing commits (see [PR #1400](https://github.com/lettuce-io/lettuce-core/commit/57489fb71a78b7d009bc88a2f95675fb0076515f) and its [polishing commit](https://github.com/lettuce-io/lettuce-core/commit/c2b2e1a0ea8d7f3b297ea16836772d42f629a7de))
* Commit messages follow the convention of a summary along with the ticket number and optionally a description in the body:

```
<text> #<ticketnumber>

We now correctly …
```

Examples:
 
* [`Rename LPOS FIRST option to RANK #1410`](https://github.com/lettuce-io/lettuce-core/commit/aa02dd07989b578a266c5e9a3ba2b406d3f0fce4)
* [`Upgrade dependencies #1431`](https://github.com/lettuce-io/lettuce-core/commit/310174566c6ad153289cd1bccd7cfad256a74911)

## Merging a Pull Request

* Does it build? Is there a test case and does the design follow how Lettuce is built?
* Minor formatting issues can be sorted out during the merge, for larger formatting issues (e.g. PR contains at least one reformatted file) we need to ask the contributor to fix it.
* Squash multiple commits into a single one, **except** if the commits are well structured (e.g. commit introducing a feature/fixing a bug plus a commit that polishes up code)
* Edit original commit message if necessary to reflect the ticket number. Add `Original pull request: #<PR>` to all commits belonging to the PR to keep changes together. 
