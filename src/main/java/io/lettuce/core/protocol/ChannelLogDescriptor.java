package io.lettuce.core.protocol;

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

    static String getId(Channel channel) {
        return String.format("0x%08x", channel.hashCode());
    }

}
