package org.mybatis.logging;

import org.apache.ibatis.logging.Log;

import java.util.function.Supplier;

/**
 * Wrapper of {@link Log}, allow log with lambda expressions.
 *
 * @author Putthiphong Boonphong
 */
public class Logger {

  private Log log;

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
