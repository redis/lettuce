/*
 * Copyright 2021-Present, Redis Ltd. and Contributors
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
package io.lettuce.core.models.stream;

import java.util.Collections;
import java.util.List;

import io.lettuce.core.StreamMessage;

/**
 * Value object representing the claimed messages reported through {@code XAUTOCLAIM}.
 *
 * @author dengliming
 * @since 6.1
 */
public class ClaimedMessages<K, V> {

    private final String id;

    private final List<StreamMessage<K, V>> messages;

    private final List<String> deletedIds;

    /**
     * Create a new {@link ClaimedMessages}.
     *
     * @param id
     * @param messages
     */
    public ClaimedMessages(String id, List<StreamMessage<K, V>> messages) {
        this(id, messages, Collections.emptyList());
    }

    /**
     * Create a new {@link ClaimedMessages} with deleted entry IDs.
     *
     * @param id
     * @param messages
     * @param deletedIds
     * @since 7.2
     */
    public ClaimedMessages(String id, List<StreamMessage<K, V>> messages, List<String> deletedIds) {
        this.id = id;
        this.messages = messages;
        this.deletedIds = deletedIds;
    }

    public String getId() {
        return id;
    }

    public List<StreamMessage<K, V>> getMessages() {
        return messages;
    }

    /**
     * Returns the IDs of pending entries that were removed from the PEL because they no longer exist in the stream.
     * <p>
     * XAUTOCLAIM returns these IDs in a third reply element since Redis 7.0. Returns an empty list when the server
     * responds with only two elements (Redis before 7.0).
     *
     * @return the IDs of PEL entries that were deleted from the stream, never {@code null}.
     * @since 7.2
     */
    public List<String> getDeletedIds() {
        return deletedIds;
    }

}
