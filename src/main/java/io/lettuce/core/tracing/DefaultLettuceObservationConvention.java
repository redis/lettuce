package io.lettuce.core.tracing;

import static io.lettuce.core.tracing.RedisObservation.*;

import java.net.InetSocketAddress;
import java.util.Locale;

import io.lettuce.core.protocol.RedisCommand;
import io.micrometer.common.KeyValues;

/**
 * Default {@link LettuceObservationConvention} implementation.
 *
 * @author Mark Paluch
 * @since 6.3
 */
public final class DefaultLettuceObservationConvention implements LettuceObservationConvention {

    private final boolean includeCommandArgsInSpanTags;

    /**
     *
     */
    DefaultLettuceObservationConvention(boolean includeCommandArgsInSpanTags) {
        this.includeCommandArgsInSpanTags = includeCommandArgsInSpanTags;
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(LettuceObservationContext context) {

        Tracing.Endpoint ep = context.getRequiredEndpoint();
        KeyValues keyValues = KeyValues.of(LowCardinalityCommandKeyNames.DATABASE_SYSTEM.withValue("redis"), //
                LowCardinalityCommandKeyNames.REDIS_COMMAND.withValue(context.getRequiredCommand().getType().toString()));

        if (ep instanceof SocketAddressEndpoint) {

            SocketAddressEndpoint endpoint = (SocketAddressEndpoint) ep;

            if (endpoint.getSocketAddress() instanceof InetSocketAddress) {

                InetSocketAddress inet = (InetSocketAddress) endpoint.getSocketAddress();
                keyValues = keyValues
                        .and(KeyValues.of(LowCardinalityCommandKeyNames.NET_SOCK_PEER_ADDR.withValue(inet.getHostString()),
                                LowCardinalityCommandKeyNames.NET_SOCK_PEER_PORT.withValue("" + inet.getPort()),
                                LowCardinalityCommandKeyNames.NET_TRANSPORT.withValue("IP.TCP")));
            } else {
                keyValues = keyValues
                        .and(KeyValues.of(LowCardinalityCommandKeyNames.NET_PEER_NAME.withValue(endpoint.toString()),
                                LowCardinalityCommandKeyNames.NET_TRANSPORT.withValue("Unix")));
            }

            RedisUriDetails details = endpoint.getRedisUriDetails();

            if (details != null) {
                keyValues = keyValues.and(LowCardinalityCommandKeyNames.DB_INDEX.withValue(details.getDatabase()));
            }
        }

        return keyValues;
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(LettuceObservationContext context) {

        RedisCommand<?, ?, ?> command = context.getRequiredCommand();
        KeyValues keyValues = KeyValues.empty();

        if (includeCommandArgsInSpanTags) {

            if (command.getArgs() != null) {
                keyValues = keyValues.and(HighCardinalityCommandKeyNames.STATEMENT
                        .withValue(command.getType().toString() + " " + command.getArgs().toCommandString()));
            }
        }

        Tracing.Endpoint ep = context.getRequiredEndpoint();

        if (ep instanceof SocketAddressEndpoint) {

            RedisUriDetails details = ((SocketAddressEndpoint) ep).getRedisUriDetails();

            if (details != null) {

                if (details.getUsername() != null) {
                    keyValues = keyValues.and(HighCardinalityCommandKeyNames.DB_USER.withValue(details.getUsername()));
                }

                if (details.getUrl() != null) {
                    keyValues = keyValues.and(HighCardinalityCommandKeyNames.DB_CONNECTION_STRING.withValue(details.getUrl()));
                }
            }
        }

        return keyValues;
    }

    @Override
    public String getContextualName(LettuceObservationContext context) {
        return context.getRequiredCommand().getType().toString().toLowerCase(Locale.ROOT);
    }

    public boolean includeCommandArgsInSpanTags() {
        return includeCommandArgsInSpanTags;
    }

}
