package com.lambdaworks.redis;

import static com.lambdaworks.redis.protocol.CommandKeyword.*;

import com.lambdaworks.redis.protocol.CommandArgs;

/**
 * Argument list builder for the redis scan commans (scan, hscan, sscan, zscan) . Static import the methods from {@link Builder}
 * and chain the method calls: <code>matches("weight_*").limit(0, 2)</code>.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 20.05.14 20:45
 */
public class ScanArgs {

    private Long count;
    private String match;

    /**
     * Static builder methods.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {

        }

        public static ScanArgs limit(long count) {
            return new ScanArgs().limit(count);
        }

        public static ScanArgs matches(String matches) {
            return new ScanArgs().match(matches);
        }
    }

    public ScanArgs match(String match) {
        this.match = match;
        return this;
    }

    public ScanArgs limit(long count) {
        this.count = count;
        return this;
    }

    <K, V> void build(CommandArgs<K, V> args) {

        if (match != null) {
            args.add(MATCH).add(match);
        }

        if (count != null) {
            args.add(COUNT).add(count);
        }

    }

}
