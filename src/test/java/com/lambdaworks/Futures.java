package com.lambdaworks;

import java.util.Collection;
import java.util.concurrent.Future;

/**
 * @author Mark Paluch
 */
public class Futures {

    /**
     * Check if all {@code futures} are {@link Future#isDone() completed}.
     * 
     * @param futures
     * @return {@literal true} if all {@code futures} are {@link Future#isDone() completed}
     */
    public static boolean areAllCompleted(Collection<? extends Future<?>> futures) {

        for (Future<?> future : futures) {
            if (!future.isDone()) {
                return false;
            }
        }
        return true;
    }
}
