// Copyright (C) 2011 - Will Glozer.  All rights reserved.
// Copyright (C) 2013 - Vincent Rischmann.  All rights reserved.

package com.lambdaworks.redis;

import com.lambdaworks.redis.protocol.CommandArgs;

/**
 * Argument list builder for the new redis <a href="http://redis.io/commands/set">SET</a> command starting from Redis 2.6.12.
 * Static import the methods from {@link Builder} and chain the method calls: {@code ex(10).nx()}.
 * 
 * @author Vincent Rischmann
 */
public class SetArgs implements CompositeArgument {

    private Long ex;
    private Long px;
    private boolean nx = false;
    private boolean xx = false;

    public static class Builder {
        /**
         * Utility constructor.
         */
        private Builder() {

        }

        public static SetArgs ex(long ex) {
            return new SetArgs().ex(ex);
        }

        public static SetArgs px(long px) {
            return new SetArgs().px(px);
        }

        public static SetArgs nx() {
            return new SetArgs().nx();
        }

        public static SetArgs xx() {
            return new SetArgs().xx();
        }
    }

    public SetArgs ex(long ex) {
        this.ex = ex;
        return this;
    }

    public SetArgs px(long px) {
        this.px = px;
        return this;
    }

    public SetArgs nx() {
        this.nx = true;
        return this;
    }

    public SetArgs xx() {
        this.xx = true;
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {
        if (ex != null) {
            args.add("EX").add(ex);
        }

        if (px != null) {
            args.add("PX").add(px);
        }

        if (nx) {
            args.add("NX");
        }

        if (xx) {
            args.add("XX");
        }
    }
}
