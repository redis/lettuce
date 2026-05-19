/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
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
package io.lettuce.core.bf;

import java.util.Objects;

/**
 * Represents the result of an insert operation in a Bloom Filter.
 *
 * @author Yordan Tsintsov
 * @since 7.6
 */
public class BfInsertValue {

    private final boolean isAdded;
    private final String message;

    public BfInsertValue(boolean isAdded, String message) {
        this.isAdded = isAdded;
        this.message = message;
    }

    public boolean getAdded() {
        return isAdded;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BfInsertValue that = (BfInsertValue) o;
        return isAdded == that.isAdded && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isAdded, message);
    }

}
