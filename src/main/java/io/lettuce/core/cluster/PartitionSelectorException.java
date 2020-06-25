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
package io.lettuce.core.cluster;

import io.lettuce.core.cluster.models.partitions.Partitions;

/**
 * Exception thrown when a partition selection fails (slot not covered, no read candidates available).
 *
 * @author Mark Paluch
 * @since 5.1
 */
@SuppressWarnings("serial")
public class PartitionSelectorException extends PartitionException {

    private final Partitions partitions;

    /**
     * Create a {@code UnknownPartitionException} with the specified detail message.
     *
     * @param msg the detail message.
     * @param partitions read-only view of the current topology view.
     */
    public PartitionSelectorException(String msg, Partitions partitions) {

        super(msg);
        this.partitions = partitions;
    }

    public Partitions getPartitions() {
        return partitions;
    }

}
