package com.lambdaworks;

import com.google.code.tempusfugit.temporal.Duration;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class Delay {

    public static void delay(Duration duration) {

        try {
            Thread.sleep(duration.inMillis());
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
