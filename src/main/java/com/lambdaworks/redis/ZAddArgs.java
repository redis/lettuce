package com.lambdaworks.redis;

import com.lambdaworks.redis.protocol.CommandArgs;

/**
 * Argument list builder for the improved redis <a href="http://redis.io/commands/zadd">ZADD</a> command starting from Redis
 * 3.0.2. Static import the methods from {@link Builder} and call the methods: {@code xx()} or {@code nx()} .
 * 
 * @author Mark Paluch
 */
public class ZAddArgs {
    private boolean nx = false;
    private boolean xx = false;
    private boolean ch = false;

    public static class Builder {
        /**
         * Utility constructor.
         */
        private Builder() {

        }

        public static ZAddArgs nx() {
            return new ZAddArgs().nx();
        }

        public static ZAddArgs xx() {
            return new ZAddArgs().xx();
        }

        public static ZAddArgs ch() {
            return new ZAddArgs().ch();
        }
    }

    private ZAddArgs nx() {
        this.nx = true;
        return this;
    }

    private ZAddArgs ch() {
        this.ch = true;
        return this;
    }

    private ZAddArgs xx() {
        this.xx = true;
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {
        if (nx) {
            args.add("NX");
        }

        if (xx) {
            args.add("XX");
        }

        if (ch) {
            args.add("CH");
        }
    }
}
