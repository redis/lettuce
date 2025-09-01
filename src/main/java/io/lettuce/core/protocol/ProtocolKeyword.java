package io.lettuce.core.protocol;

/**
 * Interface for protocol keywords providing an encoded representation.
 *
 * @author Mark Paluch
 */
public interface ProtocolKeyword {

    /**
     *
     * @return byte[] encoded representation.
     */
    byte[] getBytes();

    /**
     *
     * @return name of the command.
     */
    String toString();

    /**
     * Return the keyword as a String. This method is retained for
     * binary compatibility with existing integrations.
     *
     * @deprecated since 6.5, use {@link #toString()} instead.
     */
    @Deprecated
    default String name() {
        return this.toString();
    }
}
