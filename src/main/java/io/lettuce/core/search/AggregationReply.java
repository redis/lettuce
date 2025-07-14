/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

package io.lettuce.core.search;

import java.util.ArrayList;
import java.util.List;

public class AggregationReply<K, V> {

    private static final long NO_CURSOR = -1;

    long aggregationGroups = 1;

    List<SearchReply<K, V>> replies = new ArrayList<>();

    long cursorId = NO_CURSOR;

    public AggregationReply() {
    }

    public long getAggregationGroups() {
        return aggregationGroups;
    }

    public List<SearchReply<K, V>> getReplies() {
        return replies;
    }

    public long getCursorId() {
        return cursorId;
    }

    void setGroupCount(long value) {
        this.aggregationGroups = value;
    }

    void setCursorId(long value) {
        this.cursorId = value;
    }

    void addSearchReply(SearchReply<K, V> searchReply) {
        this.replies.add(searchReply);
    }

}
