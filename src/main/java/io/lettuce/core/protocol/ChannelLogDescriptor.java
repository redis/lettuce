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
package io.lettuce.core.protocol;

import io.netty.channel.Channel;

/**
 *
 * @author Mark Paluch
 */
class ChannelLogDescriptor {

    static String logDescriptor(Channel channel) {

        if (channel == null) {
            return "unknown";
        }

        StringBuffer buffer = new StringBuffer(64);

        buffer.append("channel=").append(getId(channel)).append(", ");

        if (channel.localAddress() != null && channel.remoteAddress() != null) {
            buffer.append(channel.localAddress()).append(" -> ").append(channel.remoteAddress());
        } else {
            buffer.append(channel);
        }

        if (!channel.isActive()) {
            if (buffer.length() != 0) {
                buffer.append(' ');
            }

            buffer.append("(inactive)");
        }

        return buffer.toString();
    }

    private static String getId(Channel channel) {
        return String.format("0x%08x", channel.hashCode());
    }

}
