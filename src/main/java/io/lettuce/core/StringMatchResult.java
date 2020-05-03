/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Result for STRALGO command
 *
 * @author dengliming
 */
public class StringMatchResult {

    private String matchString;
    private List<MatchedPosition> matches = new ArrayList<>();
    private long len;

    public StringMatchResult matchString(String matchString) {
        this.matchString = matchString;
        return this;
    }

    public StringMatchResult addMatch(MatchedPosition match) {
        this.matches.add(match);
        return this;
    }

    public StringMatchResult len(long len) {
        this.len = len;
        return this;
    }

    public String getMatchString() {
        return matchString;
    }

    public List<MatchedPosition> getMatches() {
        return matches;
    }

    public long getLen() {
        return len;
    }

    /**
     * match position in each strings
     */
    public static class MatchedPosition {
        private Position a;
        private Position b;
        private long matchLen;

        public MatchedPosition(Position a, Position b, long matchLen) {
            this.a = a;
            this.b = b;
            this.matchLen = matchLen;
        }

        public Position getA() {
            return a;
        }

        public void setA(Position a) {
            this.a = a;
        }

        public Position getB() {
            return b;
        }

        public void setB(Position b) {
            this.b = b;
        }

        public long getMatchLen() {
            return matchLen;
        }

        public void setMatchLen(long matchLen) {
            this.matchLen = matchLen;
        }
    }

    /**
     * position range
     */
    public static class Position {
        private final long start;
        private final long end;

        public Position(long start, long end) {
            this.start = start;
            this.end = end;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }
    }
}
