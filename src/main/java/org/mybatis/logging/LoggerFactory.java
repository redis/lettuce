package org.mybatis.logging;

import org.apache.ibatis.logging.LogFactory;

/**
 * LoggerFactory is a wrapper around {@link LogFactory} to support {@link Logger}.
 *
 * @author Putthiphong Boonphong
 */
public class LoggerFactory {

  public static Logger getLogger(Class<?> aClass) {
    return new Logger(LogFactory.getLog(aClass));
  }

  public static Logger getLogger(String logger) {
    return new Logger(LogFactory.getLog(logger));
  }

}
