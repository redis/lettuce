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

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;

/**
 * Redis Message for <a href="http://redis.io/topics/protocol">RESP (REdis Serialization Protocol)</a>
 */
public class RedisMessage extends AbstractReferenceCounted {

    private static final int MAX_PEEK_BULK_STRING = 20;

    private RedisMessageType type;

    // content(type!=arrays) or children(type==arrays).
    private ByteBuf content;
    private List<RedisMessage> children;

    public RedisMessage(RedisMessageType type) {
        this.type = type;
    }

    public RedisMessage(RedisMessageType type, ByteBuf content) {
        if (type.isArray()) {
            throw new IllegalArgumentException();
        }
        this.type = type;
        this.content = content;
        if (content != null) {
            content.retain(); // for hold bytebuf
        }
    }

    public RedisMessage(RedisMessageType type, List<RedisMessage> children) {
        if (!type.isArray()) {
            throw new IllegalArgumentException();
        }
        this.type = type;
        this.children = children; // do not retain here. children are already retained when new RedisMessage()
    }

    public RedisMessageType type() {
        return type;
    }

    /**
     * Content of this RedisMessage if this is not an array.
     */
    public ByteBuf content() {
        return content;
    }

    /**
     * Children of this RedisMessage if this is an array.
     */
    public List<RedisMessage> children() {
        return children;
    }

    @Override
    public RedisMessage touch(Object hint) {
        return this;
    }

    @Override
    protected void deallocate() {
        if (content != null) {
            content.release();
            content = null;
        }
        if (children != null) {
            for (RedisMessage msg : children) {
                msg.release();
            }
            children = null;
        }
    }

    @Override
    public String toString() {
        return new StringBuilder(StringUtil.simpleClassName(this)).append('[').append("type=").append(type).append(", content=")
                .append(describeContent()).append(", children=").append(children).append(']').toString();
    }

    private String describeContent() {
        if (content == null) {
            return "(null)";
        } else if (type.isInline()) {
            return new String(ByteBufUtil.getBytes(content), CharsetUtil.UTF_8);
        } else {
            int start = content.readerIndex();
            int length = content.readableBytes();
            if (length > MAX_PEEK_BULK_STRING) {
                length = MAX_PEEK_BULK_STRING;
            }
            return new String(ByteBufUtil.getBytes(content, start, length), CharsetUtil.UTF_8);
        }
    }

}
