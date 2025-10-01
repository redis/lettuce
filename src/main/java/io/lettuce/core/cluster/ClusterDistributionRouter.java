package io.lettuce.core.cluster;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;

import java.nio.ByteBuffer;

/**
 * Selects a routing strategy based on command state.
 */
class ClusterDistributionRouter {

    private final ClusterDistributionStrategy redirectedStrategy;

    private final ClusterDistributionStrategy hashSlotStrategy;

    private final ClusterDistributionStrategy keylessStrategy;

    private final ClusterDistributionStrategy defaultStrategy;

    ClusterDistributionRouter(ClusterDistributionChannelWriter writer) {
        this.redirectedStrategy = writer::executeRedirect;
        this.hashSlotStrategy = writer::executeHashSlot;
        this.keylessStrategy = writer::executeKeyless;
        this.defaultStrategy = writer::executeDefault;
    }

    ClusterDistributionStrategy getStrategy(RedisCommand<?, ?, ?> cmd) {
        // Redirect first if applicable
        if (cmd instanceof ClusterCommand && !cmd.isDone()) {
            ClusterCommand<?, ?, ?> cc = (ClusterCommand<?, ?, ?>) cmd;
            if (cc.isMoved() || cc.isAsk()) {
                return redirectedStrategy;
            }
        }

        // Evaluate key vs keyless
        CommandArgs<?, ?> args = cmd.getArgs();
        ByteBuffer encodedKey = (args != null ? args.getFirstEncodedKey() : null);

        // CLIENT commands bypass cluster routing
        if (cmd.getType() == CommandType.CLIENT) {
            return defaultStrategy;
        }

        if (encodedKey != null) {
            return hashSlotStrategy;
        }

        if (KeylessCommands.isKeyless(cmd.getType())) {
            return keylessStrategy;
        }

        return defaultStrategy;
    }

}
