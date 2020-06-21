/**
 * Copyright 2010-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mybatis.logging;

import java.util.function.Supplier;

import org.apache.ibatis.logging.Log;

/**
 * Wrapper of {@link Log}, allow log with lambda expressions.
 *
 * @author Putthiphong Boonphong
 */
public class Logger {

  private final Log log;

  Logger(Log log) {
    this.log = log;
  }

  public void error(Supplier<String> s, Throwable e) {
    log.error(s.get(), e);
  }

  public void error(Supplier<String> s) {
    log.error(s.get());
  }

  public void warn(Supplier<String> s) {
    log.warn(s.get());
  }

  public void debug(Supplier<String> s) {
    if (log.isDebugEnabled()) {
      log.debug(s.get());
    }
  }

  public void trace(Supplier<String> s) {
    if (log.isTraceEnabled()) {
      log.trace(s.get());
    }
  }

}
