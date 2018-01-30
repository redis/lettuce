/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core;

import static io.lettuce.core.protocol.CommandKeyword.COUNT;
import static io.lettuce.core.protocol.CommandKeyword.MATCH;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the redis scan commans (scan, hscan, sscan, zscan) . Static import the methods from {@link Builder}
 * and chain the method calls: {@code matches("weight_*").limit(0, 2)}.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public class ScanArgs implements CompositeArgument {

    private Long count;
    private String match;

    /**
     * Static builder methods.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {

        }

        /**
         * Create a new instance of {@link ScanArgs} with limit.
         *
         * @param count number of elements to scan
         * @return a new instance of {@link ScanArgs}
         */
        public static ScanArgs limit(long count) {
            return new ScanArgs().limit(count);
        }

        /**
         * Create a new instance of {@link ScanArgs} with match filter.
         *
         * @param matches the filter
         * @return a new instance of {@link ScanArgs}
         */
        public static ScanArgs matches(String matches) {
            return new ScanArgs().match(matches);
        }
    }

    /**
     * Match filter
     *
     * @param match the filter
     * @return the current instance of {@link ScanArgs}
     */
    public ScanArgs match(String match) {
        LettuceAssert.notNull(match, "Match must not be null");
        this.match = match;
        return this;
    }

    /**
     * Limit the scan by count
     *
     * @param count number of elements to scan
     * @return the current instance of {@link ScanArgs}
     */
    public ScanArgs limit(long count) {
        this.count = count;
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {

        if (match != null) {
            args.add(MATCH).add(match);
        }

        if (count != null) {
            args.add(COUNT).add(count);
        }

    }
}
