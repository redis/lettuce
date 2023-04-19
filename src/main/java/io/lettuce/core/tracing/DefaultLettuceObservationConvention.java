/*
 * Copyright 2022-2023 the original author or authors.
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
                LowCardinalityCommandKeyNames.REDIS_COMMAND.withValue(context.getRequiredCommand().getType().name()));

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
        }

        return keyValues;
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(LettuceObservationContext context) {

        RedisCommand<?, ?, ?> command = context.getRequiredCommand();

        if (includeCommandArgsInSpanTags) {

            if (command.getArgs() != null) {
                return KeyValues.of(HighCardinalityCommandKeyNames.STATEMENT
                        .withValue(command.getType().name() + " " + command.getArgs().toCommandString()));
            }
        }

        return KeyValues.empty();
    }

    @Override
    public String getContextualName(LettuceObservationContext context) {
        return context.getRequiredCommand().getType().name().toLowerCase(Locale.ROOT);
    }

    public boolean includeCommandArgsInSpanTags() {
        return includeCommandArgsInSpanTags;
    }

}
