package com.lambdaworks.redis.protocol;

import io.netty.channel.Channel;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 08.07.15 09:43
 */
class ChannelLogDescriptor {

    static String logDescriptor(Channel channel) {

        if (channel == null) {
            return "unknown";
        }

        StringBuffer buffer = new StringBuffer(64);

        if (channel.localAddress() != null) {
            buffer.append(channel.localAddress()).append(" -> ");
        }
        if (channel.remoteAddress() != null) {
            buffer.append(channel.remoteAddress());
        }

        if (!channel.isActive()) {
            buffer.append(" (inactive)");
        }

        return buffer.toString();
    }
}
