/*
 * Test logging utilities for capturing Log4j2 output in unit tests.
 */
package io.lettuce.test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

public final class LoggingTestUtils {

    private LoggingTestUtils() {
    }

    public static class CapturingAppender extends AbstractAppender implements AutoCloseable {

        public final List<String> messages = new CopyOnWriteArrayList<>();

        private final Level filterLevel;

        private final String appenderName;

        private final String loggerName;

        public CapturingAppender(String name, String loggerName, Level filterLevel) {
            super(name, null, null, false, null);
            this.filterLevel = filterLevel;
            this.appenderName = name;
            this.loggerName = loggerName;
        }

        public List<String> messages() {
            return messages;
        }

        @Override
        public void append(LogEvent event) {
            if (filterLevel == null || event.getLevel() == filterLevel) {
                messages.add(event.getMessage().getFormattedMessage());
            }
        }

        @Override
        public void close() {
            detachAppenderFor(appenderName, loggerName, this);
        }

    }

    public static CapturingAppender attachAppenderFor(Class<?> clazz, Level level) {
        String name = computeCallerName(clazz);
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration cfg = ctx.getConfiguration();
        Level effectiveLevel = (level != null ? level : Level.ALL);
        CapturingAppender app = new CapturingAppender(name, clazz.getName(), level);
        app.start();
        cfg.addAppender(app);
        LoggerConfig lc = cfg.getLoggerConfig(clazz.getName());
        if (!lc.getName().equals(clazz.getName())) {
            LoggerConfig custom = new LoggerConfig(clazz.getName(), effectiveLevel, true);
            cfg.addLogger(clazz.getName(), custom);
            lc = custom;
        }
        lc.addAppender(app, effectiveLevel, null);
        ctx.updateLoggers();
        return app;
    }

    private static void detachAppenderFor(String appenderName, String loggerName, CapturingAppender app) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration cfg = ctx.getConfiguration();
        LoggerConfig lc = cfg.getLoggerConfig(loggerName);
        lc.removeAppender(appenderName);
        cfg.getRootLogger().removeAppender(appenderName);
        app.stop();
        ctx.updateLoggers();
    }

    private static String computeCallerName(Class<?> clazz) {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        return Arrays.stream(st).filter(ste -> ste.getClassName().startsWith(clazz.getName())).findFirst()
                .map(ste -> ste.getClassName() + "#" + ste.getMethodName()).orElse("Appender-" + System.nanoTime());
    }

}
