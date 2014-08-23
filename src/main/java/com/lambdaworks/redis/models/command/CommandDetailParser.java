package com.lambdaworks.redis.models.command;

import static com.google.common.base.Preconditions.*;

import java.util.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

/**
 * Parser for redis <a href="http://redis.io/commands/command">COMMAND</a>/<a
 * href="http://redis.io/commands/command-info">COMMAND INFO</a>command output.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class CommandDetailParser {

    /**
     * Number of array elements for a specific command.
     */
    public static final int COMMAND_INFO_SIZE = 6;

    protected static final Map<String, CommandDetail.Flag> FLAG_MAPPING = new HashMap<String, CommandDetail.Flag>() {
        {
            put("admin", CommandDetail.Flag.ADMIN);
            put("asking", CommandDetail.Flag.ASKING);
            put("denyoom", CommandDetail.Flag.DENYOOM);
            put("fast", CommandDetail.Flag.FAST);
            put("loading", CommandDetail.Flag.LOADING);
            put("noscript", CommandDetail.Flag.NOSCRIPT);
            put("movablekeys", CommandDetail.Flag.MOVABLEKEYS);
            put("pubsub", CommandDetail.Flag.PUBSUB);
            put("random", CommandDetail.Flag.RANDOM);
            put("readonly", CommandDetail.Flag.READONLY);
            put("skip_monitor", CommandDetail.Flag.SKIP_MONITOR);
            put("sort_for_script", CommandDetail.Flag.SORT_FOR_SCRIPT);
            put("stale", CommandDetail.Flag.STALE);
            put("write", CommandDetail.Flag.WRITE);
        }
    };

    private CommandDetailParser() {
    }

    /**
     * Parse the output of the redis COMMAND/COMMAND INFO command and convert to a list of {@link CommandDetail}.
     * 
     * @param commandOutput the command output, must not be {@literal null}
     * @return RedisInstance
     */
    public static List<CommandDetail> parse(List<?> commandOutput) {
        checkArgument(commandOutput != null, "CommandOutput must not be null");

        List<CommandDetail> result = Lists.newArrayList();

        for (Object o : commandOutput) {
            if (!(o instanceof Collection<?>)) {
                continue;
            }

            Collection<?> collection = (Collection<?>) o;
            if (collection.size() != COMMAND_INFO_SIZE) {
                continue;
            }

            CommandDetail commandDetail = parseCommandDetail(collection);
            result.add(commandDetail);
        }

        return Collections.unmodifiableList(result);
    }

    private static CommandDetail parseCommandDetail(Collection<?> collection) {
        Iterator<?> iterator = collection.iterator();
        String name = (String) iterator.next();
        int arity = Ints.checkedCast(getLongFromIterator(iterator, 0));
        Object flags = iterator.next();
        int firstKey = Ints.checkedCast(getLongFromIterator(iterator, 0));
        int lastKey = Ints.checkedCast(getLongFromIterator(iterator, 0));
        int keyStepCount = Ints.checkedCast(getLongFromIterator(iterator, 0));

        Set<CommandDetail.Flag> parsedFlags = parseFlags(flags);

        return new CommandDetail(name, arity, parsedFlags, firstKey, lastKey, keyStepCount);
    }

    private static Set<CommandDetail.Flag> parseFlags(Object flags) {
        Set<CommandDetail.Flag> result = Sets.newHashSet();

        if (flags instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) flags;
            for (Object o : collection) {
                CommandDetail.Flag flag = FLAG_MAPPING.get(o);
                if (flag != null) {
                    result.add(flag);
                }
            }
        }

        return Collections.unmodifiableSet(result);
    }

    private static long getLongFromIterator(Iterator<?> iterator, long defaultValue) {
        if (iterator.hasNext()) {
            Object object = iterator.next();
            if (object instanceof String) {
                return Long.parseLong((String) object);
            }

            if (object instanceof Number) {
                return ((Number) object).longValue();
            }
        }
        return defaultValue;
    }

}
