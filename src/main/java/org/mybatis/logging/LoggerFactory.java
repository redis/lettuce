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

import org.apache.ibatis.logging.LogFactory;

/**
 * LoggerFactory is a wrapper around {@link LogFactory} to support {@link Logger}.
 *
 * @author Putthiphong Boonphong
 */
public class LoggerFactory {

  private LoggerFactory() {
    // NOP
  }

  public static Logger getLogger(Class<?> aClass) {
    return new Logger(LogFactory.getLog(aClass));
  }

  public static Logger getLogger(String logger) {
    return new Logger(LogFactory.getLog(logger));
  }

}
