package io.lettuce.test;

import java.time.Duration;

/**
 * @author Mark Paluch
 */
public class Delay {

    private Delay() {
    }

    /**
     * Sleep for the given {@link Duration}.
     *
     * @param duration
     */
    public static void delay(Duration duration) {

        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

}
