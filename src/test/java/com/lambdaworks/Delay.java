package com.lambdaworks;

import com.google.code.tempusfugit.temporal.Duration;

/**
 * @author Mark Paluch
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
