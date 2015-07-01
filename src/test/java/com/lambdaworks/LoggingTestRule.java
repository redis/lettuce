package com.lambdaworks;

import org.apache.log4j.Logger;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class LoggingTestRule implements MethodRule {

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Logger logger = Logger.getLogger(method.getMethod().getDeclaringClass());
                logger.info("---------------------------------------");
                logger.info("-- Invoke method " + method.getMethod().getDeclaringClass().getSimpleName() + "."
                        + method.getName());
                logger.info("---------------------------------------");

                try {
                    base.evaluate();
                } finally {
                    logger.info("---------------------------------------");
                    logger.info("-- Finished method " + method.getMethod().getDeclaringClass().getSimpleName() + "."
                            + method.getName());
                    logger.info("---------------------------------------");
                }
            }
        };
    }
}
