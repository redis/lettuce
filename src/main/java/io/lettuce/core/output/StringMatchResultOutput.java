/*
 * Copyright 2018-2020 the original author or authors.
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
package io.lettuce.core.output;

import static io.lettuce.core.StringMatchResult.MatchedPosition;
import static io.lettuce.core.StringMatchResult.Position;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.StringMatchResult;
import io.lettuce.core.codec.RedisCodec;

/**
 * Command output for {@code STRALGO} returning {@link StringMatchResult}.
 *
 * @author dengliming
 * @since 5.3.2
 */
public class StringMatchResultOutput<K, V> extends CommandOutput<K, V, StringMatchResult> {

    private final boolean withIdx;

    private String matchString;

    private int len;

    private List<Long> positions;

    private final List<MatchedPosition> matchedPositions = new ArrayList<>();

    public StringMatchResultOutput(RedisCodec<K, V> codec, boolean withIdx) {
        super(codec, null);
        this.withIdx = withIdx;
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (!withIdx && matchString == null) {
            matchString = (String) codec.decodeKey(bytes);
        }
    }

    @Override
    public void set(long integer) {

        this.len = (int) integer;

        if (positions == null) {
            positions = new ArrayList<>();
        }
        positions.add(integer);
    }

    @Override
    public void complete(int depth) {

        if (depth == 2) {
            matchedPositions.add(buildMatchedString(positions));
            positions = null;
        }

        if (depth == 0) {
            output = new StringMatchResult(matchString, matchedPositions, len);
        }
    }

    private static MatchedPosition buildMatchedString(List<Long> positions) {

        if (positions == null) {
            throw new IllegalStateException("No matched positions");
        }

        int size = positions.size();
        // not WITHMATCHLEN
        long matchLen = size % 2 == 0 ? 0L : positions.get(size - 1);
        return new MatchedPosition(new Position(positions.get(0), positions.get(1)),
                new Position(positions.get(2), positions.get(3)), matchLen);
    }

}
