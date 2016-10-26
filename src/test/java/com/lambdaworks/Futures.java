/*
 * Copyright 2011-2016 the original author or authors.
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
