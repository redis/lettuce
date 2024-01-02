/*
 * Copyright 2023-2024 the original author or authors.
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

import java.nio.charset.StandardCharsets;

import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Restore mode for {@code FUNCTION RESTORE}.
 *
 * @author Mark Paluch
 * @since 6.3
 */
public enum FunctionRestoreMode implements ProtocolKeyword {

    /**
     * Appends the restored libraries to the existing libraries and aborts on collision. This is the default policy.
     */
    APPEND,

    /**
     * Deletes all existing libraries before restoring the payload.
     */
    FLUSH,

    /**
     * Appends the restored libraries to the existing libraries, replacing any existing ones in case of name collisions. Note
     * that this policy doesn't prevent function name collisions, only libraries.
     */
    REPLACE;

    public final byte[] bytes;

    FunctionRestoreMode() {
        bytes = name().getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

}
