package com.lambdaworks;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class CapturingLogAppender extends WriterAppender {

    private static StringWriter writer = new StringWriter();
    private static boolean enabled = true;

    public CapturingLogAppender() {
        super();
        setWriter(new PrintWriter(writer));
    }

    public static String getContentAndReset() {
        String result = writer.getBuffer().toString();
        writer.getBuffer().setLength(0);

        return result;
    }

    @Override
    public synchronized void doAppend(LoggingEvent event) {
        if (enabled) {
            super.doAppend(event);
        }
    }

    public static void disable() {
        enabled = false;
    }

    public static void enable() {
        enabled = true;
    }
}
