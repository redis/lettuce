package com.lambdaworks.redis.protocol;

import io.netty.channel.Channel;

/**
 * @author Mark Paluch
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
            if(buffer.length() != 0){
                buffer.append(' ');
            }

            buffer.append("(inactive)");
        }

        return buffer.toString();
    }
}
