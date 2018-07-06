/*
 * Copyright 2018 the original author or authors.
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
package com.lambdaworks.redis;

import com.lambdaworks.redis.protocol.LettuceCharsets;
import com.lambdaworks.redis.protocol.ProtocolKeyword;

/**
 * Unblock type for {@code CLIENT UNBLOCK} command.
 *
 * @author Mark Paluch
 * @since 4.5
 */
public enum UnblockType implements ProtocolKeyword {

    TIMEOUT, ERROR;

    private final byte[] bytes;

    UnblockType() {
        bytes = name().getBytes(LettuceCharsets.ASCII);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }
}
