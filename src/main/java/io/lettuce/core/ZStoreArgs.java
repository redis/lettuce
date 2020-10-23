package io.lettuce.core;

/**
 * Argument list builder for the Redis <a href="http://redis.io/commands/zunionstore">ZUNIONSTORE</a>
 * and <a href="http://redis.io/commands/zinterstore">ZINTERSTORE</a> commands.
 * Static import the methods from {@link Builder} an chain the method calls: {@code weights(1, 2).max()}.
 *
 * @author Will Glozer
 * @author Xy Ma
 * @author Mark Paluch
 * @author Mikhael Sokolov
 */
public class ZStoreArgs extends ZAggregateArgs {

    public static class Builder extends ZAggregateArgs.Builder {

        /**
         * Utility constructor.
         */
        Builder() {
        }
    }
}
