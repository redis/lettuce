package io.lettuce.core.proactive;

import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.ConnectionWatchdog;
import io.lettuce.core.pubsub.PubSubCommandHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
@ChannelHandler.Sharable
public class ProactiveWatchdogCommandHandler<K, V> extends ChannelInboundHandlerAdapter implements PushListener {

    private static final Logger logger = Logger.getLogger(ProactiveWatchdogCommandHandler.class.getName());

    private static final String REBIND_CHANNEL = "__rebind";

    private ChannelHandlerContext context;

    private ConnectionWatchdog watchdog;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel {} active");

        ChannelPipeline pipeline = ctx.channel().pipeline();
        CommandHandler command = pipeline.get(CommandHandler.class);
        watchdog = pipeline.get(ConnectionWatchdog.class);
        context = ctx;

        PubSubCommandHandler<?, ?> cmdhndlr = pipeline.get(PubSubCommandHandler.class);
        cmdhndlr.getEndpoint().addListener(this);
        // Command<String, String, String> rebind =
        // new Command<>(SUBSCRIBE,
        // new PubSubOutput<>(StringCodec.UTF8),
        // new PubSubCommandArgs<>(StringCodec.UTF8).addKey(REBIND_CHANNEL));
        //
        // if (command != null) {
        // ctx.write(rebind);
        // }

        super.channelActive(ctx);
    }

    @Override
    public void onPushMessage(PushMessage message) {
        if (!message.getType().equals("message")) {
            return;
        }

        List<String> content = message.getContent().stream()
                .map(ez -> ez instanceof ByteBuffer ? StringCodec.UTF8.decodeKey((ByteBuffer) ez) : ez.toString())
                .collect(Collectors.toList());

        if (content.stream().anyMatch(c -> c.contains("type=rebind"))) {
            logger.info("Attempt to rebind to new endpoint '" + getRemoteAddress(content) + "'");
            context.fireUserEventTriggered(new ProactiveRebindEvent(getRemoteAddress(content)));
            context.fireChannelInactive();

            // context.channel().pipeline().get()
            //
            //
            // StatefulRedisConnection<String, String> connection = watchdog.getConnection();
            // connection.setTimeout();
        }
    }

    private SocketAddress getRemoteAddress(List<String> messageContents) {

        final String payload = messageContents.stream().filter(c -> c.contains("to_ep")).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("to_ep not found"));

        final String toEndpoint = Arrays.stream(payload.split(";")).filter(c -> c.contains("to_ep")).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("to_ep not found"));

        final String addressAndPort = toEndpoint.split("=")[1];
        final String address = addressAndPort.split(":")[0];
        final int port = Integer.parseInt(addressAndPort.split(":")[1]);

        return new InetSocketAddress(address, port);
    }

    /**
     *
     * Command args for Pub/Sub connections. This implementation hides the first key as PubSub keys are not keys from the
     * key-space.
     *
     * @author Mark Paluch
     * @since 4.2
     */
    static class PubSubCommandArgs<K, V> extends CommandArgs<K, V> {

        /**
         * @param codec Codec used to encode/decode keys and values, must not be {@code null}.
         */
        public PubSubCommandArgs(RedisCodec<K, V> codec) {
            super(codec);
        }

        /**
         *
         * @return always {@code null}.
         */
        @Override
        public ByteBuffer getFirstEncodedKey() {
            return null;
        }

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel inactive");

        super.channelInactive(ctx);
    }

}
