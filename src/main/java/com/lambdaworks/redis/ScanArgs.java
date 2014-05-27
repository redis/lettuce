package com.lambdaworks.redis;

import static com.lambdaworks.redis.protocol.CommandKeyword.COUNT;
import static com.lambdaworks.redis.protocol.CommandKeyword.MATCH;
import com.lambdaworks.redis.protocol.CommandArgs;

/**
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
        public static ScanArgs count(long count) {
            return new ScanArgs().count(count);
        }

        public static ScanArgs matches(String matches) {
            return new ScanArgs().match(matches);
        }
    }

    public ScanArgs match(String match) {
        this.match = match;
        return this;
    }

    public ScanArgs count(long count) {
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
