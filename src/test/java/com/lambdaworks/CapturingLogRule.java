package com.lambdaworks;

import org.apache.log4j.Logger;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * @author Mark Paluch
 */
public class CapturingLogRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        final Statement statement = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    CapturingLogAppender.getContentAndReset();
                    base.evaluate();
                } catch (AssumptionViolatedException e) {
                    throw e;
                } catch (Throwable t) {
                    Logger.getLogger(getClass()).info(t.getMessage(), t);
                    System.out.println("===========================");
                    System.out.println("Captured log during test");
                    System.out.println("===========================");
                    System.out.println(CapturingLogAppender.getContentAndReset());
                    throw t;
                } finally {
                    CapturingLogAppender.getContentAndReset();
                }
            }
        };

        return statement;
    }
}
