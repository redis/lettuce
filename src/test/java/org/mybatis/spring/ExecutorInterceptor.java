/*
 *    Copyright 2010-2012 The MyBatis Team
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.spring;

import java.util.Properties;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

/**
 * Keeps track of Executor commits, rollbacks and close status.
 *
 * The Executor is not accessible from DefaultSqlSession, so it is much easier to use an Interceptor
 * rather than subclass a new SqlSessionFactory, etc. Also, there is the potential to change the
 * default MyBatis behavior, which could change the test results.
 *
 * @version $Id$
 */
@Intercepts({
    @Signature(type = Executor.class, method = "commit", args = { boolean.class }),
    @Signature(type = Executor.class, method = "rollback", args = { boolean.class }),
    @Signature(type = Executor.class, method = "close", args = { boolean.class })
})
final class ExecutorInterceptor implements Interceptor {

  private int commitCount;

  private int rollbackCount;

  private boolean closed;

  public Object intercept(Invocation invocation) throws Throwable {
    if ("commit".equals(invocation.getMethod().getName())) {
      ++this.commitCount;
    } else if ("rollback".equals(invocation.getMethod().getName())) {
      ++this.rollbackCount;
    } else if ("close".equals(invocation.getMethod().getName())) {
      this.closed = true;
    }

    return invocation.proceed();
  }

  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  public void setProperties(Properties properties) {
    // do nothing
  }

  void reset() {
    this.commitCount = 0;
    this.rollbackCount = 0;
    this.closed = false;
  }

  int getCommitCount() {
    return this.commitCount;
  }

  int getRollbackCount() {
    return this.rollbackCount;
  }

  boolean isExecutorClosed() {
    return this.closed;
  }

}
