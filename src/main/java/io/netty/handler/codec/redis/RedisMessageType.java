/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.netty.handler.codec.redis;

/**
 * Type of <a href="http://redis.io/topics/protocol">RESP (REdis Serialization Protocol)</a>
 */
public enum RedisMessageType {

    SIMPLE_STRING((byte) '+', true), ERROR((byte) '-', true), INTEGER((byte) ':', true), BULK_STRING((byte) '$',
            false), ARRAY((byte) '*', false);

    private final byte value;
    private final boolean inline;

    RedisMessageType(byte value, boolean inline) {
        this.value = value;
        this.inline = inline;
    }

    public byte value() {
        return value;
    }

    public boolean isInline() {
        return inline;
    }

    public boolean isBulkString() {
        return this == BULK_STRING;
    }

    public boolean isArray() {
        return this == ARRAY;
    }

    public static RedisMessageType valueOf(byte value) {
        for (RedisMessageType t : values()) {
            if (t.value == value) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown RedisMessageType: " + value);
    }

}
