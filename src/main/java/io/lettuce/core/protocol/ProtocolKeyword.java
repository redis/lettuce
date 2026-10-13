package io.lettuce.core.protocol;

/**
 * Interface for protocol keywords providing an encoded representation.
 *
 * @author Mark Paluch
 * @author shariorfarhan07 (Sharior Hossain Farhan)
 */
public interface ProtocolKeyword {

    /**
     *
     * @return byte[] encoded representation.
     */
    byte[] getBytes();

    /**
     * Return the name of the command. The name identifies the command type in command latency metrics, so implementations must
     * return the same value for every instance representing the same command, typically the command name such as {@code GET}.
     * Relying on {@link Object#toString()} yields a distinct name per instance, which makes latency recorders retain a separate
     * metric for every command instance.
     *
     * @return name of the command.
     */
    String toString();

    /**
     * Return the keyword as a String. This method is retained for binary compatibility with existing integrations.
     *
     * @deprecated since 6.5, use {@link #toString()} instead.
     */
    @Deprecated
    default String name() {
        return this.toString();
    }

}
