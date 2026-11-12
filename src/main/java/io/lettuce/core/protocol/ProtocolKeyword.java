package io.lettuce.core.protocol;

/**
 * Interface for protocol keywords providing an encoded representation.
 *
 * @author Mark Paluch
 * @author Sharior Hossain Farhan
 */
public interface ProtocolKeyword {

    /**
     *
     * @return byte[] encoded representation.
     */
    byte[] getBytes();

    /**
     * Return the command name, such as {@code GET}. Metrics and tracing use this name, so it must be the same for every
     * instance of the same command.
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
