package com.lambdaworks.redis;

import static com.lambdaworks.redis.protocol.CommandKeyword.*;
import static com.lambdaworks.redis.protocol.CommandType.*;

import com.lambdaworks.redis.protocol.CommandArgs;

/**
 *
 * Argument list builder for the redis <a href="http://redis.io/commands/client-kill">CLIENT KILL</a> command. Static import the
 * methods from {@link Builder} and chain the method calls: {@code id(1).skipme()}.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public class KillArgs implements CompositeArgument {

    private static enum Type {
        NORMAL, SLAVE, PUBSUB
    }

    private Boolean skipme;
    private String addr;
    private Long id;
    private Type type;

    /**
     * Static builder methods.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {

        }

        public static KillArgs skipme() {
            return new KillArgs().skipme();
        }

        public static KillArgs addr(String addr) {
            return new KillArgs().addr(addr);
        }

        public static KillArgs id(long id) {
            return new KillArgs().id(id);
        }

        public static KillArgs typePubsub() {
            return new KillArgs().type(Type.PUBSUB);
        }

        public static KillArgs typeNormal() {
            return new KillArgs().type(Type.NORMAL);
        }

        public static KillArgs typeSlave() {
            return new KillArgs().type(Type.SLAVE);
        }

    }

    public KillArgs skipme() {
        return this.skipme(true);
    }

    public KillArgs skipme(boolean state) {
        this.skipme = state;
        return this;
    }

    public KillArgs addr(String addr) {
        this.addr = addr;
        return this;
    }

    public KillArgs id(long id) {
        this.id = id;
        return this;
    }

    public KillArgs type(Type type) {
        this.type = type;
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {

        if (skipme != null) {
            args.add(SKIPME).add(skipme.booleanValue() ? "yes" : "no");
        }

        if (id != null) {
            args.add(ID).add(id);
        }

        if (addr != null) {
            args.add(ADDR).add(addr);
        }

        if (type != null) {
            args.add(TYPE).add(type.name().toLowerCase());
        }

    }
}
