/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * @author Mark Paluch
 */
public class LoggingTestRule implements MethodRule {

    private boolean threadDumpOnFailure = false;

    public LoggingTestRule(boolean threadDumpOnFailure) {
        this.threadDumpOnFailure = threadDumpOnFailure;
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Logger logger = LogManager.getLogger(method.getMethod().getDeclaringClass());
                logger.info("---------------------------------------");
                logger.info("-- Invoke method " + method.getMethod().getDeclaringClass().getSimpleName() + "."
                        + method.getName());
                logger.info("---------------------------------------");

                try {
                    base.evaluate();
                } catch (Throwable t) {
                    if (threadDumpOnFailure) {
                        printThreadDump(logger);
                    }

                    throw t;
                } finally {
                    logger.info("---------------------------------------");
                    logger.info("-- Finished method " + method.getMethod().getDeclaringClass().getSimpleName() + "."
                            + method.getName());
                    logger.info("---------------------------------------");
                }
            }
        };
    }

    private void printThreadDump(Logger logger) {
        logger.info("---------------------------------------");

        ByteArrayOutputStream buffer = getThreadDump();
        logger.info("-- Thread dump: " + buffer.toString());

        logger.info("---------------------------------------");
    }

    private ByteArrayOutputStream getThreadDump() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[] threadIds = threadBean.getAllThreadIds();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(buffer);

        for (long tid : threadIds) {
            ThreadInfo info = threadBean.getThreadInfo(tid, 50);
            if (info == null) {
                stream.println("  Inactive");
                continue;
            }
            stream.println("Thread " + getTaskName(info.getThreadId(), info.getThreadName()) + ":");
            Thread.State state = info.getThreadState();
            stream.println("  State: " + state);
            stream.println("  Blocked count: " + info.getBlockedCount());
            stream.println("  Waited count: " + info.getWaitedCount());

            if (state == Thread.State.WAITING) {
                stream.println("  Waiting on " + info.getLockName());
            } else if (state == Thread.State.BLOCKED) {
                stream.println("  Blocked on " + info.getLockName());
                stream.println("  Blocked by " + getTaskName(info.getLockOwnerId(), info.getLockOwnerName()));
            }
            stream.println("  Stack:");
            for (StackTraceElement frame : info.getStackTrace()) {
                stream.println("    " + frame.toString());
            }
        }
        stream.flush();
        return buffer;
    }

    private static String getTaskName(long id, String name) {
        if (name == null) {
            return Long.toString(id);
        }
        return id + " (" + name + ")";
    }
}
