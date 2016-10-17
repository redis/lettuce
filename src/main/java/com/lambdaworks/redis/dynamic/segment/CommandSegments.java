package com.lambdaworks.redis.dynamic.segment;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.ProtocolKeyword;

/**
 * Value object abstracting multiple {@link CommandSegment}s.
 * 
 * @author Mark Paluch
 * @since 5.0
 */
public class CommandSegments implements Iterable<CommandSegment> {

    private final StringCommandType commandType;
    private final List<CommandSegment> segments;

    /**
     * Create {@link CommandSegments} given a {@link List} of {@link CommandSegment}s.
     * 
     * @param segments must not be {@literal null.}
     */
    public CommandSegments(List<CommandSegment> segments) {

        LettuceAssert.isTrue(!segments.isEmpty(), "Command segments must not be empty");

        this.segments = segments.size() > 1 ? Collections.unmodifiableList(segments.subList(1, segments.size()))
                : Collections.emptyList();
        this.commandType = new StringCommandType(segments.get(0).asString());
    }

    @Override
    public Iterator<CommandSegment> iterator() {
        return segments.iterator();
    }

    public ProtocolKeyword getCommandType() {
        return commandType;
    }

    public int size() {
        return segments.size();
    }

    private static class StringCommandType implements ProtocolKeyword {

        private final byte[] commandTypeBytes;
        private final String commandType;

        public StringCommandType(String commandType) {
            this.commandType = commandType;
            this.commandTypeBytes = commandType.getBytes();
        }

        @Override
        public byte[] getBytes() {
            return commandTypeBytes;
        }

        @Override
        public String name() {
            return commandType;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getCommandType().name());

        for (CommandSegment segment : segments) {
            sb.append(' ').append(segment);
        }

        return sb.toString();
    }
}
